# idm-auth-gateway — Architecture Decisions

## Context

Building a new cloud-native IAM platform (IDM) for FBDMS Digital Experience Suite. The platform is split into 6
services. This service is **idm-auth-gateway** (IDM-1) — the OAuth2/OIDC authorization server and authentication gateway
that serves as the entry point to every product in the Digital Experience Suite.

---

## Decision 1 — Token Engine: Ory Hydra

Rather than building an OAuth2/OIDC server from scratch, idm-auth-gateway will be built on top of **Ory Hydra** as the
token engine.

Hydra is:

- Go-based, cloud-native, horizontally scalable
- OpenID Certified, OAuth2.1 compliant
- Headless by design — owns token issuance, not user management or UI
- Trusted at scale (OpenAI runs their OAuth2 infrastructure on it)
- Deployable on AWS, containerised

Keycloak and ZITADEL were evaluated. Hydra was chosen for its clean bounded context alignment, performance ceiling, and
architectural fit with the multi-service design.

---

## Decision 2 — Login App and Consent App Owned by IDM-1

Hydra is headless by design — it handles token issuance but delegates the actual login UI and consent screen to an
external application via a redirect-based challenge/response contract. This means a Login App and Consent App are
required for Hydra to function at all.

This was a gap in the original idm-auth-gateway brief, which described it as owning the protocol endpoints but did not
account for these components. The gap was surfaced when Hydra was chosen as the engine.

The Login App and Consent App are **not** the Admin Console (idm-admin-console / IDM-4), which is a post-login
back-office tool for tenant administrators. The Login App is the end-user facing login screen used by all SaaS products.

Both are assigned to **idm-auth-gateway** as first-class components, not separate services. Keeping them in
idm-auth-gateway is the right boundary because they are tightly coupled to the Hydra auth flow — they call Hydra's admin
API directly and own the claim injection logic.

---

## Decision 3 — Multi-Tenant Token Model: Tenant Selector

Two models were considered for users belonging to multiple tenants:

**Rejected — Multi-tenant token:** A single token carries all tenant memberships and roles. No selector step at login.
Rejected because: token size grows with tenant count (JWTs are sent on every request), every downstream SaaS app must
understand and filter the multi-tenant claim structure, a single compromised token exposes all tenant contexts
simultaneously, and it violates least-privilege by carrying permissions irrelevant to the current operation.

**Chosen — Tenant selector:** At login, the user selects which tenant context to operate in. The token is scoped to that
single tenant.

- Token is clean and least-privilege — carries only what is needed for the selected tenant
- Switching tenants uses silent re-auth (`prompt=none`) — no password re-entry, seamless UX
- Downstream SaaS apps receive simple, single-tenant tokens with no filtering logic required
- Security blast radius of a compromised token is limited to one tenant

---

## Decision 4 — Implementation Language: Java / Spring Boot

The Login App and Consent App will be built in **Java with Spring Boot**. Hydra is deployed as a Go binary — the team
does not write Go.

Reasons:

- Team has deep Java/Spring Boot experience — correct choice for a security-critical component
- Spring Boot's HTTP client handles Hydra admin API calls cleanly
- No architectural compromise — Hydra communicates over HTTP and is language-agnostic
- Portable, no cloud-specific dependencies in application code

---

## Decision 5 — Frontend: Angular/TypeScript Bundled in Spring Boot JAR

The Login App frontend (login form, tenant selector, MFA screens) will be built in **Angular/TypeScript**, bundled into
the Spring Boot JAR at build time.

- Angular build output placed in `src/main/resources/static` — served by Spring Boot automatically
- Angular calls the Java backend via REST on the same origin — no CORS complexity
- Single deployable: one container image, one pipeline, one artifact
- Local development: Angular dev server proxies `/api` calls to Spring Boot

Security is not a concern with this approach provided the Hydra admin port (4445) is never exposed externally and no
secrets are baked into the Angular bundle.

---

## Decision 6 — Vendor Lock-in Strategy: Interface/Impl + Spring Cloud

**KMS and secrets — Interface/Impl pattern**

Runtime cryptographic operations are abstracted behind interfaces (`KeyManagementService`, `SecretsService`) with
provider-specific implementations (AWS KMS, HashiCorp Vault, local/env for dev). Spring `@ConditionalOnProperty` wires
the correct implementation at startup. Moving between providers is a config change — zero code change.

