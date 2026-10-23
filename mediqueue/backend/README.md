# MediQueue Backend — Spring Boot 3 / Java 21 (STS-Compatible)

MediQueue Backend is a healthcare scheduling and real-time outpatient queue management platform built with **Java 21**, **Spring Boot 3.3.5**, **Spring Security 6**, **JJWT**, **Spring Data JPA**, **Flyway**, and **WebSocket/STOMP**.

This project is a standard Maven project structured for seamless import, running, and debugging in **Spring Tool Suite (STS 4)** on Windows.

---

## 1. System Requirements & Prerequisites

- **Java Development Kit (JDK):** JDK 21 (Temurin 21 LTS / OpenJDK 21)
- **IDE:** Spring Tool Suite 4 (STS 4) or Eclipse IDE for Enterprise Java and Web Developers
- **Build Tool:** Apache Maven 3.9+ (bundled Maven Wrapper `mvnw.cmd` included)
- **Database:** MySQL 8.0+ running on `localhost:3306`
- **Operating System:** Windows 10/11 (PowerShell terminal supported)

---

## 2. Spring Tool Suite (STS) Import & Setup Guide

Follow these steps to import, configure, and demonstrate the backend in STS:

### Step 2.1: Verify JDK 21 Configuration in STS
1. Open Spring Tool Suite (STS).
2. Go to **Window** → **Preferences** → **Java** → **Installed JREs**.
3. Verify that **JDK 21** (e.g. `C:\Users\<username>\AppData\Local\Programs\jdk-21` or `C:\Program Files\Eclipse Adoptium\jdk-21.*`) is listed and checked.
4. If JDK 21 is not listed, click **Add...** → **Standard VM** → **Directory...** → browse to your JDK 21 home → click **Finish**.
5. Under **Java** → **Installed JREs** → **Execution Environments**, select **JavaSE-21** and confirm your JDK 21 is bound.

### Step 2.2: Import as Existing Maven Project
1. In STS, click **File** → **Import...**
2. Expand **Maven** and select **Existing Maven Projects**, then click **Next**.
3. In **Root Directory**, click **Browse...** and select the `backend` folder:
   ```
   C:\Users\<path>\mediqueue\backend
   ```
4. STS will detect `/pom.xml com.mediqueue:mediqueue-backend:0.0.1-SNAPSHOT:jar`.
5. Ensure the checkbox next to `pom.xml` is checked.
6. Click **Finish**. STS will import the project and resolve all Maven dependencies.

### Step 2.3: Lombok Support in STS
- **STS 4.15+**: Modern STS releases have Lombok support enabled automatically through the Spring Boot Language Server and the Maven `annotationProcessorPaths` defined in `pom.xml`.
- **Manual Verification (if annotations show errors)**:
  1. Locate the downloaded Lombok jar in your `~/.m2/repository/org/projectlombok/lombok/1.18.36/lombok-1.18.36.jar`.
  2. Double-click or run: `java -jar lombok-1.18.36.jar` and point the installer to your `SpringToolSuite4.exe` location.
  3. Alternatively, ensure `backend/pom.xml` compiler plugin has the annotation processor configured (already included).
  4. In STS, right-click `mediqueue-backend` → **Maven** → **Update Project...** (or Alt+F5) → check **Force Update of Snapshots/Releases** → click **OK**.

### Step 2.4: Running and Debugging in STS

#### Method A: Spring Boot Dashboard (Recommended)
1. Locate the **Spring Boot Dashboard** tab in STS (bottom left or View → Show View → Other... → Spring Boot → Boot Dashboard).
2. Expand `local`, find `mediqueue-backend`.
3. Click the green **(Re)start** button to run, or the bug icon to **Debug**.

#### Method B: Run As Spring Boot App
1. In the Package Explorer, navigate to:
   ```
   src/main/java/com/mediqueue/MediQueueApplication.java
   ```
2. Right-click `MediQueueApplication.java` → **Run As** → **Spring Boot App** (or **Debug As** → **Spring Boot App**).
3. The Console tab will display the Spring Boot banner, Flyway schema validation, and Tomcat listening on port `8080`.

#### Setting Active Profiles in STS
1. Right-click the project → **Run As** → **Run Configurations...**
2. Under **Spring Boot App**, select `mediqueue-backend`.
3. In the **Spring Boot** tab, under **Profile**, type `dev` (or under Arguments tab, VM arguments: `-Dspring.profiles.active=dev`).
4. Click **Apply** and **Run**.

---

## 3. Command-Line Verification (Windows PowerShell)

You can run, test, and package the backend directly from PowerShell inside `backend/`:

### 3.1 Run Unit & Integration Tests (130 Tests)
```powershell
.\mvnw.cmd clean test
```

### 3.2 Run Application Locally
```powershell
.\mvnw.cmd spring-boot:run
```
Or to run with an explicit profile:
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3.3 Package Executable JAR
```powershell
.\mvnw.cmd clean package -DskipTests
```
The generated archive is located at:
```
target/mediqueue-backend-0.0.1-SNAPSHOT.jar
```
To run the packaged JAR:
```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target\mediqueue-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## 4. Database Setup (MySQL 8.x)

1. Ensure MySQL service is running on `localhost:3306`:
   ```powershell
   Get-Service -Name MySQL80
   ```
2. Create development database:
   ```sql
   CREATE DATABASE IF NOT EXISTS mediqueue_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Flyway migrations (`src/main/resources/db/migration/V1__...` through `V13__...`) automatically execute upon startup.

---

## 5. Security & Configuration (`application-dev.yml` vs `application-prod.yml`)

- **Development (`dev` profile):**
  - Database: `jdbc:mysql://localhost:3306/mediqueue_dev`
  - User: `root`
  - JWT Secret: Dev fallback enabled for frictionless local testing.
  - Swagger UI: Enabled at `http://localhost:8080/swagger-ui/index.html`.
- **Production (`prod` profile):**
  - Fail-fast strict environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
  - Swagger UI disabled.
  - Fail-safe: Application halts immediately if `JWT_SECRET` is unset.

---

## 6. Endpoints & Integrations Overview

- **Health Check:** `GET http://localhost:8080/actuator/health`
- **Auth:** `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`
- **Departments:** `/api/v1/departments`, `/api/v1/admin/departments`, `PATCH /api/v1/admin/departments/{id}/status`
- **Users:** `GET /api/v1/admin/users`, `PATCH /api/v1/admin/users/{id}/status`, `PATCH /api/v1/admin/users/{id}/role`
- **Doctors:** `/api/v1/doctors`, `/api/v1/admin/doctors`
- **Appointments & Queues:** `/api/v1/appointments`, `/api/v1/queue`, `/api/v1/admin/queues`
- **WebSocket STOMP Broker:** `ws://localhost:8080/ws` (Topic: `/topic/doctors/{id}/queue`, Queue: `/user/queue/status`)
- **Document Storage:** `storage/documents/` (configured via `mediqueue.document.storage-path`)
