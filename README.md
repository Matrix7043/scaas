# SCaaS API Reference

**Base URL:** `http://localhost:8080`

All endpoints except `/auth/**` require a JWT passed as `Authorization: Bearer <token>`. Tokens expire after 1 hour. All responses use `Content-Type: application/json`.

---

## Standard Error Response

All errors return this shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Function not found",
  "path": "/functions/abc",
  "timestamp": "2025-01-01T12:00:00"
}
```

---

## Authentication

### POST /auth/register
Create a new user account. No authentication required.

**Request Body**
| Field | Type | Required | Constraints |
|---|---|---|---|
| username | string | Yes | Unique, max 100 chars |
| firstName | string | Yes | Max 50 chars |
| lastName | string | Yes | Max 50 chars |
| email | string | Yes | Valid email format, unique |
| password | string | Yes | 6–12 characters |

**Responses**
| Status | Condition | Body |
|---|---|---|
| 201 Created | Success | `"User registered successfully"` |
| 400 Bad Request | Validation failure | Error response with field message |
| 400 Bad Request | Email or username taken | `"Email already exists"` |

> Passwords are stored as BCrypt hashes and are never persisted in plain text.

---

### POST /auth/login
Authenticate and receive a JWT token. No authentication required.

**Request Body**
| Field | Type | Required | Constraints |
|---|---|---|---|
| email | string | Yes | Registered user email |
| password | string | Yes | Account password |

**Responses**
| Status | Condition | Body |
|---|---|---|
| 200 OK | Credentials valid | JWT token string (plain text) |
| 400 Bad Request | Wrong email or password | `"Wrong Email or password"` |

> Use the returned token as: `Authorization: Bearer <token>`

---

## Functions

All `/functions` endpoints require a valid JWT. Resources are **scoped per user** — users can only see and manage their own functions.

---

### POST /functions
Register a new serverless function.

**Request Body**
| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| name | string | Yes | — | Max 100 chars |
| runtime | Runtime | Yes | — | `PYTHON` |
| entryPoint | string | No | `"handler"` | Max 100 chars |
| cpuCores | double | No | `0.5` | 0.5 – 4.0 |
| mem | int | No | `256` | 100 – 1024 MB |
| pids | int | No | `50` | 10 – 64 |

**Response Body (201 Created)**
| Field | Type | Description |
|---|---|---|
| id | UUID | Unique function identifier |
| name | string | Display name |
| runtime | Runtime | `PYTHON` |
| entryPoint | string | Configured handler name |
| cpuCores | double | Allocated CPU cores |
| memory | int | Allocated memory in MB |
| pid | int | Max process count |
| deploymentStatus | DeploymentStatus | Initial value: `NOT_DEPLOYED` |
| invocationURL | string | `null` until deployed |
| hasArtifact | boolean | `false` until artifact uploaded |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |
| deployedAt | LocalDateTime | `null` until deployed |

---

### GET /functions
Paginated list of the caller's functions. Soft-deleted functions are excluded.

**Query Parameters**
| Parameter | Type | Default | Description |
|---|---|---|---|
| page | int | 0 | Zero-based page index |
| size | int | 20 | Items per page |
| sort | string | — | e.g. `sort=name,asc` |

Returns a Spring `Page` wrapper containing `FunctionResponse` objects (same schema as POST /functions response).

---

### GET /functions/{id}
Retrieve a single function by UUID.

**Path Parameters**
| Parameter | Type | Description |
|---|---|---|
| id | UUID | Function identifier |

**Responses**
| Status | Condition |
|---|---|
| 200 OK | Returns `FunctionResponse` JSON |
| 404 Not Found | Function not found or belongs to another user |

---

### PATCH /functions/{id}
Update a function's name, entry point, or resource limits. All fields are optional — only provided non-null fields are applied.

**Path Parameters**
| Parameter | Type | Description |
|---|---|---|
| id | UUID | Function identifier |

**Request Body**
| Field | Type | Constraints |
|---|---|---|
| name | string | Max 100 chars |
| entryPoint | string | Max 100 chars |
| cpuCores | double | 0.5 – 4.0 |
| mem | int | 100 – 1024 MB |
| pids | int | 10 – 64 |

**Side Effects**
- If status is `DEPLOYED` or `FAILED`, it is automatically set to `OUTDATED`.
- If status is `DEPLOYING`, the request is rejected with `409 Conflict`.

**Responses**
| Status | Condition |
|---|---|
| 200 OK | Returns updated `FunctionResponse` JSON |
| 404 Not Found | Function not found |
| 409 Conflict | `"Function cannot be updated when deployment is in progress"` |

---

### DELETE /functions/{id}
Soft-delete a function. Sets `deletedAt` — the database record is retained for audit purposes.

**Path Parameters**
| Parameter | Type | Description |
|---|---|---|
| id | UUID | Function identifier |

**Side Effects**
- Both current and deployed container instances are torn down via `DeploymentService`.
- The DB record is kept with `deletedAt` set.
- Rejected with `409` if status is `DEPLOYING`.

**Responses**
| Status | Condition |
|---|---|
| 204 No Content | Successfully deleted |
| 404 Not Found | Function not found |
| 409 Conflict | `"Function cannot be deleted when deployment is in progress"` |

---

### POST /functions/{id}/artifacts
Upload or replace the function's Python source file. Request must be `multipart/form-data` with a field named `file`.

**Path Parameters**
| Parameter | Type | Description |
|---|---|---|
| id | UUID | Function identifier |

**Form Data**
| Field | Type | Description |
|---|---|---|
| file | MultipartFile | `.py` files only |

**Side Effects**
- File is SHA-256 hashed. If hash matches the current hash, upload is rejected (no changes detected).
- Status `DEPLOYED`, `NOT_DEPLOYED`, or `FAILED` → automatically set to `OUTDATED`.
- If no prior artifact: file is saved to storage and `storagePath` is set.
- If a prior artifact exists: file overwrites it in-place at the same path.

**Responses**
| Status | Condition |
|---|---|
| 200 OK | Artifact stored successfully (empty body) |
| 400 Bad Request | Empty file, wrong extension, or no changes detected |
| 404 Not Found | Function not found |
| 409 Conflict | `"Artifact cannot be updated when deployment is in progress"` |
| 500 Internal Server Error | Storage I/O failure |

---

### POST /functions/{id}/deploy
Deploy or redeploy the function as a container.

**Path Parameters**
| Parameter | Type | Description |
|---|---|---|
| id | UUID | Function identifier |

**Deployment Flow**
1. Validate — artifact on disk, `hashCode` present, not already deploying, not already deployed with no new changes.
2. Status set to `DEPLOYING` (persisted immediately, guards against concurrent deploys via optimistic lock).
3. `DeploymentService.deploy()` is called synchronously.
4. **Success:** status → `DEPLOYED`, `deployedHashcode` synced, `invocationURL` set, `deployedAt` set.
5. **Failure:** status → `FAILED`.

**Response Body (200 OK)**
| Field | Type | Description |
|---|---|---|
| id | UUID | Function identifier |
| name | string | Function name |
| invocationURL | string | Container endpoint for invoking the function |
| status | DeploymentStatus | `DEPLOYED` on success |
| deployedAt | LocalDateTime | Deployment completion timestamp |

**Responses**
| Status | Condition |
|---|---|
| 200 OK | Deployment succeeded |
| 404 Not Found | Function, artifact, or hashCode not found |
| 409 Conflict | Already deploying, or already deployed with no changes |
| 500 Internal Server Error | `"Deployment Failed"` |

---

## Enumerations

### Runtime
| Value | Description |
|---|---|
| `PYTHON` | Python function runtime. Currently the only supported runtime. |

### DeploymentStatus
| Value | Meaning |
|---|---|
| `NOT_DEPLOYED` | Function registered but never deployed. Artifact may not exist yet. |
| `DEPLOYING` | Deployment in progress. All modifications are blocked. |
| `DEPLOYED` | Latest deployment succeeded. `invocationURL` is active. |
| `OUTDATED` | A newer artifact or config change exists. Redeploy required. |
| `FAILED` | Last deployment attempt failed. Review logs before retrying. |

---

## Deployment Status State Machine

| Trigger | Transition | Notes |
|---|---|---|
| `POST /functions` | → `NOT_DEPLOYED` | Initial state on creation |
| Upload artifact | `DEPLOYED` / `NOT_DEPLOYED` / `FAILED` → `OUTDATED` | Hash change detected |
| Update function config | `DEPLOYED` / `FAILED` → `OUTDATED` | Resource or config change |
| `POST .../deploy` | any → `DEPLOYING` | Locked; no modifications allowed |
| Deployment success | `DEPLOYING` → `DEPLOYED` | `invocationURL` set, `deployedAt` set |
| Deployment failure | `DEPLOYING` → `FAILED` | Error state, retryable |

---

## Endpoint Quick Reference

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | None | Register a new user |
| `POST` | `/auth/login` | None | Login and receive JWT |
| `POST` | `/functions` | JWT | Create a new function |
| `GET` | `/functions` | JWT | List all functions (paginated) |
| `GET` | `/functions/{id}` | JWT | Get function by ID |
| `PATCH` | `/functions/{id}` | JWT | Update function configuration |
| `DELETE` | `/functions/{id}` | JWT | Soft-delete a function |
| `POST` | `/functions/{id}/artifacts` | JWT | Upload Python artifact |
| `POST` | `/functions/{id}/deploy` | JWT | Deploy the function |
