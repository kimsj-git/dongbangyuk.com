import ReactDOM from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import reportWebVitals from "./reportWebVitals";

const rootElement = document.getElementById("root");
if (rootElement !== null) {
  const root = ReactDOM.createRoot(rootElement);
  root.render(<App />);
}

reportWebVitals();