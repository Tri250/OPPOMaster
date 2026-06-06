import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Layout from "@/components/Layout";
import Dashboard from "@/pages/Dashboard";
import Logs from "@/pages/Logs";
import ApiTest from "@/pages/ApiTest";
import Presets from "@/pages/Presets";
import Device from "@/pages/Device";
import Holidays from "@/pages/Holidays";

export default function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/logs" element={<Logs />} />
          <Route path="/api-test" element={<ApiTest />} />
          <Route path="/presets" element={<Presets />} />
          <Route path="/device" element={<Device />} />
          <Route path="/holidays" element={<Holidays />} />
        </Routes>
      </Layout>
    </Router>
  );
}
