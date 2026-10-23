package com.mediqueue.audit.controller;

import com.mediqueue.analytics.service.CsvExportService;
import com.mediqueue.audit.entity.ClinicalAuditEvent;
import com.mediqueue.audit.repository.ClinicalAuditEventRepository;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.security.JwtAuthenticationFilter;
import com.mediqueue.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClinicalAuditEventRepository auditEventRepository;

    @MockBean
    private ClinicalAuditService auditService;

    @MockBean
    private CsvExportService csvExportService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("Admin can retrieve paginated audit logs")
    void getAuditLogs_success() throws Exception {
        ClinicalAuditEvent event = ClinicalAuditEvent.builder()
                .id(1L)
                .actorUserId(10L)
                .actorRole("ADMIN")
                .action("APPOINTMENT_CANCELLED")
                .resourceType("APPOINTMENT")
                .resourceId(100L)
                .outcome("SUCCESS")
                .eventTimestamp(Instant.now())
                .safeReason("Administrative override")
                .build();

        when(auditEventRepository.findFilteredEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("action", "APPOINTMENT_CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].action").value("APPOINTMENT_CANCELLED"))
                .andExpect(jsonPath("$.data.content[0].actorRole").value("ADMIN"));
    }

    @Test
    @DisplayName("Admin can export audit logs as CSV")
    void exportAuditLogs_success() throws Exception {
        ClinicalAuditEvent event = ClinicalAuditEvent.builder()
                .id(1L)
                .actorUserId(10L)
                .actorRole("ADMIN")
                .action("USER_ROLE_CHANGED")
                .resourceType("USER")
                .resourceId(25L)
                .outcome("SUCCESS")
                .eventTimestamp(Instant.now())
                .safeReason("Role changed to DOCTOR")
                .build();

        when(auditEventRepository.findFilteredEventsList(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(event));

        byte[] csvBytes = "ID,Timestamp,ActorId,Role,Action,ResourceType,ResourceId,Outcome,Metadata\n1,2026-10-12T00:00:00Z,10,ADMIN,USER_ROLE_CHANGED,USER,25,SUCCESS,Role changed to DOCTOR\n".getBytes(StandardCharsets.UTF_8);
        when(csvExportService.exportAuditLogs(any())).thenReturn(csvBytes);

        mockMvc.perform(get("/api/v1/admin/audit-logs/export")
                        .param("action", "USER_ROLE_CHANGED"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"audit_logs.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("USER_ROLE_CHANGED")));
    }
}
