import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import Leaderboard from './pages/Leaderboard'
import TournamentHistory from './pages/TournamentHistory'
import TournamentDetail from './pages/TournamentDetail'
import PlayerProfile from './pages/PlayerProfile'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/leaderboard" element={<Leaderboard />} />
        <Route path="/tournaments" element={<TournamentHistory />} />
        <Route path="/tournaments/:id" element={<TournamentDetail />} />
        <Route path="/players/:id" element={<PlayerProfile />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
