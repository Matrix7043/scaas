import { useState, useEffect } from "react";
import { Plus, Trash2, Eye, Edit, X, User, LogOut, Cpu, HardDrive, Layers, RefreshCw, AlertCircle } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import {
    listFunctions,
    createFunction as apiCreate,
    deleteFunction as apiDelete,
} from "../api";

// ── Reusable focus-styled input ───────────────────────────────────────────────
function FocusInput({ style, ...props }) {
    const [focused, setFocused] = useState(false);
    return (
        <input
            {...props}
            style={{
                ...style,
                borderColor: focused ? "rgba(46,160,67,0.5)" : "#1e1e1e",
                boxShadow: focused ? "0 0 0 3px rgba(46,160,67,0.07)" : "none",
                outline: "none",
            }}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
        />
    );
}

// ── Status badge colours ──────────────────────────────────────────────────────
const STATUS_STYLE = {
    NOT_DEPLOYED: { bg: "rgba(255,255,255,0.04)", border: "#2a2a2a", color: "#555", label: "not deployed" },
    DEPLOYING: { bg: "rgba(227,160,0,0.1)", border: "rgba(227,160,0,0.4)", color: "#e3a000", label: "deploying…" },
    DEPLOYED: { bg: "rgba(46,160,67,0.12)", border: "rgba(46,160,67,0.4)", color: "#4ec76a", label: "deployed" },
    OUTDATED: { bg: "rgba(130,90,0,0.12)", border: "rgba(180,130,0,0.35)", color: "#c8960a", label: "outdated" },
    FAILED: { bg: "rgba(255,80,80,0.08)", border: "rgba(255,80,80,0.3)", color: "#ff6b6b", label: "failed" },
};

function StatusBadge({ status }) {
    const s = STATUS_STYLE[status] || STATUS_STYLE.NOT_DEPLOYED;
    return (
        <span style={{
            display: "inline-block",
            backgroundColor: s.bg,
            border: `1px solid ${s.border}`,
            color: s.color,
            padding: "3px 10px",
            borderRadius: "4px",
            fontSize: "10px",
            fontWeight: 700,
            letterSpacing: "0.08em",
        }}>
            {s.label}
        </span>
    );
}

// ── Single table row ──────────────────────────────────────────────────────────
function HoverRow({ fn, onDelete, onOpenEditor }) {
    const [hovered, setHovered] = useState(false);
    const [eyeHov, setEyeHov] = useState(false);
    const [editHov, setEditHov] = useState(false);
    const [delHov, setDelHov] = useState(false);

    return (
        <motion.div
            style={{
                ...S.row,
                background: hovered ? "#111" : "transparent",
                boxShadow: hovered ? "inset 0 0 0 1px rgba(46,160,67,0.06)" : "none",
            }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, x: -12 }}
            transition={{ duration: 0.2 }}
            layout
        >
            {/* Name */}
            <span style={S.rowName}>{fn.name}</span>

            {/* Entry point */}
            <span style={S.rowMono}>
                <span style={{ color: "#555" }}>{fn.entryPoint || "handler"}</span>
                <span style={{ color: "#2ea043", opacity: 0.6 }}>()</span>
            </span>

            {/* Status */}
            <span style={{ textAlign: "center" }}>
                <StatusBadge status={fn.deploymentStatus} />
            </span>

            {/* Resources */}
            <span style={S.rowMono}>
                <span style={{ color: "#444" }}>{fn.cpuCores ?? 0.5}v</span>
                <span style={{ color: "#2a2a2a" }}> · </span>
                <span style={{ color: "#444" }}>{fn.memory ?? 256}MB</span>
            </span>

            {/* Actions */}
            <div style={S.actions}>
                <button
                    title="View (read-only)"
                    style={{ ...S.iconBtn, color: eyeHov ? "#ccc" : "#555", borderColor: eyeHov ? "#2a2a2a" : "#1e1e1e", background: eyeHov ? "#1a1a1a" : "#111" }}
                    onMouseEnter={() => setEyeHov(true)}
                    onMouseLeave={() => setEyeHov(false)}
                    onClick={() => onOpenEditor(fn, "view")}
                >
                    <Eye size={13} />
                </button>
                <button
                    title="Edit"
                    style={{ ...S.iconBtn, color: editHov ? "#2ea043" : "#555", borderColor: editHov ? "rgba(46,160,67,0.3)" : "#1e1e1e", background: editHov ? "rgba(46,160,67,0.06)" : "#111" }}
                    onMouseEnter={() => setEditHov(true)}
                    onMouseLeave={() => setEditHov(false)}
                    onClick={() => onOpenEditor(fn, "edit")}
                >
                    <Edit size={13} />
                </button>
                <button
                    title="Delete"
                    style={{ ...S.deleteBtn, color: delHov ? "#ff4444" : "#ff6b6b", borderColor: delHov ? "rgba(255,80,80,0.5)" : "rgba(255,80,80,0.15)", background: delHov ? "rgba(255,80,80,0.1)" : "rgba(255,80,80,0.04)" }}
                    onMouseEnter={() => setDelHov(true)}
                    onMouseLeave={() => setDelHov(false)}
                    onClick={() => onDelete(fn.id)}
                >
                    <Trash2 size={13} />
                </button>
            </div>
        </motion.div>
    );
}

