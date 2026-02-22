import { useState } from "react";
import { Plus, Trash2, Eye, Edit, X } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const styles = {
    app: {
        minHeight: "100vh",
        backgroundColor: "#0b0b0b",
        color: "#fff",
        fontFamily: "monospace",
    },

    topBar: {
        height: "55px",
        backgroundColor: "#111",
        display: "flex",
        alignItems: "center",
        padding: "0 25px",
        borderBottom: "1px solid #1a1a1a",
        position: "sticky",
        top: 0,
        zIndex: 10,
        backdropFilter: "blur(12px)",
        boxShadow: "0 1px 0 #1a1a1a, 0 4px 20px rgba(0,0,0,0.4)",
    },

    brand: {
        color: "#2ea043",
        fontWeight: "bold",
        letterSpacing: "0.04em",
        fontSize: "15px",
    },
    env: { marginLeft: "auto", color: "#2ea043", fontSize: "13px", opacity: 0.8 },

    container: { padding: "36px 40px 60px 40px" },

    headerRow: { display: "flex", alignItems: "center", marginBottom: "22px" },

    title: {
        fontSize: "20px",
        margin: 0,
        fontWeight: 600,
        letterSpacing: "0.02em",
    },

    createBtn: {
        marginLeft: "auto",
        backgroundColor: "#2ea043",
        border: "1px solid #2ea043",
        padding: "9px 16px",
        borderRadius: "8px",
        color: "#fff",
        cursor: "pointer",
        display: "flex",
        alignItems: "center",
        gap: "6px",
        fontSize: "13px",
        fontFamily: "monospace",
        fontWeight: 600,
        transition: "all 0.2s ease",
        boxShadow: "0 0 0 0 rgba(46,160,67,0)",
    },

    table: {
        border: "1px solid #1e1e1e",
        borderRadius: "12px",
        overflow: "hidden",
        background: "#111",
        boxShadow: "0 8px 40px rgba(0,0,0,0.5), 0 1px 0 #222 inset",
        transform: "perspective(1200px) rotateX(0.4deg)",
        transformOrigin: "top center",
    },

    tableHeader: {
        display: "grid",
        gridTemplateColumns: "repeat(5, 1fr)",
        padding: "12px 18px",
        backgroundColor: "#141414",
        fontSize: "11px",
        letterSpacing: "0.1em",
        textTransform: "uppercase",
        color: "#555",
        borderBottom: "1px solid #1e1e1e",
        textAlign: "center",
    },

    row: {
        display: "grid",
        gridTemplateColumns: "repeat(5, 1fr)",
        padding: "14px 18px",
        borderTop: "1px solid #161616",
        textAlign: "center",
        alignItems: "center",
        fontSize: "13px",
        transition: "background 0.15s ease",
        cursor: "default",
    },

    rowName: {
        fontWeight: 600,
        color: "#e0e0e0",
        letterSpacing: "0.02em",
        textAlign: "center",
    },

    rowMono: {
        color: "#888",
        fontSize: "12px",
        textAlign: "center",
    },

    rowBadge: {
        display: "inline-block",
        backgroundColor: "rgba(46,160,67,0.1)",
        border: "1px solid rgba(46,160,67,0.25)",
        color: "#2ea043",
        padding: "2px 8px",
        borderRadius: "4px",
        fontSize: "11px",
        fontWeight: 600,
        letterSpacing: "0.05em",
    },

    actions: { display: "flex", justifyContent: "center", gap: "8px" },

    iconBtn: {
        backgroundColor: "#181818",
        border: "1px solid #252525",
        padding: "6px",
        borderRadius: "6px",
        cursor: "pointer",
        color: "#999",
        display: "flex",
        alignItems: "center",
        transition: "all 0.15s ease",
    },

    deleteBtn: {
        backgroundColor: "rgba(255,80,80,0.06)",
        border: "1px solid rgba(255,80,80,0.2)",
        padding: "6px",
        borderRadius: "6px",
        cursor: "pointer",
        color: "#ff6b6b",
        display: "flex",
        alignItems: "center",
        transition: "all 0.15s ease",
    },

    overlay: {
        position: "fixed",
        inset: 0,
        backgroundColor: "rgba(0,0,0,0.65)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        backdropFilter: "blur(4px)",
        zIndex: 100,
    },

    card: {
        width: "520px",
        backgroundColor: "#131313",
        padding: "32px",
        borderRadius: "18px",
        border: "1px solid #222",
        boxShadow:
            "0 40px 100px rgba(0,0,0,0.9), 0 0 0 1px rgba(255,255,255,0.03) inset",
        transform: "perspective(800px) rotateX(1deg)",
    },

    cardHeader: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        marginBottom: "24px",
    },

    cardTitle: {
        fontSize: "16px",
        fontWeight: 600,
        margin: 0,
        letterSpacing: "0.02em",
    },

    form: {
        display: "flex",
        flexDirection: "column",
        gap: "14px",
    },

    rowGroup: {
        display: "flex",
        gap: "14px",
    },

    input: {
        padding: "11px 14px",
        borderRadius: "8px",
        border: "1px solid #222",
        backgroundColor: "#0d0d0d",
        color: "#e0e0e0",
        fontFamily: "monospace",
        fontSize: "13px",
        outline: "none",
        transition: "border-color 0.2s ease, box-shadow 0.2s ease",
        width: "100%",
        boxSizing: "border-box",
    },

    inputHalf: {
        flex: 1,
        padding: "11px 14px",
        borderRadius: "8px",
        border: "1px solid #222",
        backgroundColor: "#0d0d0d",
        color: "#e0e0e0",
        fontFamily: "monospace",
        fontSize: "13px",
        outline: "none",
        transition: "border-color 0.2s ease, box-shadow 0.2s ease",
        boxSizing: "border-box",
    },

    modalActions: {
        display: "flex",
        justifyContent: "flex-end",
        gap: "10px",
        marginTop: "26px",
    },

    primaryBtn: {
        backgroundColor: "#2ea043",
        border: "1px solid #2ea043",
        padding: "9px 20px",
        borderRadius: "8px",
        color: "#fff",
        cursor: "pointer",
        fontFamily: "monospace",
        fontWeight: 600,
        fontSize: "13px",
        transition: "all 0.2s ease",
        letterSpacing: "0.04em",
    },

    secondaryBtn: {
        backgroundColor: "#181818",
        border: "1px solid #272727",
        padding: "9px 18px",
        borderRadius: "8px",
        color: "#888",
        cursor: "pointer",
        fontFamily: "monospace",
        fontSize: "13px",
        transition: "all 0.15s ease",
    },

    emptyState: {
        textAlign: "center",
        padding: "60px 20px",
        color: "#333",
        fontSize: "13px",
        letterSpacing: "0.05em",
    },
};

