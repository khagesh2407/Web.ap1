# COMPUCARE SERVICE — Native Android feature parity plan

## Product boundary

The existing AppDeploy web application remains live and unchanged:
https://compucare-service-sf3up4.v2.appdeploy.ai/

The Android application is a separate native client in this repository. It must not embed the website in a WebView or use a browser as its application UI. The system browser may be used only for the existing secure authentication redirect until a native identity flow is available.

Both clients share the existing backend APIs and Neon PostgreSQL records. Native screens must enforce the same server-side roles; hiding a menu item is not authorization.

## Source audit (web source snapshot 1794050374567)

The current web client defines these 21 modules:

| Module | Data collection / backend | Native acceptance |
|---|---|---|
| Overview | /api/bootstrap, /api/tickets, /api/operations | Role-specific dashboard, live counts, offline snapshot |
| Tickets | /api/tickets; POST /api/tickets; PUT /api/tickets/:id/status | Search/filter, detail/timeline, create, permitted status changes, assignment and engineer charges |
| Customers | operations.customers | Customer directory and customer detail, role-gated |
| Engineers | operations.engineers; action engineer.create | Roster, skills, coverage, workload, add/edit as supported |
| Schedule | operations.appointments; action schedule.create | Appointment list/detail and scheduling |
| Estimates | operations.estimates; actions estimate.create, estimate.decision | Estimate detail, line items, approval/rejection |
| Invoices & payments | operations.invoices; action invoice.create | Invoice detail, issue and due status |
| Payments | operations.payments; action payment.record | Receipt capture and payment history |
| Inventory | operations.parts; actions part.create, inventory.adjust | Stock ledger, available/reserved quantities, adjustments |
| Procurement | operations.purchaseOrders; action purchase.create | Purchase orders and expected deliveries |
| B2B accounts | operations.organizations; action organization.create | Organization accounts and contacts |
| AMC contracts | operations.amcContracts; action amc.create | Contract coverage, visits, renewals |
| SLA policies | operations.slaPolicies; action sla.create | Response/resolution policy management |
| B2B agreements | operations.agreements | Agreement detail and lifecycle |
| Partner network | operations.partners; actions partner.create, partner.verify | Partner onboarding, verification and coverage |
| Marketplace | operations.listings; action listing.create | Listing browse/create and lifecycle |
| Marketplace bids | operations.bids; actions bid.create, bid.decision | Proposals, review and decisions |
| Warranty claims | operations.warranties; actions warranty.create, warranty.update | Claim registration and status |
| Service regions | operations.areas; action service_area.create | Region and postal coverage management |
| Analytics | operations.metrics and returned datasets | Native metrics, filters and accessible charts |
| Email notifications | operations.emailDeliveries; action email.test | Delivery log, failure detail; test email admin-only |

## API contracts confirmed in backend source

- GET /api/health
- GET /api/bootstrap (authenticated; returns userId, email, name, role and support contacts)
- GET /api/services (authenticated)
- GET /api/tickets (role-scoped)
- POST /api/tickets (customer/staff role rules and server-side validation)
- PUT /api/tickets/:id/status (role- and status-scoped)
- GET /api/operations (staff/engineer; role-scoped collections)
- POST /api/operations (action-based, server-side role checks)
- POST /api/public/product-enquiry (public product enquiry)
- POST /api/native-auth/handoff and POST /api/native-auth/exchange (short-lived one-time auth handoff)

Important: the web source's operation read model aggregates the above module collections in GET /api/operations. The Android client should not invent direct database access or bypass these APIs.

## Role-based navigation from web source

- super_admin, administrator: all 21 modules
- dispatcher: Overview, Tickets, Engineers, Schedule, Service regions, Partner network, Marketplace, Marketplace bids
- support_agent: Overview, Tickets, Customers, Warranty claims
- inventory_manager: Overview, Inventory, Procurement
- finance_user: Overview, Customers, Estimates, Invoices & payments, Payments, AMC contracts
- engineer: Overview, Tickets, Schedule
- customer: Overview, Tickets

The server remains authoritative for permissions.

## Offline policy

Offline-capable local data:
- Last successful profile, role and permitted module snapshot.
- Previously synchronized tickets and ticket detail/timeline.
- Draft service requests, product enquiries and other explicitly safe create operations.
- Engineer status updates only where role, assignment and status transition are validated by the server on sync.
- Read-only snapshots for other modules where privacy and role constraints permit.

Connectivity-required:
- Initial sign-in and expired-session renewal.
- Payment capture/reconciliation, email delivery, role changes, partner verification, financial approvals, stock reservation/adjustment and other sensitive actions unless a specific idempotent offline-safe workflow is designed and approved.
- Any action that requires current server-side inventory, finance or authorization state.

All queued mutations require stable idempotency keys, visible pending/failed/conflict states, retry/backoff, and server-side validation. Do not silently discard pending work. Do not show a queued operation as completed until server acknowledgement.

## Delivery sequence

1. Baseline and preserve the current website; do not deploy Android changes to AppDeploy.
2. Audit each web route, action payload, role and response shape against backend/index.ts.
3. Establish native app architecture: repository/API layer, secure auth, Room database, WorkManager sync, navigation, design system, role-aware feature modules.
4. Port Core Service Desk: authentication, overview, ticket list/detail/timeline, ticket creation, assignment/status workflow, customer and engineer views.
5. Port Finance and Inventory: estimates, invoices, payments, stock and procurement with strict online validation for sensitive actions.
6. Port Business and Partner modules: B2B accounts/agreements, AMC, SLA, partner network, marketplace, bids, warranties and service regions.
7. Port analytics, notifications and account settings.
8. Test role matrix, API contract, offline queue/retry/conflict behavior, data isolation, accessibility, Android API compatibility and emulator/device flows.
9. Build debug QA APK, then production APK/AAB signed only with the owner's release key. Never commit signing keys or credentials.

## Current implementation status (honest baseline)

The current Android app is an early native prototype. It has native views (no WebView), secure browser-based sign-in handoff, basic ticket list/create/status, product enquiry, local ticket cache/outbox and WorkManager sync. It does **not** yet have full web feature parity. The modules above must be implemented and tested before calling the native app complete.
