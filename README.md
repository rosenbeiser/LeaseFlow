# LeaseFlow: Smart Property Lease & Utility Billing Management System

[![Build & Test](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/leaseflow/leaseflow)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.3-purple.svg)](https://getbootstrap.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**LeaseFlow** is a comprehensive, full-stack rental property and utility management platform. It pairs a **Spring Boot 3.3.x REST backend** with a **modern, vanilla ES-module web frontend** served directly from the same application. 

The system automates the lease agreement lifecycle using the **State Pattern**, computes progressive multi-tier utility bills using the **Strategy Pattern**, enforces data isolation with **Spring Security 6 & JWT**, and provides dedicated portals for landlords and tenants.

---

## 🏛️ System Architecture & Design Patterns

```
                                  ┌───────────────────────────┐
                                  │      Vanilla ES6 SPA      │
                                  │   Bootstrap 5.3 + Chart   │
                                  └─────────────┬─────────────┘
                                                │ REST (JSON)
                                  ┌─────────────▼─────────────┐
                                  │  Spring Boot REST API     │
                                  │  Security 6 + Stateless   │
                                  └──────┬─────────────┬──────┘
                                         │             │
                ┌────────────────────────┴───┐   ┌─────┴─────────────────────────┐
                │        State Pattern       │   │       Strategy Pattern        │
                │ ────────────────────────── │   │ ───────────────────────────── │
                │ Draft ──► Active           │   │ Tiered Electricity (Progress) │
                │ Active ──► Notice Served   │   │ Flat Rate Water + Sewerage    │
                │ Active ──► Terminated      │   │ Commercial Infrastructure     │
                │ Terminated ──► Settled     │   │ Factory: Selection by Type    │
                └────────────────────────────┘   └───────────────────────────────┘
                                         │
                                  ┌──────▼────────────────────┐
                                  │   PostgreSQL 16 Database  │
                                  │ (H2 for in-memory tests)  │
                                  └───────────────────────────┘
```

### 1. State Pattern: Lease Lifecycle
The lifecycle of a rental agreement is encapsulated in the `com.leaseflow.pattern.state` package:
- `DraftState` &rarr; Only `activate` allowed (flips unit status to `OCCUPIED`).
- `ActiveState` &rarr; Allows `serveNotice`, `terminate` (early termination, flips unit to `VACANT`), and `generateInvoice`.
- `NoticeServedState` &rarr; Allows `terminate` and `generateInvoice`.
- `TerminatedState` &rarr; Only `settle` allowed (calculates `refund = deposit - unpaidBalance - damages`).
- `SettledState` &rarr; Terminal state; zero transitions allowed.
- Every state transition automatically logs an immutable record in `LeaseStatusHistory`.
- `allowedActions` are computed dynamically by the current state object, enabling the UI to render **only valid action buttons**.

### 2. Strategy Pattern: Utility Billing Engine
Encapsulated in `com.leaseflow.pattern.strategy`:
- `TieredElectricityPricingStrategy`: Progressive slabs (0–100 @ $0.10, 101–300 @ $0.15, 301+ @ $0.25). E.g., **350 units = $52.50**.
- `FlatRateWaterPricingStrategy`: Fixed per-unit charge ($1.50) + mandatory sewerage charge ($5.00).
- `CommercialUtilityStrategy`: Heavy commercial infrastructure fee ($50.00 base + $0.30/unit).
- `BillingStrategyFactory`: Resolves strategies based on `(PropertyType, UtilityType)` matrix and rejects unsupported configurations (e.g. Residential GAS).

---

## 🚀 Quick Start Guide

### Option A: One-Command Run with Docker Compose (Recommended)

Make sure Docker and Docker Compose are installed and running:

```bash
# 1. Clone the repository
git clone https://github.com/leaseflow/leaseflow.git
cd leaseflow

# 2. Launch PostgreSQL and Spring Boot application
docker compose up --build
```

The database is automatically provisioned, healthchecked, and bootstrapped with the `dev` profile data seeder. Once the containers are healthy, open:
- **Public Portal & Landing Page:** [http://localhost:8888](http://localhost:8888)
- **Application Shell & Portal:** [http://localhost:8888/app.html](http://localhost:8888/app.html)
- **Interactive Swagger UI:** [http://localhost:8888/swagger-ui.html](http://localhost:8888/swagger-ui.html)
- **Actuator Health:** [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health)

---

### Option B: Local Maven Execution

Requirements: **Java 17 (JDK)**, **Maven 3.8+**, and **PostgreSQL 16**.

```bash
# 1. Set environment variables (or rely on application.yml defaults)
export DB_URL=jdbc:postgresql://localhost:5432/leaseflow
export DB_USERNAME=leaseflow
export DB_PASSWORD=leaseflow_secret_pw

# 2. Compile and run with the dev profile (enables demo data seeding)
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🔑 Pre-Seeded Demo Credentials

The application ships with realistic demo data pre-seeded in the database:

| Role | Email | Password | Description |
|---|---|---|---|
| **ADMIN** | `admin@leaseflow.dev` | `Admin@123` | System Administrator |
| **LANDLORD** | `landlord1@leaseflow.dev` | `Landlord@123` | Alice Landlord (Owns 2 properties, 5 units) |
| **LANDLORD** | `landlord2@leaseflow.dev` | `Landlord@123` | Bob Landlord (Owns 1 property, 2 units) |
| **TENANT** | `tenant1@leaseflow.dev` | `Tenant@123` | Carol Tenant (Active lease at Sunrise Apts, Unit 1A) |
| **TENANT** | `tenant2@leaseflow.dev` | `Tenant@123` | Dave Tenant (Notice served at Unit 1B) |
| **TENANT** | `tenant3@leaseflow.dev` | `Tenant@123` | Eve Tenant (Draft lease assigned at Unit 2A) |
| **TENANT** | `tenant4@leaseflow.dev` | `Tenant@123` | Frank Tenant (Commercial lease at Downtown Plaza) |

---

## 📋 REST API Endpoints Overview

All endpoints return uniform `ApiResponse<T>` envelopes (`{ success, message, data, timestamp }`).

| Method | Endpoint | Authorized Roles | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register Landlord or Tenant account |
| `POST` | `/api/v1/auth/login` | Public | Authenticate credentials & receive JWT |
| `GET` | `/api/v1/auth/me` | Authenticated | Fetch current profile |
| `POST` | `/api/v1/properties` | `LANDLORD` | Create property with initial units |
| `GET` | `/api/v1/properties` | `LANDLORD` | Paginated list of owned properties |
| `GET` | `/api/v1/properties/{id}/units`| `LANDLORD` | List units in a property |
| `GET` | `/api/v1/users/tenants` | `LANDLORD`, `ADMIN` | Search tenants by email for lease assignment |
| `POST` | `/api/v1/leases` | `LANDLORD` | Create draft lease with overlap validation |
| `GET` | `/api/v1/leases` | `LANDLORD` | Paginated list of leases filtered by status |
| `GET` | `/api/v1/leases/my-leases` | `TENANT` | List tenant's own leases |
| `GET` | `/api/v1/leases/{id}` | `LANDLORD`, `TENANT` | Full lease detail + timeline & allowed actions |
| `PUT` | `/api/v1/leases/{id}/activate` | `LANDLORD` | Transition DRAFT &rarr; ACTIVE |
| `PUT` | `/api/v1/leases/{id}/notice` | `LANDLORD` | Transition ACTIVE &rarr; NOTICE_SERVED |
| `PUT` | `/api/v1/leases/{id}/terminate`| `LANDLORD` | Transition ACTIVE/NOTICE &rarr; TERMINATED |
| `POST` | `/api/v1/leases/{id}/settle` | `LANDLORD` | Settle deposit with damage deduction |
| `POST` | `/api/v1/meter-readings` | `LANDLORD` | Record meter reading with auto-derivation |
| `GET` | `/api/v1/meter-readings` | `LANDLORD` | Meter reading history for a unit |
| `POST` | `/api/v1/invoices/generate/{leaseId}`| `LANDLORD` | Generate monthly invoice with itemized line items |
| `GET` | `/api/v1/invoices` | `LANDLORD` | Filter invoices by status & billing month |
| `GET` | `/api/v1/invoices/my-bills` | `TENANT` | Tenant's own bills with breakdown |
| `GET` | `/api/v1/invoices/{id}` | `LANDLORD`, `TENANT` | Itemized invoice breakdown & payments |
| `POST` | `/api/v1/invoices/{id}/pay` | `TENANT` | Submit balance payment with validation |
| `GET` | `/api/v1/payments/my` | `TENANT` | Paginated tenant payment history |
| `POST` | `/api/v1/billing/estimate` | Authenticated | Live calculation using Strategy Factory |
| `GET` | `/api/v1/dashboard/landlord` | `LANDLORD` | Landlord analytical KPIs & revenue charts |
| `GET` | `/api/v1/dashboard/tenant` | `TENANT` | Resident dashboard with due bill countdown |

---

## 🧪 Testing

The test suite covers unit logic, design pattern invariant enforcement, and MockMvc integration tests using an in-memory **H2 PostgreSQL-compatibility mode** database:

```bash
# Run all unit and integration tests
mvn clean test

# Run full build with verification
mvn clean verify
```

### Test Suites Included:
1. `BillingStrategyTest`: Progressive slab boundaries (`0`, `100`, `101`, `300`, `301`, and `350` units), water pricing, commercial rates, and factory selection.
2. `LeaseStatePatternTest`: Verifies all legal transitions succeed, illegal operations throw `InvalidStateTransitionException`, and unit occupancy states toggle automatically.
3. `AuthControllerTest`: Self-registration, admin registration rejection, duplicate email detection, and JWT profile access.
4. `InvoiceControllerTest`: Invoice generation, duplicate month rejection (HTTP 409), tenant invoice access, and cross-tenant data isolation.
5. `DashboardControllerTest`: Role enforcement and access guard verification for `/dashboard/landlord` and `/dashboard/tenant`.
6. `RoleEnforcementTest`: Unauthenticated (401) and role violation (403) assertions on protected endpoints.
7. `FrontendSmokeTest`: Static resource availability and CSP/security header verification.

---

## 📸 Screenshots Showcase

| View | Description |
|---|---|
| **Public Landing & Bill Estimator** | Hero banner, GoF architecture grid, interactive live tariff calculator |
| **Landlord Analytics Dashboard** | KPI cards, 6-month revenue bar chart, portfolio occupancy donut chart |
| **Lease Lifecycle & Stepper** | Agreement management with strictly permitted action buttons and vertical audit timeline |
| **Tenant Portal & Bill Payment** | Next due bill alert with countdown, expandable itemized billing statements, and Pay Now modal |
| **Printable Invoice Receipt** | Formatted invoice statement with `@media print` layout |

---

## 👥 Team Work Distribution

| Roll No. | Role | Branch |
|----------|------|--------|
| **2207082** | Team Lead — Security & Infrastructure | `feature/auth-security` |
| **2207083** | Business Logic & Design Patterns | `feature/state-billing-patterns` |
| **2207084** | REST API, Tests & Frontend | `feature/rest-api-tests` |

See [`TEAM_WORK_DISTRIBUTION.md`](TEAM_WORK_DISTRIBUTION.md) for detailed member task assignments, branch naming conventions, commit logs, pull request review procedures, and the CI failure demonstration workflow.

