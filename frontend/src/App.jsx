import { useState } from "react";
import LoginPage from "./pages/login.jsx";
import FunctionsPage from "./pages/function.jsx";
import Editor from "./pages/editor.jsx";

// Functions state lives here so it survives page switches
function App() {
  const [page, setPage] = useState("login");
  const [editorState, setEditorState] = useState({ fn: null, mode: "view" });
  const [functions, setFunctions] = useState([]);
  const [user] = useState({ name: "dev_user", joined: "Feb 2026" });

  const openEditor = (fn, mode) => {
    setEditorState({ fn, mode });
    setPage("editor");
  };

  // Compute resource usage from functions list
  const resources = {
    cpu: functions.reduce((a, f) => a + (Number(f.cpu) || 0), 0),
    threads: functions.reduce((a, f) => a + (Number(f.threads) || 0), 0),
    storage: functions.reduce((a, f) => a + (Number(f.storage) || 0), 0),
    count: functions.length,
  };

  if (page === "login") {
    return <LoginPage onSuccess={() => setPage("functions")} />;
  }

  if (page === "editor") {
    return (
      <Editor
        fn={editorState.fn}
        mode={editorState.mode}
        onBack={() => setPage("functions")}
      />
    );
  }

  return (
    <FunctionsPage
      functions={functions}
      setFunctions={setFunctions}
      onOpenEditor={openEditor}
      user={user}
      resources={resources}
      onLogout={() => setPage("login")}
    />
  );
}

export default App;