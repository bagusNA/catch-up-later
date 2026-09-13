# Slice 1 — Identity

**Milestone:** `S1 Identity`
**Outcome:** A user can log in on the web, log out, and connect the extension
to the backend with a secure token. First-run setup creates the only account.
**Depends on:** `S0`.

## Goal

Unify the existing backend auth with the web shell and the extension, under the
versioned API and the first-run bootstrap model.

## In scope

- `/api/v1` auth endpoints and error envelope for all auth flows.
- First-run bootstrap; open registration disabled by default.
- Bearer access/refresh tokens for the extension; session + CSRF for web.
- Login, logout, onboarding, and protected app shell on the web.
- Extension options page: backend URL, connect, logout, secure token storage.
- Account/settings basics (display name, password change, logout).

## Out of scope

- Multi-user invites and admin user management (future).
- Reader defaults and storage info (`S11`).

## Acceptance criteria

- [ ] With an empty database, the web app routes to a setup screen that creates
      the first account; setup is unavailable afterward.
- [ ] Login succeeds with correct credentials and fails generically otherwise.
- [ ] Unauthenticated web routes redirect to login; authenticated routes render
      the shell with Library, Favorites, Tags, Settings.
- [ ] The extension can log in, stores tokens securely, refreshes them, calls an
      authenticated endpoint, and logs out.
- [ ] A revoked/expired extension token returns `401` and the client refreshes
      or prompts re-login.
- [ ] Existing backend auth tests pass against `/api/v1` and the new envelope.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S1.1 | Migrate auth endpoints to `/api/v1` and the spec error envelope | backend | feature |
| S1.2 | Add first-run bootstrap and disable open registration | backend | feature |
| S1.3 | Add access/refresh token issuance, rotation, and revocation | backend | feature |
| S1.4 | Build web login page and session-aware auth composable | frontend | feature |
| S1.5 | Build initial-setup/onboarding screen | frontend | feature |
| S1.6 | Build authenticated app shell and navigation (Library, Favorites, Tags, Settings) | frontend | feature |
| S1.7 | Add route protection middleware and 401 handling | frontend | feature |
| S1.8 | Build extension options/connection UI with secure token storage | extension | feature |
| S1.9 | Add extension background token refresh and authenticated request helper | extension | feature |
| S1.10 | Build account settings (display name, password change, logout) | frontend | feature |
| S1.11 | Add auth integration tests across web and token paths | backend | test |

## Verification

- Fresh database → setup → login → logout cycle.
- Extension connects and survives an access-token expiry via refresh.
- Ownership checks resolve the same user for session and token callers.

## Risks

- Token rotation correctness (reuse detection, clock skew). Keep refresh
  single-flight in the background worker and test it.