function HoverRow({ fn, onDelete }) {
    const [hovered, setHovered] = useState(false);
    const [eyeHovered, setEyeHovered] = useState(false);
    const [editHovered, setEditHovered] = useState(false);
    const [delHovered, setDelHovered] = useState(false);

    return (
        <motion.div
            style={{
                ...styles.row,
                background: hovered ? "#161616" : "transparent",
                boxShadow: hovered
                    ? "inset 0 0 0 1px rgba(46,160,67,0.08)"
                    : "none",
            }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, x: -10 }}
            transition={{ duration: 0.2 }}
            layout
        >
            <span style={styles.rowName}>{fn.name}</span>
            <span style={styles.rowMono}>
                {fn.params ? (
                    <span style={{ color: "#666" }}>({fn.params})</span>
                ) : (
                    <span style={{ color: "#333" }}>—</span>
                )}{" "}
                {fn.returnType && (
                    <span style={{ color: "#2ea043" }}>→ {fn.returnType}</span>
                )}
            </span>
            <span style={styles.rowMono}>{fn.timeout ? `${fn.timeout}s` : "—"}</span>
            <span>
                <span style={styles.rowBadge}>{fn.uptime}</span>
            </span>
            <div style={styles.actions}>
                <button
                    style={{
                        ...styles.iconBtn,
                        color: eyeHovered ? "#fff" : "#666",
                        borderColor: eyeHovered ? "#333" : "#252525",
                        background: eyeHovered ? "#222" : "#181818",
                    }}
                    onMouseEnter={() => setEyeHovered(true)}
                    onMouseLeave={() => setEyeHovered(false)}
                >
                    <Eye size={14} />
                </button>
                <button
                    style={{
                        ...styles.iconBtn,
                        color: editHovered ? "#fff" : "#666",
                        borderColor: editHovered ? "#333" : "#252525",
                        background: editHovered ? "#222" : "#181818",
                    }}
                    onMouseEnter={() => setEditHovered(true)}
                    onMouseLeave={() => setEditHovered(false)}
                >
                    <Edit size={14} />
                </button>
                <button
                    style={{
                        ...styles.deleteBtn,
                        color: delHovered ? "#ff4444" : "#ff6b6b",
                        borderColor: delHovered
                            ? "rgba(255,80,80,0.5)"
                            : "rgba(255,80,80,0.2)",
                        background: delHovered
                            ? "rgba(255,80,80,0.12)"
                            : "rgba(255,80,80,0.06)",
                    }}
                    onMouseEnter={() => setDelHovered(true)}
                    onMouseLeave={() => setDelHovered(false)}
                    onClick={() => onDelete(fn.id)}
                >
                    <Trash2 size={14} />
                </button>
            </div>
        </motion.div>
    );
}

