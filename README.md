# College ERP Backend — Module 1: Foundation (Auth + Department)

This is the first installment of the full College ERP System Backend, built module-by-module.
Later messages will add: Course/Subject, Student/Faculty, Enrollment/SubjectAllocation,
Attendance, Exam/Result, Notice, Fees, Timetable, Library, Assignment/Submission,
LeaveRequest, Notification, File, AuditLog, and Reports/Dashboard.

## Stack
Java 17, Spring Boot 3.3.4, Spring Data JPA/Hibernate, MySQL, Spring Security + JWT,
Jakarta Validation, springdoc-openapi (Swagger), Lombok, MapStruct (wired for later modules),
iText7 + Apache POI (wired for later PDF/Excel report modules).

## What's included in this module
- Project setup (`pom.xml`, `application.yml`, `ErpApplication`)
- Shared foundation used by every future module:
  - `BaseEntity` (id + audit timestamps)
  - Global exception handling (`ResourceNotFoundException`, `DuplicateResourceException`,
    `BadRequestException`, `InvalidOperationException`, `UnauthorizedException` →
    consistent `timestamp/status/message/path` JSON errors)
  - `PageResponse<T>` — standard pagination envelope for every list endpoint
- Full JWT authentication (access + refresh tokens, BCrypt, email verification,
  forgot/reset password) — `AuthController` + `AuthServiceImpl`
- Role-based authorization (`ADMIN`, `FACULTY`, `STUDENT`) via `@PreAuthorize`
- Department module — full CRUD + search + pagination + activate/deactivate
- Swagger UI with bearer-token auth wired in
- `DataSeeder` auto-inserts the 3 roles on first startup
- Reference SQL schema (Hibernate `ddl-auto=update` also auto-creates/evolves it)

## Running it
1. Create/point to a MySQL 8+ instance. Either let Hibernate create the schema
   (`ddl-auto=update`, default) or run `schema/01_foundation_schema.sql` yourself.
2. Set env vars (or edit `application.yml` directly):
   ```
   DB_USERNAME=root
   DB_PASSWORD=yourpassword
   JWT_SECRET=<base64 32+ byte secret>   # a dev default is already set
   MAIL_USERNAME=you@gmail.com           # only needed for real email sending
   MAIL_PASSWORD=app-password
   ```
3. `mvn spring-boot:run`
4. Swagger UI: `http://localhost:8080/swagger-ui.html`

## API Reference — Module 1

### Auth (`/api/auth`) — all public
| Method | Endpoint | Body |
|---|---|---|
| POST | `/api/auth/register` | `RegisterRequest` |
| POST | `/api/auth/login` | `LoginRequest` |
| POST | `/api/auth/refresh` | `RefreshTokenRequest` |
| GET | `/api/auth/verify-email?token=...` | — |
| POST | `/api/auth/forgot-password` | `ForgotPasswordRequest` |
| POST | `/api/auth/reset-password` | `ResetPasswordRequest` |

### Departments (`/api/departments`) — auth required; writes are ADMIN-only
| Method | Endpoint | Notes |
|---|---|---|
| POST | `/api/departments` | ADMIN only |
| PUT | `/api/departments/{id}` | ADMIN only |
| GET | `/api/departments/{id}` | any authenticated user |
| GET | `/api/departments?page=0&size=20&sort=name` | paginated |
| GET | `/api/departments/search?keyword=cs` | paginated |
| PATCH | `/api/departments/{id}/deactivate` | ADMIN only |
| PATCH | `/api/departments/{id}/activate` | ADMIN only |
| DELETE | `/api/departments/{id}` | ADMIN only, fails if referenced |

## Example requests (Postman-ready)

**Register**
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "admin1",
  "email": "admin1@college.edu",
  "password": "SecurePass123",
  "firstName": "Aditi",
  "lastName": "Sharma",
  "phone": "9876543210",
  "roles": ["ADMIN"]
}
```

**Login**
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "admin1",
  "password": "SecurePass123"
}
```
Response:
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresInMs": 3600000,
  "user": { "id": 1, "username": "admin1", "roles": ["ADMIN"], "...": "..." }
}
```

**Create Department** (send `Authorization: Bearer <accessToken>`)
```http
POST /api/departments
Content-Type: application/json
Authorization: Bearer eyJhbGciOi...

{
  "name": "Computer Science & Engineering",
  "code": "CSE",
  "description": "Department of Computer Science and Engineering"
}
```

**Error response shape** (any failure)
```json
{
  "timestamp": "2026-08-14T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Department not found with id = '99'",
  "path": "/api/departments/99"
}
```

## Next installments
Course & Subject → Student & Faculty (+ SubjectAllocation) → AcademicSession & Enrollment →
Attendance → Exam & Result → Notice & Notification → Fees (FeeStructure/FeePayment) →
Timetable → Library (Book/BookTransaction) → Assignment/Submission → LeaveRequest →
File uploads → AuditLog → Dashboard & PDF/Excel Reports.