// ── Profile dropdown panel ────────────────────────────────────────────────────
function ProfilePanel({ user, resources, onLogout, onClose }) {
    const [logoutHov, setLogoutHov] = useState(false);
    const bars = [
        { label: "Functions", icon: <Layers size={12} />, value: resources.count, max: 20, unit: "" },
        { label: "CPU Cores", icon: <Cpu size={12} />, value: resources.cpu, max: 32, unit: " cores" },
        { label: "Memory", icon: <HardDrive size={12} />, value: resources.storage, max: 10240, unit: " MB" },
    ];
    return (
        <motion.div
            style={S.profilePanel}
            initial={{ opacity: 0, y: -8, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.97 }}
            transition={{ duration: 0.18, ease: "easeOut" }}
        >
            <div style={S.profileHeader}>
                <div style={S.profileAvatar}>
                    {(user.name || "?").slice(0, 2).toUpperCase()}
                </div>
                <div>
                    <div style={S.profileName}>{user.name}</div>
                    <div style={S.profileMeta}>{user.email || ""}</div>
                </div>
                <button style={S.profileClose} onClick={onClose}><X size={13} /></button>
            </div>

            <div style={S.profileDivider} />

            <div style={S.profileSection}>
                <div style={S.profileSectionLabel}>Resource Usage</div>
                {bars.map((b) => {
                    const pct = Math.min((b.value / b.max) * 100, 100);
                    const color = pct > 80 ? "#ff6b6b" : pct > 50 ? "#e3a000" : "#2ea043";
                    return (
                        <div key={b.label} style={S.resourceRow}>
                            <div style={S.resourceLabelRow}>
                                <span style={{ color: "#444", display: "flex", alignItems: "center", gap: "5px" }}>
                                    {b.icon}
                                    <span style={S.resourceLabel}>{b.label}</span>
                                </span>
                                <span style={{ ...S.resourceValue, color }}>{b.value}{b.unit}</span>
                            </div>
                            <div style={S.barTrack}>
                                <motion.div
                                    style={{ ...S.barFill, backgroundColor: color }}
                                    initial={{ width: 0 }}
                                    animate={{ width: `${pct}%` }}
                                    transition={{ duration: 0.5, ease: "easeOut" }}
                                />
                            </div>
                        </div>
                    );
                })}
            </div>

            <div style={S.profileDivider} />

            <button
                style={{ ...S.logoutBtn, color: logoutHov ? "#ff6b6b" : "#444", borderColor: logoutHov ? "rgba(255,80,80,0.2)" : "transparent", backgroundColor: logoutHov ? "rgba(255,80,80,0.05)" : "transparent" }}
                onMouseEnter={() => setLogoutHov(true)}
                onMouseLeave={() => setLogoutHov(false)}
                onClick={onLogout}
            >
                <LogOut size={13} />
                <span>Sign out</span>
            </button>
        </motion.div>
    );
}

