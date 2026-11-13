# TrainReservation frontend

Responsive React/Vite client for the supported TrainReservation REST APIs.

## Run locally

1. Start the backend on `http://localhost:8080`.
2. Copy `.env.example` to `.env.local`; leave `VITE_API_BASE_URL` empty for the local Vite proxy, or set it to the API origin for a deployment with backend CORS configured.
3. Run `npm install`, then `npm run dev`.

The client uses session-scoped bearer-token storage. `sessionStorage` is readable by JavaScript and is not an XSS defense. The Vite development server proxies `/api` to `http://localhost:8080`, avoiding cross-origin requests in local development. A deployed frontend using a separate API origin requires backend CORS configuration. Never put passwords, database credentials, or JWT signing secrets in a `VITE_*` variable; Vite exposes those values to browsers.

## Implemented integrations

- Registration and sign-in; API access tokens are held in `sessionStorage` and attached as bearer tokens.
- Public train search, train details, and class-specific availability/fare.
- Authenticated booking history, PNR lookup, and cancellation requests.
- Admin train, station, and train-route management, guarded in the UI by the role returned at sign-in. The backend remains responsible for authorization.
- API errors and unavailable backend capabilities are displayed rather than replaced with mock data.

## Backend contract limitations

- Train search and availability responses now include the matching `trainRunId`, which is required by `POST /api/bookings`. The client sends that ID with the passenger list. A backend deployment must include this additive API update for booking submission to work.
- There is no public station lookup endpoint. Search accepts a station code or numeric station ID typed by the user.
- There is no user profile read/update endpoint. The profile screen only displays the user information returned at sign-in.
- Admin schedule management, all-bookings, user-list, and dashboard-statistics APIs are not present. Their screens identify the missing APIs and display no fabricated data.
- Cancellation responses with a `REQUESTED` refund status are presented as pending; they are not described as paid or processed.

The current backend creates bookings with payment pending; no payment-processing endpoint is available, so the frontend does not simulate payment.

## Validation

- `npm run build`
- `npm run lint`

PostgreSQL-backed backend tests are separate from these frontend checks.
