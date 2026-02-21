import { useState } from "react";
import { LogIn, UserPlus, AlertCircle } from "lucide-react";

function LoginPage() {
    const [mode, setMode] = useState("login");
    const [status, setStatus] = useState("● Ready");
    const [error, setError] = useState("");

    const handleSubmit = (e) => {
        e.preventDefault();

        const formData = new FormData(e.target);
        const values = Object.fromEntries(formData.entries());

        if (!values.username || !values.password) {
            setError("Please complete all required fields.");
            return;
        }

        if (mode === "register") {
            if (!values.fullName || !values.confirmPassword) {
                setError("Please complete all required fields.");
                return;
            }

            if (values.password !== values.confirmPassword) {
                setError("Passwords do not match.");
                return;
            }
        }

        setError("");
        setStatus("● Processing...");

        setTimeout(() => {
            setStatus("● Success");
        }, 1000);
    };

    return (
        <div style={styles.app}>
            {/* Top Bar */}
            <div style={styles.topBar}>
                <span style={styles.brand}>SCaaS</span>
                <span style={styles.status}>{status}</span>
            </div>

            {/* Center Section */}
            <div style={styles.center}>
                <div style={styles.box}>
                    <h1 style={styles.title}>
                        {mode === "login" ? "Welcome Back" : "Create Account"}
                    </h1>

                    {error && (
                        <div style={styles.error}>
                            <AlertCircle size={16} />
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} noValidate style={styles.form}>
                        {mode === "register" && (
                            <input
                                name="fullName"
                                type="text"
                                placeholder="Full Name"
                                style={styles.input}
                            />
                        )}

                        <input
                            name="username"
                            type="text"
                            placeholder="Username"
                            style={styles.input}
                        />

                        <input
                            name="password"
                            type="password"
                            placeholder="Password"
                            style={styles.input}
                        />

                        {mode === "register" && (
                            <input
                                name="confirmPassword"
                                type="password"
                                placeholder="Confirm Password"
                                style={styles.input}
                            />
                        )}

                        <button type="submit" style={styles.button}>
                            {mode === "login" ? <LogIn size={16} /> : <UserPlus size={16} />}
                            {mode === "login" ? "Login" : "Register"}
                        </button>
                    </form>

                    <p style={styles.switchText}>
                        {mode === "login"
                            ? "Don't have an account?"
                            : "Already have an account?"}
                        <span
                            style={styles.switchLink}
                            onClick={() =>
                                setMode(mode === "login" ? "register" : "login")
                            }
                        >
                            {mode === "login" ? " Register" : " Login"}
                        </span>
                    </p>
                </div>
            </div>
        </div>
    );
}

export default LoginPage;

const styles = {
    app: {
        minHeight: "100vh",
        backgroundColor: "#0f0f0f",
        display: "flex",
        flexDirection: "column",
        fontFamily: "monospace",
        color: "#fff",
    },

    topBar: {
        height: "50px",
        backgroundColor: "#1a1a1a",
        display: "flex",
        alignItems: "center",
        padding: "0 20px",
        borderBottom: "1px solid #333",
    },

    brand: {
        fontWeight: "bold",
        color: "#2ea043",
    },

    status: {
        marginLeft: "auto",
        color: "#2ea043",
        fontSize: "14px",
    },

    center: {
        flex: 1,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "40px 20px",
    },

    box: {
        width: "100%",
        maxWidth: "420px",
        backgroundColor: "#1e1e1e",
        padding: "35px",
        borderRadius: "16px",
        border: "1px solid #333",
        boxShadow: "0 0 40px rgba(0,0,0,0.6)",
    },

    title: {
        textAlign: "center",
        marginBottom: "20px",
    },

    error: {
        backgroundColor: "rgba(255, 80, 80, 0.1)",
        border: "1px solid rgba(255, 80, 80, 0.4)",
        color: "#ff6b6b",
        padding: "10px",
        borderRadius: "8px",
        fontSize: "13px",
        marginBottom: "15px",
        display: "flex",
        alignItems: "center",
        gap: "8px",
        justifyContent: "center",
    },

    form: {
        display: "flex",
        flexDirection: "column",
        gap: "14px",
    },

    input: {
        padding: "10px",
        borderRadius: "8px",
        border: "1px solid #333",
        backgroundColor: "#0f0f0f",
        color: "#fff",
        fontFamily: "monospace",
        fontSize: "14px",
        outline: "none",
    },

    button: {
        marginTop: "10px",
        padding: "10px",
        borderRadius: "8px",
        border: "none",
        backgroundColor: "#2ea043",
        color: "#fff",
        fontWeight: "bold",
        cursor: "pointer",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "6px",
        fontSize: "14px",
    },

    switchText: {
        marginTop: "18px",
        textAlign: "center",
        fontSize: "13px",
        color: "#aaa",
    },

    switchLink: {
        color: "#2ea043",
        cursor: "pointer",
        fontWeight: "bold",
    },
};