const BASE = "/api";

// ─── Internal helper ──────────────────────────────────────────────────────────

async function request(path, options = {}, auth = true) {
  const headers = {
    "Content-Type": "application/json",
    // X-Requested-With tells Spring Security this is an AJAX request,
    // which bypasses CSRF token checks on the server side.
    "X-Requested-With": "XMLHttpRequest",
    ...(options.headers || {}),
  };

  if (auth) {
    const token = localStorage.getItem("scaas_token");
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${BASE}${path}`, {
    ...options,
    headers,
  });

  // 204 No Content (e.g. DELETE success)
  if (response.status === 204) return null;

  const text = await response.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }

  if (!response.ok) {
    const msg =
      (typeof data === "object" && data?.message) ||
      `HTTP ${response.status}: ${response.statusText}`;
    throw new Error(msg);
  }

  return data;
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

/**
 * Register a new user account.
 * POST /auth/register
 */
export async function register(body) {
  return request("/auth/register", {
    method: "POST",
    body: JSON.stringify(body),
  }, false);
}

/**
 * Login — backend returns { accessToken, refreshToken }.
 * Saves both to localStorage.
 * POST /auth/login
 */
export async function login(body) {
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify(body),
  }, false);
  // Backend actually returns just the raw JWT string
  localStorage.setItem("scaas_token", data);
  return data;
}

/**
 * Refresh access token using stored refresh token.
 * POST /auth/refresh
 */
export async function refreshToken() {
  const refreshToken = localStorage.getItem("scaas_refresh_token");
  if (!refreshToken) throw new Error("No refresh token stored");
  const data = await request("/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  }, false);
  localStorage.setItem("scaas_token", data.accessToken);
  localStorage.setItem("scaas_refresh_token", data.refreshToken);
  return data;
}

/**
 * Logout — invalidates current refresh token server-side.
 * POST /auth/logout
 */
export async function logout() {
  const token = localStorage.getItem("scaas_refresh_token");
  try {
    await request("/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken: token }),
    });
  } finally {
    localStorage.removeItem("scaas_token");
    localStorage.removeItem("scaas_refresh_token");
  }
}

/**
 * Logout all devices.
 * POST /auth/logout-all
 */
export async function logoutAll() {
  try {
    await request("/auth/logout-all", { method: "POST" });
  } finally {
    localStorage.removeItem("scaas_token");
    localStorage.removeItem("scaas_refresh_token");
  }
}

export function getToken() {
  return localStorage.getItem("scaas_token");
}

// ─── Functions ────────────────────────────────────────────────────────────────

/**
 * Create a new serverless function.
 * POST /functions
 */
export async function createFunction(body) {
  return request("/functions", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/**
 * List all functions (paginated).
 * GET /functions?page=0&size=50&sort=createdAt,desc
 * Returns Spring Page — use .content for the array.
 */
export async function listFunctions(page = 0, size = 50) {
  return request(`/functions?page=${page}&size=${size}&sort=createdAt,desc`);
}

/**
 * Get a single function by UUID.
 * GET /functions/{id}
 */
export async function getFunction(id) {
  return request(`/functions/${id}`);
}

/**
 * Update function config.
 * PATCH /functions/{id}
 */
export async function updateFunction(id, body) {
  return request(`/functions/${id}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

/**
 * Soft-delete a function.
 * DELETE /functions/{id} → 204 No Content
 */
export async function deleteFunction(id) {
  return request(`/functions/${id}`, { method: "DELETE" });
}

/**
 * Upload a .py artifact for a function.
 * POST /functions/{id}/artifacts (multipart/form-data)
 * Returns 200 with empty body on success.
 */
export async function uploadArtifact(id, file) {
  const token = localStorage.getItem("scaas_token");
  const formData = new FormData();
  formData.append("file", file);

  // multipart upload — also needs X-Requested-With for CSRF bypass
  const response = await fetch(`${BASE}/functions/${id}/artifacts`, {
    method: "POST",
    headers: {
      "X-Requested-With": "XMLHttpRequest",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: formData,
    // Do NOT set Content-Type — browser sets multipart boundary automatically
  });

  if (response.status === 200 || response.status === 204) return null;

  const text = await response.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }

  if (!response.ok) {
    const msg = (typeof data === "object" && data?.message) || `HTTP ${response.status}`;
    throw new Error(msg);
  }

  return data;
}

/**
 * Fetch the raw artifact (.py source) for a function.
 * GET /functions/{id}/artifacts
 * Returns the file content as a plain string.
 */
export async function getArtifact(id) {
  const token = localStorage.getItem("scaas_token");
  const response = await fetch(`${BASE}/functions/${id}/artifacts`, {
    method: "GET",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (response.status === 404) return null; // no artifact yet
  if (!response.ok) {
    const text = await response.text();
    let data;
    try { data = JSON.parse(text); } catch { data = text; }
    const msg = (typeof data === "object" && data?.message) || `HTTP ${response.status}`;
    throw new Error(msg);
  }

  return response.text(); // raw Python source
}

/**
 * Deploy (or redeploy) a function.
 * POST /functions/{id}/deploy
 * Returns DeploymentResponse: { id, name, invocationURL, status, deployedAt }
 */
export async function deployFunction(id) {
  return request(`/functions/${id}/deploy`, { method: "POST" });
}

/**
 * Invoke a deployed function via its invocationURL.
 * POST <invocationURL>
 * The invocationURL is on the runner service (:8080), not the main API (:8081).
 */
export async function invokeFunction(invocationURL, payload = {}) {
  const token = localStorage.getItem("scaas_token");
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const response = await fetch(invocationURL, {
    method: "POST",
    headers,
    body: JSON.stringify({
      version: "v1",
      event: payload,
      context: {},
    }),
  });

  if (!response.ok) {
    const text = await response.text();
    let data;
    try { data = JSON.parse(text); } catch { data = text; }
    const msg = (typeof data === "object" && data?.message) || `HTTP ${response.status}: ${response.statusText}`;
    throw new Error(msg);
  }

  const text = await response.text();
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}