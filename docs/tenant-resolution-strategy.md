# Tenant Resolution Strategy

## Context

When a user arrives at the Login App via an OAuth/OIDC authorization request, the Login App must determine which tenant
the user belongs to in order to route authentication correctly (local auth, SAML SSO, OIDC federation, etc.).

The question is: how does the Login App know which tenant it is serving?

---

## Options Considered

### Option 1 — Email domain (user inputs it)

User enters their email on the Login App. The Login App extracts the domain and looks up tenant config in IDM-2.

- Requires user interaction — user must type their email before any routing decision is made
- Works when the app has no prior knowledge of which tenant is coming
- Typical for B2C or mixed-audience scenarios

### Option 2 — `login_hint` parameter in the OAuth request

The client app passes the tenant identifier explicitly in the authorization request:

```
GET /oauth2/auth?...&login_hint=acmecorp
```

The Login App reads `login_hint` from the Hydra login challenge. No user interaction needed — the app already knows
which tenant it is serving (e.g. from its own subdomain or configuration).

### Option 3 — Subdomain of the authorization URL

Each tenant gets a subdomain on the authorization server URL:

```
https://acmecorp.auth.yourdomain.com/oauth2/auth
https://globex.auth.yourdomain.com/oauth2/auth
```

The tenant identifier is encoded in the hostname. The Login App extracts it from the `Host` header (or
`X-Forwarded-Host` behind a proxy). No user interaction needed, no extra OAuth parameters required.

### Option 4 — Subdomain of the app URL translated to `login_hint`

The client app is deployed on a tenant-specific subdomain (e.g. `acmecorp.projectapp.com`). The app translates its own
subdomain into a `login_hint` before redirecting to the authorization server. Functionally equivalent to Option 2 but
the signal originates from the app URL rather than explicit configuration.

### Option 5 — `acr_values` parameter

Tenant hint carried in `acr_values`. Not recommended — `acr_values` has a defined purpose (Authentication Context Class
Reference, e.g. MFA level) and overloading it for tenant routing is a semantic smell.

### Option 6 — Custom OAuth parameter

Non-standard `tenant=acmecorp` parameter in the authorization request. Hydra passes unknown parameters through to the
login challenge so the Login App can read it. Simple but not spec-compliant.

---

## Selected Approach: Option 2 (Primary) + Option 3 (Complementary)

**Rationale**: This platform is B2B SaaS serving enterprise tenants. The client app always knows which tenant it is
serving (from its own subdomain or deployment config), so `login_hint` is a natural and explicit contract. Option 2 is
chosen as primary because it is portable — tenant resolution has no dependency on infrastructure topology, wildcard DNS,
or Hydra's `login_url` configuration. Option 3 (subdomain) is retained as a complementary signal and is still worth
implementing for URL-level tenant isolation and branding, but the Login App must not rely on it as the sole resolution
mechanism.

### Primary: Option 2 — `login_hint`

The client app passes the tenant identifier in the authorization request:

```
GET /oauth2/auth?...&login_hint=acmecorp
```

The Login App reads `login_hint` from the Hydra login challenge payload. No user interaction, no infrastructure
dependency, no `Host` header parsing required.

### Complementary: Option 3 — Subdomain on Authorization URL

Each tenant gets a subdomain on the authorization server URL:

```
https://acmecorp.auth.yourdomain.com/oauth2/auth
https://globex.auth.yourdomain.com/oauth2/auth
```

This provides URL-level tenant isolation and enables per-tenant branding on the Login App. The subdomain is also used as
a consistency check — if both `login_hint` and the subdomain are present, they must agree.

#### Infrastructure requirements

Two pieces of infrastructure are required, both straightforward on AWS:

```
DNS:  *.auth.yourdomain.com  →  wildcard A/CNAME record pointing to the ALB
TLS:  *.auth.yourdomain.com  →  wildcard certificate issued via ACM
```

With these in place, `acmecorp.auth.yourdomain.com` and `globex.auth.yourdomain.com` both resolve to the same gateway
instance. No per-tenant DNS records, no per-tenant deployments.

#### How the Login App extracts the tenant from the subdomain

The browser sets the `Host` header automatically on every HTTP request. When Hydra redirects the user to the Login App,
the redirect URL preserves the public subdomain:

```
Hydra redirects to: https://acmecorp.auth.yourdomain.com/login?login_challenge=abc123
Browser requests:   GET /login?login_challenge=abc123
                    Host: acmecorp.auth.yourdomain.com
```

The Login App extracts the tenant from the hostname:

```java
String host = request.getServerName(); // "acmecorp.auth.yourdomain.com"
String subdomain = host.split("\\.")[0]; // "acmecorp"
// look up tenant config for "acmecorp" from IDM-2
```

In practice this extraction lives in a `TenantResolutionFilter` that runs before any controller, placing the resolved
`tenant_id` in request scope. Controllers never touch the `Host` header directly.

---

## Subtlety: Hydra `login_url` Configuration

When Option 3 (subdomain) is used as a complementary signal, Hydra must be configured with a `login_url` that uses the
**public-facing domain**, not an internal service name:

```
# Correct — preserves subdomain through the redirect
login_url = https://auth.yourdomain.com/login

# Wrong — internal service name, subdomain is lost
login_url = http://login-app:8080/login
```

If Hydra's `login_url` uses an internal hostname, the browser redirect goes to that internal address, which:

1. Fails immediately — internal Kubernetes service names are not resolvable from the public internet
2. Even if reachable via a proxy, the `Host` header would contain the internal name, not the tenant subdomain

This is why Option 2 (`login_hint`) is the primary resolution mechanism — it has no such dependency. The subdomain
approach works correctly only when Hydra's `login_url` uses the public domain, which is a deployment constraint that
must be documented and enforced. Changing it to an internal address silently breaks subdomain-based resolution, but
`login_hint` continues to work regardless.

---

## X-Forwarded-Host and Spring Configuration

In production, the Login App sits behind an AWS ALB. The ALB terminates TLS and forwards requests internally. The
original `Host` header from the browser (`acmecorp.auth.yourdomain.com`) is preserved by the ALB in the
`X-Forwarded-Host` header.

Spring Boot must be configured to trust and read `X-Forwarded-Host` rather than the rewritten `Host` header:

```properties
server.forward-headers-strategy=framework

# Trust X-Forwarded-* headers only from private IP ranges (your own proxy/load balancer).
# Requests from any other IP have these headers stripped before the app sees them.
# Covers all standard private CIDRs — safe default for on-prem and private cloud deployments.
# On AWS ALB this is redundant (ALB strips client-supplied forwarded headers itself) but harmless.
server.tomcat.remoteip.internal-proxies=10\.0\.0\.0/8,172\.16\.0\.0/12,192\.168\.0\.0/16
```

With this set, `request.getServerName()` automatically reads from `X-Forwarded-Host` when present, falling back to
`Host` when not. The Login App code is unchanged — the same extraction logic works correctly in both local dev (no
proxy, reads `Host`) and production (behind ALB, reads `X-Forwarded-Host`).

**Security note**: `X-Forwarded-*` headers should only be trusted from your own proxy. AWS ALB strips and rewrites these
headers from incoming client requests before forwarding, preventing spoofing. The Login App must not be directly
internet-facing without the ALB in front.

This property is already set in `application.properties`.
