# Login Flow Cases

All authentication paths ultimately result in Hydra issuing an OAuth2/OIDC token. SAML and OIDC federation are
alternative authentication mechanisms that plug into the Login App's auth step — they are not parallel token paths.

---

## Case 1 — User navigates directly to a SaaS app (SP-initiated, most common)

User clicks app URL or bookmark → app has no session → app server responds with HTTP 302 to Hydra's `/oauth2/auth`
endpoint (standard OAuth2 authorization request) → browser follows redirect:

```
GET https://auth.yourdomain.com/oauth2/auth
  ?response_type=code
  &client_id=someapp
  &scope=openid
  &state=xK9mP2qR                                              (see: state)
  &redirect_uri=https://someapp.yourdomain.com/callback
  &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM (see: PKCE)
  &code_challenge_method=S256
  &login_hint=acmecorp
```

Hydra creates [login challenge](#login-challenge) → redirects to Login App:

```
GET https://auth.yourdomain.com/login?login_challenge=abc123
```

Login App resolves tenant from `login_hint` (primary) or subdomain (fallback) → queries IDM-2 for tenant auth config →
determines auth path:

- `local` → show login form (email + password)
- `saml` → skip login form, redirect to corporate IdP (Case 1b)
- `oidc` → skip login form, redirect to external OIDC provider (Case 1c)

If `login_hint` is absent and subdomain is also not resolvable, Login App shows an email input first, extracts the
domain, then queries IDM-2 to determine the auth path.

### Case 1a — Local authentication (username/password)

Login App queries IDM-2 → tenant config says local auth.

#### Case 1a-i — No MFA configured

- Credentials verified against IDM-2
- Login App accepts Hydra [login challenge](#login-challenge)
- Hydra redirects to Consent App → [consent challenge](#consent-challenge) accepted → code issued
- SaaS app exchanges code + [code_verifier](#pkce) for tokens → user lands in app

#### Case 1a-ii — MFA configured

- Credentials verified against IDM-2
- Login App calls IDM-5 MFA verifier → MFA screen shown (TOTP / SMS / etc.)
- MFA verified → Login App accepts Hydra login challenge → same token path as 1a-i

### Case 1b — SAML SSO (corporate IdP)

- Login App queries IDM-2 → tenant config says SAML, returns IdP metadata URL
- Login App builds SAMLRequest → redirects user to corporate IdP

```
GET https://adfs.corp.com/adfs/ls?SAMLRequest=base64...&RelayState=abc123
```

- Corporate IdP authenticates user (its own login screen — outside gateway control)
- Corporate IdP POSTs SAML assertion back to gateway ACS endpoint

```
POST https://auth.yourdomain.com/saml/acs
  SAMLResponse=base64SignedXML...&RelayState=abc123
```

- IDM-5 validates assertion signature and conditions (audience, expiry)
- IDM-5 extracts user attributes → passes identity back to Login App
- Login App accepts Hydra login challenge → tokens issued → user lands in app

### Case 1c — OIDC federation (external OIDC IdP, e.g. Google, Azure AD)

- Login App queries IDM-2 → tenant config says OIDC federation
- Login App calls IDM-5 OIDC federation client → redirects user to external OIDC provider
- External provider authenticates user → redirects back with authorization code
- IDM-5 exchanges code → validates ID Token → extracts identity → passes back to Login App
- Login App accepts Hydra login challenge → tokens issued → user lands in app

---

## Case 2 — User clicks app from corporate portal (IdP-initiated SAML)

Corporate portal has a tile for a DxP product. User clicks tile → corporate IdP POSTs assertion directly to gateway ACS
endpoint with no prior request from the gateway:

```
POST https://auth.yourdomain.com/saml/acs
  SAMLResponse=base64SignedXML...
```

- No prior login challenge exists
- IDM-5 validates assertion → extracts identity
- Login App creates a new Hydra login challenge programmatically via Hydra admin API
- Login App immediately accepts it (user is already authenticated via SAML)
- Hydra issues code → Login App redirects user to target app
- Tokens issued → user lands in app

---

## Case 3 — User is already authenticated (SSO across apps)

User is in App A (has valid Hydra session) → navigates to App B.

App B redirects to gateway with `prompt=none`:

```
GET https://auth.yourdomain.com/oauth2/auth
  ?response_type=code&client_id=appB&scope=openid&prompt=none&...
```

- Hydra detects existing session → skips Login App entirely
- Hydra issues code immediately
- App B exchanges code for tokens → user lands in App B
- No login screen shown

---

## Case 4 — User switches tenant context

User is logged into Tenant A → clicks "Switch to Tenant B".

App sends silent re-auth request via `prompt=none` with new tenant hint:

```
GET https://auth.yourdomain.com/oauth2/auth
  ?response_type=code&client_id=app&scope=openid&prompt=none&login_hint=tenantB
```

- Hydra checks session → Login App is invoked with tenant selector
- User selects Tenant B → Login App injects new tenant claims via Hydra admin API
- New token issued scoped to Tenant B → app receives new token

---

## Case 5 — Machine to machine (no user)

Background service needs to call a protected API. Service POSTs directly to Hydra token endpoint:

```
POST https://auth.yourdomain.com/oauth2/token
  grant_type=client_credentials
  &client_id=...
  &client_secret=...
  &scope=...
```

- Hydra validates client credentials → issues access token directly
- No Login App, no Consent App, no SAML, no MFA involved
- Token represents the service identity, not a user

---

## Case 6 — Token expired, user still active

App receives 401 from a resource API. App POSTs refresh token to Hydra:

```
POST https://auth.yourdomain.com/oauth2/token
  grant_type=refresh_token
  &refresh_token=...
  &client_id=...
```

- Hydra validates refresh token → issues new access token
- No user interaction, no Login App involved

---

# Background Concepts

---

## PKCE

**Proof Key for Code Exchange** — a security extension to the Authorization Code flow that protects against
authorization code interception attacks.

The problem it solves: if an attacker intercepts the authorization code from the redirect URL, they could exchange it
for tokens. PKCE ensures only the original app that started the flow can exchange the code.

**How it works:**

```
1. App generates a random secret:
   code_verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

2. App hashes it:
   code_challenge = BASE64URL(SHA256(code_verifier))
                  = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

3. App sends code_challenge in the authorization request (Step 1):
   &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
   &code_challenge_method=S256
   Hydra stores the code_challenge alongside the authorization code.

4. App sends code_verifier when exchanging the code for tokens:
   POST /oauth2/token
     &code=4/0AX4XfWh...
     &code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
   Hydra hashes the verifier and checks it matches the stored challenge.
   An attacker who intercepted the code but lacks the verifier → rejected.
```

The `code_verifier` never travels through the browser — only its hash (`code_challenge`) does. PKCE is mandated by OAuth
2.1 for all clients.

---

## state

A random value generated by the app and included in the authorization request. Hydra returns it unchanged in the
redirect back to the app. The app verifies it matches what it sent.

**What it protects against**: CSRF — an attacker tricking the browser into completing an OAuth flow the user did not
initiate, potentially injecting an attacker-controlled authorization code into the user's session.

```
App generates:  state=xK9mP2qR
Sends in Step 1: &state=xK9mP2qR
Hydra returns:   ?code=ABC&state=xK9mP2qR
App verifies:    received state matches stored state → proceed
                 mismatch → reject, possible CSRF attack
```

`state` and `code_verifier` are complementary — `state` protects the redirect back to the app, PKCE protects the code
exchange step.

---

## Login Challenge

A short-lived opaque token Hydra generates when it needs the Login App to authenticate a user. It represents a paused
OAuth flow.

**Flow:**

```
1. Hydra receives /oauth2/auth, determines login is needed
2. Hydra creates a record capturing the full auth request context
   (client, scopes, redirect_uri, PKCE challenge, login_hint, etc.)
3. Hydra generates a challenge token and redirects to Login App:
   GET /login?login_challenge=abc123
4. Login App calls Hydra admin API to fetch the context:
   GET /admin/oauth2/auth/requests/login?login_challenge=abc123
5. Login App authenticates the user
6. Login App accepts the challenge, passing the subject (user UUID):
   PUT /admin/oauth2/auth/requests/login/accept?login_challenge=abc123
   { "subject": "uuid-123", "remember": true }
7. Hydra resumes the OAuth flow
```

If the Login App rejects the challenge (wrong credentials, MFA failure), Hydra returns an error to the client app.

---

## Consent Challenge

The same pause/resume pattern as the login challenge, but for the consent step. Issued by Hydra after login is accepted,
directed at the Consent App.

**Flow:**

```
1. Hydra accepts the login → issues consent_challenge → redirects to Consent App:
   GET /consent?consent_challenge=xyz789
2. Consent App calls Hydra admin API to fetch context:
   GET /admin/oauth2/auth/requests/consent?consent_challenge=xyz789
3. Consent App resolves roles/permissions from IDM-2
4. Consent App accepts the challenge, injecting token claims:
   PUT /admin/oauth2/auth/requests/consent/accept?consent_challenge=xyz789
   {
     "grant_scope": ["openid", "offline_access"],
     "session": {
       "access_token": {
         "tenant_id": "acmecorp",
         "roles": ["admin"],
         "permissions": ["feature:export", "feature:send-campaign"]
       }
     }
   }
5. Hydra issues the authorization code → OAuth flow completes
```

For first-party SaaS clients, the Consent App auto-accepts — no UI is shown to the user.
