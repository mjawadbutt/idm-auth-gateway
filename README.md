# idm-auth-gateway

OAuth2 / OIDC authentication gateway for the FBDMS Digital Experience Suite. Built on
[Ory Hydra](https://github.com/ory/hydra) with a Spring Boot Login App and Consent App.

This PoC demonstrates:

- **Ory Hydra as the token engine** — OpenID Certified, OAuth2.1 compliant, headless by design
- **Authorization code flow with PKCE end-to-end** — from browser through Login App and Consent App to token issuance
- **Both OIDC and OAuth2** — include `openid` in scope for an ID token + access token; omit it for access token only

---

## Repository Structure

```
idm-auth-gateway/
├── hydra/
│   ├── bin/                            # Downloaded Hydra binaries (git-ignored, auto-downloaded)
│   ├── config/
│   │   ├── hydra.dev.yml               # Local dev config — SQLite, ports 4444/4445
│   │   └── hydra.prod.yml              # Production config template
│   ├── clients/
│   │   └── dev-clients.json            # OAuth2 client definitions for local dev
│   └── scripts/
│       ├── start-dev.sh                # Downloads binary if needed, runs migration, starts Hydra
│       └── register-clients.sh         # Registers dev-clients.json via Hydra admin API
│
├── login-app/                          # Spring Boot Login App
│   ├── src/main/client/                # Angular 18 client — built by Maven, output to static/login-app/
│   │   └── src/app/
│   │       ├── login-page/             # LoginPageComponent + LoginForm, TenantSelector, MfaForm
│   │       ├── model/                  # TypeScript interfaces
│   │       └── service/                # LoginApiService
│   ├── src/main/web-resources/
│   │   └── test-harness.html           # Dev test harness — copied to static/ by Maven
│   ├── src/main/java/.../loginapp/
│   │   ├── controller/                 # LoginController — /idm-auth-gateway/login, /tenant, /mfa
│   │   ├── service/                    # LoginService — Hydra challenge lifecycle
│   │   ├── hydra/                      # HydraAdminClient
│   │   ├── model/                      # Request/response records
│   │   └── config/                     # SecurityConfig, MockBeansConfig, RestClientConfig
│   └── pom.xml
│
├── consent-app/                        # Spring Boot Consent App
│   ├── src/main/resources/static/
│   │   └── consent/index.html          # Auto-POSTs consent challenge on load
│   ├── src/main/java/.../consentapp/
│   │   ├── controller/                 # ConsentController — /idm-auth-gateway/consent
│   │   ├── service/                    # ConsentService — claim injection, first-party auto-accept
│   │   └── config/                     # SecurityConfig, MockBeansConfig, RestClientConfig
│   └── pom.xml
│
├── idm-common/                         # Shared interfaces, DTOs, exceptions — plain Java, no Spring Boot
│   └── src/main/java/.../common/
│       ├── service/                    # IdentityDirectoryService, MfaService, TenantSelectorService, AuditService
│       ├── model/                      # UserIdentity, TenantContext, TenantSelectionResult, MfaChallenge
│       └── exception/                  # AuthenticationException, TenantResolutionException
│
├── mocks/                              # Local dev mock implementations of idm-common interfaces
│   └── src/main/java/.../mocks/
│       ├── MockIdentityDirectoryService.java   # alice (multi-tenant, no MFA), bob (single-tenant, MFA)
│       ├── MockMfaService.java                 # Always succeeds
│       ├── MockTenantSelectorService.java
│       └── MockAuditService.java
│
├── docs/
│   ├── idm-auth-gateway-architecture-decisions.md
│   └── idm-auth-gateway-component-design.md
│
└── pom.xml                             # Parent POM — all modules, shared dependency versions
```

---

## Prerequisites

| Tool    | Version | Notes                             |
| ------- | ------- | --------------------------------- |
| Java    | 17+     | LTS                               |
| Maven   | 3.9+    |                                   |
| Node.js | 20+     | Angular build is invoked by Maven |
| curl    | any     | Used by Hydra scripts             |
| jq      | any     | Used by register-clients.sh       |
| bash    | any     | macOS / Linux / Git Bash / WSL2   |

---

## Setup (Clean Checkout)

### Windows only — WSL2 / AlmaLinux 9 one-time setup

Hydra requires Linux to run with SQLite. Windows developers must use WSL2.

**1. Install AlmaLinux 9** (PowerShell as Administrator):

```powershell
wsl --install -d AlmaLinux-9
```

Create a Linux username and password when prompted.

**2. Enable mirrored networking** — create or edit `C:\Users\<your-username>\.wslconfig`:

```ini
[wsl2]
networkingMode=mirrored
```

Restart WSL:

```powershell
wsl --shutdown
wsl -d AlmaLinux-9
```

**3. Configure corporate proxy** — add to `/etc/dnf/dnf.conf`:

```bash
sudo tee -a /etc/dnf/dnf.conf <<EOF
proxy=http://<username>:<password>@mbkproxy.fxdms.net:8080
sslverify=false
EOF
```

Add to `~/.bashrc`:

```bash
echo 'export http_proxy=http://<username>:<password>@mbkproxy.fxdms.net:8080' >> ~/.bashrc
echo 'export https_proxy=http://<username>:<password>@mbkproxy.fxdms.net:8080' >> ~/.bashrc
echo 'export no_proxy=localhost,127.0.0.1' >> ~/.bashrc
source ~/.bashrc
```

**4. Install tools:**

```bash
sudo dnf install -y curl jq tar
```

Optionally set AlmaLinux as the default WSL distribution:

```powershell
wsl --set-default AlmaLinux-9
```

---

### Build — all platforms

From the repo root, run once after a clean checkout to build all modules and install them into the local Maven
repository:

```bash
mvn clean install
```

This also runs the Angular build for login-app. After this, IntelliJ run configs work without needing a full rebuild
each time.

---

## Running and Testing

### 1. Start Hydra

**macOS / Linux:**

```bash
chmod +x hydra/scripts/start-dev.sh
./hydra/scripts/start-dev.sh
```

**Windows** — open the AlmaLinux WSL2 terminal:

```bash
cd /mnt/c/projects/idmx-platform/idm-auth-gateway
./hydra/scripts/start-dev.sh
```

On first run this downloads the Hydra binary and runs the SQLite migration. On subsequent runs it just starts Hydra.
Leave this terminal open — Hydra runs in the foreground.

### 2. Register dev OAuth2 clients

In a new terminal, after Hydra is up:

```bash
./hydra/scripts/register-clients.sh
```

Safe to re-run at any time — existing clients are updated, not duplicated. Only needs to be re-run if `dev-clients.json`
changes or the Hydra database is wiped.

### 3. Start Login App

Use the IntelliJ run configuration **LoginAppApplication**, or from the command line:

```bash
mvn spring-boot:run -pl login-app -am -Dspring-boot.run.profiles=winLocal
```

Runs on port **8080**. The Angular client is served at `http://localhost:8080/login-app/`.

### 4. Start Consent App

Use the IntelliJ run configuration **ConsentAppApplication**, or from the command line:

```bash
mvn spring-boot:run -pl consent-app -am -Dspring-boot.run.profiles=winLocal
```

Runs on port **8082**.

### 5. Run the test harness

Open in a browser:

```
http://localhost:8080/test-harness.html
```

The harness simulates a SaaS app initiating an OAuth2 authorization code flow with PKCE.

**How to use:**

1. Select a **Client ID** from the dropdown — the client secret fills in automatically.
2. Click **▶ Start Login Flow** — the browser redirects to the Angular login page.
3. Enter credentials and complete the flow (tenant selection and/or MFA if applicable).
4. After consent, the browser redirects back to the harness showing the authorization code.
5. Click **Exchange Code for Tokens** — the harness exchanges the code for tokens and displays the result including
   decoded ID token claims.

**Test users:**

| Username | Password   | Tenants                        | MFA                  | Notes                       |
| -------- | ---------- | ------------------------------ | -------------------- | --------------------------- |
| `alice`  | `password` | `tenant-acme`, `tenant-globex` | No                   | Shows tenant selector       |
| `bob`    | `password` | `tenant-acme`                  | Yes — enter any code | Single tenant, MFA required |

**Dev clients:**

| Client ID           | Secret                     |
| ------------------- | -------------------------- |
| `test-saas-app`     | `test-saas-app-secret`     |
| `idm-admin-console` | `idm-admin-console-secret` |

Any user can log in via any client — users and clients are independent.

---

## Ports

| Service      | Port |
| ------------ | ---- |
| Hydra Public | 4444 |
| Hydra Admin  | 4445 |
| Login App    | 8080 |
| Consent App  | 8082 |

---

## Documentation

| Document                                                                                             | Description                                          |
| ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| [`docs/idm-auth-gateway-architecture-decisions.md`](docs/idm-auth-gateway-architecture-decisions.md) | Architecture and technology decisions with rationale |
| [`docs/idm-auth-gateway-component-design.md`](docs/idm-auth-gateway-component-design.md)             | Component map, auth flow, interaction diagram        |

---

## PoC Status

### Covered

| Area                              | Detail                                                                                   |
| --------------------------------- | ---------------------------------------------------------------------------------------- |
| Hydra deployment                  | SQLite-backed local dev setup, binary auto-download, migration                           |
| Authorization code flow with PKCE | Full end-to-end — browser → Hydra → Login App → Consent App → token                      |
| Login App — Angular client        | Credential form, tenant selector, MFA screen                                             |
| Login App — backend               | Hydra challenge lifecycle, credential verification, tenant resolution, MFA orchestration |
| Consent App                       | Auto-accept for first-party clients, custom claim injection                              |
| Custom token claims               | `tenant_id`, `tenant_name`, `roles`, `permissions` injected at consent time              |
| Multi-tenant flow                 | Tenant selector shown for users with multiple tenants (alice)                            |
| MFA flow                          | MFA screen shown and verified (bob) — mock always passes                                 |
| OIDC                              | ID token issued when `openid` scope requested — claims visible in test harness           |
| OAuth2                            | Access token + refresh token issued regardless of `openid` scope                         |
| Test harness                      | Simulates a SaaS app — PKCE generation, auth code callback, token exchange               |

### Not Yet Built

| Area                                   | Detail                                                                                          |
| -------------------------------------- | ----------------------------------------------------------------------------------------------- |
| Real identity store (IDM-2)            | `IdentityDirectoryService` is mocked — no real LDAP, AD, or database behind it                  |
| Real MFA (IDM-5)                       | `MfaService` is mocked — no real TOTP, WebAuthn, or SMS                                         |
| Federation                             | No upstream SAML/OIDC provider configured in Hydra; IDM-5 federation adapter not built          |
| Third-party consent UI                 | `ConsentService` signals `consentUiRequired=true` for non-first-party clients but no UI exists  |
| Logout                                 | Hydra config has a logout URL but no logout endpoint is implemented in Login App                |
| Token revocation / back-channel logout | Hydra supports it — not wired                                                                   |
| Silent re-auth (`prompt=none`)         | Hydra supports tenant switching without re-entering credentials — not tested                    |
| `KeyManagementService` real impl       | Interface defined — AWS KMS and Vault implementations not built                                 |
| `SecretsService` real impl             | Interface defined — AWS Secrets Manager and Vault implementations not built                     |
| Spring Cloud secrets integration       | Secrets are in properties files — Spring Cloud Vault/AWS SM not wired                           |
| Audit pipeline (IDM-6)                 | `AuditService` is mocked — events are not emitted to any real pipeline                          |
| Production Hydra config                | `hydra.prod.yml` is a template — PostgreSQL DSN, KMS secrets, and production URLs not filled in |
| Production client registration         | Only dev scripts exist — no automated client onboarding tooling                                 |
