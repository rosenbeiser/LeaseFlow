# Security Architecture — LeaseFlow

> **Author:** Student 2207082  
> **Domain:** Authentication, Authorization & Infrastructure Security

---

## 1. Authentication Flow

LeaseFlow uses stateless JWT (JSON Web Token) authentication. No server-side session state is kept.

```
Client                    Spring Security Filter Chain              Database
  │                               │                                     │
  │  POST /api/v1/auth/login      │                                     │
  │  {email, password}  ─────────►│                                     │
  │                               │  loadUserByUsername(email) ────────►│
  │                               │◄──────────── UserDetails ───────────│
  │                               │  BCrypt.matches(password, hash)     │
  │                               │  generateToken(userId, email, role) │
  │◄─── 200 OK {token, ...} ──────│                                     │
  │                               │                                     │
  │  GET /api/v1/... Bearer <tok> │                                     │
  │  ────────────────────────────►│                                     │
  │                               │  JwtAuthenticationFilter:           │
  │                               │    validateToken(token) ✓           │
  │                               │    setAuthentication(principal)     │
  │                               │  @PreAuthorize check passes         │
  │◄─── 200 OK {data} ────────────│                                     │
```

## 2. JWT Token Structure

```
Header:  {"alg":"HS256","typ":"JWT"}
Payload: {
  "sub":  "landlord1@leaseflow.dev",   // Spring Security username
  "id":   1,                            // User database id
  "role": "ROLE_LANDLORD",             // Single role (no multi-role)
  "iat":  1704067200,                  // Issued at (Unix timestamp)
  "exp":  1704153600                   // Expires at (24 h default)
}
Signature: HMAC-SHA256(base64(header)+"."+base64(payload), secretKey)
```

**Key properties:**
- Secret is supplied via environment variable `JWT_SECRET` — never hardcoded
- Minimum 32-character secret enforced at startup
- Token expiry configurable via `JWT_EXPIRATION_MS` (default: 86 400 000 ms = 24 h)

## 3. Role-Based Access Control

| Role | Access |
|------|--------|
| `ROLE_ADMIN` | All endpoints; platform-wide stats |
| `ROLE_LANDLORD` | Own properties, units, leases, invoices, readings |
| `ROLE_TENANT` | Own lease, bills, payments only |

Enforcement is done with `@PreAuthorize` at the controller method level (method security) **and** at the service layer (ownership checks). Double enforcement prevents IDOR (Insecure Direct Object Reference) vulnerabilities.

## 4. Password Storage

Passwords are hashed with **BCrypt** (strength 12 by default via Spring Security). Plaintext passwords are never stored or logged.

## 5. CORS Policy

Cross-Origin Resource Sharing is configured in `CorsConfig.java`:
- Allowed origin: configurable via `app.cors.allowed-origin` (defaults to `http://localhost:8888`)
- Never uses wildcard (`*`) with credentials enabled
- Pre-flight results cached 1 hour to reduce OPTIONS round-trips

## 6. Security Headers

Spring Security's default security headers are active:
- `X-Frame-Options: DENY` — clickjacking protection
- `X-Content-Type-Options: nosniff` — MIME sniffing protection
- `Strict-Transport-Security` — HTTPS enforcement (when behind TLS terminator)
- `Cache-Control: no-store` — prevents sensitive responses from being cached

## 7. Endpoint Classification

| Classification | Examples |
|---------------|---------|
| Public (no auth) | `POST /auth/login`, `POST /auth/register`, static assets, Swagger UI |
| Authenticated (any role) | `GET /api/v1/auth/me` |
| LANDLORD or ADMIN | All property/unit/lease/invoice CRUD endpoints |
| TENANT only | `/api/v1/tenant/**` |
| ADMIN only | `/api/v1/admin/**` |

## 8. Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | ✅ | — | HMAC-SHA256 signing secret (min 32 chars) |
| `JWT_EXPIRATION_MS` | ❌ | `86400000` | Token expiry in ms (24 h) |
| `DB_PASSWORD` | ✅ | — | PostgreSQL password |
| `DB_URL` | ❌ | localhost/leaseflow | PostgreSQL JDBC URL |
