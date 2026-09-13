# catch-up-later — Authentication Starter Kit

A small, self-hosted Spring Boot MVC application with a production-minded,
session-based authentication module: registration, login, logout, password
hashing, database-backed users and roles, Flyway migrations, server-side
authorization, consistent JSON errors, and integration tests against a real
SQLite database.

## Stack

- Kotlin, Java 25 toolchain
- Spring Boot 4 / Spring MVC (servlet), Spring Security 7, Spring Data JPA
- Flyway migrations, SQLite (production and tests)
- Bean Validation, springdoc-openapi
- JUnit 5, MockMvc, and a real-servlet-container integration test

Reactive/WebFlux is intentionally not used.

## Project layout

```
com.bagusna.catchuplater
├── common/error          # ApiError, ApiException hierarchy, @RestControllerAdvice
├── config                # SecurityConfig, SecurityProperties, JSON security handlers,
│                         # session cookie customizer, required-data validator
├── auth
│   ├── api               # AuthController, request/response DTOs
│   ├── application        # AuthenticationService (login), CurrentUserService
│   └── security           # AppUserPrincipal, DbUserDetailsService
├── user
│   ├── api/dto            # RegistrationRequest, CurrentUserResponse
│   ├── application        # UserRegistrationService
│   ├── domain             # User, Role, RoleNames
│   └── persistence        # UserRepository, RoleRepository
└── admin/api              # AdminController (ROLE_ADMIN example)
```

Each feature keeps its domain, persistence, application and API layers separate.
Controllers are thin and delegate to `@Transactional` application services.
Persistence entities are never returned from controllers.

## Local setup

Requirements: JDK 25 (the Gradle toolchain handles the compiler) and the
Gradle wrapper.

```bash
# The SQLite database lives in ./data/app.db. The directory is created for you
# by the committed .gitkeep, but you can also create it explicitly:
mkdir -p data

# Run against plain HTTP locally (disables the Secure cookie flag):
./gradlew bootRun --args='--spring.profiles.active=local'
```

The default profile expects HTTPS because session cookies are `Secure` by
default. Use the `local` profile (or set `app.security.session.cookie-secure=false`)
when running over plain HTTP.

Flyway runs automatically at startup and creates the schema. Hibernate runs in
`ddl-auto: validate`, so the entity model is checked against the migrated schema
and the application fails fast on a mismatch.

### Database

- `spring.datasource.url` defaults to `jdbc:sqlite:./data/app.db`.
- Foreign keys are enabled per connection with
  `spring.datasource.hikari.connection-init-sql: PRAGMA foreign_keys=ON`.
- Migrations live in `src/main/resources/db/migration`.

## Configuration

All settings can be supplied through the usual Spring Boot mechanisms
(`application.yaml`, environment variables, command line, external config).

| Property | Default | Purpose |
| --- | --- | --- |
| `app.security.session.cookie-name` | `CUL_SESSION` | Session cookie name |
| `app.security.session.cookie-secure` | `true` | `Secure` cookie flag (set `false` for local HTTP) |
| `app.security.session.cookie-same-site` | `Lax` | `Lax`, `Strict`, `None` or `Omitted` |
| `app.security.session.timeout` | `30m` | Session timeout |
| `app.security.registration.enabled` | `true` | Toggle self-registration |
| `app.security.registration.min-password-length` | `12` | Minimum password length |
| `app.security.registration.max-password-length` | `128` | Maximum password length |

`SecurityProperties` is `@Validated` and rejects invalid values at startup
(for example, `SameSite=None` without `cookie-secure=true`, or an unknown
`cookie-same-site`). `RequiredDataValidator` additionally fails fast if the
`ROLE_USER` / `ROLE_ADMIN` rows are missing.

## API

All request and response bodies are JSON. Errors share one envelope:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": {
      "fieldErrors": [
        { "field": "email", "message": "must be a well-formed email address" }
      ]
    },
    "requestId": "req_1f2c9a7b"
  }
}
```

Every response also carries an `X-Request-ID` header; a client-supplied
`X-Request-ID` is echoed back and included in the envelope.

New endpoints are served under the versioned base `/api/v1`. The existing auth
endpoints still live under `/api/auth` and are moved to `/api/v1/auth` in the
identity slice.

Stable error codes: `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `DUPLICATE_EMAIL`,
`WEAK_PASSWORD`, `REGISTRATION_DISABLED`, `INVALID_CREDENTIALS`,
`UNAUTHENTICATED`, `ACCESS_DENIED`, `NOT_FOUND`, `METHOD_NOT_ALLOWED`,
`UNSUPPORTED_MEDIA_TYPE`, `INTERNAL_ERROR`.