function FocusInput({ style, ...props }) {
    const [focused, setFocused] = useState(false);
    return (
        <input
            {...props}
            style={{
                ...style,
                borderColor: focused ? "rgba(46,160,67,0.5)" : "#222",
                boxShadow: focused ? "0 0 0 3px rgba(46,160,67,0.08)" : "none",
            }}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
        />
    );
}

function FunctionsPage() {
    const [functions, setFunctions] = useState([]);
    const [showModal, setShowModal] = useState(false);
    const [btnHovered, setBtnHovered] = useState(false);

    const [form, setForm] = useState({
        name: "",
        returnType: "",
        params: "",
        cpu: "",
        threads: "",
        storage: "",
        timeout: "",
    });

    const closeModal = () => {
        setShowModal(false);
        setForm({ name: "", returnType: "", params: "", cpu: "", threads: "", storage: "", timeout: "" });
    };

    const createFunction = () => {
        if (!form.name) return;
        setFunctions([...functions, { id: Date.now(), ...form, uptime: "live" }]);
        closeModal();
    };

    const deleteFunction = (id) => {
        setFunctions(functions.filter((f) => f.id !== id));
    };

    return (
        <div style={styles.app}>
            {/* TOP BAR */}
            <div style={styles.topBar}>
                <span style={styles.brand}>SCaaS Cloudlet</span>
                <span style={styles.env}>● Control Panel</span>
            </div>

            <div style={styles.container}>
                <div style={styles.headerRow}>
                    <h1 style={styles.title}>Functions</h1>
                    <motion.button
                        style={{
                            ...styles.createBtn,
                            boxShadow: btnHovered
                                ? "0 0 18px rgba(46,160,67,0.35)"
                                : "0 0 0 0 rgba(46,160,67,0)",
                            backgroundColor: btnHovered ? "#35b84e" : "#2ea043",
                        }}
                        onClick={() => setShowModal(true)}
                        onMouseEnter={() => setBtnHovered(true)}
                        onMouseLeave={() => setBtnHovered(false)}
                        whileTap={{ scale: 0.96 }}
                    >
                        <Plus size={14} /> Create Function
                    </motion.button>
                </div>

                {/* TABLE */}
                <div style={styles.table}>
                    <div style={styles.tableHeader}>
                        <span>Name</span>
                        <span>Signature</span>
                        <span>Timeout</span>
                        <span>Status</span>
                        <span>Actions</span>
                    </div>

                    <AnimatePresence>
                        {functions.length === 0 ? (
                            <div style={styles.emptyState}>
                                No functions deployed yet
                            </div>
                        ) : (
                            functions.map((fn) => (
                                <HoverRow key={fn.id} fn={fn} onDelete={deleteFunction} />
                            ))
                        )}
                    </AnimatePresence>
                </div>
            </div>

            {/* MODAL */}
            <AnimatePresence>
                {showModal && (
                    <motion.div
                        style={styles.overlay}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={(e) => e.target === e.currentTarget && closeModal()}
                    >
                        <motion.div
                            style={styles.card}
                            initial={{ y: 20, opacity: 0, scale: 0.97 }}
                            animate={{ y: 0, opacity: 1, scale: 1 }}
                            exit={{ y: 16, opacity: 0, scale: 0.97 }}
                            transition={{ duration: 0.22, ease: "easeOut" }}
                        >
                            <div style={styles.cardHeader}>
                                <h2 style={styles.cardTitle}>Deploy Python Function</h2>
                                <motion.div
                                    whileHover={{ rotate: 90, scale: 1.1 }}
                                    transition={{ duration: 0.15 }}
                                    onClick={closeModal}
                                    style={{ cursor: "pointer", color: "#555", display: "flex" }}
                                >
                                    <X size={17} />
                                </motion.div>
                            </div>

                            <div style={styles.form}>
                                <FocusInput
                                    placeholder="Function Name"
                                    value={form.name}
                                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                                    style={styles.input}
                                />
                                <div style={styles.rowGroup}>
                                    <FocusInput
                                        placeholder="Return Type"
                                        value={form.returnType}
                                        onChange={(e) => setForm({ ...form, returnType: e.target.value })}
                                        style={styles.inputHalf}
                                    />
                                    <FocusInput
                                        placeholder="Parameters (comma separated)"
                                        value={form.params}
                                        onChange={(e) => setForm({ ...form, params: e.target.value })}
                                        style={styles.inputHalf}
                                    />
                                </div>
                                <div style={styles.rowGroup}>
                                    <FocusInput
                                        type="number"
                                        placeholder="CPU Cores"
                                        value={form.cpu}
                                        onChange={(e) => setForm({ ...form, cpu: e.target.value })}
                                        style={styles.inputHalf}
                                    />
                                    <FocusInput
                                        type="number"
                                        placeholder="Thread Count"
                                        value={form.threads}
                                        onChange={(e) => setForm({ ...form, threads: e.target.value })}
                                        style={styles.inputHalf}
                                    />
                                </div>
                                <div style={styles.rowGroup}>
                                    <FocusInput
                                        type="number"
                                        placeholder="Storage (MB)"
                                        value={form.storage}
                                        onChange={(e) => setForm({ ...form, storage: e.target.value })}
                                        style={styles.inputHalf}
                                    />
                                    <FocusInput
                                        type="number"
                                        placeholder="Timeout (seconds)"
                                        value={form.timeout}
                                        onChange={(e) => setForm({ ...form, timeout: e.target.value })}
                                        style={styles.inputHalf}
                                    />
                                </div>
                            </div>

                            <div style={styles.modalActions}>
                                <button style={styles.secondaryBtn} onClick={closeModal}>
                                    Cancel
                                </button>
                                <motion.button
                                    style={styles.primaryBtn}
                                    onClick={createFunction}
                                    whileHover={{ boxShadow: "0 0 20px rgba(46,160,67,0.4)" }}
                                    whileTap={{ scale: 0.96 }}
                                >
                                    Deploy
                                </motion.button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

export default FunctionsPage;