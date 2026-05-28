
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Home } from '@/pages/Home';
import { Detail } from '@/pages/Detail';
import { Settings } from '@/pages/Settings';

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/detail/:id" element={<Detail />} />
        <Route path="/settings" element={<Settings />} />
      </Routes>
    </Router>
  );
}
