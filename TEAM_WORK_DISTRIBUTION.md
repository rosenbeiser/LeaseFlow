# LeaseFlow — Team Work Distribution & Git / PR Protocol

This document outlines the collaborative engineering workflow, responsibilities, branching strategies, and pull request procedures used by the 3-member development team for **LeaseFlow: Smart Property Lease & Utility Billing Management System**.

---

## Team Members

| Roll No. | Git Identity | Email | Role |
|----------|-------------|-------|------|
| **2207082** | Student 2207082 | 2207082@leaseflow.dev | Team Lead — Security & Infrastructure |
| **2207083** | Student 2207083 | 2207083@leaseflow.dev | Business Logic & Design Patterns |
| **2207084** | Student 2207084 | 2207084@leaseflow.dev | REST API, Tests & Frontend |

---

## 1. Member Responsibilities & Branch Allocation

```
                                  ┌───────────────────────────┐
                                  │       master (Production) │
                                  └─────────────▲─────────────┘
                                                │ (Release merge)
                                  ┌─────────────┴─────────────┐
                                  │      develop (Integration)│
                                  └──────▲────────▲────────▲──┘
                                         │        │        │
                   ┌─────────────────────┘        │        └─────────────────────┐
                   │                              │                              │
         ┌─────────┴───────────────┐  ┌───────────┴────────────┐  ┌─────────────┴──────────┐
         │  feature/auth-security  │  │feature/state-billing-  │  │  feature/rest-api-tests │
         │  Roll: 2207082          │  │patterns Roll: 2207083  │  │  Roll: 2207084          │
         └─────────────────────────┘  └────────────────────────┘  └─────────────────────────┘
```

---

### Member 1 — Roll No. 2207082 (Team Lead: Security, Infrastructure, DevOps)

**Git Branch:** `feature/auth-security`  
**Git Email:** `2207082@leaseflow.dev`

**Core Domains:**
- Spring Security 6 configuration with stateless JWT authentication
- JWT token provider, authentication filter, entry points
- BCrypt password encoding and user principal
- CORS configuration (`CorsConfig.java`)
- Security constants registry (`SecurityConstants.java`)
- Docker containerization (Dockerfile, docker-compose.yml)
- GitHub Actions CI/CD pipeline
- Frontend foundation: API client, router, auth pages
- Security architecture documentation

**Key Commits (feature/auth-security):**
```
3c9933c  feat(security): add SecurityConstants registry and CorsConfig bean
843ccb9  docs(security): add security architecture documentation
a883d87  merge(feature/auth-security): integrate JWT security constants, CORS config, and security docs [2207082]
```

**Primary Files:**
- `src/main/java/com/leaseflow/security/SecurityConstants.java` ← New
- `src/main/java/com/leaseflow/config/CorsConfig.java` ← New
- `src/main/java/com/leaseflow/security/JwtTokenProvider.java`
- `src/main/java/com/leaseflow/security/JwtAuthenticationFilter.java`
- `src/main/java/com/leaseflow/security/JwtAuthenticationEntryPoint.java`
- `src/main/java/com/leaseflow/security/JwtAccessDeniedHandler.java`
- `src/main/java/com/leaseflow/security/UserPrincipal.java`
- `src/main/java/com/leaseflow/security/UserDetailsServiceImpl.java`
- `src/main/java/com/leaseflow/config/SecurityConfig.java`
- `src/main/java/com/leaseflow/controller/AuthController.java`
- `src/main/resources/static/js/api.js`
- `src/main/resources/static/js/auth.js`
- `src/main/resources/static/js/router.js`
- `src/main/resources/static/css/theme.css`
- `Dockerfile`
- `docker-compose.yml`
- `.github/workflows/ci-cd.yml`
- `docs/SECURITY.md` ← New

---

### Member 2 — Roll No. 2207083 (Business Logic, Database Architecture, Design Patterns)

**Git Branch:** `feature/state-billing-patterns`  
**Git Email:** `2207083@leaseflow.dev`