**Application startup secrets — Spring Cloud**

Spring Cloud Vault / Spring Cloud AWS populates Spring's `Environment` at startup. Application code uses `@Value` and
`application.yml` — the secrets provider is invisible to the code.

**Hydra secrets — Init container / entrypoint script**

Hydra cannot use Spring Cloud. Its secrets are injected via environment variables populated by a provider-agnostic
entrypoint script or Vault Agent init container before Hydra starts. Provider is switched via a `SECRETS_PROVIDER`
environment variable.

---

## Decision 7 — Monorepo Structure

idm-auth-gateway is structured as a monorepo containing all components: Hydra config/scripts, Login App, Consent App,
shared interfaces (`idm-common`), and local dev mocks.

Reasons for monorepo over separate repos:

- `idm-common` defines the interfaces (`IdentityDirectoryService`, `MfaService`, `KeyManagementService`) that are
  consumed by both the Login App and Consent App. A monorepo allows these to be built and versioned together without a
  separate publish/consume cycle.
- All idm-auth-gateway components are deployed together as a unit — there is no scenario where Login App ships without
  Hydra config or without the Consent App. A single repo reflects that reality.
- Single pipeline: one CI run builds, tests, and validates all components together, catching integration issues early.
- Simpler for a small focused team — no cross-repo PR coordination for changes that span Login App and shared
  interfaces.

A Maven parent POM ties the Java modules together (`idm-common`, `login-app`, `consent-app`, `mocks`). Hydra config and
scripts are not a Maven module — they are plain files in the `hydra/` directory.

---

## Decision 8 — Local Dev: No Docker Required

Hydra runs as a native binary on developer machines. No Docker required locally. Production uses containers.

Hydra v26.2.0 is downloaded automatically by the dev startup script on first run.

---

## Decision 9 — Local Dev Database: SQLite for macOS and Linux

Hydra's dev config uses **SQLite** (file-based, zero setup). The SQLite-enabled Hydra binary is published for macOS
(Intel and Apple Silicon) and Linux (x86_64 and arm64).

The standard Windows Hydra binary does not include SQLite — it requires CGo and a C toolchain which Ory does not ship
for Windows. This is why Windows developers use WSL2 (Decision 10) rather than running Hydra natively on Windows. Inside
WSL2 (AlmaLinux), the Linux SQLite binary is used and works without any database setup.

---

## Decision 10 — Windows Developer Setup: WSL2 with AlmaLinux 9

Windows developers run Hydra via **WSL2 with AlmaLinux 9** rather than natively.

Reasons:

- The Linux Hydra binary includes SQLite — zero database setup
- AlmaLinux 9 is RHEL-compatible, matching the production Linux environment
- AlmaLinux is preferred over Ubuntu for consistency with RHEL production servers
- WSL2 on Windows 11 automatically forwards ports — Hydra on WSL2 is reachable at `localhost` from Windows apps
- AlmaLinux vs Rocky Linux: no functional difference for this use case; AlmaLinux 9 chosen as the standard

WSL2 setup (one-time, ~10 minutes):

```powershell
# PowerShell as Administrator
wsl --install -d AlmaLinux-9
```

```bash
# Inside AlmaLinux WSL2 terminal
sudo dnf install -y curl jq tar
```

---

## Revised Service Boundaries (with Hydra)

| Service   | Name                    | Responsibility                                                                                                                                                                                               |
| --------- | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **IDM-1** | **idm-auth-gateway**    | Hydra deployment/config, Login App, Consent App, token claim schema, key rotation, client registration. Owns the full auth flow end-to-end.                                                                  |
| **IDM-2** | **idm-identity-store**  | Canonical identity store. Tenant model, users, groups, roles, permissions. Exposes a purpose-built fast read path for credential verification and tenant/role resolution at login time (critical auth path). |
| **IDM-3** | **idm-identity-api**    | REST management API over idm-identity-store. Own authz policy and rate limiting.                                                                                                                             |
| **IDM-4** | **idm-admin-console**   | Admin Console SPA. Post-login admin experience. Logs in via idm-auth-gateway's Login App (same Hydra flow).                                                                                                  |
| **IDM-5** | **idm-auth-federation** | Adapter/plugin layer only — not an orchestrator. Provides SAML handler, OIDC federation client, MFA verifier. Called by idm-auth-gateway's Login App. Flow control stays in idm-auth-gateway.                |
| **IDM-6** | **idm-platform-ops**    | Audit pipeline, migration tooling, CI/CD, IaC baseline. Cross-cutting.                                                                                                                                       |

