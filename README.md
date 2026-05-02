# idm-auth-gateway

OAuth2 / OIDC authentication gateway for the FBDMS Digital Experience Suite. The entry point to all products in the
suite. Built on [Ory Hydra](https://github.com/ory/hydra) with a Spring Boot Login App and Consent App.

## Repository Structure

```
idm-auth-gateway/
│
├── hydra/                              # Hydra binary config, dev scripts, client definitions
│   ├── bin/                            # Downloaded Hydra binaries (git-ignored, auto-downloaded)
│   ├── config/
│   │   ├── hydra.dev.yml               # Local dev config — SQLite, ports 4444/4445, dev secrets
│   │   └── hydra.prod.yml              # Production config template — PostgreSQL, KMS, env var secrets
│   ├── clients/
│   │   └── dev-clients.json            # OAuth2 client definitions registered for local dev
│   └── scripts/
│       ├── start-dev.sh                # macOS / Linux / WSL2 — downloads binary if needed, starts Hydra
│       ├── start-dev.ps1               # Windows PowerShell equivalent
│       └── register-clients.sh         # Registers dev-clients.json via Hydra admin API
│
├── login-app/                          # Spring Boot + React Login App (IDM-1 core build)
│   ├── frontend/                       # React/TypeScript source — login form, tenant selector, MFA screens
│   │   ├── src/
│   │   │   ├── components/             # Reusable UI components (LoginForm, TenantSelector, MfaChallenge)
│   │   │   ├── pages/                  # Page-level components (LoginPage, ErrorPage)
│   │   │   ├── api/                    # Typed API client — calls Spring Boot backend
│   │   │   └── main.tsx                # React entry point
│   │   ├── public/                     # Static assets (favicon, etc.)
│   │   ├── package.json
│   │   └── vite.config.ts              # Vite config — dev proxy to :8080, build output to ../src/main/resources/static
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fbdms/idm/loginapp/
│   │   │   │   ├── LoginAppApplication.java        # Spring Boot entry point
│   │   │   │   ├── config/                         # Spring config classes (Security, Web, CORS)
│   │   │   │   ├── controller/                     # HTTP controllers (LoginController, LogoutController)
│   │   │   │   ├── service/                        # Business logic (LoginService, TenantSelectorService)
│   │   │   │   ├── hydra/                          # Hydra admin API client (challenge accept/reject)
│   │   │   │   └── audit/                          # Audit event emission to IDM-6
│   │   │   └── resources/
│   │   │       ├── static/                         # React build output lands here (git-ignored)
│   │   │       ├── application.yml                 # Base config
│   │   │       ├── application-local.yml           # Local dev overrides (git-ignored)
│   │   │       └── application-prod.yml            # Production config (no secrets — uses Spring Cloud)
│   │   └── test/
│   │       └── java/com/fbdms/idm/loginapp/
│   │           ├── controller/                     # Controller slice tests
│   │           ├── service/                        # Service unit tests
│   │           └── integration/                    # Full flow integration tests (Hydra + mocks)
│   └── pom.xml                         # Login App Maven module — depends on idm-common
│
├── consent-app/                        # Spring Boot Consent App
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fbdms/idm/consentapp/
│   │   │   │   ├── ConsentAppApplication.java      # Spring Boot entry point
│   │   │   │   ├── config/
│   │   │   │   ├── controller/                     # ConsentController
│   │   │   │   ├── service/                        # ConsentService — claim injection logic
│   │   │   │   └── hydra/                          # Hydra admin API client (consent accept/reject)
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-local.yml           # (git-ignored)
│   │   │       └── application-prod.yml
│   │   └── test/
│   └── pom.xml                         # Consent App Maven module — depends on idm-common
│
├── idm-common/                         # Shared interfaces, DTOs, constants — consumed by login-app and consent-app
│   └── src/
│       └── main/java/com/fbdms/idm/common/
│           ├── service/
│           │   ├── IdentityDirectoryService.java   # Interface — IDM-2 contract (credential verify, tenant/role lookup)
│           │   ├── MfaService.java                 # Interface — IDM-5 contract (MFA trigger, verify)
│           │   ├── KeyManagementService.java        # Interface — KMS abstraction (sign, rotate)
│           │   └── SecretsService.java             # Interface — secrets abstraction (get, put)
│           ├── dto/
│           │   ├── UserIdentity.java               # Returned by IDM-2 on credential verification
│           │   ├── TenantContext.java               # Tenant + roles resolved for token claim injection
│           │   └── MfaChallenge.java               # MFA challenge/response contract with IDM-5
│           └── exception/
│               ├── AuthenticationException.java
│               └── TenantResolutionException.java
│   └── pom.xml                         # idm-common Maven module — no Spring Boot, plain Java
│
├── mocks/                              # Local dev mock implementations of idm-common interfaces
│   └── src/
│       └── main/java/com/fbdms/idm/mocks/
│           ├── MockIdentityDirectoryService.java   # Returns hardcoded users, tenants, roles
│           ├── MockMfaService.java                 # Always returns MFA success
│           ├── MockKeyManagementService.java        # Signs with a local dev key
│           └── MockSecretsService.java             # Reads from environment variables
│   └── pom.xml                         # Mocks Maven module — depends on idm-common
│
├── docs/
│   ├── idm-auth-gateway-architecture-decisions.md  # All architecture and technology decisions with rationale
│   └── idm-auth-gateway-component-design.md        # Component map, auth flow, interaction diagram
│
├── pom.xml                             # Maven parent POM — declares all modules, shared dependency versions
├── .gitignore
└── README.md
```

## Prerequisites

| Tool    | Version | Notes                           |
| ------- | ------- | ------------------------------- |
| Java    | 21+     | LTS                             |
| Maven   | 3.9+    |                                 |
| Node.js | 20+     | For React frontend              |
| curl    | any     | For download scripts            |
| jq      | any     | For client registration script  |
| bash    | any     | macOS/Linux/Git Bash on Windows |

## Running Locally (No Docker Required)

### Step 1 — Start Hydra

**macOS / Linux (RHEL):**

```bash
chmod +x hydra/scripts/start-dev.sh
./hydra/scripts/start-dev.sh
```

**Windows (PowerShell — recommended):**

```powershell
.\hydra\scripts\start-dev.ps1
```

**Windows (Git Bash — alternative):**

```bash
./hydra/scripts/start-dev.sh
```

This will:

- Detect your OS and architecture
- Download the correct Hydra v26.2.0 binary (first run only)
- Run the SQLite database migration
- Start Hydra on ports 4444 (public) and 4445 (admin)

> **Windows:** The standard Windows Hydra binary does not include SQLite. Windows developers should use WSL2 with
> AlmaLinux 9 and run the bash script inside it — see
> [Architecture Decisions](docs/idm-auth-gateway-architecture-decisions.md) Decision 10 for setup steps.

### Step 2 — Register Dev OAuth2 Clients

In a new terminal:

```bash
./hydra/scripts/register-clients.sh
```

### Step 3 — Verify Hydra is Running

```bash
# OIDC discovery endpoint
curl http://localhost:4444/.well-known/openid-configuration

# Health check
curl http://localhost:4445/health/ready
```

### Step 4 — Start Login App (coming soon)

```bash
cd login-app
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Step 5 — Start React Dev Server (coming soon)

```bash
cd login-app/frontend
npm install
npm run dev
```

## Dev OAuth2 Clients

| Client ID           | Purpose                         | Redirect URI                     |
| ------------------- | ------------------------------- | -------------------------------- |
| `test-saas-app`     | Simulates a downstream SaaS app | `http://localhost:9000/callback` |
| `idm-admin-console` | IDM-4 Admin Console             | `http://localhost:4000/callback` |

## Ports

| Service          | Port |
| ---------------- | ---- |
| Hydra Public     | 4444 |
| Hydra Admin      | 4445 |
| Login App        | 8080 |
| React Dev Server | 3000 |

## Documentation

| Document                                                                                             | Description                                              |
| ---------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| [`docs/idm-auth-gateway-architecture-decisions.md`](docs/idm-auth-gateway-architecture-decisions.md) | All architecture and technology decisions with rationale |
| [`docs/idm-auth-gateway-component-design.md`](docs/idm-auth-gateway-component-design.md)             | Component map, auth flow, interaction diagram            |