// ── Main page ─────────────────────────────────────────────────────────────────
function FunctionsPage({ functions, setFunctions, onOpenEditor, user, resources, onLogout }) {
    const [showModal, setShowModal] = useState(false);
    const [showProfile, setShowProfile] = useState(false);
    const [loading, setLoading] = useState(true);
    const [apiError, setApiError] = useState("");   // page-level error banner
    const [creating, setCreating] = useState(false);
    const [deleteId, setDeleteId] = useState(null); // id currently being deleted

    // Form state — fields match the API exactly
    const [form, setForm] = useState({
        name: "", entryPoint: "", cpuCores: "", mem: "", pids: "",
    });
    const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

    // ── Load functions on mount ──────────────────────────────────────────────
    useEffect(() => {
        fetchFunctions();
    }, []);

    async function fetchFunctions() {
        setLoading(true);
        setApiError("");
        try {
            // listFunctions() returns a Spring Page — .content holds the array
            const page = await listFunctions();
            setFunctions(page.content ?? []);
        } catch (err) {
            setApiError(err.message || "Failed to load functions.");
        } finally {
            setLoading(false);
        }
    }

    // ── Create ───────────────────────────────────────────────────────────────
    const closeModal = () => {
        setShowModal(false);
        setForm({ name: "", entryPoint: "", cpuCores: "", mem: "", pids: "" });
    };

    const handleCreate = async () => {
        if (!form.name.trim()) return;
        setCreating(true);
        try {
            const body = {
                name: form.name.trim(),
                runtime: "PYTHON",                    // only supported runtime
                ...(form.entryPoint && { entryPoint: form.entryPoint.trim() }),
                ...(form.cpuCores && { cpuCores: parseFloat(form.cpuCores) }),
                ...(form.mem && { mem: parseInt(form.mem, 10) }),
                ...(form.pids && { pids: parseInt(form.pids, 10) }),
            };
            const newFn = await apiCreate(body);        // POST /functions → FunctionResponse
            setFunctions((prev) => [newFn, ...prev]);   // prepend to list
            closeModal();
        } catch (err) {
            // show error inside the modal
            setForm((f) => ({ ...f, _error: err.message || "Failed to create function." }));
        } finally {
            setCreating(false);
        }
    };

    // ── Delete ───────────────────────────────────────────────────────────────
    const handleDelete = async (id) => {
        setDeleteId(id);
        try {
            await apiDelete(id);                        // DELETE /functions/{id} → 204
            setFunctions((prev) => prev.filter((f) => f.id !== id));
        } catch (err) {
            setApiError(err.message || "Failed to delete function.");
        } finally {
            setDeleteId(null);
        }
    };

    return (
        <div style={S.app} onClick={() => showProfile && setShowProfile(false)}>

            {/* ── TOP BAR ── */}
            <div style={S.topBar}>
                <span style={S.brand}>SCaaS Cloudlet</span>

                <div style={S.topBarRight}>
                    <span style={S.topBarStatus}>Control Panel</span>
                    <div style={S.topBarDivider} />

                    {/* Refresh */}
                    <motion.button
                        style={{ ...S.iconBtnTop, color: loading ? "#2ea043" : "#444" }}
                        onClick={fetchFunctions}
                        whileTap={{ scale: 0.88 }}
                        title="Refresh functions"
                        animate={loading ? { rotate: 360 } : { rotate: 0 }}
                        transition={loading ? { repeat: Infinity, duration: 0.9, ease: "linear" } : {}}
                    >
                        <RefreshCw size={13} />
                    </motion.button>

                    <div style={S.topBarDivider} />

                    {/* Profile */}
                    <div style={{ position: "relative" }} onClick={(e) => e.stopPropagation()}>
                        <motion.button
                            style={{
                                ...S.profileBtn,
                                borderColor: showProfile ? "rgba(46,160,67,0.3)" : "#1e1e1e",
                                color: showProfile ? "#2ea043" : "#555",
                                backgroundColor: showProfile ? "rgba(46,160,67,0.06)" : "transparent",
                            }}
                            onClick={() => setShowProfile((v) => !v)}
                            whileTap={{ scale: 0.95 }}
                            title="Profile & resources"
                        >
                            <User size={13} />
                            <span style={{ fontSize: "11px", letterSpacing: "0.04em" }}>{user?.name}</span>
                        </motion.button>

                        <AnimatePresence>
                            {showProfile && (
                                <ProfilePanel
                                    user={user}
                                    resources={resources}
                                    onLogout={onLogout}
                                    onClose={() => setShowProfile(false)}
                                />
                            )}
                        </AnimatePresence>
                    </div>
                </div>
            </div>

            <div style={S.container}>
                {/* Page header */}
                <div style={S.headerRow}>
                    <div>
                        <h1 style={S.title}>Functions</h1>
                        <p style={S.titleSub}>
                            {loading ? "Loading…" : `${functions.length} function${functions.length !== 1 ? "s" : ""}`}
                        </p>
                    </div>
                    <motion.button
                        style={S.createBtn}
                        onClick={() => setShowModal(true)}
                        whileHover={{ boxShadow: "0 0 18px rgba(46,160,67,0.3)", backgroundColor: "#35b84e" }}
                        whileTap={{ scale: 0.96 }}
                    >
                        <Plus size={13} /> Create Function
                    </motion.button>
                </div>

                {/* Page-level error banner */}
                <AnimatePresence>
                    {apiError && (
                        <motion.div
                            style={S.errorBanner}
                            initial={{ opacity: 0, height: 0 }}
                            animate={{ opacity: 1, height: "auto" }}
                            exit={{ opacity: 0, height: 0 }}
                        >
                            <AlertCircle size={13} />
                            <span>{apiError}</span>
                            <button style={S.errorClose} onClick={() => setApiError("")}><X size={11} /></button>
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* TABLE */}
                <div style={S.table}>
                    <div style={S.tableHeader}>
                        <span>Name</span>
                        <span>Entry Point</span>
                        <span>Status</span>
                        <span>Resources</span>
                        <span>Actions</span>
                    </div>

                    <AnimatePresence>
                        {loading ? (
                            <motion.div style={S.emptyState} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                                <motion.span
                                    animate={{ opacity: [1, 0.3, 1] }}
                                    transition={{ repeat: Infinity, duration: 1 }}
                                >Loading functions…</motion.span>
                            </motion.div>
                        ) : functions.length === 0 ? (
                            <motion.div style={S.emptyState} initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                                <span>No functions yet — create one to get started</span>
                            </motion.div>
                        ) : (
                            functions.map((fn) => (
                                <HoverRow
                                    key={fn.id}
                                    fn={fn}
                                    onDelete={handleDelete}
                                    onOpenEditor={onOpenEditor}
                                    isDeleting={deleteId === fn.id}
                                />
                            ))
                        )}
                    </AnimatePresence>
                </div>
            </div>

            {/* ── CREATE MODAL ── */}
            <AnimatePresence>
                {showModal && (
                    <motion.div
                        style={S.overlay}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={(e) => e.target === e.currentTarget && closeModal()}
                    >
                        <motion.div
                            style={S.card}
                            initial={{ y: 20, opacity: 0, scale: 0.97 }}
                            animate={{ y: 0, opacity: 1, scale: 1 }}
                            exit={{ y: 14, opacity: 0, scale: 0.97 }}
                            transition={{ duration: 0.22, ease: "easeOut" }}
                        >
                            <div style={S.cardAccent} />
                            <div style={S.cardInner}>
                                <div style={S.cardHeader}>
                                    <div>
                                        <h2 style={S.cardTitle}>Create Function</h2>
                                        <p style={S.cardSub}>Python · SCaaS Runtime</p>
                                    </div>
                                    <motion.div
                                        whileHover={{ rotate: 90 }}
                                        transition={{ duration: 0.15 }}
                                        onClick={closeModal}
                                        style={{ cursor: "pointer", color: "#333", display: "flex" }}
                                    >
                                        <X size={16} />
                                    </motion.div>
                                </div>

                                {/* Error inside modal */}
                                <AnimatePresence>
                                    {form._error && (
                                        <motion.div
                                            style={S.modalError}
                                            initial={{ opacity: 0, height: 0 }}
                                            animate={{ opacity: 1, height: "auto" }}
                                            exit={{ opacity: 0, height: 0 }}
                                        >
                                            <AlertCircle size={13} />
                                            <span>{form._error}</span>
                                        </motion.div>
                                    )}
                                </AnimatePresence>

                                <div style={S.form}>
                                    {/* Function name — required */}
                                    <FocusInput
                                        placeholder="Function Name *"
                                        value={form.name}
                                        onChange={set("name")}
                                        style={S.input}
                                    />

                                    {/* Entry point — optional, defaults to "handler" */}
                                    <FocusInput
                                        placeholder="Entry Point (default: handler)"
                                        value={form.entryPoint}
                                        onChange={set("entryPoint")}
                                        style={S.input}
                                    />

                                    {/* Resource limits */}
                                    <div style={S.rowGroup}>
                                        <div style={{ flex: 1 }}>
                                            <FocusInput
                                                type="number"
                                                placeholder="CPU Cores (0.5–4.0)"
                                                value={form.cpuCores}
                                                onChange={set("cpuCores")}
                                                min="0.5" max="4" step="0.5"
                                                style={S.inputHalf}
                                            />
                                        </div>
                                        <div style={{ flex: 1 }}>
                                            <FocusInput
                                                type="number"
                                                placeholder="Memory MB (100–1024)"
                                                value={form.mem}
                                                onChange={set("mem")}
                                                min="100" max="1024" step="64"
                                                style={S.inputHalf}
                                            />
                                        </div>
                                    </div>

                                    <FocusInput
                                        type="number"
                                        placeholder="Max Processes / PIDs (10–64, default: 50)"
                                        value={form.pids}
                                        onChange={set("pids")}
                                        min="10" max="64"
                                        style={S.input}
                                    />
                                </div>

                                <div style={S.modalActions}>
                                    <button style={S.secondaryBtn} onClick={closeModal} disabled={creating}>
                                        Cancel
                                    </button>
                                    <motion.button
                                        style={{ ...S.primaryBtn, opacity: creating ? 0.7 : 1 }}
                                        onClick={handleCreate}
                                        disabled={creating || !form.name.trim()}
                                        whileHover={{ boxShadow: "0 0 20px rgba(46,160,67,0.35)" }}
                                        whileTap={{ scale: 0.96 }}
                                    >
                                        {creating ? (
                                            <motion.span
                                                animate={{ opacity: [1, 0.4, 1] }}
                                                transition={{ repeat: Infinity, duration: 0.9 }}
                                            >Creating…</motion.span>
                                        ) : "Create"}
                                    </motion.button>
                                </div>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

export default FunctionsPage;

// ── Styles ────────────────────────────────────────────────────────────────────
const S = {
    app: { minHeight: "100vh", backgroundColor: "#0b0b0b", color: "#fff", fontFamily: "monospace" },
    topBar: {
        height: "52px", backgroundColor: "#0f0f0f", display: "flex", alignItems: "center",
        padding: "0 22px", borderBottom: "1px solid #1a1a1a", position: "sticky", top: 0,
        zIndex: 50, boxShadow: "0 1px 0 #1a1a1a, 0 4px 24px rgba(0,0,0,0.5)",
    },
    brand: { color: "#2ea043", fontWeight: "bold", letterSpacing: "0.05em", fontSize: "14px" },
    topBarRight: { marginLeft: "auto", display: "flex", alignItems: "center", gap: "10px" },
    topBarStatus: { fontSize: "11px", color: "#333", letterSpacing: "0.07em", textTransform: "uppercase" },
    topBarDivider: { width: "1px", height: "16px", backgroundColor: "#1a1a1a" },
    iconBtnTop: {
        background: "transparent", border: "none", cursor: "pointer",
        display: "flex", alignItems: "center", padding: "4px",
        transition: "color 0.15s ease",
    },
    profileBtn: {
        display: "flex", alignItems: "center", gap: "6px", padding: "5px 11px",
        borderRadius: "6px", border: "1px solid", cursor: "pointer", fontFamily: "monospace",
        transition: "all 0.15s ease",
    },

    // Profile panel
    profilePanel: {
        position: "absolute", top: "calc(100% + 10px)", right: 0, width: "260px",
        backgroundColor: "#111", border: "1px solid #1e1e1e", borderTop: "2px solid #2ea043",
        borderRadius: "10px", boxShadow: "0 0 0 1px #0a0a0a, 0 20px 50px rgba(0,0,0,0.9)",
        zIndex: 200, overflow: "hidden",
    },
    profileHeader: { display: "flex", alignItems: "center", gap: "10px", padding: "16px 16px 14px" },
    profileAvatar: {
        width: "34px", height: "34px", borderRadius: "8px",
        backgroundColor: "rgba(46,160,67,0.1)", border: "1px solid rgba(46,160,67,0.2)",
        color: "#2ea043", display: "flex", alignItems: "center", justifyContent: "center",
        fontSize: "11px", fontWeight: 700, letterSpacing: "0.05em", flexShrink: 0,
    },
    profileName: { fontSize: "12px", fontWeight: 700, color: "#ddd", letterSpacing: "0.02em" },
    profileMeta: { fontSize: "10px", color: "#333", marginTop: "2px", letterSpacing: "0.03em" },
    profileClose: { marginLeft: "auto", background: "transparent", border: "none", color: "#333", cursor: "pointer", display: "flex", padding: "2px", alignItems: "center" },
    profileDivider: { height: "1px", backgroundColor: "#161616" },
    profileSection: { padding: "14px 16px", display: "flex", flexDirection: "column", gap: "12px" },
    profileSectionLabel: { fontSize: "9px", color: "#333", letterSpacing: "0.1em", textTransform: "uppercase" },
    resourceRow: { display: "flex", flexDirection: "column", gap: "5px" },
    resourceLabelRow: { display: "flex", justifyContent: "space-between", alignItems: "center" },
    resourceLabel: { fontSize: "11px", color: "#444", letterSpacing: "0.03em" },
    resourceValue: { fontSize: "11px", fontWeight: 700, letterSpacing: "0.03em" },
    barTrack: { height: "3px", backgroundColor: "#1a1a1a", borderRadius: "2px", overflow: "hidden" },
    barFill: { height: "100%", borderRadius: "2px" },
    logoutBtn: {
        width: "100%", display: "flex", alignItems: "center", gap: "8px", padding: "12px 16px",
        background: "transparent", border: "1px solid transparent", cursor: "pointer",
        fontFamily: "monospace", fontSize: "12px", letterSpacing: "0.04em",
        transition: "all 0.15s ease", borderRadius: "0",
    },

    // Page
    container: { padding: "32px 40px 60px 40px" },
    headerRow: { display: "flex", alignItems: "flex-start", marginBottom: "22px" },
    title: { fontSize: "18px", margin: "0 0 4px", fontWeight: 700, letterSpacing: "0.02em" },
    titleSub: { margin: 0, fontSize: "11px", color: "#333", letterSpacing: "0.06em" },
    createBtn: {
        marginLeft: "auto", backgroundColor: "#2ea043", border: "1px solid #2ea043",
        padding: "9px 15px", borderRadius: "7px", color: "#fff", cursor: "pointer",
        display: "flex", alignItems: "center", gap: "6px", fontSize: "12px",
        fontFamily: "monospace", fontWeight: 700, letterSpacing: "0.04em", transition: "all 0.2s ease",
    },

    // Error banner
    errorBanner: {
        display: "flex", alignItems: "center", gap: "8px",
        backgroundColor: "rgba(255,80,80,0.07)", border: "1px solid rgba(255,80,80,0.25)",
        color: "#ff6b6b", padding: "9px 14px", borderRadius: "8px",
        fontSize: "12px", marginBottom: "16px", overflow: "hidden",
    },
    errorClose: {
        marginLeft: "auto", background: "transparent", border: "none",
        color: "#ff6b6b", cursor: "pointer", display: "flex", padding: "2px",
    },

    // Table
    table: {
        border: "1px solid #161616", borderRadius: "12px", overflow: "hidden",
        background: "#0d0d0d", boxShadow: "0 8px 40px rgba(0,0,0,0.5)",
        transform: "perspective(1200px) rotateX(0.35deg)", transformOrigin: "top center",
    },
    tableHeader: {
        display: "grid", gridTemplateColumns: "repeat(5, 1fr)",
        padding: "11px 20px", backgroundColor: "#0f0f0f",
        fontSize: "10px", letterSpacing: "0.12em", textTransform: "uppercase",
        color: "#555", borderBottom: "1px solid #161616", textAlign: "center",
    },
    row: {
        display: "grid", gridTemplateColumns: "repeat(5, 1fr)",
        padding: "13px 20px", borderTop: "1px solid #111", textAlign: "center",
        alignItems: "center", fontSize: "13px", transition: "background 0.15s ease, box-shadow 0.15s ease",
    },
    rowName: { fontWeight: 700, color: "#f0f0f0", letterSpacing: "0.02em", textAlign: "center", fontSize: "13px" },
    rowMono: { color: "#888", fontSize: "12px", textAlign: "center" },
    actions: { display: "flex", justifyContent: "center", gap: "6px" },
    iconBtn: { border: "1px solid #1e1e1e", padding: "6px", borderRadius: "6px", cursor: "pointer", display: "flex", alignItems: "center", transition: "all 0.15s ease" },
    deleteBtn: { padding: "6px", borderRadius: "6px", cursor: "pointer", display: "flex", alignItems: "center", transition: "all 0.15s ease", border: "1px solid" },
    emptyState: { display: "flex", flexDirection: "column", alignItems: "center", gap: "8px", padding: "64px 20px", color: "#3a3a3a", fontSize: "12px", letterSpacing: "0.08em" },

    // Modal
    overlay: { position: "fixed", inset: 0, backgroundColor: "rgba(0,0,0,0.7)", display: "flex", justifyContent: "center", alignItems: "center", backdropFilter: "blur(6px)", zIndex: 100 },
    card: { width: "480px", backgroundColor: "#111", borderRadius: "14px", border: "1px solid #2a2a2a", boxShadow: "0 0 0 1px #0a0a0a, 0 24px 60px rgba(0,0,0,0.95)", overflow: "hidden" },
    cardAccent: { height: "3px", background: "linear-gradient(90deg, #111 0%, #2ea043 40%, #1a6b2e 100%)" },
    cardInner: { padding: "28px 28px 24px" },
    cardHeader: { display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "18px" },
    cardTitle: { fontSize: "15px", fontWeight: 700, margin: "0 0 4px", letterSpacing: "0.02em" },
    cardSub: { margin: 0, fontSize: "11px", color: "#333", letterSpacing: "0.05em" },
    modalError: { display: "flex", alignItems: "center", gap: "8px", backgroundColor: "rgba(255,80,80,0.07)", border: "1px solid rgba(255,80,80,0.25)", color: "#ff6b6b", padding: "9px 12px", borderRadius: "7px", fontSize: "12px", marginBottom: "14px", overflow: "hidden" },
    form: { display: "flex", flexDirection: "column", gap: "12px" },
    rowGroup: { display: "flex", gap: "12px" },
    input: { padding: "10px 13px", borderRadius: "7px", border: "1px solid #1e1e1e", backgroundColor: "#080808", color: "#ddd", fontFamily: "monospace", fontSize: "13px", transition: "border-color 0.2s ease, box-shadow 0.2s ease", width: "100%", boxSizing: "border-box" },
    inputHalf: { flex: 1, padding: "10px 13px", borderRadius: "7px", border: "1px solid #1e1e1e", backgroundColor: "#080808", color: "#ddd", fontFamily: "monospace", fontSize: "13px", transition: "border-color 0.2s ease, box-shadow 0.2s ease", boxSizing: "border-box", width: "100%" },
    modalActions: { display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "22px" },
    primaryBtn: { backgroundColor: "#2ea043", border: "1px solid #2ea043", padding: "9px 20px", borderRadius: "7px", color: "#fff", cursor: "pointer", fontFamily: "monospace", fontWeight: 700, fontSize: "12px", letterSpacing: "0.05em", transition: "all 0.2s ease" },
    secondaryBtn: { backgroundColor: "#111", border: "1px solid #1e1e1e", padding: "9px 18px", borderRadius: "7px", color: "#444", cursor: "pointer", fontFamily: "monospace", fontSize: "12px", transition: "all 0.15s ease" },
};