**Core Domains:**
- JPA entity design (User, Property, Unit, Lease, MeterReading, Invoice, Payment, etc.)
- Spring Data JPA repositories with custom JPQL queries
- State Pattern: LeaseState interface, all concrete state classes, LeaseStateFactory
- Strategy Pattern: BillingStrategy interface, all concrete pricing strategies, BillingStrategyFactory
- Business services: PropertyService, LeaseService
- DataSeeder with realistic demo data
- Landlord UI pages (dashboard, properties, leases, readings, invoices)
- Design patterns documentation

**Key Commits (feature/state-billing-patterns):**
```
10a2329  docs(patterns): enhance State and Strategy interfaces with lifecycle diagram and Javadoc
4cfc081  docs(patterns): add comprehensive design patterns documentation with UML diagrams
290bc77  merge(feature/state-billing-patterns): integrate enhanced pattern interfaces and design docs [2207083]
```

**Primary Files:**
- `src/main/java/com/leaseflow/pattern/state/LeaseState.java` ← Enhanced
- `src/main/java/com/leaseflow/pattern/state/DraftState.java`
- `src/main/java/com/leaseflow/pattern/state/ActiveState.java`
- `src/main/java/com/leaseflow/pattern/state/NoticeServedState.java`
- `src/main/java/com/leaseflow/pattern/state/TerminatedState.java`
- `src/main/java/com/leaseflow/pattern/state/SettledState.java`
- `src/main/java/com/leaseflow/pattern/state/LeaseStateFactory.java`
- `src/main/java/com/leaseflow/pattern/strategy/BillingStrategy.java` ← Enhanced
- `src/main/java/com/leaseflow/pattern/strategy/TieredElectricityPricingStrategy.java`
- `src/main/java/com/leaseflow/pattern/strategy/FlatRateWaterPricingStrategy.java`
- `src/main/java/com/leaseflow/pattern/strategy/CommercialUtilityStrategy.java`
- `src/main/java/com/leaseflow/pattern/strategy/BillingStrategyFactory.java`
- `src/main/java/com/leaseflow/entity/` (all entity files)
- `src/main/java/com/leaseflow/repository/` (all repository files)
- `src/main/java/com/leaseflow/service/PropertyService.java`
- `src/main/java/com/leaseflow/service/LeaseService.java`
- `src/main/java/com/leaseflow/service/DataSeeder.java`
- `src/main/resources/static/js/pages/landlordDashboard.js`
- `src/main/resources/static/js/pages/landlordProperties.js`
- `src/main/resources/static/js/pages/landlordLeases.js`
- `src/main/resources/static/js/pages/landlordReadings.js`
- `src/main/resources/static/js/pages/landlordInvoices.js`
- `docs/DESIGN_PATTERNS.md` ← New

---

### Member 3 — Roll No. 2207084 (REST API Layer, Tests, Frontend Tenant UI)

**Git Branch:** `feature/rest-api-tests`  
**Git Email:** `2207084@leaseflow.dev`

**Core Domains:**
- All REST controllers (Property, Lease, Invoice, MeterReading, Dashboard, Payment, Billing, User, Profile)
- DTOs with Bean Validation (request/response envelopes)
- Global exception handler (`@RestControllerAdvice`)
- Password change and profile update endpoints
- Comprehensive unit tests (BillingStrategy, State Pattern)
- MockMvc integration tests (Auth, Invoice, Dashboard, Role Enforcement)
- Frontend smoke test
- Tenant UI pages (dashboard, bills, payments)
- Invoice detail view with print support
- API reference documentation

**Key Commits (feature/rest-api-tests):**
```
d6bf704  feat(profile): add ProfileController with GET/PUT profile and password change endpoints
65406f8  docs(api): add comprehensive REST API reference documentation
0b84c9e  merge(feature/rest-api-tests): integrate ProfileController, enhanced UserService, and API docs [2207084]
```

