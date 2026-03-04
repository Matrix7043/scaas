import { useState, useEffect, useCallback, useRef } from "react";
import { Play, ArrowLeft, Lock, Unlock, Save, RotateCcw, Terminal, Code2, X, AlertTriangle, Trash2 } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

import CodeMirror from "@uiw/react-codemirror";
import { python } from "@codemirror/lang-python";
import { createTheme } from "@uiw/codemirror-themes";
import { tags as t } from "@lezer/highlight";

// ── SCaaS-matched dark theme for CodeMirror ──────────────────────────────────
const scaasTheme = createTheme({
  theme: "dark",
  settings: {
    background: "#0d0d0d",
    foreground: "#c8c8c8",
    caret: "#2ea043",
    selection: "rgba(46,160,67,0.15)",
    selectionMatch: "rgba(46,160,67,0.08)",
    lineHighlight: "rgba(255,255,255,0.02)",
    gutterBackground: "#0b0b0b",
    gutterForeground: "#2a2a2a",
    gutterBorder: "transparent",
    gutterActiveForeground: "#444",
  },
  styles: [
    { tag: t.comment, color: "#333", fontStyle: "italic" },
    { tag: t.keyword, color: "#2ea043", fontWeight: "600" },
    { tag: t.string, color: "#6aab6a" },
    { tag: t.number, color: "#7ec8e8" },
    { tag: t.bool, color: "#2ea043" },
    { tag: t.null, color: "#2ea043" },
    { tag: t.operator, color: "#888" },
    { tag: t.punctuation, color: "#555" },
    { tag: t.definition(t.variableName), color: "#c8c8c8" },
    { tag: t.function(t.variableName), color: "#a8d8a8" },
    { tag: t.className, color: "#b8e8b8", fontWeight: "600" },
    { tag: t.typeName, color: "#7ec8e8" },
    { tag: t.angleBracket, color: "#555" },
    { tag: t.self, color: "#2ea043", fontStyle: "italic" },
    { tag: t.propertyName, color: "#aaa" },
    { tag: t.attributeName, color: "#aaa" },
    { tag: t.variableName, color: "#c8c8c8" },
    { tag: t.derefOperator, color: "#555" },
  ],
});

// ── Unsaved changes dialog ────────────────────────────────────────────────────
function UnsavedDialog({ fn, onSave, onDiscard, onCancel }) {
  return (
    <motion.div
      style={S.dialogOverlay}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={(e) => e.target === e.currentTarget && onCancel()}
    >
      <motion.div
        style={S.dialog}
        initial={{ y: 12, opacity: 0, scale: 0.97 }}
        animate={{ y: 0, opacity: 1, scale: 1 }}
        exit={{ y: 8, opacity: 0, scale: 0.97 }}
        transition={{ duration: 0.18, ease: "easeOut" }}
      >
        <div style={S.dialogIconWrap}>
          <AlertTriangle size={16} color="#e3a000" />
        </div>
        <h3 style={S.dialogTitle}>Unsaved changes</h3>
        <p style={S.dialogBody}>
          You have unsaved changes to{" "}
          <span style={{ color: "#ddd" }}>{fn?.name || "this file"}.py</span>.
          If you leave now they'll be lost.
        </p>
        <div style={S.dialogActions}>
          <button style={S.dialogCancel} onClick={onCancel}>Stay</button>
          <button style={S.dialogDiscard} onClick={onDiscard}>Discard & Leave</button>
          <button style={S.dialogSave} onClick={onSave}>Save & Leave</button>
        </div>
      </motion.div>
    </motion.div>
  );
}

