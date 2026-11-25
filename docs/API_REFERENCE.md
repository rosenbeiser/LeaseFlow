# API Reference — LeaseFlow REST API

> **Author:** Student 2207084  
> **Domain:** REST Controllers, DTOs, Tests, Frontend Pages

**Base URL:** `http://localhost:8888/api/v1`  
**Auth:** Bearer JWT token in `Authorization` header  
**Interactive Docs:** `http://localhost:8888/swagger-ui.html`

---

## Authentication

### POST `/auth/login`
Login and receive a JWT token.

**Request body:**
```json
{ "email": "landlord1@leaseflow.dev", "password": "Landlord@123" }
```
**Response:**
```json
{
  "token": "eyJhbGc...",
  "tokenType": "Bearer",
  "userId": 2,
  "email": "landlord1@leaseflow.dev",
  "fullName": "Alice Johnson",
  "role": "ROLE_LANDLORD"
}
```

### POST `/auth/register`
Register a new user (TENANT or LANDLORD).

### GET `/auth/me`
Get current user profile. Requires auth.

---

## Properties (LANDLORD / ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/properties` | List landlord's properties (paginated) |
| POST | `/properties` | Create a new property |
| GET | `/properties/{id}` | Get property details |
| PUT | `/properties/{id}` | Update property |
| DELETE | `/properties/{id}` | Delete property (only if no active leases) |
| GET | `/properties/{id}/units` | List units within a property |

---

## Leases (LANDLORD / ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/leases` | List leases (paginated, filterable by status) |
| POST | `/leases` | Create a DRAFT lease |
| GET | `/leases/{id}` | Get lease details with allowed actions |
| POST | `/leases/{id}/activate` | Activate a DRAFT lease |
| POST | `/leases/{id}/serve-notice` | Serve notice on an ACTIVE lease |
| POST | `/leases/{id}/terminate` | Terminate a lease |
| POST | `/leases/{id}/settle` | Settle a TERMINATED lease |
| POST | `/leases/{id}/invoice` | Generate an invoice for the billing period |

---

## Meter Readings (LANDLORD / ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/readings` | List readings (filterable by lease/unit) |
| POST | `/readings` | Record a new meter reading |
| GET | `/readings/{id}` | Get a reading |

---

## Invoices (LANDLORD / ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/invoices` | List invoices (paginated) |
| GET | `/invoices/{id}` | Get invoice with line items |
| POST | `/invoices/{id}/send` | Mark invoice as SENT |

---

## Payments (TENANT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/tenant/bills` | List tenant's bills |
| POST | `/tenant/payments` | Record a payment |
| GET | `/tenant/payments` | List tenant's payments |

---

## Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard` | Get role-specific dashboard KPIs |

**Landlord response:**
```json
{
  "propertyCount": 2,
  "totalUnits": 5,
  "occupiedUnits": 2,
  "vacantUnits": 3,
  "occupancy": 40.0,
  "activeLeasesCount": 2,
  "outstandingBalance": 3015.00,
  "overdueInvoicesCount": 1
}
```

---

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Validation error (body contains `fieldErrors` map) |
| 401 | Missing or invalid JWT |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 409 | Conflict (e.g. invalid state transition) |
| 500 | Internal server error |

---

## Demo Accounts

| Role | Email | Password |
|------|-------|----------|
| ADMIN | `admin@leaseflow.dev` | `Admin@123` |
| LANDLORD | `landlord1@leaseflow.dev` | `Landlord@123` |
| LANDLORD | `landlord2@leaseflow.dev` | `Landlord@123` |
| TENANT | `tenant1@leaseflow.dev` | `Tenant@123` |
| TENANT | `tenant2@leaseflow.dev` | `Tenant@123` |
