# RetailAI Security Architecture & Multi-Tenant Isolation (Phase 8)

## Overview
RetailAI implements a statelessly authenticated security architecture using **Spring Security** and **JSON Web Tokens (JWT)**.
It provides complete multi-tenant context extraction and enforces fine-grained Role-Based Access Control (RBAC).

---

## 1. Registration Security Model & Role Assignment

### Public Registration (`POST /api/v1/auth/register`)
- **Organization Creation:** Public registration is used exclusively to register a **NEW** organization. Knowing an existing `organizationCode` MUST NOT grant tenant membership or privilege escalation.
- **Duplicate Code Rejection:** If `organizationCode` already exists in the database, public registration rejects the request with HTTP `409 Conflict` (`OrganizationAlreadyExistsException`).
- **Server-Assigned Role:** The founder registering a new organization is automatically assigned the `ADMIN` role by the server. Any `role` field specified in the client request body is ignored for public registration to prevent privilege escalation.
- **JWT Issuance:** Registration issues a signed JWT containing claims (`userId`, `organizationId`, `organizationCode`, `role`) derived strictly from the server-persisted entity.

### Existing Organization User Creation
- Public users cannot join existing organizations simply by specifying an `organizationCode`.
- Adding team members (`STORE_MANAGER`, `INVENTORY_MANAGER`, `ADMIN`) to an existing tenant is restricted to authenticated `ADMIN`s of that tenant (e.g., via `createUserInTenant` within the tenant security context).

---

## 2. JWT Architecture & Token Structure
- **Algorithm:** HMAC SHA-512 (`HS512`).
- **Configuration:**
  - `JWT_SECRET` / `jwt.secret`: 256-bit+ secret key configured via environment variables.
  - `JWT_EXPIRATION_MS` / `jwt.expiration-ms`: Default 24 hours (`86400000` ms).
- **Claims Structure:**
  - `sub`: User email address.
  - `userId`: Internal database identifier for the user.
  - `organizationId`: Multi-tenant organization primary key.
  - `organizationCode`: Unique code identifier for the tenant organization.
  - `role`: Granted authority/role (`ADMIN`, `STORE_MANAGER`, `INVENTORY_MANAGER`).

---

## 3. TenantContext & Isolation Mechanism
- `JwtAuthenticationFilter` intercepts incoming HTTP requests containing `Authorization: Bearer <token>`.
- Validates token signature and expiration.
- Constructs `SecurityUserDto` and populates `SecurityContextHolder`.
- Sets `TenantContext.setTenantId(organizationId)`.
- Thread-local cleanup (`TenantContext.clear()`) is performed inside `finally` block to prevent ThreadLocal leaks in thread pools.
- Client-supplied HTTP headers or body overrides CANNOT manipulate tenant context; identity is strictly extracted from verified JWT claims.

---

## 4. Endpoints Summary

| Endpoint | Method | Permitted Roles / Auth | Description |
|---|---|---|---|
| `/api/health` | `GET` | Public (`permitAll`) | System health status check |
| `/api/v1/auth/register` | `POST` | Public (`permitAll`) | Register NEW organization & first user as ADMIN |
| `/api/v1/auth/login` | `POST` | Public (`permitAll`) | Authenticate user and issue JWT token |
| `/api/v1/security/me` | `GET` | Authenticated | Fetch current user & tenant security info |
| `/api/v1/security/tenant-info` | `GET` | Authenticated | Fetch current active tenant ID |
| `/api/v1/security/admin-only` | `GET` | `hasRole('ADMIN')` | Admin-only restricted endpoint |

---

## 5. Environment Variables

| Variable | Property Key | Default Value | Description |
|---|---|---|---|
| `JWT_SECRET` | `jwt.secret` | (Development fallback key) | HMAC SHA-512 signing secret |
| `JWT_EXPIRATION_MS` | `jwt.expiration-ms` | `86400000` (24h) | Token validity duration in milliseconds |

---

## 6. Role-Based Access Control (RBAC)

1. `ROLE_ADMIN`:
   - Full administration rights over organization, stores, products, inventory, users, and security settings.
2. `ROLE_STORE_MANAGER`:
   - Store level management and sales monitoring permissions within tenant scope.
3. `ROLE_INVENTORY_MANAGER`:
   - Stock level tracking and replenishment management permissions within tenant scope.
