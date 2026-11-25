# Design Patterns — LeaseFlow

> **Author:** Student 2207083  
> **Domain:** Business Logic, Design Patterns, Data Layer

---

## Overview

LeaseFlow implements two classic Gang-of-Four design patterns that are explicitly
required by the CSE 3220 course: the **State Pattern** for lease lifecycle management
and the **Strategy Pattern** for utility billing calculations.

---

## 1. State Pattern — Lease Lifecycle

### Problem

A lease passes through a fixed set of states: `DRAFT → ACTIVE → NOTICE_SERVED → TERMINATED → SETTLED`.
Not every action is valid in every state. For example:
- You cannot terminate a `DRAFT` lease (it was never active).
- You cannot generate an invoice for a `TERMINATED` lease.
- You cannot activate a lease that is already `ACTIVE`.

Without a formal pattern, service code ends up with long `if/else` chains scattered
across multiple methods — fragile, hard to test, easy to get wrong.

### Solution

Each state is encapsulated in its own class implementing the `LeaseState` interface:

```
<<interface>>
LeaseState
  + activate(lease, actor): Lease
  + serveNotice(lease, actor): Lease
  + terminate(lease, actor): Lease
  + generateInvoice(lease, actor): Lease
  + settle(lease, actor, damages, notes): Lease
  + getAllowedActions(): List<String>

DraftState          ActiveState         NoticeServedState
  activate ✓          serveNotice ✓       terminate ✓
  others → throw      terminate ✓         others → throw
                      generateInvoice ✓

TerminatedState     SettledState
  settle ✓            all → throw
  others → throw
```

### State Transition Diagram

```
                   activate()
       ┌─ DRAFT ─────────────────► ACTIVE ─────────────────────────┐
       │                              │                             │
       │                   serveNotice()                            │
       │                              ▼                             │
       │                       NOTICE_SERVED                        │
       │                              │                             │
       │                       terminate()                          │
       │                              ▼                             │
       └────────────────────────► TERMINATED ──settle()──► SETTLED  │
                                      ▲                             │
                                      └─────────terminate()─────────┘
```

### Key Files

| File | Responsibility |
|------|---------------|
| `LeaseState.java` | Interface — defines all operations + `getAllowedActions()` |
| `DraftState.java` | Allows only `activate()` |
| `ActiveState.java` | Allows `serveNotice()`, `terminate()`, `generateInvoice()` |
| `NoticeServedState.java` | Allows `terminate()`, `generateInvoice()` |
| `TerminatedState.java` | Allows only `settle()` |
| `SettledState.java` | Terminal state — no transitions allowed |
| `LeaseStateFactory.java` | Creates the correct state object from a `LeaseStatus` enum |

---

## 2. Strategy Pattern — Utility Billing

### Problem

Different property types and utility types use different pricing formulas:
- Residential electricity: tiered pricing (lower rate for first 200 kWh, higher after)
- Residential water/gas: flat rate per unit
- Commercial (all utilities): single negotiated rate per unit

Combining all these rules in one class violates the Open/Closed Principle and makes
adding a new utility type a surgery on existing, working code.

### Solution

Each pricing algorithm is a separate class implementing `BillingStrategy`:

```
<<interface>>
BillingStrategy
  + calculate(unitsConsumed, ratePerUnit): BigDecimal
  + getStrategyName(): String

TieredElectricityPricingStrategy   FlatRateWaterPricingStrategy   CommercialUtilityStrategy
  Residential electricity             Residential water/gas           All commercial utilities
  First 200 kWh @ base rate           Flat rate × units consumed      Flat rate × units
  After 200 kWh @ base rate × 1.5
```

### Pricing Rules

| Strategy | Rule | Example |
|----------|------|---------|
| `TieredElectricityPricingStrategy` | First 200 kWh at base rate; excess at base × 1.5 | 350 kWh @ ৳10 → (200×10) + (150×15) = ৳4,250 |
| `FlatRateWaterPricingStrategy` | Units × base rate, flat | 30 m³ @ ৳25 → ৳750 |
| `CommercialUtilityStrategy` | Units × base rate, flat | 1,000 kWh @ ৳12 → ৳12,000 |

### Key Files

| File | Responsibility |
|------|---------------|
| `BillingStrategy.java` | Interface — `calculate()` + `getStrategyName()` |
| `TieredElectricityPricingStrategy.java` | Tiered electricity pricing |
| `FlatRateWaterPricingStrategy.java` | Flat-rate water/gas pricing |
| `CommercialUtilityStrategy.java` | Commercial flat-rate pricing |
| `BillingStrategyFactory.java` | Selects strategy from `PropertyType` + `UtilityType` |

---

## 3. How the Patterns Interact

When a landlord generates an invoice:

```
1. LeaseService.generateInvoice(leaseId, actorUser)
      │
2.    ▼  State Pattern: delegates to current LeaseState
   leaseState.generateInvoice(lease, actor)
      │
3.    ▼  ActiveState (or NoticeServedState) validates the transition
   InvoiceService.generateForLease(lease)
      │
4.    ▼  Strategy Pattern: picks pricing algorithm
   BillingStrategyFactory.getStrategy(propertyType, utilityType)
      │
5.    ▼  Concrete strategy calculates the charge
   strategy.calculate(unitsConsumed, ratePerUnit)
      │
6.    ▼  Invoice + InvoiceLineItems persisted
   Invoice saved with status PENDING
```
