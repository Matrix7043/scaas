/**
 * api.js — Central API layer for SCaaS frontend
 *
 * All backend calls go through here. Every function:
 *  1. Attaches the JWT token from localStorage (if present)
 *  2. Makes a fetch() call to our Vite proxy → Spring Boot at :8080
 *  3. Returns parsed JSON (or throws an error with a clean message)
 *
 * The Vite proxy is configured in vite.config.js:
 *   /api/** → http://localhost:8080/**
 * So calling fetch('/api/functions') hits http://localhost:8080/functions
 */

const BASE = "/api";

// ─── Internal helper ──────────────────────────────────────────────────────────

/**
 * Core fetch wrapper. Used by all API functions.
 * @param {string} path      - e.g. "/auth/login" or "/functions"
 * @param {object} options   - standard fetch options (method, body, headers, etc.)
 * @param {boolean} auth     - whether to attach the JWT Authorization header
 */
async function request(path, options = {}, auth = true) {
    const headers = {
        "Content-Type": "application/json",
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

    // For 204 No Content (e.g. DELETE success), return null
    if (response.status === 204) return null;

    // Parse the response body
    const text = await response.text();
    let data;
    try {
        data = JSON.parse(text);
    } catch {
        data = text; // some endpoints return plain text (e.g. register success message)
    }

    // If HTTP error, throw with backend's message
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
 * @param {{ username, firstName, lastName, email, password }} body
 */
export async function register(body) {
    return request("/auth/register", {
        method: "POST",
        body: JSON.stringify(body),
    }, false);
}

/**
 * Login and receive a JWT token string.
 * POST /auth/login
 * Automatically saves the token to localStorage on success.
 * @param {{ email, password }} body
 * @returns {string} JWT token
 */
export async function login(body) {
    const token = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify(body),
    }, false);
    // The backend returns a plain JWT string
    localStorage.setItem("scaas_token", token);
    return token;
}

/**
 * Remove the JWT token from storage (client-side logout).
 * There is no logout endpoint — the token just expires server-side.
 */
export function logout() {
    localStorage.removeItem("scaas_token");
}

/**
 * Check if a token is currently stored.
 */
export function getToken() {
    return localStorage.getItem("scaas_token");
}

// ─── Functions ────────────────────────────────────────────────────────────────

/**
 * Create a new serverless function.
 * POST /functions
 * @param {{ name, runtime?, entryPoint?, cpuCores?, mem?, pids? }} body
 * @returns {FunctionResponse}
 */
export async function createFunction(body) {
    return request("/functions", {
        method: "POST",
        body: JSON.stringify(body),
    });
}

/**
 * List all functions for the authenticated user (paginated).
 * GET /functions?page=0&size=50
 * @returns Spring Page object — use `.content` to get the array of functions
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
 * Update a function's config (name, entryPoint, resources).
 * PATCH /functions/{id}
 * Only fields you include will be updated.
 * @param {string} id
 * @param {{ name?, entryPoint?, cpuCores?, mem?, pids? }} body
 */
export async function updateFunction(id, body) {
    return request(`/functions/${id}`, {
        method: "PATCH",
        body: JSON.stringify(body),
    });
}

/**
 * Soft-delete a function.
 * DELETE /functions/{id}
 * Returns null (204 No Content).
 */
export async function deleteFunction(id) {
    return request(`/functions/${id}`, { method: "DELETE" });
}

/**
 * Upload a Python .py file as the function's artifact.
 * POST /functions/{id}/artifacts (multipart/form-data)
 * @param {string} id   - function UUID
 * @param {File} file   - the .py File object from an <input type="file"> or a Blob
 */
export async function uploadArtifact(id, file) {
    const token = localStorage.getItem("scaas_token");
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${BASE}/functions/${id}/artifacts`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
        // NOTE: Do NOT set Content-Type here — the browser sets it automatically
        //       with the correct multipart boundary for FormData.
    });

    if (response.status === 204 || response.status === 200) return null;

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
 * Deploy (or redeploy) a function as a container.
 * POST /functions/{id}/deploy
 * @returns {{ id, name, invocationURL, status, deployedAt }}
 */
export async function deployFunction(id) {
    return request(`/functions/${id}/deploy`, { method: "POST" });
}
