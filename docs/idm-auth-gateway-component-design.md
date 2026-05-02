# idm-auth-gateway — Component Design

## Internal Components

idm-auth-gateway consists of three internal components:

**Ory Hydra** The token engine. Deployed and configured, not written. Owns all OAuth2/OIDC protocol endpoints. Exposes
two ports:

- Public port (4444) — browsers and SaaS apps talk to this. The protocol surface: `/authorize`, `/token`, `/userinfo`,
  `/introspect`, `/revoke`, `/.well-known/openid-configuration`, `/jwks`
- Admin port (4445) — internal only, never exposed externally. Login App and Consent App call this to accept/reject
  challenges and manage clients.

**Login App (Spring Boot + React)** End-user facing. The page users land on when they click "Sign In" on any SaaS
product. Responsibilities:

- Receives `login_challenge` from Hydra via redirect
- Renders login form, tenant selector, MFA screens
- Verifies credentials against IDM-2
- Triggers MFA via IDM-5 if required
- Handles tenant selection for multi-tenant users
- Accepts or rejects the login challenge via Hydra admin API
- Injects subject and session context into the challenge response

**Consent App (Spring Boot)** Handles OAuth2 consent after login. Responsibilities:

- Receives `consent_challenge` from Hydra via redirect
- Resolves roles and permissions from IDM-2
- Auto-accepts consent for first-party SaaS clients (no UI shown)
- Injects final token claims (`tenant_id`, roles, permissions) via Hydra admin API
- Shows consent UI for third-party clients if required in future

---

## External Dependencies

**IDM-2 (idm-identity-store)** Called synchronously in the critical auth path:

- Step 1 — credential verification at login time
- Step 2 — tenant membership and role/permission resolution at consent time

IDM-2 must expose a purpose-built fast read interface for these two operations, separate from its general CRUD surface.

**IDM-5 (idm-auth-federation)** Called by the Login App when MFA or federation is required:

- MFA adapter — TOTP verification, WebAuthn, hardware key
- Federation adapter — redirects to external IdP (SAML/OIDC) and handles the callback

IDM-5 is an adapter/plugin layer. It does not own flow control — the Login App orchestrates when to call it.

**IDM-6 (idm-platform-ops)** Receives events emitted by the Login App and Hydra:

- Login success / failure
- Token issued
- Token revoked
- Consent granted / revoked

---

## Component Interaction Diagram

```
                        Browser / SaaS App
                               │
                    ┌──────────▼──────────┐
                    │     Hydra :4444      │  (public — internet facing via ALB)
                    │  /authorize /token   │
                    │  /userinfo /jwks     │
                    └──────────┬──────────┘
                               │ login_challenge redirect
                    ┌──────────▼──────────┐
                    │     Login App :8080  │◄──── IDM-5 (MFA / federation adapters)
                    │  Spring Boot + React │
                    └──────┬──────┬───────┘
                           │      │
              credentials  │      │ audit events
                    ┌──────▼──┐   │
                    │  IDM-2  │   │
                    │ (fast   │   ▼
                    │  read)  │  IDM-6
                    └─────────┘
                               │ consent_challenge redirect
                    ┌──────────▼──────────┐
                    │    Consent App       │◄──── IDM-2 (role/permission resolution)
                    │    Spring Boot       │
                    └──────────┬──────────┘
                               │ accept consent + inject claims
                    ┌──────────▼──────────┐
                    │   Hydra :4445        │  (admin — internal only, never public)
                    │   admin API          │
                    └─────────────────────┘
```

---

## Authorization Code Flow with PKCE (End to End)

1. SaaS app redirects browser to Hydra `/authorize` with `client_id`, `redirect_uri`, `scope`, `code_challenge`
2. Hydra checks for active login session — none found, redirects to Login App with `?login_challenge=<token>`
3. Login App calls Hydra admin API to fetch challenge details, renders login form
4. User submits credentials → Login App calls IDM-2 to verify
5. IDM-2 returns user identity and tenant memberships
6. Multiple tenants → tenant selector shown; single tenant → skipped
7. MFA required → Login App calls IDM-5 MFA adapter → user completes challenge
8. Login App calls Hydra admin API: accept login (passes `subject=user_id`)
9. Hydra redirects browser back to `/authorize`, then to Consent App with `?consent_challenge=<token>`
10. Consent App calls IDM-2 to resolve roles/permissions for the selected tenant
11. Consent App calls Hydra admin API: accept consent (injects `tenant_id`, roles, permissions as token claims)
12. Hydra issues authorization code, redirects browser to SaaS app `redirect_uri`
13. SaaS app backend calls `POST /token` with code + `code_verifier`
14. Hydra returns `access_token`, `id_token`, `refresh_token`
15. SaaS app validates token via JWKS or `/introspect` — user is authenticated, token carries tenant context

---

## What is Built vs Configured

| Component                                | Build or Configure | Notes                                                |
| ---------------------------------------- | ------------------ | ---------------------------------------------------- |
| Ory Hydra                                | Configure          | Deploy as binary/container, write `hydra.yml` config |
| Login App                                | Build              | Spring Boot + React, team writes this                |
| Consent App                              | Build              | Spring Boot, team writes this                        |
| Hydra storage (SQLite/PostgreSQL)        | Configure          | Run `hydra migrate sql`                              |
| Hydra OAuth2 client registrations        | Configure          | Register each SaaS app via Hydra admin API           |
| `KeyManagementService` interface + impls | Build              | AWS KMS, Vault, local dev implementations            |
| `SecretsService` interface + impls       | Build              | AWS SM, Vault, env var implementations               |
| IDM-2 client (in Login/Consent App)      | Build              | HTTP client calling IDM-2's fast read interface      |
| IDM-5 client (in Login App)              | Build              | HTTP client calling IDM-5 MFA/federation adapters    |

---

## Secrets Architecture

Two complementary mechanisms within the Login and Consent Apps:

**Spring Cloud (startup-time config)** Fetches secrets from Vault or AWS Secrets Manager at application startup and
populates Spring's `Environment`. Application code uses `@Value` — the provider is transparent. Covers: DB passwords,
Hydra admin URL/credentials, cookie secrets.

**Interface/Impl pattern (runtime crypto)** Active cryptographic operations (signing, key rotation calls) go through
`KeyManagementService`. Provider implementations are wired via `@ConditionalOnProperty`. Covers: token signing
operations, KMS interactions.

**Hydra secrets (entrypoint script)** Hydra is a separate process with no Spring context. Its secrets are injected as
environment variables by a provider-agnostic entrypoint script before Hydra starts. `SECRETS_PROVIDER=aws|vault`
controls which source is used — no image rebuild needed.
