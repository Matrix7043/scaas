import { useState } from "react";
import { LogIn, UserPlus, AlertCircle } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { login as apiLogin, register as apiRegister } from "../api";

// Router integration note:
// Replace the onSuccess prop callback with useNavigate() from react-router-dom
// e.g. const navigate = useNavigate(); then navigate("/functions")

function FocusInput({ style, type = "text", ...props }) {
    const [focused, setFocused] = useState(false);
    return (
        <input
            type={type}
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

function LoginPage({ onSuccess }) {
    const [mode, setMode] = useState("login");
    const [status, setStatus] = useState("idle"); // idle | loading | success | error
    const [error, setError] = useState("");
    // Login needs: email, password
    // Register needs: firstName, lastName, username, email, password, confirmPassword
    const [form, setForm] = useState({
        firstName: "", lastName: "", username: "",
        email: "", password: "", confirmPassword: "",
    });

    const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

    const handleSubmit = async () => {
        setError("");

        // Basic client-side validation
        if (!form.email || !form.password) {
            setError("Please complete all required fields.");
            return;
        }
        if (mode === "register") {
            if (!form.firstName || !form.lastName || !form.username || !form.confirmPassword) {
                setError("Please complete all required fields.");
                return;
            }
            if (form.password !== form.confirmPassword) {
                setError("Passwords do not match.");
                return;
            }
            if (form.password.length < 6 || form.password.length > 12) {
                setError("Password must be 6–12 characters.");
                return;
            }
        }

        setStatus("loading");
        try {
            if (mode === "login") {
                // POST /auth/login → returns JWT string, saved to localStorage by api.js
                const token = await apiLogin({ email: form.email, password: form.password });
                // Decode the username from the JWT payload (middle part, base64)
                let userName = form.email;
                try {
                    const payload = JSON.parse(atob(token.split(".")[1]));
                    userName = payload.sub || payload.username || form.email;
                } catch { /* if decode fails, fall back to email */ }
                setStatus("success");
                setTimeout(() => onSuccess?.(token, { name: userName, email: form.email }), 600);
            } else {
                // POST /auth/register
                await apiRegister({
                    firstName: form.firstName,
                    lastName: form.lastName,
                    username: form.username,
                    email: form.email,
                    password: form.password,
                });
                // On success, auto-login the user
                const token = await apiLogin({ email: form.email, password: form.password });
                let userName = form.username;
                try {
                    const payload = JSON.parse(atob(token.split(".")[1]));
                    userName = payload.sub || payload.username || form.username;
                } catch { /* fall back */ }
                setStatus("success");
                setTimeout(() => onSuccess?.(token, { name: userName, email: form.email }), 600);
            }
        } catch (err) {
            setStatus("idle");
            setError(err.message || "Something went wrong. Try again.");
        }
    };

    const switchMode = () => {
        setMode((m) => (m === "login" ? "register" : "login"));
        setError("");
        setStatus("idle");
        setForm({ firstName: "", lastName: "", username: "", email: "", password: "", confirmPassword: "" });
    };

    const isLoading = status === "loading";
    const isSuccess = status === "success";

    return (
        <div style={styles.app}>
            {/* TOP BAR */}
            <div style={styles.topBar}>
                <span style={styles.brand}>SCaaS Cloudlet</span>
                <div style={styles.topBarRight}>
                    <span style={styles.topBarDot} />
                    <span style={styles.topBarStatus}>
                        {isLoading ? "Processing..." : isSuccess ? "Authenticated" : "Login"}
                    </span>
                </div>
            </div>

            {/* BACKGROUND GRID */}
            <div style={styles.bgGrid} />

            {/* CENTER */}
            <div style={styles.center}>
                <motion.div
                    key={mode}
                    style={styles.card}
                    initial={{ opacity: 0, y: 18, scale: 0.97 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ duration: 0.25, ease: "easeOut" }}
                >
                    {/* Card header accent */}
                    <div style={styles.cardAccent} />

                    <div style={styles.cardInner}>
                        {/* Icon */}
                        <motion.div
                            style={styles.iconWrap}
                            animate={{ boxShadow: isSuccess ? "0 0 24px rgba(46,160,67,0.5)" : "0 0 0px rgba(46,160,67,0)" }}
                            transition={{ duration: 0.4 }}
                        >
                            {mode === "login"
                                ? <LogIn size={20} color="#2ea043" />
                                : <UserPlus size={20} color="#2ea043" />}
                        </motion.div>

                        <h1 style={styles.title}>{mode === "login" ? "Welcome Back" : "Create Account"}</h1>
                        <p style={styles.subtitle}>{mode === "login" ? "Sign in to your workspace" : "Join the SCaaS platform"}</p>

                        {/* Error */}
                        <AnimatePresence>
                            {error && (
                                <motion.div
                                    style={styles.errorBox}
                                    initial={{ opacity: 0, y: -6, height: 0 }}
                                    animate={{ opacity: 1, y: 0, height: "auto" }}
                                    exit={{ opacity: 0, height: 0 }}
                                    transition={{ duration: 0.18 }}
                                >
                                    <AlertCircle size={14} />
                                    <span>{error}</span>
                                </motion.div>
                            )}
                        </AnimatePresence>

                        {/* FORM */}
                        <div style={styles.form}>
                            {/* Register-only fields: first name, last name, username */}
                            <AnimatePresence>
                                {mode === "register" && (
                                    <motion.div
                                        style={{ display: "flex", flexDirection: "column", gap: "11px" }}
                                        initial={{ opacity: 0, height: 0 }}
                                        animate={{ opacity: 1, height: "auto" }}
                                        exit={{ opacity: 0, height: 0 }}
                                        transition={{ duration: 0.2 }}
                                    >
                                        <div style={{ display: "flex", gap: "10px" }}>
                                            <FocusInput
                                                placeholder="First Name"
                                                value={form.firstName}
                                                onChange={set("firstName")}
                                                style={{ ...styles.input, flex: 1 }}
                                            />
                                            <FocusInput
                                                placeholder="Last Name"
                                                value={form.lastName}
                                                onChange={set("lastName")}
                                                style={{ ...styles.input, flex: 1 }}
                                            />
                                        </div>
                                        <FocusInput
                                            placeholder="Username"
                                            value={form.username}
                                            onChange={set("username")}
                                            style={styles.input}
                                        />
                                    </motion.div>
                                )}
                            </AnimatePresence>

                            {/* Email — used for both login and register */}
                            <FocusInput
                                type="email"
                                placeholder="Email"
                                value={form.email}
                                onChange={set("email")}
                                style={styles.input}
                            />
                            <FocusInput
                                type="password"
                                placeholder="Password"
                                value={form.password}
                                onChange={set("password")}
                                style={styles.input}
                            />

                            {/* Confirm password — register only */}
                            <AnimatePresence>
                                {mode === "register" && (
                                    <motion.div
                                        initial={{ opacity: 0, height: 0 }}
                                        animate={{ opacity: 1, height: "auto" }}
                                        exit={{ opacity: 0, height: 0 }}
                                        transition={{ duration: 0.2 }}
                                    >
                                        <FocusInput
                                            type="password"
                                            placeholder="Confirm Password"
                                            value={form.confirmPassword}
                                            onChange={set("confirmPassword")}
                                            style={styles.input}
                                        />
                                    </motion.div>
                                )}
                            </AnimatePresence>

                            <motion.button
                                style={{
                                    ...styles.submitBtn,
                                    backgroundColor: isSuccess ? "#25883a" : "#2ea043",
                                    opacity: isLoading ? 0.8 : 1,
                                }}
                                onClick={handleSubmit}
                                disabled={isLoading || isSuccess}
                                whileHover={{ boxShadow: "0 0 20px rgba(46,160,67,0.35)" }}
                                whileTap={{ scale: 0.97 }}
                            >
                                {isLoading ? (
                                    <motion.span
                                        animate={{ opacity: [1, 0.4, 1] }}
                                        transition={{ repeat: Infinity, duration: 1 }}
                                    >
                                        Processing...
                                    </motion.span>
                                ) : isSuccess ? (
                                    "✓ Success"
                                ) : mode === "login" ? (
                                    <><LogIn size={14} /> Sign In</>
                                ) : (
                                    <><UserPlus size={14} /> Register</>
                                )}
                            </motion.button>
                        </div>

                        {/* Switch mode */}
                        <p style={styles.switchText}>
                            {mode === "login" ? "Don't have an account?" : "Already have an account?"}
                            <span style={styles.switchLink} onClick={switchMode}>
                                {mode === "login" ? " Register" : " Sign in"}
                            </span>
                        </p>
                    </div>
                </motion.div>

                {/* Bottom label */}
                <p style={styles.footerNote}>SCaaS Cloudlet · Secure Runtime Platform</p>
            </div>
        </div>
    );
}

export default LoginPage;

const styles = {
    app: {
        minHeight: "100vh",
        backgroundColor: "#0b0b0b",
        display: "flex",
        flexDirection: "column",
        fontFamily: "monospace",
        color: "#fff",
        position: "relative",
        overflow: "hidden",
    },

    bgGrid: {
        position: "absolute",
        inset: 0,
        backgroundImage: `
      linear-gradient(rgba(46,160,67,0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(46,160,67,0.03) 1px, transparent 1px)
    `,
        backgroundSize: "40px 40px",
        pointerEvents: "none",
        zIndex: 0,
    },

    topBar: {
        height: "52px",
        backgroundColor: "#0f0f0f",
        display: "flex",
        alignItems: "center",
        padding: "0 22px",
        borderBottom: "1px solid #1a1a1a",
        position: "relative",
        zIndex: 10,
        boxShadow: "0 1px 0 #1a1a1a, 0 4px 24px rgba(0,0,0,0.5)",
    },

    brand: {
        color: "#2ea043",
        fontWeight: "bold",
        letterSpacing: "0.05em",
        fontSize: "14px",
    },

    topBarRight: {
        marginLeft: "auto",
        display: "flex",
        alignItems: "center",
        gap: "7px",
    },

    topBarDot: {
        width: "6px",
        height: "6px",
        borderRadius: "50%",
        backgroundColor: "#2ea043",
        boxShadow: "0 0 6px rgba(46,160,67,0.6)",
    },

    topBarStatus: {
        fontSize: "12px",
        color: "#2ea043",
        opacity: 0.75,
        letterSpacing: "0.04em",
    },

    center: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "40px 20px",
        position: "relative",
        zIndex: 1,
    },

    card: {
        width: "100%",
        maxWidth: "400px",
        backgroundColor: "#0f0f0f",
        borderRadius: "16px",
        border: "1px solid #1e1e1e",
        boxShadow: "0 30px 80px rgba(0,0,0,0.8), 0 0 0 1px rgba(255,255,255,0.02) inset",
        overflow: "hidden",
        transform: "perspective(900px) rotateX(0.5deg)",
    },

    cardAccent: {
        height: "2px",
        background: "linear-gradient(90deg, transparent, #2ea043, transparent)",
        opacity: 0.6,
    },

    cardInner: {
        padding: "32px 30px 28px",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
    },

    iconWrap: {
        width: "48px",
        height: "48px",
        borderRadius: "12px",
        backgroundColor: "rgba(46,160,67,0.08)",
        border: "1px solid rgba(46,160,67,0.2)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        marginBottom: "18px",
    },

    title: {
        fontSize: "18px",
        fontWeight: 700,
        margin: "0 0 6px",
        letterSpacing: "0.02em",
        textAlign: "center",
    },

    subtitle: {
        fontSize: "12px",
        color: "#444",
        margin: "0 0 22px",
        letterSpacing: "0.04em",
        textAlign: "center",
    },

    errorBox: {
        width: "100%",
        backgroundColor: "rgba(255,80,80,0.07)",
        border: "1px solid rgba(255,80,80,0.25)",
        color: "#ff6b6b",
        padding: "9px 12px",
        borderRadius: "7px",
        fontSize: "12px",
        marginBottom: "14px",
        display: "flex",
        alignItems: "center",
        gap: "8px",
        overflow: "hidden",
        boxSizing: "border-box",
    },

    form: {
        width: "100%",
        display: "flex",
        flexDirection: "column",
        gap: "11px",
    },

    input: {
        width: "100%",
        padding: "11px 13px",
        borderRadius: "8px",
        border: "1px solid #1e1e1e",
        backgroundColor: "#080808",
        color: "#ddd",
        fontFamily: "monospace",
        fontSize: "13px",
        transition: "border-color 0.2s ease, box-shadow 0.2s ease",
        boxSizing: "border-box",
        display: "block",
    },

    submitBtn: {
        marginTop: "6px",
        width: "100%",
        padding: "11px",
        borderRadius: "8px",
        border: "1px solid #2ea043",
        color: "#fff",
        fontWeight: 700,
        cursor: "pointer",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "7px",
        fontSize: "13px",
        fontFamily: "monospace",
        letterSpacing: "0.04em",
        transition: "background 0.2s ease",
    },

    switchText: {
        marginTop: "20px",
        fontSize: "12px",
        color: "#444",
        textAlign: "center",
    },

    switchLink: {
        color: "#2ea043",
        cursor: "pointer",
        fontWeight: 700,
        letterSpacing: "0.02em",
    },

    footerNote: {
        marginTop: "24px",
        fontSize: "11px",
        color: "#242424",
        letterSpacing: "0.06em",
        textAlign: "center",
    },
};