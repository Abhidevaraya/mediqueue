# MediQueue — Smart Hospital Appointment & Real-Time Queue Management Frontend

MediQueue Frontend is a healthcare web application built with **React 18**, **TypeScript**, **Vite**, and **Tailwind CSS**. Designed under the curated **Lavender + White + Indigo** healthcare design system, it provides complete role-scoped portals for Patients, Doctors, and Hospital Administrators, integrated with the Spring Boot 3 backend and STOMP WebSocket real-time queues.

---

## 1. Architecture & Portals

### 1.1 Patient Health Portal (`/patient`)
- **My Health Home:** Active queue tokens, today's appointments, fast-action shortcuts.
- **Find a Specialist (`/patient/doctors`):** Multi-filter directory (department, specialty, sorting) with live appointment slot reservation.
- **My Appointments (`/patient/appointments`):** Complete booking lifecycle management, cancellation, rescheduling, and status tracking.
- **Live Queue Status (`/patient/queue`):** Real-time arrival check-in, token progression, dynamic door-to-consultation wait-time estimation, and STOMP subscription updates.
- **Medical Records & Rx (`/patient/records`):** Patient-isolated access to approved clinician visit summaries, immutable prescriptions, follow-up instructions, and secure medical document uploads/downloads.

### 1.2 Doctor Clinical Console (`/doctor`)
- **Clinical Dashboard:** Daily appointment counts, active OPD clinic indicators, queue call-next action.
- **OPD Queue Console (`/doctor/queue`):** Room-level token calling, consultation start/completion, patient skipping with audit reasons, requeuing, and queue pause/resume controls.
- **Consultations & Rx Workspace (`/doctor/consultations`):** Real-time medical encounter documentation, diagnostic notes, multi-item prescription issuance with revision tracking, and follow-up scheduling.
- **My Schedule (`/doctor/schedule`):** Weekly shift management, slot durations, break hours, and leave exceptions.

### 1.3 Hospital Administration Dashboard (`/admin`)
- **Operational Overview (`/admin`):** Live executive metrics (today's bookings, completions, active OPD clinics, waiting patients, average wait and consultation times).
- **User Directory (`/admin/users`):** Account searching, role filters, account activation/blocking, and role reassignments.
- **Doctor Management (`/admin/doctors`):** Practitioner onboarding, credential verification, specialties, and fee configuration.
- **Department Catalog (`/admin/departments`):** Department directory, creation, description updates, and active status toggling.
- **Queue Operations (`/admin/queues`):** Hospital-wide OPD room monitoring, active token inspection, and pause/resume overrides.
- **Doctor Schedules (`/admin/schedules`):** Cross-department shift inspection and break interval auditing.
- **Analytics & BI (`/admin/analytics`):** Date-range filtered reporting for appointment attendance/cancellation rates, empirical wait-time percentiles, doctor workloads, and raw CSV exports.
- **Clinical Audit Trail (`/admin/audit`):** HIPAA/compliance audit log inspection, actor/action/outcome filtering, and CSV export.

---

## 2. Technology Stack

- **Core Framework:** React 18.3.1, TypeScript 5.6.3, Vite 5.4.10
- **Styling & Design System:** Tailwind CSS 3.4.14, Lucide React icons
- **State Management & Server Queries:** `@tanstack/react-query` 5.59.16
- **Routing:** React Router DOM 6.27.0
- **HTTP Client:** Axios 1.7.7 with centralized interceptors, Bearer JWT injection, and session invalidation
- **Real-Time Messaging:** `@stomp/stompjs` 7.0.0 over native WebSockets
- **Validation & Forms:** React Hook Form 7.53.1 + Zod 3.23.8
- **Testing:** Vitest 2.1.3, React Testing Library, jsdom

---

## 3. Environment Variables

Create `.env` based on `.env.example`:

```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `VITE_API_BASE_URL` | Backend HTTP API base URL (no trailing slash) | `http://localhost:8080` |
| `VITE_APP_ENV` | Environment mode (`development`, `production`, `test`) | `development` |

> **WebSocket Note:** The STOMP client automatically derives the WebSocket endpoint from `VITE_API_BASE_URL` (e.g. `http://localhost:8080` → `ws://localhost:8080/ws`, `https://api.mediqueue.org` → `wss://api.mediqueue.org/ws`).

---

## 4. Getting Started

### 4.1 Prerequisites
- Node.js 18.x or 20.x LTS
- npm 9.x or higher

### 4.2 Installation
```bash
npm install
```

### 4.3 Running Locally
```bash
npm run dev
```
The application will launch at `http://localhost:5173`.

### 4.4 Running Tests
```bash
# Run unit and component test suite
npm test

# Run tests in watch mode
npm run test:watch
```

### 4.5 Type Checking
```bash
npm run typecheck
```

### 4.6 Production Build
```bash
npm run build
```
The optimized production bundle will be output to `dist/`.

---

## 5. Deployment & Production Hosting

### 5.1 Single-Page Application (SPA) Routing Fallback
Because MediQueue uses client-side routing (`react-router-dom`), any server hosting the static assets must rewrite non-file requests to `/index.html`.

#### Nginx Configuration
```nginx
server {
    listen 80;
    server_name app.mediqueue.org;
    root /var/www/mediqueue/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Optional reverse proxy for backend REST and WebSocket
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://localhost:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
    }
}
```

#### Apache (`.htaccess`)
```apache
<IfModule mod_rewrite.c>
  RewriteEngine On
  RewriteBase /
  RewriteRule ^index\.html$ - [L]
  RewriteCond %{REQUEST_FILENAME} !-f
  RewriteCond %{REQUEST_FILENAME} !-d
  RewriteRule . /index.html [L]
</IfModule>
```

#### Docker Deployment
A multi-stage Docker build can be used to compile and serve with Nginx:
```dockerfile
# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 6. Security & Privacy Guarantees

1. **Token Hygiene:** JWT tokens are stored via `tokenStorage` and wiped immediately upon logout or on receiving `401 Unauthorized`.
2. **Access Separation:** UI route guards (`RoleRoute`, `ProtectedRoute`) prevent unauthorized view rendering; authoritative access control is strictly enforced by Spring Security at the backend API layer.
3. **Data Protection:** Medical records, diagnoses, and passwords are never cached in persistent localStorage or logged to console outputs.
4. **CSV Injection Defense:** Exports sanitize cells starting with formula triggers (`=`, `+`, `-`, `@`).