### CSRF

State-changing requests are protected by a double-submit cookie. Fetch a token
first and send it back in the `X-XSRF-TOKEN` header. The same token is written
to the `XSRF-TOKEN` cookie.

```bash
curl -c cookies.txt http://localhost:8080/api/auth/csrf
# {"headerName":"X-XSRF-TOKEN","parameterName":"_csrf","token":"..."}
```

### Register

```bash
curl -b cookies.txt -c cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: <token>' \
  -d '{"email":"user@example.com","password":"correct-horse-battery-staple","displayName":"User"}' \
  http://localhost:8080/api/auth/register
```

`201 Created` with `{ "id", "email", "displayName", "roles" }`. Duplicate emails
return `409 DUPLICATE_EMAIL`; the database enforces case-insensitive uniqueness.

### Login

`POST /api/auth/login` with `{ "email", "password" }`. On success the security
context is stored in the HTTP session and a hardened session cookie is issued.
Invalid credentials return `401 INVALID_CREDENTIALS` with a generic
`"Invalid email or password."` message, whether or not the email exists.

### Current user

`GET /api/auth/me` returns the authenticated user, or `401 UNAUTHENTICATED`.

### Health

`GET /api/v1/health` is public and returns `{ "status": "UP", "application": "catch-up-later" }`. Dependency-aware readiness checks are added in the operations slice.

### Logout

`POST /api/auth/logout` invalidates the HTTP session, clears the security
context and deletes the session cookie. It returns `204 No Content`.

### Admin example

`GET /api/admin/overview` requires `ROLE_ADMIN` and is protected both by the URL
authorization rule and by `@PreAuthorize`.

## Authentication flow

1. **Registration** (`UserRegistrationService`): normalize the email, check for
   an existing account, enforce the configured password policy, encode the
   password with Spring Security's `DelegatingPasswordEncoder` (BCrypt), attach
   `ROLE_USER`, and insert. The database unique constraint is the final
   authority; a constraint violation is translated into `409`.
2. **Login** (`AuthService`): authenticate with the
   `AuthenticationManager`, apply `ChangeSessionIdAuthenticationStrategy`
   (session-fixation protection), then persist the `SecurityContext` through a
   `DelegatingSecurityContextRepository` (request attribute + HTTP session).
   This mirrors what Spring Security's own authentication filter does.
3. **Authorization**: Spring Security's filter chain enforces the URL rules and
   `@EnableMethodSecurity` enforces `@PreAuthorize`.
4. **Logout**: Spring Security's `LogoutFilter` invalidates the session, clears
   authentication and deletes the session cookie.

Passwords are never stored or logged in plaintext. The password hash is not part
of any API response.

## Roles and bootstrapping an admin

Migrations seed `ROLE_USER` and `ROLE_ADMIN`. Registration always assigns
`ROLE_USER`. To promote an existing account to admin:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@example.com' AND r.name = 'ROLE_ADMIN';
```

## Security assumptions and decisions

- **Transport**: production traffic must be HTTPS. Session cookies default to
  `Secure`, `HttpOnly`, `SameSite=Lax` with a 30-minute timeout.
- **Sessions**: server-side servlet sessions are the source of truth; there is
  no in-memory user store, no JWT and no external session store.
- **CSRF**: enabled for all state-changing requests using a cookie-backed token.
  `CookieCsrfTokenRepository.withHttpOnlyFalse()` is paired with the plain
  `CsrfTokenRequestAttributeHandler`; the token in the cookie, the response body
  and the request header are identical, which keeps JSON clients simple.
- **Credentials**: `PasswordEncoderFactories.createDelegatingPasswordEncoder()`
  (BCrypt today, upgradable later). No custom cryptography.
- **Enumeration**: login failures are indistinguishable between unknown emails
  and wrong passwords. Registration intentionally reports duplicates because the
  user must know the account exists.
- **Errors**: internal exceptions are logged with full detail server-side and
  never returned to clients. Filter-level 401/403 responses use the same
  `ApiError` shape as MVC errors.
- **Data**: uniqueness and role links are enforced by database constraints and
  foreign keys, not only by application checks.

## Tests

```bash
./gradlew test
```

The suite runs Flyway migrations against real, file-based SQLite databases
(including the production migration scripts) and covers:

- registration success, duplicate (including different casing), validation
  errors, password-policy enforcement and CSRF rejection
- login success, invalid password, unknown email, invalid payload, session
  creation and logout/session invalidation
- authorization for regular users, admins and anonymous callers
- persistence: database-enforced case-insensitive uniqueness, role links, audit
  timestamps and BCrypt password hashing
- security configuration validation and the real servlet container's session
  cookie attributes
