# RetailAI Security Architecture & Multi-Tenant Isolation (Phase 8)

## Overview
RetailAI implements a first-party, statelessly authenticated security architecture using **Spring Security** and **JSON Web Tokens (JWT)**.
It provides complete multi-tenant context extraction and enforces fine-grained Role-Based Access Control (RBAC).

---

## Architecture Components

### 1. Authentication Flow
1. **Registration (`POST /api/v1/auth/register`)**:
   - Accepts user email, password, full name, role, and organization details (`organizationCode` or `organizationName`).
   - If an `organizationCode` is provided, the user is associated with the existing organization.
   - If `organizationCode` is not provided, a new `Organization` entity is automatically created.
   - Passwords are hashed using **BCrypt** (`PasswordEncoder` with cost factor 10) prior to storage.
   - Returns a JWT token containing user and tenant claims.

2. **Login (`POST /api/v1/auth/login`)**:
   - Authenticates credentials against `CustomUserDetailsService` using `DaoAuthenticationProvider`.
   - Validates BCrypt hashed password.
   - Returns an `AuthResponse` with a signed JWT token and user summary details.

### 2. JWT Architecture & Token Structure
- **Algorithm**: HMAC SHA-512 (`HS512`).
- **Configuration**:
  - `JWT_SECRET` / `jwt.secret`: 256-bit+ secret key configured via environment variables.
  - `JWT_EXPIRATION_MS` / `jwt.expiration-ms`: Default 24 hours (`86400000` ms).
- **Claims Structure**:
  - `sub`: User email address.
  - `userId`: Internal database identifier for the user.
  - `organizationId`: Multi-tenant organization primary key.
  - `organizationCode`: Unique code identifier for the tenant organization.
  - `role`: Granted authority/role (`ADMIN`, `STORE_MANAGER`, `INVENTORY_MANAGER`).

### 3. TenantContext & Isolation Mechanism
- `JwtAuthenticationFilter` intercepts incoming HTTP requests containing `Authorization: Bearer <token>`.
- Validates token signature and expiration.
- Constructs `SecurityUserDto` and populates `SecurityContextHolder`.
- Context listener/filter sets `TenantContext.setTenantId(organizationId)`.
- Thread-local cleanup is performed inside `finally` block to prevent ThreadLocal leaks in thread pools.
- Client-supplied HTTP headers or body overrides CANNOT manipulate tenant context; identity is strictly extracted from verified JWT claims.

---

## Endpoints Summary

| Endpoint | Method | Permitted Roles / Auth | Description |
|---|---|---|---|
| `/api/health` | `GET` | Public (`permitAll`) | System health status check |
| `/api/v1/auth/register` | `POST` | Public (`permitAll`) | Register user and create/link organization |
| `/api/v1/auth/login` | `POST` | Public (`permitAll`) | Authenticate user and issue JWT token |
| `/api/v1/security/me` | `GET` | Authenticated | Fetch current user & tenant security info |
| `/api/v1/security/tenant-info` | `GET` | Authenticated | Fetch current active tenant ID |
| `/api/v1/security/admin-only` | `GET` | `hasRole('ADMIN')` | Admin-only restricted endpoint |

---

## Environment Variables

| Variable | Property Key | Default Value | Description |
|---|---|---|---|
| `JWT_SECRET` | `jwt.secret` | (Development fallback key) | HMAC SHA-512 signing secret |
| `JWT_EXPIRATION_MS` | `jwt.expiration-ms` | `86400000` (24h) | Token validity duration in milliseconds |

---

## Role-Based Access Control (RBAC)

1. `ROLE_ADMIN`:
   - Full administration rights over organization, stores, products, inventory, users, and security settings.
2. `ROLE_STORE_MANAGER`:
   - Store level management and sales monitoring permissions within tenant scope.
3. `ROLE_INVENTORY_MANAGER`:
   - Stock level tracking and replenishment management permissions within tenant scope.
