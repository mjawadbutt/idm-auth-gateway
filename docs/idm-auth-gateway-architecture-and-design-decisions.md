# idm-auth-gateway — Architecture & Design Decisions

## Context

Building a new cloud-native IAM platform (IDM) for FBDMS Digital Experience Suite. The platform is split into 6
services. This service is **idm-auth-gateway** (IDM-1) — the OAuth2/OIDC authorization server and authentication gateway
that serves as the entry point to every product in the Digital Experience Suite.

---

# Architecture Decisions

> Structural choices about system shape, component boundaries, and technology selection. Hard to reverse, high impact.

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

# Design Decisions

> Feature-level implementation choices within the established architecture.

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

---

## Decision 12 — Tenant Resolution Strategy

### Context

When a user arrives at the Login App via an OAuth/OIDC authorization request, the Login App must determine which tenant
the user belongs to in order to route authentication correctly (local auth, SAML SSO, OIDC federation, etc.).

### Options Considered

**Option 1 — Email domain (user inputs it)** User enters their email on the Login App. The Login App extracts the domain
and looks up tenant config in IDM-2. Requires user interaction. Typical for B2C or mixed-audience scenarios.

**Option 2 — `login_hint` parameter in the OAuth request** The client app passes the tenant identifier explicitly in the
authorization request:

```
GET /oauth2/auth?...&login_hint=acmecorp
```

The Login App reads `login_hint` from the Hydra login challenge. No user interaction needed — the app already knows
which tenant it is serving.

**Option 3 — Subdomain of the authorization URL** Each tenant gets a subdomain on the authorization server URL
(`acmecorp.auth.yourdomain.com`). The tenant identifier is encoded in the hostname, extracted from the `Host` /
`X-Forwarded-Host` header. No user interaction, no extra OAuth parameters.

**Option 4 — Subdomain of the app URL translated to `login_hint`** Functionally equivalent to Option 2 but the signal
originates from the client app's own subdomain rather than explicit configuration.

**Option 5 — `acr_values` parameter** Rejected — `acr_values` has a defined purpose (Authentication Context Class
Reference) and overloading it for tenant routing is a semantic smell.

**Option 6 — Custom OAuth parameter** Non-standard `tenant=acmecorp` parameter. Simple but not spec-compliant.

### Decision: Option 2 (Primary) + Option 3 (Fallback)

**Rationale**: This platform is B2B SaaS serving enterprise tenants. The client app always knows which tenant it is
serving, so `login_hint` is a natural and explicit contract. Option 2 is chosen as primary because it is portable —
tenant resolution has no dependency on infrastructure topology, wildcard DNS, or Hydra's `login_url` configuration.
Option 3 (subdomain) is used as a fallback only when `login_hint` is absent — it must not be relied upon as the primary
signal.

### Primary: Option 2 — `login_hint`

The client app passes the tenant identifier in the authorization request. The Login App reads `login_hint` from the
Hydra login challenge payload. No user interaction, no infrastructure dependency, no `Host` header parsing required.

This is the expected path for all well-behaved clients.

### Fallback: Option 3 — Subdomain on Authorization URL

Used only when `login_hint` is not present in the login challenge. The Login App extracts the tenant from the
`X-Forwarded-Host` / `Host` header as a secondary resolution attempt.

This provides URL-level tenant isolation and enables per-tenant branding as a side effect, but its primary role in the
resolution logic is fallback only.

Infrastructure required (for fallback to function):

```
DNS:  *.auth.yourdomain.com  →  wildcard A/CNAME record pointing to the ALB
TLS:  *.auth.yourdomain.com  →  wildcard certificate issued via ACM
```

The Login App extracts the tenant from the hostname via a `TenantResolutionFilter` that runs before any controller:

```java
String host = request.getServerName(); // "acmecorp.auth.yourdomain.com"
String subdomain = host.split("\\.")[0]; // "acmecorp"
// look up tenant config for "acmecorp" from IDM-2
```

### Constraint: Hydra `login_url` Must Use Public Domain

When Option 3 is active, Hydra's `login_url` must use the public-facing domain, not an internal service name:

```
# Correct
login_url = https://auth.yourdomain.com/login

# Wrong — internal name loses the subdomain, browser cannot resolve it
login_url = http://login-app:8080/login
```

This is why Option 2 is primary — `login_hint` has no such deployment dependency.

### Spring Configuration

The Login App must be configured to read `X-Forwarded-Host` from the ALB rather than the rewritten `Host` header. Both
properties are set in `application.properties`:

```properties
# Enable reading of X-Forwarded-* headers set by the proxy/load balancer (e.g. X-Forwarded-Host for tenant resolution).
server.forward-headers-strategy=framework

# Trust X-Forwarded-* headers only from requests originating within private IP ranges (i.e. from our own/internal proxy).
# For requests from outside these ranges, SpringBoot will strip out the forwarded headers before the app sees them.
server.tomcat.remoteip.internal-proxies=10\.0\.0\.0/8,172\.16\.0\.0/12,192\.168\.0\.0/16
```

Note that:

- `server.forward-headers-strategy=framework` — `request.getServerName()` reads from `X-Forwarded-Host` when present,
  falling back to `Host` in local dev. The extraction logic is unchanged across environments.
- `server.tomcat.remoteip.internal-proxies` — `X-Forwarded-*` headers are only trusted from your own proxy. AWS ALB
  strips client-supplied forwarded headers before forwarding, so this is redundant on ALB but enforces the same
  constraint for non-AWS deployments (nginx, HAProxy, on-prem load balancers).

---

## Decision 13 — User Identity Model: UUID Surrogate Key + Linked Identity Natural Key

### Context

Hydra requires a globally unique, stable `subject` value to maintain session-to-user mappings. Users can be created
locally in IDM-2 or arrive via external identity providers (SAML, OIDC). Both cases must produce a consistent, globally
unique identifier that Hydra can use as `subject`.

### Decision

Every user in IDM-2 is assigned a **generated UUID as the surrogate key**. This UUID is passed to Hydra as `subject` and
used as the stable user reference across all downstream services.

User identity is tracked via **linked identities** — a separate collection per user that records where the user came
from. The natural key of a linked identity is the combination of three fields:

```
(tenant_id, provider, external_id)
```

This triple is enforced as a unique constraint — no duplicate entries. One user may have multiple linked identity rows
(e.g. local + SAML after a tenant migrates to SSO), but each `(tenant_id, provider, external_id)` combination is unique
across the entire platform.

### Rationale

- The UUID surrogate key decouples all downstream references from mutable user attributes (email, username). If a user's
  email changes, only the linked identity record is updated — the UUID and all downstream references are unchanged.
- The linked identity model handles both locally created users and JIT-provisioned federated users uniformly. The Login
  App always resolves identity via the same lookup: `(tenant_id, provider, external_id) → UUID`.
- A user belonging to multiple tenants has one UUID but multiple linked identity rows — one per `(tenant_id, provider)`
  combination. The UUID is the stable anchor across all tenant contexts.

### Recommended Values

**`provider`**: use `{protocol}:{issuer-or-entity-id}` — e.g. `local:idm`, `saml:adfs.acmecorp.com`,
`oidc:accounts.google.com`. For OIDC use the `iss` claim value; for SAML use the IdP `entityID`.

**`external_id`**:

- Local users (`local:idm`) → email address
- OIDC providers → `sub` claim (or `oid` for Azure AD)
- SAML providers → persistent `NameID` or stable opaque attribute (`objectGUID`, Okta user ID)

See `docs/user-identity-model.md` for the full reference including provider/external_id option tables, example records,
and email considerations.