**Primary Files:**
- `src/main/java/com/leaseflow/controller/ProfileController.java` ← New
- `src/main/java/com/leaseflow/controller/PropertyController.java`
- `src/main/java/com/leaseflow/controller/LeaseController.java`
- `src/main/java/com/leaseflow/controller/InvoiceController.java`
- `src/main/java/com/leaseflow/controller/MeterReadingController.java`
- `src/main/java/com/leaseflow/controller/DashboardController.java`
- `src/main/java/com/leaseflow/controller/PaymentController.java`
- `src/main/java/com/leaseflow/controller/BillingController.java`
- `src/main/java/com/leaseflow/exception/GlobalExceptionHandler.java`
- `src/main/java/com/leaseflow/dto/` (all DTO files)
- `src/main/java/com/leaseflow/service/UserService.java` ← Enhanced
- `src/test/java/com/leaseflow/pattern/strategy/BillingStrategyTest.java`
- `src/test/java/com/leaseflow/pattern/state/LeaseStatePatternTest.java`
- `src/test/java/com/leaseflow/controller/AuthControllerTest.java`
- `src/test/java/com/leaseflow/controller/InvoiceControllerTest.java`
- `src/test/java/com/leaseflow/controller/DashboardControllerTest.java`
- `src/test/java/com/leaseflow/controller/RoleEnforcementTest.java`
- `src/test/java/com/leaseflow/FrontendSmokeTest.java`
- `src/main/resources/static/js/pages/tenantDashboard.js`
- `src/main/resources/static/js/pages/tenantBills.js`
- `src/main/resources/static/js/pages/tenantPayments.js`
- `src/main/resources/static/js/pages/invoiceDetail.js`
- `docs/API_REFERENCE.md` ← New

---

## 2. Merged Feature Branches (Actual Git History)

```
*   0b84c9e  merge(feature/rest-api-tests)         [2207084]
|\ 
| * 65406f8  docs(api): add comprehensive REST API reference
| * d6bf704  feat(profile): add ProfileController with password change
|/ 
*   290bc77  merge(feature/state-billing-patterns)  [2207083]
|\ 
| * 4cfc081  docs(patterns): add comprehensive design patterns documentation
| * 10a2329  docs(patterns): enhance State/Strategy interfaces with Javadoc
|/ 
*   a883d87  merge(feature/auth-security)            [2207082]
|\ 
| * 843ccb9  docs(security): add security architecture documentation
| * 3c9933c  feat(security): add SecurityConstants and CorsConfig
|/ 
* 1e99c7b  fix: resolve Hibernate 6 enum JPQL, admin role, port config
* 1d7dc8c  fix(config): remove spring.profiles.active from profile yml
* ca5f0eb  feat: complete LeaseFlow implementation (full stack)
```

---

## 3. Git Commit Conventions

All commits follow the **Conventional Commits** specification:

| Prefix | Used For |
|--------|----------|
| `feat:` | New features or capabilities |
| `fix:` | Bug fixes |
| `docs:` | Documentation additions or changes |
| `test:` | Test additions or refactoring |
| `chore:` | Build, config, or dependency updates |
| `style:` | CSS or formatting changes |
| `merge:` | Branch merge commits |

---

## 4. CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/ci-cd.yml`) runs automatically on:
- Push to any branch
- Pull request to `develop` or `master`

### Pipeline Stages:

```
Push to branch
      │
      ▼
test-and-verify
  mvn clean verify
  (runs all 7 test classes on H2 in-memory DB)
      │
      ▼ (only on develop/master push)
docker-build
  docker build -t leaseflow .
  (validates multi-stage Docker build)
```

### CI Failure Demonstration Procedure:

To demonstrate the CI pipeline's regression protection:

1. Create branch: `git checkout -b test/ci-failure-demo`
2. In `BillingStrategyTest.java` line ~56, change `"350, 52.50"` to `"350, 52.00"`
3. Commit and push — the CI pipeline will fail with:
   ```
   BillingStrategyTest.testTieredElectricityBoundaries expected: <52.00> but was: <52.50>
   ```
4. Restore the value to `"350, 52.50"` and push — pipeline turns green ✅

---

## 5. GitHub Setup Instructions

### Push to GitHub:

```bash
# 1. Create a new repository on GitHub (do NOT initialize with README)
# 2. Add the remote:
git remote add origin https://github.com/YOUR_USERNAME/leaseflow.git

# 3. Push all branches and tags:
git push -u origin develop
git push origin master
git push origin feature/auth-security
git push origin feature/state-billing-patterns
git push origin feature/rest-api-tests
git push origin --tags
```

### Each member can verify their commits:
```bash
git log --author="2207082" --oneline
git log --author="2207083" --oneline
git log --author="2207084" --oneline
```
