# RailYatra
<h2 align="center">TrainReservation System</h2>

A railway reservation system built as a Java/Spring Boot REST API with a React/Vite web client. The application supports authentication, train search and availability, user bookings and cancellations, and administrator management of stations, trains, and train routes.

> **Implementation status:** The frontend and backend are in active development. Payment processing, schedule/run administration, coach/seat administration, public station lookup, profile editing, and several admin reporting APIs are not implemented. See [Known limitations](#known-limitations) before using the application.

## Contents

- [Features](#features)
- [Technology stack](#technology-stack)
- [Repository structure](#repository-structure)
- [Architecture](#architecture)
- [Data model and business rules](#data-model-and-business-rules)
- [Prerequisites](#prerequisites)
- [Configuration and local setup](#configuration-and-local-setup)
- [API reference](#api-reference)
- [Frontend pages](#frontend-pages)
- [Testing and verification](#testing-and-verification)
- [Security and deployment notes](#security-and-deployment-notes)
- [Known limitations](#known-limitations)

## Features

### User capabilities

- Register an account and sign in using email and password.
- Search trains by source station, destination station, and journey date.
- View train details and ordered route stops.
- Check class-specific seat availability and estimated fare.
- Create a booking with passenger details when a matching train run is available.
- View personal booking history and retrieve booking details by PNR.
- Request cancellation before the source departure and view the cancellation/refund-request status.

### Administrator capabilities

- Create, list, search, update, and deactivate stations.
- Create, list, search, update, and deactivate trains.
- Create and manage the ordered station stops for a train route.
- View a small overview of train and station counts in the frontend.

Station and train deactivation are soft deactivations; the APIs do not physically delete those records or their historical relationships.

## Technology stack

| Area | Technology |
|---|---|
| Backend language | Java 17 |
| Backend framework | Spring Boot 4.1.2 |
| HTTP API | Spring Web MVC |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 18 (configured for `trainreservation_db`) |
| Authentication | Spring Security, JWT (HMAC-SHA256), BCrypt |
| Validation | Jakarta Bean Validation |
| Backend build | Maven Wrapper |
| Frontend | React 19, React Router, Vite 8 |
| Frontend styling | Tailwind CSS |
| Frontend HTTP client | Axios |
| Backend tests | JUnit 5, Spring test support, Mockito |

## Repository structure

```text
TrainReservation/
├── backend/
│   ├── src/main/java/com/trainreservation/backend/
│   │   ├── controller/       # REST controllers
│   │   ├── dto/request/      # Validated API requests
│   │   ├── dto/response/     # API response DTOs
│   │   ├── entity/           # JPA entities
│   │   ├── enums/            # Roles, statuses, train types/classes
│   │   ├── exception/        # Domain exceptions and API error handling
│   │   ├── repository/       # Spring Data repositories
│   │   ├── security/         # JWT, authentication, authorization
│   │   └── service/          # Business interfaces and implementations
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── src/test/java/        # Unit, controller, and PostgreSQL tests
│   ├── mvnw
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   │   ├── auth/             # Session state and route guards
│   │   ├── components/       # Shared layout and UI components
│   │   ├── lib/              # API client and formatting helpers
│   │   └── pages/            # User and administrator pages
│   ├── .env.example
│   ├── package.json
│   └── vite.config.js
└── README.md
```

## Architecture

The backend follows a layered structure:

1. **Controllers** bind REST routes, validate request DTOs, and return the standard API envelope.
2. **Services** own business rules, transaction boundaries, authorization checks that depend on the requester, and response mapping.
3. **Repositories** perform database queries and locking.
4. **Entities** model persistence relationships and database constraints.
5. **DTOs** keep API contracts separate from JPA entities.
6. **Security** authenticates bearer JWTs and protects endpoints by role.

The frontend is a single-page React application. It calls the backend through an Axios client, attaches the bearer token for authenticated requests, uses route guards to improve navigation, and displays API errors. Route guards are a user-experience measure only; backend authorization is authoritative.

Backend responses use an envelope shaped like:

```json
{
  "success": true,
  "message": "Trains retrieved successfully",
  "data": []
}
```

## Data model and business rules

The main entities are:

- **User** — unique email, profile details, BCrypt password hash, and `USER` or `ADMIN` role.
- **Station** — unique station code, name, city/state, and active state.
- **Train** — unique train number, name, type, and active state.
- **TrainStop** — ordered station stops on a train route, with stop/day numbers, times, and distance from source.
- **TrainClass** — class code/name and base fare for a train.
- **TrainRun** — a train on a journey date with a running status; a train/date pair is unique.
- **Coach / Seat** — seat inventory associated with a train run and class.
- **Booking / Passenger / Payment / Cancellation** — reservation, passenger, payment-status, and cancellation records.
- **SeatAllocation** — seat occupancy by train run and route segment.

Important behavior:

- Search and availability responses include the matching `trainRunId`; booking creation uses that ID together with the route, date, class, and passengers.
- Journey dates in the past are rejected. A train that has already departed from the requested source is not returned as bookable.
- Route order and segment availability are checked on the backend, not trusted to the browser.
- A database uniqueness constraint protects each `(train run, seat, segment)` allocation. A seat can be reused for a non-overlapping journey segment.
- Fare is `class base fare + journey distance × configured per-kilometre rate`, rounded to two decimal places. The default rate is `1.00` per kilometre.
- New bookings are created with booking and payment status `PENDING`. No payment gateway or payment-processing endpoint is implemented.
- Cancellation is permitted only for eligible pending/confirmed bookings before the scheduled source departure. The default refund quote is 80% of the fare. Cancellation sets the refund status to `REQUESTED` and releases seat allocations; it does **not** process a monetary refund.

## Prerequisites

- Java 17.
- PostgreSQL 18 running locally or reachable from the backend.
- Node.js `^20.19.0` or `>=22.12.0` and npm (required by the installed Vite 8 version).
- The included Maven Wrapper; a separate Maven installation is not required.

The configured database is:

```text
Database: trainreservation_db
URL:      jdbc:postgresql://localhost:5432/trainreservation_db
Username: postgres (default; configurable)
Password: supplied through DB_PASSWORD
```

Create the database if it does not already exist. The application currently uses Hibernate `ddl-auto=update` to update the schema at startup; review this setting before production use and use a controlled migration process for production databases.

## Configuration and local setup

### 1. Configure and start the backend (PowerShell)

From the project root, configure the database credentials in the current terminal. Do not commit credentials or place them in source files:

```powershell
Set-Location .\backend
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<your-local-postgres-password>"
```

`JWT_SECRET` is required. It must be a Base64-encoded key of at least 32 bytes. Generate a fresh development key in the same terminal:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Optional backend settings and defaults:

| Environment variable | Purpose | Default |
|---|---|---|
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | Required |
| `JWT_SECRET` | Base64 HMAC signing key, at least 32 decoded bytes | Required |
| `JWT_ISSUER` | JWT issuer | `https://trainreservation.local` |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds | `3600000` (1 hour) |
| `FARE_PER_KM_RATE` | Fare rate per kilometre | `1.00` |
| `CANCELLATION_REFUND_PERCENTAGE` | Refund quote percentage as a decimal | `0.80` |

Start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`.

### 2. Configure and start the frontend

In a second terminal:

```powershell
Set-Location .\frontend
npm install
Copy-Item .env.example .env.local
npm run dev
```

The Vite development server normally runs on `http://localhost:5173` and proxies `/api` requests to `http://localhost:8080`. The empty `VITE_API_BASE_URL` in `.env.local` uses this proxy. For a separately hosted frontend/API, set `VITE_API_BASE_URL` to the API origin and configure backend CORS or a same-origin reverse proxy.

Never put database credentials or JWT signing keys in a `VITE_*` variable; Vite variables are delivered to the browser.

### 3. Initial data and administrator access

Public registration always creates a `USER` account. There is no public admin-registration endpoint or admin bootstrap script. Provision an `ADMIN` account through an approved, secure server-side process.

Train search and booking require data for stations, train routes, train classes, train runs, coaches, and seats. Admin APIs currently manage stations, trains, and routes, but do not manage schedules/runs or seat inventory. Those records must be provisioned by an existing trusted process before search, availability, and booking can be exercised.

## API reference

All endpoints are under `/api`. Admin endpoints require an authenticated `ADMIN` token. Unless marked public, endpoints require a valid bearer JWT.

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a `USER`; returns `201 Created`. |
| `POST` | `/api/auth/login` | Public | Authenticate and return access token and user details. |

Registration request:

```json
{
  "fullName": "Asha Kumar",
  "email": "asha@example.com",
  "phoneNumber": "+91 98765 43210",
  "password": "replace-with-a-strong-password"
}
```

Login request:

```json
{
  "email": "asha@example.com",
  "password": "replace-with-a-strong-password"
}
```

### Train search and availability

These `GET /api/trains/**` endpoints are public.

| Method | Endpoint | Query parameters | Description |
|---|---|---|---|
| `GET` | `/api/trains/search` | `source`, `destination`, `date`, optional `classCode` | Search matching train runs. Source/destination accept station codes or station IDs. |
| `GET` | `/api/trains/{trainId}` | — | Get train details and route stops. |
| `GET` | `/api/trains/{trainId}/availability` | `source`, `destination`, `date`, `classCode` | Get segment availability, fare, and train-run ID. |

Supported class codes are `SL`, `3A`, `2A`, `1A`, `CC`, and `EC`. Search results include matching class options; availability is checked for a selected class.

Example:

```text
GET /api/trains/search?source=NDLS&destination=CNB&date=2026-12-01
GET /api/trains/10/availability?source=NDLS&destination=CNB&date=2026-12-01&classCode=SL
```

### Bookings

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/bookings` | Authenticated `USER` | Create a booking; returns `201 Created`. |
| `GET` | `/api/bookings/my-bookings?page=0&size=20` | Authenticated user | Return the requester's own paginated booking history. |
| `GET` | `/api/bookings/{pnr}` | Owner or `ADMIN` | Retrieve booking and passenger details by PNR. |
| `POST` | `/api/bookings/{pnr}/cancel` | Owner or `ADMIN` | Cancel an eligible booking and request a refund quote. |

Booking request:

```json
{
  "trainRunId": 1001,
  "sourceStation": "NDLS",
  "destinationStation": "CNB",
  "journeyDate": "2026-12-01",
  "classCode": "SL",
  "passengers": [
    {
      "fullName": "Asha Kumar",
      "age": 28,
      "gender": "FEMALE"
    }
  ]
}
```

Use the `trainRunId` returned by search or availability; the numeric ID above is illustrative. Passenger genders are `FEMALE`, `MALE`, or `OTHER`.

### Administrator: stations

All endpoints below require `ADMIN`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/admin/stations` | Create a station. |
| `GET` | `/api/admin/stations?page=0&size=10&sortBy=stationName&direction=asc` | Paginated station list. |
| `GET` | `/api/admin/stations/search?keyword=Delhi` | Partial station name/code/city search. |
| `GET` | `/api/admin/stations/{id}` | Get one station. |
| `PUT` | `/api/admin/stations/{id}` | Update station details. |
| `PATCH` | `/api/admin/stations/{id}/deactivate` | Deactivate without deleting historical route references. |

### Administrator: trains

All endpoints below require `ADMIN`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/admin/trains` | Create train identity details. |
| `GET` | `/api/admin/trains?page=0&size=10&sortBy=trainName&direction=asc` | Paginated train list; optional `active=true` or `active=false`. |
| `GET` | `/api/admin/trains/search?keyword=Rajdhani&page=0&size=10` | Partial train number/name search. |
| `GET` | `/api/admin/trains/{id}` | Get one train. |
| `PUT` | `/api/admin/trains/{id}` | Update train identity details. |
| `PATCH` | `/api/admin/trains/{id}/deactivate` | Deactivate without deleting historical records. |

Train types are `EXPRESS`, `SUPERFAST`, `PASSENGER`, `MAIL`, `RAJDHANI`, `SHATABDI`, `DURONTO`, and `OTHER`.

### Administrator: routes

All endpoints below require `ADMIN`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/admin/trains/{trainId}/route` | Create a route with its ordered stops. |
| `GET` | `/api/admin/trains/{trainId}/route` | Retrieve route stops. |
| `PUT` | `/api/admin/trains/{trainId}/route` | Replace/update the route stop list. |
| `POST` | `/api/admin/trains/{trainId}/route/stops` | Append a route stop. |
| `PUT` | `/api/admin/trains/{trainId}/route/stops/{stopId}` | Update one stop. |
| `DELETE` | `/api/admin/trains/{trainId}/route/stops/{stopId}` | Remove one stop. |

Route edits are validated by the backend and may be rejected when existing train runs depend on the route.

## Frontend pages

| Path | Purpose |
|---|---|
| `/` | Landing page and train search form. |
| `/register`, `/login` | Account registration and sign-in. |
| `/trains/search` | Train results and class availability/fare. |
| `/trains/:trainId` | Train details and route stops. |
| `/booking/start` | Passenger form and booking submission when a run ID is available. |
| `/dashboard`, `/bookings`, `/bookings/:pnr` | User summary, history, booking details, and cancellation. |
| `/pnr` | PNR lookup form. |
| `/profile` | Read-only account details from the sign-in response. |
| `/admin` | Admin overview and supported management links. |
| `/admin/trains`, `/admin/stations`, `/admin/routes` | Supported admin management screens. |
| `/admin/schedules`, `/admin/bookings`, `/admin/users` | Explicitly unavailable until backend APIs are implemented. |

The frontend accepts station codes or numeric IDs in train search because no public station lookup endpoint exists.

## Testing and verification

### Backend

From `backend`:

```powershell
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

The PostgreSQL integration tests use the configured PostgreSQL database directly; the project does not include H2 as a fallback. Ensure PostgreSQL is running, the database exists, and `DB_USERNAME`/`DB_PASSWORD` are correct before running the complete suite. Unit and controller tests can be run without a live PostgreSQL server, depending on the selected test classes.

Existing test areas include controller/security tests, service tests, repository tests, and PostgreSQL integration tests for station/train/route management, booking creation, and seat allocation.

### Frontend

From `frontend`:

```powershell
npm run lint
npm run build
npm run preview
```

There is currently no dedicated frontend unit-test or end-to-end-test script in `package.json`. A successful frontend build/lint does not verify connectivity to PostgreSQL or the live backend.

## Security and deployment notes

- Passwords are hashed with BCrypt and are not included in user response DTOs.
- JWT signing keys are read from `JWT_SECRET`; the key must be Base64 encoded and decode to at least 32 bytes.
- Admin endpoints are protected by backend `ADMIN` role checks. Frontend navigation checks do not replace backend authorization.
- Booking creation is restricted to `USER`; users can only access their own bookings. Admins may inspect/cancel bookings through the PNR endpoints.
- The frontend stores access tokens in `sessionStorage`. JavaScript can read this storage; it is not an XSS defense. Apply normal XSS prevention practices and serve the frontend over HTTPS in production.
- The Vite proxy is for local development. For deployment, use a reverse proxy/same-origin architecture or configure a strict backend CORS allowlist for the frontend origin.
- `spring.jpa.show-sql=true` and `spring.jpa.hibernate.ddl-auto=update` are present in the current application properties. Review both for production; schema evolution should be managed deliberately.
- Never commit `.env.local`, passwords, signing keys, or production secrets.

## Known limitations

- **Payment:** Bookings are created with payment status `PENDING`. There is no payment gateway, payment confirmation endpoint, or automated refund processing.
- **Schedules and seat inventory:** There are no admin APIs for train runs/schedules, coaches, or seats. These are prerequisites for end-to-end search and booking.
- **Stations for public search:** No public station list/search endpoint exists; users must enter a station code or station ID.
- **User profile:** No `/api/users/me` read/update endpoint exists. The frontend displays the sign-in response as a read-only profile.
- **Admin reporting:** There are no admin APIs for all bookings, user listing, or dashboard statistics. The frontend does not fabricate this data.
- **Production CORS:** Backend CORS is not configured in the current security configuration; configure it if the frontend calls a separate API origin directly.
- **Database migration:** Hibernate schema auto-update is configured; there is no versioned database migration tool or migration history in the current project.
