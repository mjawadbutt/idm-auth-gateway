# Login Flow Cases

All authentication paths ultimately result in Hydra issuing an OAuth2/OIDC token. SAML and OIDC federation are
alternative authentication mechanisms that plug into the Login App's auth step — they are not parallel token paths.

---

## Case 1 — User navigates directly to a SaaS app (SP-initiated, most common)

User clicks app URL or bookmark → app has no session → redirects to gateway:

```
GET https://auth.yourdomain.com/oauth2/auth
  ?response_type=code&client_id=someapp&scope=openid&state=xyz&redirect_uri=...
```

Hydra creates login challenge → redirects to Login App:

```
GET https://auth.yourdomain.com/login?login_challenge=abc123
```

Login App resolves tenant from `login_hint` (primary) or subdomain (complementary) → shows login form.

### Case 1a — Local authentication (username/password)

Login App queries IDM-2 → tenant config says local auth.

#### Case 1a-i — No MFA configured

- Credentials verified against IDM-2
- Login App accepts Hydra login challenge
- Hydra redirects to Consent App → consent accepted → code issued
- SaaS app exchanges code for tokens → user lands in app

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
