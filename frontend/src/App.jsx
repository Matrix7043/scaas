import { useState } from "react";
import LoginPage from "./pages/login.jsx";
import FunctionsPage from "./pages/function.jsx";
import Editor from "./pages/editor.jsx";
import { logout as apiLogout, getToken } from "./api.js";

// Functions state lives here so it survives page switches
function App() {
  const [page, setPage] = useState(() => (getToken() ? "functions" : "login"));
  const [editorState, setEditorState] = useState({ fn: null, mode: "view" });
  const [functions, setFunctions] = useState([]);
  // user comes from the backend on login (name decoded from JWT, email from form)
  const [user, setUser] = useState(null);

  const openEditor = (fn, mode) => {
    setEditorState({ fn, mode });
    setPage("editor");
  };

  // Called by LoginPage after a successful login/register + JWT received
  const handleLoginSuccess = (_token, userData) => {
    setUser(userData);
    setFunctions([]); // clear any stale list from a previous session
    setPage("functions");
  };

  const handleLogout = () => {
    apiLogout();          // removes token from localStorage
    setUser(null);
    setFunctions([]);
    setPage("login");
  };

  // Compute resource usage from functions list
  const resources = {
    cpu: functions.reduce((a, f) => a + (Number(f.cpuCores) || 0), 0),
    storage: functions.reduce((a, f) => a + (Number(f.memory) || 0), 0),
    count: functions.length,
  };

  if (page === "login") {
    return <LoginPage onSuccess={handleLoginSuccess} />;
  }

  if (page === "editor") {
    return (
      <Editor
        fn={editorState.fn}
        mode={editorState.mode}
        onBack={() => setPage("functions")}
        onFunctionUpdated={(updatedFn) => {
          // Keep the functions list in sync when artifact/deploy changes happen in editor
          setFunctions((prev) =>
            prev.map((f) => (f.id === updatedFn.id ? updatedFn : f))
          );
        }}
      />
    );
  }

  return (
    <FunctionsPage
      functions={functions}
      setFunctions={setFunctions}
      onOpenEditor={openEditor}
      user={user ?? { name: "user", email: "" }}
      resources={resources}
      onLogout={handleLogout}
    />
  );
}

export default App;
