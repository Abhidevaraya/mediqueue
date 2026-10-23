package com.mediqueue.medicaldocument;

import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.medicaldocument.dto.MedicalDocumentResponse;
import com.mediqueue.medicaldocument.entity.MedicalDocument;
import com.mediqueue.medicaldocument.enums.DocumentCategory;
import com.mediqueue.medicaldocument.repository.MedicalDocumentRepository;
import com.mediqueue.medicaldocument.service.MedicalDocumentService;
import com.mediqueue.medicaldocument.storage.FileStorageService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalDocumentServiceTest {

    @Mock
    private MedicalDocumentRepository documentRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ConsultationRecordRepository consultationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClinicalAuditService auditService;

    @InjectMocks
    private MedicalDocumentService documentService;

    private User patientUser;
    private Patient patient;
    private AuthenticatedUser patientAuth;
    private AuthenticatedUser anotherPatientAuth;

    @BeforeEach
    void setUp() {
        patientUser = User.builder().id(10L).fullName("Patient One").email("p1@example.com").role(UserRole.PATIENT).accountStatus(AccountStatus.ACTIVE).build();
        patient = Patient.builder().id(100L).user(patientUser).build();
        patientAuth = new AuthenticatedUser(patientUser);

        User anotherUser = User.builder().id(20L).fullName("Patient Two").email("p2@example.com").role(UserRole.PATIENT).accountStatus(AccountStatus.ACTIVE).build();
        anotherPatientAuth = new AuthenticatedUser(anotherUser);
    }

    @Test
    @DisplayName("Upload document succeeds for patient uploading own document")
    void uploadDocument_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blood_report.pdf", "application/pdf", "Dummy PDF content".getBytes()
        );

        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(10L)).thenReturn(Optional.of(patientUser));
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> {
            MedicalDocument doc = invocation.getArgument(0);
            doc.setId(500L);
            return doc;
        });

        MedicalDocumentResponse response = documentService.uploadDocument(
                patientAuth, 100L, null, null, DocumentCategory.LAB_REPORT, file
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getDocumentCategory()).isEqualTo(DocumentCategory.LAB_REPORT);
        assertThat(response.getOriginalFileName()).isEqualTo("blood_report.pdf");

        verify(fileStorageService).store(any(InputStream.class), anyString(), eq("application/pdf"), anyLong());
        verify(documentRepository).save(any(MedicalDocument.class));
        verify(auditService).recordEvent(eq(10L), eq("PATIENT"), eq("DOCUMENT_UPLOADED"), eq("MEDICAL_DOCUMENT"), eq(500L), eq(100L), eq("SUCCESS"), anyString());
    }

    @Test
    @DisplayName("Upload rejects empty file")
    void uploadDocument_emptyFile_throwsBusinessException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        assertThatThrownBy(() -> documentService.uploadDocument(
                patientAuth, 100L, null, null, DocumentCategory.LAB_REPORT, emptyFile
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot upload an empty file");

        verifyNoInteractions(fileStorageService, documentRepository);
    }

    @Test
    @DisplayName("Upload rejects unsupported MIME type")
    void uploadDocument_unsupportedType_throwsBusinessException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "script.exe", "application/x-msdownload", "MALICIOUS".getBytes()
        );

        assertThatThrownBy(() -> documentService.uploadDocument(
                patientAuth, 100L, null, null, DocumentCategory.OTHER, badFile
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported file type");

        verifyNoInteractions(fileStorageService, documentRepository);
    }

    @Test
    @DisplayName("Upload cleans up stored file if metadata database persistence fails")
    void uploadDocument_databaseError_cleansUpStoredFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "PNG Content".getBytes()
        );

        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(10L)).thenReturn(Optional.of(patientUser));
        when(documentRepository.save(any(MedicalDocument.class))).thenThrow(new RuntimeException("DB Connection down"));

        assertThatThrownBy(() -> documentService.uploadDocument(
                patientAuth, 100L, null, null, DocumentCategory.OTHER, file
        )).isInstanceOf(RuntimeException.class);

        // Verifies cleanup of stored file on DB failure
        verify(fileStorageService).delete(anyString());
    }

    @Test
    @DisplayName("Download document succeeds for owner patient")
    void downloadDocument_success() {
        MedicalDocument document = MedicalDocument.builder()
                .id(500L)
                .documentReference("DOC-20261012-ABC123")
                .patient(patient)
                .uploadedByUser(patientUser)
                .originalFileName("scan.pdf")
                .contentType("application/pdf")
                .storageKey("storage-key-uuid.pdf")
                .isDeleted(false)
                .build();

        when(documentRepository.findById(500L)).thenReturn(Optional.of(document));
        Resource mockResource = new ByteArrayResource("PDF DATA".getBytes());
        when(fileStorageService.loadAsResource("storage-key-uuid.pdf")).thenReturn(mockResource);

        var payload = documentService.downloadDocument(patientAuth, 500L);

        assertThat(payload).isNotNull();
        assertThat(payload.filename()).isEqualTo("scan.pdf");
        assertThat(payload.contentType()).isEqualTo("application/pdf");
        assertThat(payload.resource()).isEqualTo(mockResource);

        verify(auditService).recordEvent(eq(10L), eq("PATIENT"), eq("DOCUMENT_DOWNLOADED"), eq("MEDICAL_DOCUMENT"), eq(500L), eq(100L), eq("SUCCESS"), anyString());
    }

    @Test
    @DisplayName("Download document denied for another patient")
    void downloadDocument_accessDeniedForOtherPatient() {
        MedicalDocument document = MedicalDocument.builder()
                .id(500L)
                .patient(patient)
                .uploadedByUser(patientUser)
                .storageKey("key.pdf")
                .isDeleted(false)
                .build();

        when(documentRepository.findById(500L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.downloadDocument(anotherPatientAuth, 500L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission");

        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Delete document marks record as deleted and removes stored file")
    void deleteDocument_success() {
        MedicalDocument document = MedicalDocument.builder()
                .id(500L)
                .documentReference("DOC-20261012-ABC123")
                .patient(patient)
                .uploadedByUser(patientUser)
                .storageKey("key.pdf")
                .isDeleted(false)
                .build();

        when(documentRepository.findById(500L)).thenReturn(Optional.of(document));

        documentService.deleteDocument(patientAuth, 500L);

        assertThat(document.getIsDeleted()).isTrue();
        assertThat(document.getDeletedAt()).isNotNull();
        verify(documentRepository).save(document);
        verify(fileStorageService).delete("key.pdf");
        verify(auditService).recordEvent(eq(10L), eq("PATIENT"), eq("DOCUMENT_DELETED"), eq("MEDICAL_DOCUMENT"), eq(500L), eq(100L), eq("SUCCESS"), anyString());
    }
}