// ── Main Editor ───────────────────────────────────────────────────────────────
function Editor({ fn, mode = "view", onBack }) {
  const defaultCode =
    fn?.code ||
    `def ${fn?.name || "my_function"}(${fn?.params || ""}):\n    # Write your code here\n    pass\n`;

  const [code, setCode] = useState(defaultCode);
  const [savedCode, setSavedCode] = useState(defaultCode);
  const [output, setOutput] = useState([]);
  const [runStatus, setRunStatus] = useState("idle");
  const [terminalOpen, setTerminalOpen] = useState(true);
  const [saveFlash, setSaveFlash] = useState(false);
  const [showUnsaved, setShowUnsaved] = useState(false);
  const [lineCount, setLineCount] = useState(defaultCode.split("\n").length);
  const termBottomRef = useRef(null);

  const isEditable = mode === "edit";
  const isDirty = isEditable && code !== savedCode;

  // Auto-scroll terminal
  useEffect(() => {
    termBottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [output]);

  // Ctrl+S
  useEffect(() => {
    if (!isEditable) return;
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "s") {
        e.preventDefault();
        if (isDirty) handleSave();
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [isEditable, isDirty, code]);

  const handleCodeChange = useCallback((val) => {
    setCode(val);
    setLineCount(val.split("\n").length);
  }, []);

  const handleSave = () => {
    setSavedCode(code);
    setSaveFlash(true);
    setTimeout(() => setSaveFlash(false), 1400);
  };

  const handleReset = () => setCode(savedCode);

  const handleBack = () => {
    if (isDirty) setShowUnsaved(true);
    else onBack();
  };

  const pushOutput = (text, type = "default") =>
    setOutput((o) => [...o, { text, type }]);

  const runCode = () => {
    if (runStatus === "running") return;
    setRunStatus("running");
    setTerminalOpen(true);
    setOutput([]);
    pushOutput(`$ Initializing SCaaS runtime...`, "system");
    setTimeout(() => {
      pushOutput(`$ python ${fn?.name || "function"}.py`, "system");
      setTimeout(() => {
        pushOutput(``, "blank");
        pushOutput(`Hello SCaaS`, "output");
        pushOutput(`Editor + Terminal UI`, "output");
        pushOutput(``, "blank");
        pushOutput(`$ Process exited with code 0`, "success");
        setRunStatus("done");
        setTimeout(() => setRunStatus("idle"), 2500);
      }, 700);
    }, 400);
  };

  const statusColor = {
    idle: "#2ea043",
    running: "#e3a000",
    done: "#2ea043",
    error: "#ff6b6b",
  }[runStatus];

  const statusLabel = {
    idle: "● Ready",
    running: "● Running",
    done: "● Done",
    error: "● Error",
  }[runStatus];

  const outputColor = {
    blank: "transparent",
    system: "#2a3d2a",
    output: "#c8c8c8",
    success: "#2ea043",
    error: "#ff6b6b",
    default: "#444",
  };

  return (
    <div style={S.app}>
      {/* Ambient background */}
      <div style={{ position: "fixed", inset: 0, pointerEvents: "none", zIndex: 0, overflow: "hidden" }}>
        <div style={{ position: "absolute", top: "-20%", left: "-10%", width: "600px", height: "600px", borderRadius: "50%", background: "radial-gradient(circle, rgba(46,160,67,0.06) 0%, transparent 70%)", filter: "blur(40px)" }} />
        <div style={{ position: "absolute", bottom: "-15%", right: "-10%", width: "700px", height: "700px", borderRadius: "50%", background: "radial-gradient(circle, rgba(20,120,120,0.05) 0%, transparent 70%)", filter: "blur(50px)" }} />
        <div style={{ position: "absolute", top: "15%", right: "5%", width: "350px", height: "350px", borderRadius: "50%", background: "radial-gradient(circle, rgba(180,100,20,0.03) 0%, transparent 70%)", filter: "blur(40px)" }} />
      </div>

      {/* ── TOP BAR — matches FunctionsPage exactly ── */}
      <div style={S.topBar}>

        {/* Brand */}
        <span style={S.brand}>SCaaS Cloudlet</span>

        <div style={S.topBarDivider} />

        {/* Back */}
        <motion.button
          style={{ ...S.backBtn, position: "relative" }}
          onClick={handleBack}
          whileHover={{ color: "#ccc", borderColor: "#2a2a2a", backgroundColor: "#161616" }}
          whileTap={{ scale: 0.95 }}
        >
          <ArrowLeft size={12} />
          <span>Functions</span>
          {isDirty && <span style={S.dirtyDot} />}
        </motion.button>

        <div style={S.topBarDivider} />

        {/* Breadcrumb */}
        <div style={S.breadcrumb}>
          <Code2 size={11} style={{ color: "#2a2a2a", marginRight: "5px" }} />
          <span style={{ color: "#555" }}>{fn?.name || "editor"}</span>
          <span style={{ color: "#2ea043", opacity: 0.55 }}>.py</span>
        </div>

        {/* Mode badge */}
        <div style={{
          ...S.modeBadge,
          backgroundColor: isEditable ? "rgba(46,160,67,0.07)" : "transparent",
          borderColor: isEditable ? "rgba(46,160,67,0.2)" : "#1a1a1a",
          color: isEditable ? "#2ea043" : "#2a2a2a",
        }}>
          {isEditable ? <Unlock size={9} /> : <Lock size={9} />}
          <span>{isEditable ? "Editing" : "View Only"}</span>
        </div>

        {/* Right controls */}
        <div style={S.right}>

          {/* Save */}
          <AnimatePresence>
            {isEditable && (
              <motion.button
                initial={{ opacity: 0, x: 6 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 6 }}
                style={{
                  ...S.saveBtn,
                  opacity: isDirty ? 1 : 0.25,
                  cursor: isDirty ? "pointer" : "default",
                  backgroundColor: saveFlash ? "rgba(46,160,67,0.1)" : "transparent",
                  borderColor: saveFlash ? "rgba(46,160,67,0.35)" : "#1a1a1a",
                  color: saveFlash ? "#2ea043" : "#555",
                }}
                onClick={() => isDirty && handleSave()}
                title="Save  Ctrl+S"
              >
                <Save size={12} />
                <span>{saveFlash ? "Saved!" : "Save"}</span>
              </motion.button>
            )}
          </AnimatePresence>

          {/* Reset */}
          <AnimatePresence>
            {isEditable && isDirty && (
              <motion.button
                initial={{ opacity: 0, scale: 0.88 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.88 }}
                style={S.iconBtn}
                onClick={handleReset}
                title="Reset to last save"
                whileHover={{ color: "#ccc", borderColor: "#2a2a2a" }}
              >
                <RotateCcw size={12} />
              </motion.button>
            )}
          </AnimatePresence>

          {/* Terminal toggle */}
          <button
            style={{
              ...S.iconBtn,
              color: terminalOpen ? "#2ea043" : "#333",
              borderColor: terminalOpen ? "rgba(46,160,67,0.2)" : "#1a1a1a",
              backgroundColor: terminalOpen ? "rgba(46,160,67,0.05)" : "transparent",
            }}
            onClick={() => setTerminalOpen((o) => !o)}
            title="Toggle terminal"
          >
            <Terminal size={12} />
          </button>

          <div style={S.topBarDivider} />

          {/* Run */}
          <motion.button
            style={{
              ...S.runBtn,
              backgroundColor: runStatus === "running" ? "#1d5c2b" : "#2ea043",
            }}
            onClick={runCode}
            disabled={runStatus === "running"}
            whileHover={{ boxShadow: "0 0 18px rgba(46,160,67,0.3)", backgroundColor: "#35b84e" }}
            whileTap={{ scale: 0.95 }}
          >
            <Play size={12} fill="#fff" />
            <span>{runStatus === "running" ? "Running..." : "Run"}</span>
          </motion.button>

          <div style={S.topBarDivider} />

          {/* Status */}
          <motion.span
            style={{ ...S.status, color: statusColor }}
            animate={{ opacity: runStatus === "running" ? [1, 0.3, 1] : 1 }}
            transition={{ repeat: runStatus === "running" ? Infinity : 0, duration: 0.8 }}
          >
            {statusLabel}
          </motion.span>
        </div>
      </div>

      {/* ── META BAR — thin info strip ── */}
      <div style={S.metaBar}>
        <span style={S.metaItem}>python</span>
        <span style={S.metaSep}>·</span>
        <span style={S.metaItem}>{lineCount} {lineCount === 1 ? "line" : "lines"}</span>
        {isDirty && (
          <>
            <span style={S.metaSep}>·</span>
            <span style={{ ...S.metaItem, color: "#5a4200" }}>● unsaved</span>
          </>
        )}
        {!isDirty && isEditable && (
          <>
            <span style={S.metaSep}>·</span>
            <span style={{ ...S.metaItem, color: "#1a4d26" }}>✓ saved</span>
          </>
        )}
        {fn?.cpu && (
          <>
            <span style={{ ...S.metaSep, marginLeft: "auto" }}>cpu: {fn.cpu}</span>
            <span style={S.metaSep}>·</span>
          </>
        )}
        {fn?.timeout && <span style={S.metaItem}>timeout: {fn.timeout}s</span>}
        {fn?.returnType && (
          <>
            <span style={fn?.timeout ? S.metaSep : { ...S.metaSep, marginLeft: "auto" }}>·</span>
            <span style={S.metaItem}>→ {fn.returnType}</span>
          </>
        )}
      </div>

      {/* ── EDITOR ── */}
      <div style={S.editorWrap}>
        <div style={{ position: "relative", flex: 1, overflow: "hidden" }}>
          <CodeMirror
            value={code}
            extensions={[python()]}
            theme={scaasTheme}
            onChange={handleCodeChange}
            readOnly={!isEditable}
            height="100%"
            width="100%"
            style={{ height: "100%", fontSize: "13px", lineHeight: "1.7" }}
          />
          {!isEditable && <div style={S.viewOverlay} />}
        </div>
      </div>

      {/* ── TERMINAL ── */}
      <AnimatePresence>
        {terminalOpen && (
          <motion.div
            style={S.terminal}
            initial={{ height: 0 }}
            animate={{ height: "26vh" }}
            exit={{ height: 0 }}
            transition={{ duration: 0.18, ease: "easeInOut" }}
          >
            {/* Terminal header */}
            <div style={S.termHeader}>
              <Terminal size={10} style={{ color: "#2a2a2a" }} />
              <span style={S.termTitle}>output</span>
              <span style={S.termFile}>{fn?.name || "—"}.py</span>
              <div style={{ marginLeft: "auto", display: "flex", gap: "4px", alignItems: "center" }}>
                {output.length > 0 && (
                  <button style={S.termIconBtn} onClick={() => setOutput([])} title="Clear">
                    <Trash2 size={11} />
                  </button>
                )}
                <button style={S.termIconBtn} onClick={() => setTerminalOpen(false)} title="Close">
                  <X size={11} />
                </button>
              </div>
            </div>

            {/* Output lines */}
            <div style={S.termOutput}>
              {output.length === 0 ? (
                <span style={S.termEmpty}>$ run your function to see output</span>
              ) : (
                <>
                  {output.map((line, i) => (
                    <div
                      key={i}
                      style={{
                        ...S.termLine,
                        color: outputColor[line.type] || "#444",
                        minHeight: line.type === "blank" ? "10px" : undefined,
                      }}
                    >
                      {line.text || ""}
                    </div>
                  ))}
                  {runStatus === "running" && (
                    <motion.span
                      animate={{ opacity: [1, 0] }}
                      transition={{ repeat: Infinity, duration: 0.5 }}
                      style={{ color: "#2ea043", fontSize: "12px" }}
                    >▋</motion.span>
                  )}
                  <div ref={termBottomRef} />
                </>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── UNSAVED DIALOG ── */}
      <AnimatePresence>
        {showUnsaved && (
          <UnsavedDialog
            fn={fn}
            onSave={() => { handleSave(); setTimeout(() => onBack(), 80); }}
            onDiscard={() => { setShowUnsaved(false); onBack(); }}
            onCancel={() => setShowUnsaved(false)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

export default Editor;

// ── STYLES — all tokens match FunctionsPage / LoginPage ──────────────────────
const S = {
  app: {
    height: "100vh",
    width: "100vw",
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#0b0b0b",
    color: "#fff",
    fontFamily: "monospace",
    overflow: "hidden",
  },

  // Top bar — exact same height/bg/shadow as FunctionsPage
  topBar: {
    height: "52px",
    display: "flex",
    alignItems: "center",
    gap: "10px",
    padding: "0 22px",
    backgroundColor: "rgba(15,15,15,0.88)",
    backdropFilter: "blur(12px)",
    borderBottom: "1px solid #1a1a1a",
    flexShrink: 0,
    zIndex: 10,
    position: "relative",
    boxShadow: "0 1px 0 #1a1a1a, 0 4px 24px rgba(0,0,0,0.5)",
  },

  brand: {
    color: "#2ea043",
    fontWeight: "bold",
    letterSpacing: "0.05em",
    fontSize: "14px",
    flexShrink: 0,
  },

  topBarDivider: {
    width: "1px",
    height: "16px",
    backgroundColor: "#1a1a1a",
    flexShrink: 0,
  },

  backBtn: {
    display: "flex",
    alignItems: "center",
    gap: "5px",
    padding: "5px 10px",
    borderRadius: "6px",
    border: "1px solid #1e1e1e",
    color: "#666",
    backgroundColor: "transparent",
    cursor: "pointer",
    fontSize: "11px",
    fontFamily: "monospace",
    letterSpacing: "0.04em",
    flexShrink: 0,
    transition: "all 0.15s ease",
  },

  dirtyDot: {
    position: "absolute",
    top: "4px",
    right: "4px",
    width: "5px",
    height: "5px",
    borderRadius: "50%",
    backgroundColor: "#e3a000",
    boxShadow: "0 0 5px rgba(227,160,0,0.6)",
  },

  breadcrumb: {
    display: "flex",
    alignItems: "center",
    fontSize: "12px",
    letterSpacing: "0.02em",
    flexShrink: 0,
  },

  modeBadge: {
    display: "flex",
    alignItems: "center",
    gap: "5px",
    padding: "3px 9px",
    borderRadius: "4px",
    border: "1px solid",
    fontSize: "10px",
    letterSpacing: "0.07em",
    fontWeight: 700,
    flexShrink: 0,
  },

  right: {
    marginLeft: "auto",
    display: "flex",
    alignItems: "center",
    gap: "8px",
  },

  saveBtn: {
    display: "flex",
    alignItems: "center",
    gap: "5px",
    padding: "5px 11px",
    borderRadius: "6px",
    border: "1px solid",
    fontSize: "11px",
    fontFamily: "monospace",
    letterSpacing: "0.04em",
    fontWeight: 600,
    transition: "all 0.2s ease",
    flexShrink: 0,
  },

  iconBtn: {
    display: "flex",
    alignItems: "center",
    padding: "5px 7px",
    borderRadius: "6px",
    border: "1px solid #1a1a1a",
    backgroundColor: "transparent",
    color: "#333",
    cursor: "pointer",
    transition: "all 0.15s ease",
  },

  runBtn: {
    display: "flex",
    alignItems: "center",
    gap: "6px",
    border: "1px solid #2ea043",
    color: "#fff",
    padding: "6px 14px",
    cursor: "pointer",
    borderRadius: "7px",
    fontWeight: 700,
    fontFamily: "monospace",
    fontSize: "12px",
    letterSpacing: "0.04em",
    transition: "all 0.15s ease",
  },

  status: {
    fontSize: "11px",
    letterSpacing: "0.04em",
    flexShrink: 0,
    minWidth: "70px",
    textAlign: "right",
  },

  // Thin meta bar below topbar
  metaBar: {
    height: "26px",
    backgroundColor: "#0d0d0d",
    borderBottom: "1px solid #141414",
    display: "flex",
    alignItems: "center",
    padding: "0 22px",
    gap: "10px",
    flexShrink: 0,
  },

  metaItem: {
    fontSize: "10px",
    color: "#484848",
    letterSpacing: "0.06em",
    textTransform: "lowercase",
  },

  metaSep: {
    fontSize: "10px",
    color: "#333",
  },

  // Editor
  editorWrap: {
    flex: 1,
    display: "flex",
    flexDirection: "column",
    overflow: "hidden",
  },

  viewOverlay: {
    position: "absolute",
    inset: 0,
    backgroundColor: "rgba(0,0,0,0.1)",
    pointerEvents: "none",
    zIndex: 2,
  },

  // Terminal — same bg family as the rest
  terminal: {
    backgroundColor: "#0b0b0b",
    borderTop: "1px solid #1a1a1a",
    display: "flex",
    flexDirection: "column",
    flexShrink: 0,
    overflow: "hidden",
  },

  termHeader: {
    height: "30px",
    backgroundColor: "#0f0f0f",
    borderBottom: "1px solid #161616",
    display: "flex",
    alignItems: "center",
    padding: "0 16px",
    gap: "8px",
    flexShrink: 0,
  },

  termTitle: {
    fontSize: "10px",
    color: "#484848",
    letterSpacing: "0.08em",
    textTransform: "uppercase",
  },

  termFile: {
    fontSize: "10px",
    color: "#383838",
    letterSpacing: "0.04em",
  },

  termIconBtn: {
    display: "flex",
    alignItems: "center",
    background: "transparent",
    border: "none",
    color: "#444",
    cursor: "pointer",
    padding: "3px 4px",
    borderRadius: "3px",
    transition: "color 0.12s ease",
  },

  termOutput: {
    flex: 1,
    padding: "10px 22px",
    overflowY: "auto",
    display: "flex",
    flexDirection: "column",
  },

  termLine: {
    fontSize: "12px",
    lineHeight: 1.75,
    fontFamily: "monospace",
    whiteSpace: "pre-wrap",
  },

  termEmpty: {
    fontSize: "12px",
    color: "#3a3a3a",
    letterSpacing: "0.04em",
  },

  // Unsaved dialog — same card style as FunctionsPage modal
  dialogOverlay: {
    position: "fixed",
    inset: 0,
    backgroundColor: "rgba(0,0,0,0.7)",
    backdropFilter: "blur(6px)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 200,
  },

  dialog: {
    width: "380px",
    backgroundColor: "#111",
    border: "1px solid #2a2a2a",
    borderTop: "3px solid #e3a000",
    borderRadius: "14px",
    padding: "28px 26px 22px",
    boxShadow: "0 0 0 1px #0a0a0a, 0 24px 60px rgba(0,0,0,0.95)",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "10px",
  },

  dialogIconWrap: {
    width: "38px",
    height: "38px",
    borderRadius: "9px",
    backgroundColor: "rgba(227,160,0,0.07)",
    border: "1px solid rgba(227,160,0,0.18)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: "2px",
  },

  dialogTitle: {
    margin: 0,
    fontSize: "14px",
    fontWeight: 700,
    color: "#e0e0e0",
    letterSpacing: "0.02em",
  },

  dialogBody: {
    margin: "2px 0 8px",
    fontSize: "12px",
    color: "#444",
    textAlign: "center",
    lineHeight: 1.7,
    letterSpacing: "0.02em",
  },

  dialogActions: {
    display: "flex",
    gap: "8px",
    width: "100%",
  },

  dialogCancel: {
    flex: 1,
    padding: "9px",
    borderRadius: "7px",
    border: "1px solid #1e1e1e",
    backgroundColor: "transparent",
    color: "#444",
    cursor: "pointer",
    fontFamily: "monospace",
    fontSize: "11px",
    fontWeight: 600,
    letterSpacing: "0.04em",
    transition: "all 0.15s ease",
  },

  dialogDiscard: {
    flex: 1,
    padding: "9px",
    borderRadius: "7px",
    border: "1px solid rgba(255,80,80,0.2)",
    backgroundColor: "rgba(255,80,80,0.05)",
    color: "#ff6b6b",
    cursor: "pointer",
    fontFamily: "monospace",
    fontSize: "11px",
    fontWeight: 600,
    letterSpacing: "0.04em",
    transition: "all 0.15s ease",
  },

  dialogSave: {
    flex: 1,
    padding: "9px",
    borderRadius: "7px",
    border: "1px solid #2ea043",
    backgroundColor: "#2ea043",
    color: "#fff",
    cursor: "pointer",
    fontFamily: "monospace",
    fontSize: "11px",
    fontWeight: 700,
    letterSpacing: "0.04em",
    transition: "all 0.15s ease",
  },
};