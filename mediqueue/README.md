# MediQueue — Smart Hospital Appointment & Real-Time Queue Management Platform

MediQueue is organized into two independent, cleanly separated projects:

- **`backend/`**: Java 21, Spring Boot 3.3.5, Spring Security, JWT, Flyway, MySQL, WebSocket/STOMP. Fully configured for **Spring Tool Suite (STS)**, Eclipse, or CLI Maven development.
- **`frontend/`**: React 18, TypeScript, Vite, Tailwind CSS (Lavender + White + Indigo healthcare UI). Developed using Antigravity / VS Code.

---

## Quick Navigation

- [Backend STS & Maven Guide (`backend/README.md`)](file:///backend/README.md)
- [Frontend React & Vite Guide (`frontend/README.md`)](file:///frontend/README.md)

---

## Directory Structure

```
MediQueue/
├── backend/                  # Java 21 / Spring Boot 3 Maven Project (STS-Compatible)
│   ├── src/
│   │   ├── main/             # Spring Boot source code & Flyway migrations
│   │   └── test/             # JUnit 5 integration & unit test suite
│   ├── pom.xml               # Maven configuration
│   ├── mvnw / mvnw.cmd       # Maven Wrapper scripts
│   ├── .mvn/                 # Maven Wrapper binaries
│   ├── .env.example          # Backend environment variable template
│   ├── .gitignore            # Backend-specific ignore rules
│   └── README.md             # Complete STS Import & Execution Guide
│
├── frontend/                 # React 18 / TypeScript / Vite Frontend
│   ├── src/                  # React components, features, hooks, API services
│   ├── public/               # Favicon & static public assets
│   ├── package.json          # Node dependencies & npm scripts
│   ├── vite.config.ts        # Vite build configuration
│   ├── tsconfig.json         # TypeScript strict configuration
│   ├── .env.example          # Frontend environment variable template
│   ├── .gitignore            # Frontend-specific ignore rules
│   └── README.md             # Frontend Architecture & Portals Guide
│
├── compose.yaml              # Docker Compose local orchestrator (MySQL + Backend)
├── .env.example              # Root environment template
├── .gitignore                # Root repository ignore rules
└── README.md                 # Project Overview & Architecture Guide
```

---

## 1. Phase 1 Scope & Features

- **Domain Models & Entities:**
  - `User`: Common identity entity supporting roles (`PATIENT`, `DOCTOR`, `ADMIN`), account statuses (`ACTIVE`, `BLOCKED`, `PENDING`), optimistic locking (`@Version`), and BCrypt password encryption.
  - `Patient`: 1-to-1 profile linked to `User` for personal demographic details.
  - `Doctor`: 1-to-1 profile linked to `User`, associated with a hospital `Department`, professional credentials, and verification status (`PENDING`, `VERIFIED`, `REJECTED`).
  - `Department`: Medical department entity with unique normalized names and active status flags.
  - `RevokedToken`: Cryptographic SHA-256 token revocation store ensuring immediate token invalidation on logout.
- **Authentication & Security:**
  - Stateless JWT authentication using JJWT (HMAC-SHA256).
  - Patient self-registration (`POST /api/v1/auth/register`) strictly assigning `PATIENT` role.
  - Credential authentication (`POST /api/v1/auth/login`) with generic error messages protecting email enumeration.
  - Token revocation logout (`POST /api/v1/auth/logout`) persisting token signatures to prevent reuse.
  - Immediate enforcement of account block status on active JWT sessions.
  - Secure first-administrator bootstrap mechanism (`AdminBootstrapRunner`).
- **Patient Profile Management:**
  - Self-service profile retrieval (`GET /api/v1/patients/me`) and updates (`PUT /api/v1/patients/me`).
  - Strict ownership enforcement—patients can only access and update their own profiles.
  - Sensitive account fields (role, email, password hash, status) cannot be changed via profile endpoints.
- **Public Directories (Read-Only):**
  - Department directory (`GET /api/v1/departments`, `GET /api/v1/departments/{id}`) returning active departments.
  - Doctor directory (`GET /api/v1/doctors`, `GET /api/v1/doctors/{id}`) returning active, verified doctors with pagination and sort validation.
- **Administration & Moderation (ADMIN Only):**
  - User moderation (`GET /api/v1/admin/users`, `PATCH /api/v1/admin/users/{id}/status`) with admin self-lockout protection.
  - Department management (`POST /api/v1/admin/departments`, `PUT /api/v1/admin/departments/{id}`, `PATCH /api/v1/admin/departments/{id}/status`).
  - Doctor onboarding and verification (`POST /api/v1/admin/doctors`, `PUT /api/v1/admin/doctors/{id}`, `PATCH /api/v1/admin/doctors/{id}/verification`, `PATCH /api/v1/admin/doctors/{id}/status`).
- **Standardized API Responses & Global Exception Handling:**
  - Unified success and error envelope contracts.
  - Custom exceptions (`ResourceNotFoundException`, `ConflictException`, `BusinessException`, `UnauthorizedException`).
- **Database Migrations:**
  - Authoritative Flyway migrations (V1 to V5) targeting MySQL 8.x.

---

## 2. Technology Stack & Prerequisites

- **Language:** Java 21 (Temurin JDK 21 / OpenJDK 21 LTS)
- **Framework:** Spring Boot 3.3.5
- **Build Tool:** Maven (Maven Wrapper `mvnw.cmd` / `mvnw` included)
- **Database:** MySQL 8.x
- **ORM / Persistence:** Spring Data JPA, Hibernate 6.5.x
- **Database Migrations:** Flyway 10.x (`flyway-mysql`)
- **Security:** Spring Security 6.x, JJWT 0.12.6, BCrypt
- **API Documentation:** Springdoc OpenAPI / Swagger UI 2.6.0
- **Testing:** JUnit 5, Mockito, Spring Boot Test, MockMvc

---

## 3. Database Setup (MySQL 8.x)

Log in to your local MySQL 8.0 server and execute:

```sql
CREATE DATABASE IF NOT EXISTS mediqueue_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mediqueue_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 4. Environment Variables Configuration

Copy `.env.example` to your deployment environment or set the following variables:

| Variable | Description | Default (Local Dev) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active profile (`dev`, `prod`, `test`) | `dev` |
| `SERVER_PORT` | HTTP server port | `8080` |
| `DB_URL` | MySQL JDBC Connection URL | `jdbc:mysql://localhost:3306/mediqueue_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL database password | `root` |
| `JWT_SECRET` | Minimum 256-bit HMAC-SHA256 secret key | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Token validity duration in milliseconds | `86400000` (24 hours) |
| `FRONTEND_URL` | Allowed CORS origins (comma-separated) | `http://localhost:3000,http://localhost:5173` |
| `ADMIN_BOOTSTRAP_EMAIL` | Optional initial admin email | `admin@mediqueue.com` |
| `ADMIN_BOOTSTRAP_PASSWORD` | Optional initial admin password | `AdminSecure@2026!` |
| `ADMIN_BOOTSTRAP_NAME` | Initial admin full name | `System Administrator` |

---

## 5. How to Build, Test, and Run

### Running Tests (PowerShell)

```powershell
.\mvnw.cmd clean test
```

### Building the Production JAR

```powershell
.\mvnw.cmd clean package
```

### Running the Application

```powershell
# Set optional admin bootstrap credentials if provisioning the first admin
$env:ADMIN_BOOTSTRAP_EMAIL="admin@mediqueue.com"
$env:ADMIN_BOOTSTRAP_PASSWORD="AdminSecure@2026!"
$env:SPRING_PROFILES_ACTIVE="dev"

java -jar target/mediqueue-backend-0.0.1-SNAPSHOT.jar
```

Or using Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

---

## 6. API Documentation (Swagger UI)

- **Swagger UI Interactive Interface:**  
  [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI 3.0 JSON Specification:**  
  [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> In production (`application-prod.yml`), Swagger UI and OpenAPI documentation endpoints are disabled by default for security.

---

## 7. Implemented API Endpoints Summary

### Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/register` — Public patient registration.
- `POST /api/v1/auth/login` — Authenticate and receive JWT access token.
- `POST /api/v1/auth/logout` — Revoke active JWT access token.
- `GET /api/v1/auth/me` — Get current authenticated principal details.

### Patient Profile (`/api/v1/patients`)
- `GET /api/v1/patients/me` — Retrieve authenticated patient profile (`ROLE_PATIENT`).
- `PUT /api/v1/patients/me` — Update authenticated patient demographic info (`ROLE_PATIENT`).

### Departments (`/api/v1/departments` & `/api/v1/admin/departments`)
- `GET /api/v1/departments` — List active departments (Public).
- `GET /api/v1/departments/{id}` — Get active department by ID (Public).
- `GET /api/v1/admin/departments` — List all departments (`ROLE_ADMIN`).
- `GET /api/v1/admin/departments/{id}` — Get any department by ID (`ROLE_ADMIN`).
- `POST /api/v1/admin/departments` — Create department (`ROLE_ADMIN`).
- `PUT /api/v1/admin/departments/{id}` — Update department details (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/departments/{id}/status` — Toggle active status (`ROLE_ADMIN`).

### Doctors (`/api/v1/doctors` & `/api/v1/admin/doctors`)
- `GET /api/v1/doctors` — Search verified, active doctors with pagination & filters (Public).
- `GET /api/v1/doctors/{id}` — View verified doctor profile (Public).
- `GET /api/v1/admin/doctors` — List all doctors with admin filters (`ROLE_ADMIN`).
- `GET /api/v1/admin/doctors/{id}` — View complete doctor admin record (`ROLE_ADMIN`).
- `POST /api/v1/admin/doctors` — Onboard new doctor (`ROLE_ADMIN`).
- `PUT /api/v1/admin/doctors/{id}` — Update doctor professional details (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/doctors/{id}/verification` — Update verification status (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/doctors/{id}/status` — Toggle doctor account status (`ROLE_ADMIN`).

### Administration (`/api/v1/admin/users`)
- `GET /api/v1/admin/users` — List registered users with role and status filters (`ROLE_ADMIN`).
- `GET /api/v1/admin/users/{id}` — Retrieve specific user account details (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/users/{id}/status` — Block or activate user account (`ROLE_ADMIN`).

---

## 8. API Response Envelope

### Standard Success Response
```json
{
  "success": true,
  "message": "Patient profile retrieved successfully",
  "data": {
    "id": 1,
    "userId": 2,
    "fullName": "Alice Smith",
    "email": "alice.smith@mediqueue.com"
  },
  "timestamp": "2026-10-12T15:30:28.666051400Z"
}
```

### Standard Error Response
```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "details": [
    "email: Please provide a valid email address",
    "password: Password must be between 8 and 100 characters"
  ],
  "path": "/api/v1/auth/register",
  "timestamp": "2026-10-12T15:30:28.666051400Z"
}
```

---

## 9. Security Architecture & Policies

1. **Password Encoding:** BCrypt with standard 10 rounds cost factor. Raw passwords are never stored or logged.
2. **Stateless JWT with Immediate Revocation:**
   - On logout, a cryptographic SHA-256 hash of the token is persisted into the `revoked_tokens` table.
   - `JwtAuthenticationFilter` validates each request against both token expiration and the revocation store.
   - User account status is loaded from the database on every authenticated request, guaranteeing that blocking a compromised user takes immediate effect.
3. **Admin Self-Lockout Prevention:**
   - `UserService` prevents administrators from blocking their own accounts via the API.
4. **Bootstrapping the First Administrator:**
   - Environment variables `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` trigger `AdminBootstrapRunner` on application startup.
   - If an admin account already exists in the database, the bootstrap runner safely skips execution.

---

## 10. Phase 2: Doctor Scheduling + Appointment Management + Slot Booking + Concurrency Control

Phase 2 introduces a database-backed scheduling engine, appointment lifecycle management, slot inventory coordination, and concurrency-safe booking.

### 10.1 Phase 2 Scope & Features

- **Doctor Schedules:**
  - `DoctorSchedule` entity stores weekly recurring working hours, slot duration (minutes), slot capacity, break intervals, active status, and optimistic locking version.
  - Multi-interval support: doctors can define separate working shifts on the same weekday without artificial constraints.
  - Strict validation: start time before end time, break times enclosed within working hours, positive duration fitting neatly in intervals, and duplicate/overlapping schedule rejection.
  - Doctor ownership security: doctors can only manage their own schedules; administrators can manage any doctor's schedule; patients are denied.

- **Schedule Exceptions:**
  - `ScheduleException` entity supports full-day leave, hospital/doctor holidays, partial-day unavailability, and exceptional working hours.
  - Precedence rules: full-day leave or holiday removes all availability; partial unavailability carves out overlapping slots; exceptional hours override default weekly hours.

- **Availability & Slot Generation Engine:**
  - `AvailabilityService` generates bookable slots dynamically from schedules and exceptions.
  - Operates using the configured hospital timezone (`mediqueue.hospital.timezone: Asia/Kolkata`).
  - Slots accurately calculate `totalCapacity`, `bookedCount`, `remainingCapacity`, and `available` boolean.
  - Past slots and dates are automatically excluded.

- **Appointment Lifecycle & Reference Generation:**
  - `Appointment` entity with states: `BOOKED`, `CHECKED_IN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`.
  - Non-guessable unique reference: `APT-YYYYMMDD-XXXXXX` generated using cryptographic secure random strings and validated for uniqueness.
  - State machine validation: only valid transitions are permitted; patients cannot arbitrarily transition appointments to `COMPLETED` or `IN_PROGRESS`.

- **Concurrency-Safe Slot Booking & Counter Coordination:**
  - Dedicated `SlotInventory` entity coordinated via MySQL `INSERT IGNORE` and pessimistic write locks (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`).
  - Guarantees zero overbooking even when multiple concurrent threads race to book the same slot simultaneously.
  - Atomic capacity tracking: successful booking increments `bookedCount`; cancellation immediately decrements `bookedCount` and releases the slot.

- **Atomic Rescheduling with Deterministic Lock Ordering:**
  - Rescheduling preserves the original appointment reference and updates slot times transactionally.
  - Acquires pessimistic locks on both old and new slots in deterministic chronological order (`oldStart.isBefore(newStart)`) to eliminate MySQL InnoDB deadlocks.
  - If the target slot is full or conflicts with existing appointments, the transaction rolls back cleanly, leaving the original booking untouched.

- **Phase 3 Queue Integration Readiness:**
  - `AppointmentBookingHook` interface and `DefaultAppointmentBookingHook` implementation provide a decoupled transactional extension point.
  - When Phase 3 introduces real-time queue tokens and live queues, Phase 3 components can register an `@Primary` or `@EventListener` hook without modifying booking core logic.

---

## 11. Database Migrations (V1 to V9)

All database schema evolutions are managed via Flyway migrations targeting MySQL 8.x:

| Version | Migration Script | Description |
|---|---|---|
| `V1` | `V1__create_users.sql` | Users table with roles, statuses, and BCrypt credentials |
| `V2` | `V2__create_departments.sql` | Medical departments directory |
| `V3` | `V3__create_patients.sql` | Patient demographic profiles linked to users |
| `V4` | `V4__create_doctors.sql` | Doctor professional profiles linked to users and departments |
| `V5` | `V5__create_revoked_tokens.sql` | Cryptographic JWT token revocation registry |
| `V6` | `V6__create_doctor_schedules.sql` | Doctor weekly recurring schedules with breaks and capacity |
| `V7` | `V7__create_schedule_exceptions.sql` | Doctor leave, holiday, and exceptional working hour overrides |
| `V8` | `V8__create_slot_inventories.sql` | Persistent slot inventory table with pessimistic locking and unique constraints |
| `V9` | `V9__create_appointments.sql` | Appointment records with unique references, lifecycle statuses, and indexes |

---

## 12. Implemented API Endpoints Summary

### Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/register` — Public patient registration.
- `POST /api/v1/auth/login` — Authenticate and receive JWT access token.
- `POST /api/v1/auth/logout` — Revoke active JWT access token.
- `GET /api/v1/auth/me` — Get current authenticated principal details.

### Patient Profile (`/api/v1/patients`)
- `GET /api/v1/patients/me` — Retrieve authenticated patient profile (`ROLE_PATIENT`).
- `PUT /api/v1/patients/me` — Update authenticated patient demographic info (`ROLE_PATIENT`).

### Doctor Schedules & Availability (`/api/v1/doctors` & `/api/v1/doctor/schedules`)
- `GET /api/v1/doctors/{id}/slots?date=YYYY-MM-DD` — Public endpoint to query generated available slots for a doctor on a specific date.
- `GET /api/v1/doctors/{id}/schedules` — Public endpoint to view published weekly schedules of a doctor.
- `POST /api/v1/doctor/schedules` — Create a weekly schedule shift (`ROLE_DOCTOR` or `ROLE_ADMIN`).
- `PUT /api/v1/doctor/schedules/{id}` — Update an existing schedule shift (`Owner DOCTOR` or `ROLE_ADMIN`).
- `POST /api/v1/doctor/schedule-exceptions` — Register leave, holiday, or exceptional hours (`ROLE_DOCTOR` or `ROLE_ADMIN`).
- `GET /api/v1/doctor/schedule-exceptions` — List doctor's schedule exceptions (`ROLE_DOCTOR` or `ROLE_ADMIN`).

### Appointments (`/api/v1/appointments` & `/api/v1/doctor/appointments` & `/api/v1/admin/appointments`)
- `POST /api/v1/appointments` — Concurrency-safe appointment booking (`ROLE_PATIENT`).
- `GET /api/v1/appointments/my` — Paged appointment history for the authenticated patient (`ROLE_PATIENT`).
- `GET /api/v1/appointments/{id}` — Retrieve appointment details (Patient owner, assigned Doctor, or Admin).
- `PATCH /api/v1/appointments/{id}/cancel` — Cancel appointment and atomically release capacity (Patient owner, Doctor, or Admin).
- `PATCH /api/v1/appointments/{id}/reschedule` — Reschedule appointment to a new slot (Patient owner or Admin).
- `GET /api/v1/doctor/appointments/today` — Doctor's schedule of appointments for today in hospital timezone (`ROLE_DOCTOR` or `ROLE_ADMIN`).
- `GET /api/v1/admin/appointments` — Administrative appointment search with filters for doctor, patient, department, status, and date range (`ROLE_ADMIN`).

### Queue Management & Operations (`/api/v1/queue` & `/api/v1/doctor/queue`)
- `POST /api/v1/queue/check-in` — Idempotent check-in generating a unique stable token (`ROLE_PATIENT`, `ROLE_DOCTOR`, `ROLE_ADMIN`).
- `GET /api/v1/queue/my-status` — Retrieve authenticated patient's active queue entries (`ROLE_PATIENT`).
- `GET /api/v1/queue/my-status/wait-estimate` — Real-time smart wait-time prediction for patient's active entry (`ROLE_PATIENT`).
- `GET /api/v1/doctors/{id}/queue/today` — Public/authenticated today's queue view (patients receive sanitized tokens; doctors/admins receive full queue records).
- `POST /api/v1/queue/next` — Atomic call-next operation serialized with pessimistic row lock (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/queue/{id}/call` — Call specific queue entry (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/queue/{id}/recall` — Recall a called token with notification reminder (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/queue/{id}/skip` — Skip absent patient with audit reason (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/queue/{id}/requeue` — Requeue skipped patient at the tail of the waiting queue (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/queue/{id}/start` — Start consultation and transition appointment to `IN_PROGRESS` (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/queue/{id}/complete` — Complete consultation and transition appointment to `COMPLETED` (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/doctor/queue/pause` — Persistently pause doctor queue with reason (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `POST /api/v1/doctor/queue/resume` — Resume paused doctor queue (`ROLE_DOCTOR`, `ROLE_ADMIN`).
- `GET /api/v1/queue/{id}/events` — Audit event timeline for a queue entry (Patient owner, assigned Doctor, or Admin).
- `GET /api/v1/queue/{id}/wait-estimate` — Wait-time estimate for a specific queue entry.

### In-App Notifications (`/api/v1/notifications`)
- `GET /api/v1/notifications` — Paged notifications for the authenticated user with unread count.
- `PATCH /api/v1/notifications/{id}/read` — Mark notification as read (User owner).
- `PATCH /api/v1/notifications/read-all` — Mark all user notifications as read.

### Departments (`/api/v1/departments` & `/api/v1/admin/departments`)
- `GET /api/v1/departments` — List active departments (Public).
- `GET /api/v1/departments/{id}` — Get active department by ID (Public).
- `GET /api/v1/admin/departments` — List all departments (`ROLE_ADMIN`).
- `GET /api/v1/admin/departments/{id}` — Get any department by ID (`ROLE_ADMIN`).
- `POST /api/v1/admin/departments` — Create department (`ROLE_ADMIN`).
- `PUT /api/v1/admin/departments/{id}` — Update department details (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/departments/{id}/status` — Toggle active status (`ROLE_ADMIN`).

### Doctors (`/api/v1/doctors` & `/api/v1/admin/doctors`)
- `GET /api/v1/doctors` — Search verified, active doctors with pagination & filters (Public).
- `GET /api/v1/doctors/{id}` — View verified doctor profile (Public).
- `GET /api/v1/admin/doctors` — List all doctors with admin filters (`ROLE_ADMIN`).
- `GET /api/v1/admin/doctors/{id}` — View complete doctor admin record (`ROLE_ADMIN`).
- `POST /api/v1/admin/doctors` — Onboard new doctor (`ROLE_ADMIN`).
- `PUT /api/v1/admin/doctors/{id}` — Update doctor professional details (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/doctors/{id}/verification` — Update verification status (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/doctors/{id}/status` — Toggle doctor account status (`ROLE_ADMIN`).

### Administration (`/api/v1/admin/users`)
- `GET /api/v1/admin/users` — List registered users with role and status filters (`ROLE_ADMIN`).
- `GET /api/v1/admin/users/{id}` — Retrieve specific user account details (`ROLE_ADMIN`).
- `PATCH /api/v1/admin/users/{id}/status` — Block or activate user account (`ROLE_ADMIN`).

---

## 13. Concurrency Strategy & Capacity Safety

When patients compete simultaneously for slots:
1. **Dynamic Inventory Coordination:** An atomic MySQL `INSERT IGNORE` ensures the `slot_inventories` row exists without failing concurrent transactions.
2. **Pessimistic Write Locking:** Competitors acquire a pessimistic write lock (`SELECT ... FOR UPDATE`) on the target `SlotInventory` row.
3. **Database-Backed Capacity Check:** Transactions inspect the actual locked `bookedCount` vs `totalCapacity`.
4. **Zero Double-Booking Guarantee:** Only requests with free capacity increment `bookedCount` and persist the appointment; excess requests receive a controlled `409 ConflictException`.
5. **Deterministic Lock Ordering:** Multi-slot operations (rescheduling) acquire locks ordered chronologically by `slotStartTime`, eliminating MySQL InnoDB deadlocks.
6. **Multi-Threaded MySQL Verification:** Verified using real multi-threaded tests (`Phase2DatabaseAndConcurrencyTest`) with 8 concurrent threads competing for 2 slots against MySQL 8.0, confirming exactly 2 succeed and 6 are rejected with zero database corruption or overbooking.

---

## 14. Phase 3: Real-Time Queue Management, WebSocket & Prediction

### 14.1 Queue Lifecycle & State Transitions

The queue lifecycle enforces explicit state transitions validated by `QueueTransitionValidator`:

```text
[CHECK-IN] ──> WAITING ──> CALLED ──> IN_PROGRESS ──> COMPLETED
                  │           │
                  │           └──> WAITING (on recall/timeout)
                  │           └──> SKIPPED ──> WAITING (requeue)
                  │           └──> CANCELLED
                  │
                  └──> SKIPPED ──> WAITING (requeue)
                  └──> CANCELLED
```

- **Idempotent Check-In:** If a patient calls check-in multiple times, the existing `QueueEntry` is returned without issuing duplicate tokens.
- **Uniqueness Constraints:**
  - `uk_qe_appointment UNIQUE (appointment_id)` — An appointment can only have one queue entry.
  - `uk_qe_doc_date_token UNIQUE (doctor_id, queue_date, token_number)` — Token numbers are strictly unique per doctor and date.
- **Token Allocation Scope:** Tokens are allocated sequentially per doctor and hospital queue date (`doctorId + queueDate`). Row-level pessimistic locking (`SELECT ... FOR UPDATE`) on `doctor_queue_sessions` guarantees sequential, conflict-free token issuance.

### 14.2 Concurrency Architecture: Token Allocation & Call-Next

- **Two-Phase Session Creation:** To eliminate MySQL InnoDB insert gap/next-key deadlocks under heavy concurrent check-in bursts, `DoctorQueueSessionService.ensureSessionExists` executes in an isolated `REQUIRES_NEW` transaction.
- **Serialized Call-Next:** `QueueService.callNext` acquires a pessimistic row write lock on `DoctorQueueSession`. The session row persists `current_called_token` and `current_in_progress_token`. Competing doctor requests reading the locked row immediately receive `409 ConflictException` if a patient is already called or in progress.
- **Multi-Threaded MySQL Verification:** Validated in `Phase3DatabaseAndConcurrencyTest` where:
  - 8 concurrent patient threads check in simultaneously: Tokens 1 through 8 are allocated sequentially with 0 errors.
  - 4 concurrent doctor threads execute `callNext` simultaneously: Exactly 1 thread successfully calls Token 1; the other 3 threads receive `409 ConflictException`.

### 14.3 Smart Waiting-Time Prediction

Estimates are calculated by `WaitingTimePredictionService` using real operational data:
1. **Eligible Patients Ahead:** Counts active `WAITING` queue entries checked in before the target patient for the doctor/date.
2. **Median Duration Baseline:** Computes the mathematical median of completed consultations for that doctor (bounded to the last 20 consultations). This avoids distortion from statistical outliers.
3. **In-Progress Consultation Deduction:** If a patient is currently `IN_PROGRESS`, the remaining time is calculated as `max(0, medianDuration - elapsedMinutes)`.
4. **Transparent Fallback:** If fewer than 3 completed consultations exist for the doctor, the system falls back to the doctor's configured slot duration or the 15-minute standard baseline (`predictionMethod = "CONFIGURED_SLOT_FALLBACK"`), explicitly flagged with `usesFallback = true`.
5. **Queue Pause Indicator:** If the doctor's queue is paused, the response sets `queuePaused = true` and prefixes a warning notice.

### 14.4 WebSocket / STOMP Real-Time Infrastructure

- **Handshake Endpoint:** `/ws` (with SockJS fallback enabled).
- **Authentication:** STOMP `CONNECT` frames are intercepted by `JwtChannelInterceptor`. The client must supply `Authorization: Bearer <token>` in the native STOMP headers. The interceptor verifies signature, expiration, revocation, and active account status, establishing an authenticated `Principal`.
- **Destination Authorization:**
  - `/topic/doctors/{doctorId}/queue` — Only authorized doctors managing that profile (or admins) may subscribe to full updates.
  - `/user/queue/notifications` — Private user destination; delivery is strictly scoped to the authenticated user's ID.
- **Post-Commit Publication Guarantee:** Real-time messages are broadcast via `QueueEventPublisher` using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. If a database transaction fails or rolls back, no false event is ever broadcast to subscribers.

### 14.5 Database Migrations (V10 & V11)

- `V10__create_queue_tables.sql`:
  - `doctor_queue_sessions` (tracks `last_token_number`, pause state, timestamps)
  - `queue_entries` (queue tickets with tokens, timestamps, references)
  - `queue_events` (immutable audit log of all transitions)
  - `notifications` (in-app notifications store)
  - `prediction_logs` (historical evaluation of predictions vs actuals)
- `V11__add_session_active_tokens.sql`:
  - Adds `current_called_token` and `current_in_progress_token` to `doctor_queue_sessions` for row-lock serializability.

---

## 15. Phase 4: Patient Health Records, Consultation History, Prescriptions, Follow-ups & Secure Documents

Phase 4 extends MediQueue with clinical record keeping, clinician-authored visit summaries, multi-item prescription management, post-consultation follow-ups, and private medical document storage with strict resource-level authorization.

### 15.1 Architecture & Domain Boundary

The clinical domain is strictly separated from operational queue scheduling:
- **Queue State vs Clinical Consultation:** Calling a token or marking a queue entry `IN_PROGRESS` manages patient arrival and waiting-room flow. Clinical consultation start (`POST /api/v1/doctor/consultations/{appointmentId}/start`) and completion (`POST /api/v1/doctor/consultations/{consultationId}/complete`) represent the authoritative medical encounter boundary with server-recorded timestamps.
- **ConsultationRecord:** Serves as the single authoritative anchor for all clinical artifacts (`VisitSummary`, `Prescription`, `FollowUp`, `MedicalDocument`).
- **Phase 3 Prediction Integration:** When calculating waiting times, `WaitingTimePredictionService` queries `ConsultationRecordRepository.findRecentCompletedConsultations` as its primary authoritative source of actual consultation durations. Outliers (duration <= 0 or > 180 min) are filtered out, and fallback to `QueueEntry` or configured slot durations is preserved if insufficient clinical history exists.

### 15.2 Clinical Lifecycle & Immutability Rules

1. **Consultation Lifecycle:**
   - Legal states: `SCHEDULED` → `IN_PROGRESS` → `COMPLETED` (or `CANCELLED`).
   - Starting an already in-progress consultation or completing before starting is rejected with `ConflictException` / `BusinessException`.
   - Completed consultations are final; repeated completions are rejected to prevent duplicate duration samples.
2. **Visit Summary Draft & Approval:**
   - Clinicians author draft visit summaries (`POST /api/v1/medical-records`) containing chief complaint, diagnosis, clinical notes, and treatment plan.
   - Drafts can be edited by the authoring clinician (`PATCH /api/v1/medical-records/{id}`).
   - Approval (`POST /api/v1/medical-records/{id}/approve`) locks the summary (`status = APPROVED`).
   - If revisions are required after approval (`POST /api/v1/medical-records/{id}/revise`), the existing approved version is archived into `visit_summary_revisions` with reason and timestamp, incrementing the version number.
   - **Patient Isolation:** Patients can ONLY view summaries that belong to them AND have `APPROVED` status (`GET /api/v1/medical-records/my`, `GET /api/v1/medical-records/{id}`). Drafts and internal clinical notes of unapproved summaries remain inaccessible to patients.
3. **Prescription Issuance & Revisions:**
   - Prescriptions are issued atomically with multiple line items (`POST /api/v1/prescriptions`). If any item validation fails, the entire transaction rolls back.
   - Issued prescriptions are immutable (`status = ISSUED`). They cannot be silently updated.
   - Corrections require creating a linked revision (`POST /api/v1/prescriptions/{id}/revise`), which marks the original prescription as `REVISED`, sets `parent_prescription_id`, and archives the revision reason.
   - Prescriptions can be formally cancelled by the authoring clinician (`PATCH /api/v1/prescriptions/{id}/cancel`).
   - Patients can view only their own issued prescriptions (`GET /api/v1/prescriptions/my`, `GET /api/v1/prescriptions/{id}`).
4. **Follow-Up Management & In-App Reminders:**
   - Authorized clinicians recommend follow-up dates and clinical instructions (`POST /api/v1/follow-ups`).
   - Recommended dates cannot be in the past.
   - Follow-up creation automatically creates an in-app reminder in `notifications` table (`type = FOLLOW_UP_REMINDER`).
   - Patients can view their scheduled follow-ups (`GET /api/v1/follow-ups/my`).
5. **Secure Medical Document Management:**
   - Uploads (`POST /api/v1/medical-documents`) validate file size (max 10MB) and allowlisted MIME types (`application/pdf`, `image/jpeg`, `image/png`).
   - Stored in a private, unexposed local directory (`storage/documents`) using opaque UUID storage keys (`storageKey`). Original filenames are sanitized for display only.
   - SHA-256 checksums are calculated and stored in metadata for integrity verification.
   - If database metadata persistence fails, the uploaded file is automatically cleaned up.
   - Downloads (`GET /api/v1/medical-documents/{id}/download`) enforce caller authorization, set safe content headers (`Content-Disposition: attachment; filename="..."`), and stream the file without exposing filesystem paths.

### 15.3 Resource-Level Security & Clinical Privacy

- **Patient Isolation:** Authenticated patients can only access clinical records, prescriptions, visit summaries, and documents where `patient.user.id == principal.id`. Guessing other IDs results in `403 AccessDeniedException` or `404 ResourceNotFoundException`.
- **Clinician Responsibility:** A doctor can only start/complete consultations, author summaries, and issue prescriptions for appointments where they are the assigned doctor (`appointment.doctor.id == doctor.id`).
- **Administrative Access Boundaries:** Administrators manage operational workflows, users, and audit logs. They do not have uninhibited read access to draft or private clinical summaries unless acting within an explicitly audited governance policy.
- **Append-Only Clinical Audit:** All high-value clinical mutations (`CONSULTATION_STARTED`, `CONSULTATION_COMPLETED`, `VISIT_SUMMARY_APPROVED`, `PRESCRIPTION_ISSUED`, `DOCUMENT_UPLOADED`, `DOCUMENT_DOWNLOADED`) are logged to `clinical_audit_events` in an independent transaction (`REQUIRES_NEW`). Medical notes, passwords, and file contents are strictly excluded from audit logs and application logs.

### 15.4 Database Migration (V12)

Migration `V12__create_phase4_clinical_tables.sql` establishes 8 MySQL tables:
- `consultation_records` (appointment FK, patient FK, doctor FK, status, timestamps, duration)
- `visit_summaries` (consultation FK, patient FK, doctor FK, status, version, clinical content)
- `visit_summary_revisions` (visit summary FK, revision notes, archived content, version history)
- `prescriptions` (consultation FK, patient FK, doctor FK, status, version, parent prescription FK)
- `prescription_items` (prescription FK, medication name, dosage, frequency, duration days, instructions)
- `follow_ups` (consultation FK, patient FK, doctor FK, recommended date, status, reminder status)
- `medical_documents` (patient FK, uploader FK, opaque storage key, sanitized filename, checksum, size)
- `clinical_audit_events` (actor ID, action, resource type, resource ID, outcome, timestamps)

### 15.5 Phase 4 API Reference

| Method | Endpoint | Allowed Roles | Description & Authorization |
|---|---|---|---|
| `POST` | `/api/v1/doctor/consultations/{appointmentId}/start` | `DOCTOR`, `ADMIN` | Starts clinical consultation; doctor must be assigned to appointment |
| `POST` | `/api/v1/doctor/consultations/{consultationId}/complete` | `DOCTOR`, `ADMIN` | Completes consultation; records actual duration and triggers prediction update |
| `GET` | `/api/v1/doctor/consultations/{consultationId}` | `DOCTOR`, `ADMIN` | Retrieves consultation by ID |
| `GET` | `/api/v1/doctor/consultations` | `DOCTOR`, `ADMIN` | Lists doctor's consultations (paginated, status/date filters) |
| `GET` | `/api/v1/consultations/my` | `PATIENT` | Lists authenticated patient's consultations (paginated) |
| `POST` | `/api/v1/medical-records` | `DOCTOR` | Creates a draft visit summary for an active consultation |
| `PATCH` | `/api/v1/medical-records/{id}` | `DOCTOR` | Updates a draft visit summary (authoring doctor only) |
| `POST` | `/api/v1/medical-records/{id}/approve` | `DOCTOR` | Approves visit summary (locks content, makes visible to patient) |
| `POST` | `/api/v1/medical-records/{id}/revise` | `DOCTOR` | Creates a revised version of an approved summary (archives prior version) |
| `GET` | `/api/v1/medical-records/{id}` | `DOCTOR`, `PATIENT`, `ADMIN` | Retrieves summary (patients can only view approved summaries) |
| `GET` | `/api/v1/medical-records/my` | `PATIENT` | Lists authenticated patient's approved summaries (paginated) |
| `GET` | `/api/v1/doctor/medical-records` | `DOCTOR` | Lists doctor's visit summaries (drafts + approved) |
| `POST` | `/api/v1/prescriptions` | `DOCTOR` | Issues a multi-item prescription atomically |
| `POST` | `/api/v1/prescriptions/{id}/revise` | `DOCTOR` | Issues a revised prescription linked to parent prescription |
| `PATCH` | `/api/v1/prescriptions/{id}/cancel` | `DOCTOR` | Cancels an issued prescription |
| `GET` | `/api/v1/prescriptions/{id}` | `DOCTOR`, `PATIENT`, `ADMIN` | Retrieves prescription details and items |
| `GET` | `/api/v1/prescriptions/my` | `PATIENT` | Lists authenticated patient's prescriptions (paginated) |
| `GET` | `/api/v1/doctor/prescriptions` | `DOCTOR` | Lists doctor's issued prescriptions |
| `POST` | `/api/v1/follow-ups` | `DOCTOR` | Schedules a follow-up recommendation and in-app reminder |
| `PATCH` | `/api/v1/follow-ups/{id}/complete` | `DOCTOR`, `ADMIN` | Marks a follow-up as completed |
| `PATCH` | `/api/v1/follow-ups/{id}/cancel` | `DOCTOR`, `ADMIN` | Cancels a follow-up |
| `GET` | `/api/v1/follow-ups/{id}` | `DOCTOR`, `PATIENT`, `ADMIN` | Retrieves follow-up details |
| `GET` | `/api/v1/follow-ups/my` | `PATIENT` | Lists authenticated patient's follow-ups (paginated) |
| `GET` | `/api/v1/doctor/follow-ups` | `DOCTOR` | Lists doctor's scheduled follow-ups |
| `POST` | `/api/v1/medical-documents` | `PATIENT`, `DOCTOR`, `ADMIN` | Uploads a medical document (multipart/form-data, PDF/JPEG/PNG, max 10MB) |
| `GET` | `/api/v1/medical-documents/{id}` | `PATIENT`, `DOCTOR`, `ADMIN` | Retrieves document metadata |
| `GET` | `/api/v1/medical-documents/{id}/download` | `PATIENT`, `DOCTOR`, `ADMIN` | Securely streams document with Content-Disposition headers |
| `GET` | `/api/v1/medical-documents/my` | `PATIENT` | Lists authenticated patient's documents (paginated) |
| `DELETE` | `/api/v1/medical-documents/{id}` | `PATIENT`, `DOCTOR`, `ADMIN` | Soft-deletes document metadata and removes stored file |

### 15.6 Storage & Configuration Settings

| Environment Variable | Description | Default |
|---|---|---|
| `MEDIQUEUE_DOCUMENT_STORAGE_PATH` | Private directory path for document file storage | `storage/documents` |
| `MEDIQUEUE_DOCUMENT_MAX_SIZE` | Maximum permitted file upload size | `10MB` |
| `MEDIQUEUE_DOCUMENT_ALLOWED_TYPES` | Permitted MIME types | `application/pdf,image/jpeg,image/png` |

---

## 16. Phase 5: Final Integration, Administration, Analytics, Audit, Security Hardening & Deployment Readiness

Phase 5 represents the final backend integration phase of MediQueue. It connects the clinical, appointment, queue, and user operations of Phases 1–4 into an integrated, auditable, hardened platform ready for frontend consumption and containerized deployment.

### 16.1 Administrator Operations Module (`/api/v1/admin/...`)

1. **User Administration:**
   - Search users with pagination and multi-field filters (`q` across email/name/phone, `role`, `status`).
   - Self-demotion and self-lockout protection: An authenticated administrator is strictly prevented from revoking their own `ADMIN` role or blocking their own account.
   - Modifying user roles is logged to `clinical_audit_events` under `USER_ROLE_CHANGED`.
2. **Doctor & Department Administration:**
   - Full administration of doctor profiles, verification statuses (`VERIFIED`, `REJECTED`), departments, and active/inactive states.
   - Non-destructive management preserving historical appointment and clinical records.
3. **Doctor Schedule Oversight:**
   - Search doctor schedules across dates and departments (`GET /api/v1/admin/schedules`).
   - Identify scheduling conflicts without bypassing Phase 2 domain constraints.
4. **Administrative Appointment Cancellation Overrides:**
   - Endpoint: `POST /api/v1/admin/appointments/{id}/cancel`
   - Requires explicit administrative reason (`reason`).
   - Reuses core domain services: atomically updates appointment status to `CANCELLED`, cancels corresponding queue entries if present, sends in-app notifications, and records `APPOINTMENT_CANCELLED` audit events.
5. **Hospital Queue Monitoring & Oversight:**
   - Real-time queue session summaries (`GET /api/v1/admin/queues?date=YYYY-MM-DD`).
   - Live breakdown of tokens: `waitingCount`, `calledCount`, `inProgressCount`, `completedCount`, `skippedCount`, `isPaused`.
   - Administrative pause and resume operations (`POST /api/v1/admin/queues/{sessionId}/pause`, `POST /api/v1/admin/queues/{sessionId}/resume`) with live STOMP WebSocket broadcast to subscribers.

### 16.2 Real Database-Backed Analytics Module

All analytics are strictly computed from real persisted records in MySQL using aggregated SQL projections. No synthetic or hardcoded metrics are returned.

- **Overview (`GET /api/v1/admin/analytics/overview`):**
  - Active queues, total patients, verified doctors, active departments, today's appointments breakdown, and average wait time.
- **Appointment Metrics (`GET /api/v1/admin/analytics/appointments`):**
  - Aggregations grouped by date: `totalAppointments`, `bookedCount`, `completedCount`, `cancelledCount`, `missedCount`.
  - Attendance Rate: `(completedCount / totalAppointments) * 100` (excluding zero-division).
  - Cancellation Rate: `(cancelledCount / totalAppointments) * 100`.
- **Waiting-Time Analytics (`GET /api/v1/admin/analytics/wait-times`):**
  - Actual wait time computed strictly between `check_in_timestamp` and `consultation_start_timestamp`.
  - Statistical distribution: `averageWaitMinutes`, `medianWaitMinutes`, `p90WaitMinutes`.
  - Prediction accuracy: Mean Absolute Error (MAE) comparing predicted wait times against actual observed consultation start times.
- **Doctor & Department Workload:**
  - `GET /api/v1/admin/analytics/doctors`: Total appointments, completed consultations, and average consultation duration per clinician.
  - `GET /api/v1/admin/analytics/departments`: Total appointments, completed appointments, and unique doctor counts per department.

### 16.3 CSV Export & Spreadsheet Formula Injection Defense

- Analytics endpoints support direct CSV download:
  - `GET /api/v1/admin/analytics/appointments/export`
  - `GET /api/v1/admin/analytics/wait-times/export`
  - `GET /api/v1/admin/audit-logs/export`
- **Spreadsheet Formula Injection Defense (CSV Injection):**
  - In accordance with OWASP guidance, any cell content starting with formula-triggering characters (`=`, `+`, `-`, `@`, tab, carriage return) has a single quote (`'`) prepended before serialization.
  - Double quotes are escaped as `""`, and cells containing commas or newlines are wrapped in standard RFC 4180 quotes.
  - Safe download headers (`Content-Disposition: attachment; filename="..."`, `Content-Type: text/csv; charset=UTF-8`) are enforced.

### 16.4 Centralized Audit Logging & Governance

- Endpoint: `GET /api/v1/admin/audit-logs`
- Filterable by date range (`from`, `to`), `action`, `actorId`, `resourceType`, `resourceId`, and `outcome`.
- Strictly restricted to `ADMIN` roles.
- Excludes passwords, tokens, full clinical notes, or binary documents to preserve patient confidentiality.

### 16.5 Notification Delivery Reliability & Bounded Retries

- Notification states: `PENDING`, `SENT`, `FAILED`, `RETRY_SCHEDULED`.
- External notification provider abstraction: `ExternalNotificationProvider` interface with `LoggingNotificationProvider` implementation.
- Bounded retries: Notifications that experience transient delivery failures are scheduled for retry with exponential backoff up to a maximum attempt limit (default: 3).
- Notifications are saved and dispatched exactly once per creation flow.

### 16.6 Application Security & Production Hardening

- **Authentication Rate Limiting:**
  - In-memory `RateLimitingFilter` limits sensitive endpoints (`/api/v1/auth/login`, `/api/v1/auth/register`) to a maximum of 30 requests per minute per IP address. Exceeded limits return `429 Too Many Requests`.
- **Health Probes & Observability:**
  - Spring Boot Actuator endpoints enabled: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/info`.
  - Accessible without authentication for Kubernetes/container orchestrators, while sensitive metrics remain restricted.
- **Clinical Privacy:**
  - Zero leakage of clinical notes, diagnoses, or stack traces in error envelopes or application logs.

### 16.7 Database Migration V13

Migration `V13__add_phase5_enhancements_and_indexes.sql` applies:
- New columns in `notifications`: `delivery_status`, `retry_count`, `last_attempt_at`, `failure_reason`.
- Composite performance indexes:
  - `idx_notifications_user_created` on `notifications(user_id, created_at)`
  - `idx_notifications_delivery` on `notifications(delivery_status, retry_count)`
  - `idx_consultation_analytics` on `consultation_records(doctor_id, status, actual_start_timestamp)`
  - `idx_queue_entries_wait_analytics` on `queue_entries(doctor_id, status, queue_date, check_in_timestamp)`
  - `idx_audit_actor_action` on `clinical_audit_events(actor_id, action, timestamp)`
  - `idx_audit_resource` on `clinical_audit_events(resource_type, resource_id, timestamp)`

### 16.8 Phase 5 Administration & Analytics API Reference

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| `GET` | `/api/v1/admin/users` | `ADMIN` | Search users with pagination and filters (`q`, `role`, `status`) |
| `PATCH` | `/api/v1/admin/users/{id}/role` | `ADMIN` | Update user role with self-demotion prevention |
| `GET` | `/api/v1/admin/schedules` | `ADMIN` | Search doctor schedules across doctors, departments, dates |
| `POST` | `/api/v1/admin/appointments/{id}/cancel` | `ADMIN` | Administrative appointment override with reason |
| `GET` | `/api/v1/admin/queues` | `ADMIN` | Operational queue overview across all doctors for a date |
| `POST` | `/api/v1/admin/queues/{sessionId}/pause` | `ADMIN` | Administratively pause a queue session |
| `POST` | `/api/v1/admin/queues/{sessionId}/resume` | `ADMIN` | Administratively resume a paused queue session |
| `GET` | `/api/v1/admin/analytics/overview` | `ADMIN` | Hospital operational metrics overview |
| `GET` | `/api/v1/admin/analytics/appointments` | `ADMIN` | Appointment volume, breakdown, and attendance rates |
| `GET` | `/api/v1/admin/analytics/appointments/export`| `ADMIN` | Download appointment analytics as CSV |
| `GET` | `/api/v1/admin/analytics/wait-times` | `ADMIN` | Waiting time distributions (mean, median, p90) & MAE error |
| `GET` | `/api/v1/admin/analytics/wait-times/export` | `ADMIN` | Download wait time analytics as CSV |
| `GET` | `/api/v1/admin/analytics/doctors` | `ADMIN` | Doctor clinical workload & average consultation duration |
| `GET` | `/api/v1/admin/analytics/departments` | `ADMIN` | Department appointment volume & clinician allocation |
| `GET` | `/api/v1/admin/audit-logs` | `ADMIN` | Centralized audit log query with multi-field filters |
| `GET` | `/api/v1/admin/audit-logs/export` | `ADMIN` | Export filtered audit events as CSV |
| `GET` | `/actuator/health` | Public | Liveness and readiness health status |
| `GET` | `/actuator/info` | Public | Application metadata and build info |

---

## 17. Docker, Deployment & Operations

### 17.1 Local Development with Docker Compose

To launch the complete MediQueue stack (MySQL 8.0 database + Spring Boot backend) using Docker Compose:

```bash
docker compose up -d --build
```

Compose configuration (`compose.yaml`):
- **MySQL Container:** `mediqueue-mysql` on port `3306`, with healthcheck `mysqladmin ping` and persistent named volume `mediqueue_mysql_data`.
- **Backend Container:** `mediqueue-backend` built via multi-stage `Dockerfile`, running as non-root user `mediqueue` on port `8080`, with healthcheck against `/actuator/health`.

### 17.2 Production Profile (`application-prod.yml`)

Run with the `prod` profile:
```bash
java -Dspring.profiles.active=prod -jar target/mediqueue-backend-0.0.1-SNAPSHOT.jar
```

Key production protections:
- Flyway migration verification (`validateOnMigrate = true`).
- Hibernate schema auto-generation disabled (`ddl-auto = none`).
- Strict CORS policy requiring explicit origins via `FRONTEND_URL`.
- Structured logging with clinical payload masking.

### 17.3 Backup and Disaster Recovery Guide

1. **MySQL Database Backup:**
   ```bash
   mysqldump -h localhost -u mediqueue_user -p \
     --single-transaction --quick --routines --triggers \
     mediqueue_prod | gzip > backup_mediqueue_$(date +%Y%m%d_%H%M%S).sql.gz
   ```
2. **Medical Documents Storage Backup:**
   ```bash
   tar -czvf backup_documents_$(date +%Y%m%d_%H%M%S).tar.gz storage/documents/
   ```
3. **Restoration Procedure:**
   ```bash
   # 1. Restore Database
   gunzip < backup_mediqueue_20261012.sql.gz | mysql -h localhost -u mediqueue_user -p mediqueue_prod
   # 2. Restore Medical Documents
   tar -xzvf backup_documents_20261012.tar.gz -C ./
   # 3. Verify Application Health
   curl -f http://localhost:8080/actuator/health/readiness
   ```
4. **Migration Failure Recovery:**
   - Never manually edit applied migration files in `db/migration`.
   - In the event of a failed migration, inspect the `flyway_schema_history` table, address root causes with a forward-compatible repair migration (`VXX__...`), and invoke Flyway repair if appropriate.




