# Miembro — Membership Subscription Service

![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apachemaven)
![H2](https://img.shields.io/badge/Database-H2%20In--Memory-lightblue)
![License](https://img.shields.io/badge/License-MIT-yellow)

Miembro is a REST API for a tiered membership subscription system built with Java 17 and Spring Boot 3. Supports three membership plans (Monthly, Quarterly, Yearly) across three benefit tiers (Silver, Gold, Platinum) with full lifecycle management: subscribe, upgrade, downgrade, cancel, and renew.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [How It Works](#how-it-works)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Limitations](#limitations)
- [License](#license)

---

## Features

- **Membership Plans** — Monthly (1 month), Quarterly (3 months), Yearly (12 months), each with its own pricing
- **Tiers** — Silver, Gold, Platinum with progressively richer benefit sets
- **Benefits** — Free delivery, extra 10% discount, priority support; mapped per tier
- **Membership Lifecycle** — subscribe, upgrade, downgrade, cancel (with 7-day grace period), renew
- **User CRUD** — create, read, update, delete users
- **Catalog Endpoint** — browse all plan × tier × benefit combinations
- **Swagger UI** — interactive API docs at `/swagger-ui/index.html`
- **H2 Console** — inspect the in-memory database at `/h2-console`
- **Seed Data** — two users (Alice, Bob), all tiers, benefits, and 9 plans pre-loaded on startup

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.6 |
| Persistence | Spring Data JPA (Hibernate) |
| Database | H2 (in-memory) |
| Boilerplate reduction | Lombok |
| Input validation | Spring Validation |
| API docs | Springdoc OpenAPI 2.5.0 |
| Build tool | Maven |

---

## How It Works

1. **Data model** — Seven JPA entities: `User`, `Plan`, `Tier`, `Benefit`, `TierBenefit` (join), `Membership`, and `Transaction`. Plans embed a `defaultTier` reference so subscribing to a plan automatically assigns the matching tier.
2. **Subscription flow** — A `POST /membership/subscribe` call creates a `Membership` record linking user, plan, and tier with `startDate`, `expiryDate`, and `status = "active"`.
3. **Upgrade / Downgrade** — Updates the active membership's plan and tier in-place. Upgrades recalculate `expiryDate`; downgrades take effect immediately but keep the existing expiry.
4. **Cancel** — Sets `status = "cancelled"`, records `cancelledAt`, and adds a 7-day `gracePeriodUntil`.
5. **Renew** — Extends `expiryDate` and `nextRenewalAt` by the plan's duration and updates `lastRenewedAt`.
6. **Payment** — Mocked; all payment calls always succeed. No real payment integration.
7. **Seed data** — `data.sql` inserts two users, three tiers, three benefits, six tier-benefit mappings, and nine plans on every startup.

---

## Prerequisites

- Java 17 or newer
- Maven 3.x

---

## Installation

```bash
git clone https://github.com/archiskhuspe/membership-subscription-service.git
cd membership-subscription-service
mvn clean install
```

---

## Configuration

No environment variables are required for local development. The app uses an H2 in-memory database with default credentials.

`src/main/resources/application.yml` key settings:

| Setting | Value |
|---------|-------|
| JDBC URL | `jdbc:h2:mem:testdb` |
| Username | `sa` |
| Password | *(empty)* |
| DDL mode | `create-drop` (schema recreated on each start) |

---

## Usage

**Start the server:**

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

**Explore the API interactively:**

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`, Password: *(leave blank)*

**Test with Postman:**

Import `firstclub_membership_api.postman_collection.json` into Postman. The collection includes 28 pre-built requests covering all endpoints and edge cases (non-existent user, already-subscribed, etc.).

**Quick flow example (curl):**

```bash
# 1. Subscribe user 1 to plan 5 (Quarterly - Gold)
curl -X POST http://localhost:8080/membership/subscribe \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "planId": 5, "autoRenew": true}'

# 2. Check membership status
curl "http://localhost:8080/membership/status?userId=1"

# 3. Upgrade to plan 8 (Yearly - Gold)
curl -X PUT http://localhost:8080/membership/upgrade \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "targetPlanId": 8}'

# 4. Cancel membership
curl -X POST http://localhost:8080/membership/cancel \
  -H "Content-Type: application/json" \
  -d '{"userId": 1}'
```

---

## API Reference

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/membership/users` | Create a new user |
| `GET` | `/membership/users` | List all users |
| `GET` | `/membership/users/{id}` | Get user by ID |
| `PUT` | `/membership/users/{id}` | Update user by ID |
| `DELETE` | `/membership/users/{id}` | Delete user by ID |

### Plans & Tiers

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/membership/plans` | List all plans with pricing |
| `GET` | `/membership/tiers` | List all tiers with their benefits |
| `GET` | `/membership/tiers/{tierId}/benefits` | List benefits for a specific tier |
| `GET` | `/membership/catalog` | All plan × tier × benefit combinations |

### Membership Lifecycle

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/membership/subscribe` | Subscribe to a plan |
| `GET` | `/membership/status` | Get active membership status (`?userId=`) |
| `PUT` | `/membership/upgrade` | Upgrade to a different plan/tier |
| `PUT` | `/membership/downgrade` | Downgrade to a different plan/tier |
| `POST` | `/membership/cancel` | Cancel active membership |
| `POST` | `/membership/renew` | Renew membership by one plan duration |

---

## Project Structure

```
membership-subscription-service/
├── src/
│   ├── main/
│   │   ├── java/com/firstclub/membership/
│   │   │   ├── MembershipApplication.java
│   │   │   ├── controller/
│   │   │   │   └── MembershipController.java
│   │   │   ├── dto/                        # Request/response DTOs
│   │   │   ├── model/                      # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Plan.java
│   │   │   │   ├── Tier.java
│   │   │   │   ├── Benefit.java
│   │   │   │   ├── TierBenefit.java
│   │   │   │   ├── Membership.java
│   │   │   │   └── Transaction.java
│   │   │   ├── repository/                 # Spring Data JPA repositories
│   │   │   └── service/
│   │   │       ├── MembershipService.java
│   │   │       └── impl/
│   │   │           └── MembershipServiceImpl.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── data.sql                    # Seed data
│   └── test/
│       └── java/com/firstclub/membership/
│           ├── MembershipControllerTest.java
│           └── MembershipControllerFundamentalTest.java
├── firstclub_membership_api.postman_collection.json
├── pom.xml
└── README.md
```

---

## Limitations

- **In-memory database only** — all data is lost when the application restarts; H2 is not suitable for persistent storage.
- **Payment flows are mocked** — all payment and proration calls always succeed; no real payment gateway is integrated.
- **No authentication or authorization** — all endpoints are publicly accessible; the API is not suitable for deployment as-is.
- **No persistent transaction log** — the `Transaction` entity exists in the data model but is not populated by any service operation.
- **Proration is not calculated** — upgrade/downgrade responses include a `"Prorated amount applied."` string but no actual amount is computed.
- **Local demo only** — not intended for production use.

---

## License

Released under the [MIT License](LICENSE).
