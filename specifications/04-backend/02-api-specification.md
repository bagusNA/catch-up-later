# API Specification

The API is illustrative and should be implemented as versioned REST endpoints under `/api/v1`.

## Authentication

- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/me`

Authentication mechanism may be secure cookie sessions or short-lived access tokens with refresh tokens. The implementation MUST avoid exposing long-lived secrets to page content.

## Content items

- `GET /content-items`
- `POST /content-items` — normally created by capture processing
- `GET /content-items/{id}`
- `PATCH /content-items/{id}`
- `DELETE /content-items/{id}`
- `POST /content-items/{id}/favorite`
- `DELETE /content-items/{id}/favorite`
- `POST /content-items/{id}/status`
- `GET /content-items/{id}/artifacts`
- `GET /content-items/{id}/reader`

List query parameters:

- `q`
- `contentType`
- `status`
- `favorite`
- `tag`
- `sort`
- `page`
- `pageSize`

All results MUST be scoped to the authenticated user.

## Capture

- `POST /captures`
- `GET /captures/{captureId}`
- `POST /captures/{captureId}/retry`
- `DELETE /captures/{captureId}`

`POST /captures` MUST accept an idempotency key.

Suggested response:

```json
{
  "captureId": "cap_123",
  "status": "PROCESSING",
  "contentItemId": null,
  "artifactId": null,
  "warnings": []
}
```

## Reader

- `GET /content-items/{id}/reader`
- `GET /artifacts/{artifactId}/manifest`
- `GET /artifacts/{artifactId}/assets/{assetKey}`
- `GET /artifacts/{artifactId}/file`

Reader endpoints MUST enforce ownership and artifact availability.

## Reading state

- `GET /content-items/{id}/reading-state`
- `PUT /content-items/{id}/reading-state`

Suggested request:

```json
{
  "status": "IN_PROGRESS",
  "progressPercent": 42.5,
  "position": {
    "type": "ARTICLE_TEXT_OFFSET",
    "value": 12000
  }
}
```

## Tags

- `GET /tags`
- `POST /tags`
- `DELETE /tags/{id}`
- `PUT /content-items/{id}/tags`

## Error format

```json
{
  "error": {
    "code": "CAPTURE_INVALID_PACKAGE",
    "message": "The capture package is invalid.",
    "details": {},
    "requestId": "req_123"
  }
}
```

The API MUST not expose internal stack traces, filesystem paths, SQL, or secrets.