---

## Resolved Clarification Items

| Item                         | Resolution                                                                                                                                                                  |
| ---------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Build vs adopt decision      | Adopt Ory Hydra                                                                                                                                                             |
| MFA ownership boundary       | idm-auth-gateway's Login App orchestrates; idm-auth-federation provides adapters. Interface is Hydra's login challenge API.                                                 |
| Multi-tenancy in token layer | idm-identity-store owns tenant data. Login App queries it at login, injects tenant context as custom claims via Hydra's admin API. Claim schema is single-tenant per token. |
| Session management scope     | Handled by Hydra (back-channel logout, revocation, refresh token rotation). Remaining work is configuration and lifetime policy.                                            |
| Login App ownership gap      | Explicitly assigned to idm-auth-gateway.                                                                                                                                    |

---

## Open Items

> Not blockers for idm-auth-gateway construction.

- **WAO dependency** — deferred, not relevant to idm-auth-gateway
- **Token lifetime policy** — configuration decision, made during construction
- **Back-channel logout URI registration per SaaS client** — operational/onboarding concern

---

## Decision 11 — Single Logout: OIDC Back-Channel Logout via Hydra

### Approach

OIDC Back-Channel Logout is the selected SLO mechanism. It is server-to-server — no browser redirects, no iframe
dependencies, no requirement for the user's browser to remain open. Hydra supports it natively.

Front-Channel Logout was rejected: it relies on the browser loading logout URIs via iframes, which are unreliable
(blocked by browsers, requires user's browser to be open and active).

Token-only revocation is necessary but not sufficient for SLO. Revoking the access token does not terminate app-level
sessions, and stateless JWT validation at resource APIs means the token remains accepted until natural expiry
regardless.

### What "logout" means in this stack

A complete logout must terminate three things:

```
1. The Hydra session          → prevents silent re-auth via prompt=none or refresh token
2. Each SaaS app's session    → prevents the user remaining "logged in" at the app level
3. The refresh token          → prevents token renewal after session termination
```

Revoking only the access token addresses none of these reliably.

### Flow

```
User clicks logout in any SaaS app
    ↓
SaaS app calls Login App logout endpoint:
  POST /logout
  Authorization: Bearer <access_token>
    ↓
Login App:
  1. Calls Hydra admin API to revoke the session
     → Hydra identifies all clients that participated in the session
     → Hydra POSTs a signed logout_token JWT to each client's registered backchannel_logout_uri (server-to-server)
  2. Revokes the refresh token via Hydra revocation endpoint
  3. Clears the Login App's own session cookie
    ↓
Each SaaS app's backchannel_logout_uri endpoint:
  1. Validates the logout_token JWT (signature, iss, aud, events claim)
  2. Extracts sub from the logout_token
  3. Invalidates the local session for that sub
```

### Implementation requirements

**idm-auth-gateway (Login App):**

- Expose `POST /logout` endpoint — accepts the user's access token, drives the full logout sequence
- Call Hydra's session revocation API to trigger back-channel logout propagation
- Call Hydra's token revocation endpoint to revoke the refresh token
- Clear the Login App session cookie in the response

**Each SaaS client (at onboarding):**

- Register a `backchannel_logout_uri` in Hydra at client registration time
- Implement the `backchannel_logout_uri` endpoint:
    - Validate the `logout_token` JWT — verify signature against Hydra's JWKS, check `iss`, `aud`, `events` claim
    - Invalidate the local session for the `sub` in the token
    - Return `200 OK` on success (Hydra retries on non-2xx)

**Hydra configuration:**

- `backchannel_logout_session_required: true` — enforces that clients must support back-channel logout
- `backchannel_logout_delay` — configurable delay between logout calls to clients if needed

### Security notes

- The `logout_token` is signed by Hydra — the receiving app must verify the signature before acting on it
- The `backchannel_logout_uri` endpoint must not require the user's session cookie to function — it is called
  server-to-server by Hydra, not by the user's browser
- `backchannel_logout_uri` registration is an operational/onboarding concern — documented as an open item and enforced
  during SaaS client onboarding via IDM-3
