import { useEffect, useState } from 'react'
import Layout from '../components/Layout'

interface Tournament {
  id: number
  date: string
  status: string
  participantCount: number
}

interface LeaderboardEntry {
  rank: number
  player: { id: number; displayName: string; avatarUrl: string | null }
  eloRating: number
}

export default function Dashboard() {
  const [currentTournament, setCurrentTournament] = useState<Tournament | null>(null)
  const [topPlayers, setTopPlayers] = useState<LeaderboardEntry[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      fetch('/api/tournaments/current').then(r => r.ok ? r.json() : null).catch(() => null),
      fetch('/api/leaderboard?type=elo&period=all').then(r => r.json()).catch(() => []),
    ]).then(([tournament, leaderboard]) => {
      if (tournament?.tournament) setCurrentTournament(tournament.tournament)
      setTopPlayers(leaderboard.slice(0, 5))
      setLoading(false)
    })
  }, [])

  return (
    <Layout currentPage="dashboard">
      <h2 className="text-2xl font-bold mb-6">Dashboard</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* Current/Next Tournament */}
        <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6 transition-shadow hover:shadow-md">
          <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
            <span className="text-2xl">🏓</span> Tournament
          </h3>
          {loading ? (
            <p className="text-gray-400 animate-pulse">Loading...</p>
          ) : currentTournament ? (
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                {new Date(currentTournament.date).toLocaleDateString('en-US', {
                  weekday: 'long', month: 'long', day: 'numeric',
                })}
              </p>
              <div className="flex items-center gap-3">
                <span className={`px-2 py-1 rounded text-xs font-medium ${
                  currentTournament.status === 'active'
                    ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300'
                    : 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300'
                }`}>
                  {currentTournament.status}
                </span>
                <span className="text-sm text-gray-600 dark:text-gray-400">
                  {currentTournament.participantCount} players
                </span>
              </div>
              <a href={`/tournaments/${currentTournament.id}`} className="mt-3 inline-block text-sm text-primary-600 hover:underline">
                View details &rarr;
              </a>
            </div>
          ) : (
            <p className="text-gray-500 dark:text-gray-400 text-sm">No active tournament. The next one will start automatically.</p>
          )}
        </div>

        {/* Top Players */}
        <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6 transition-shadow hover:shadow-md">
          <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
            <span className="text-2xl">🏆</span> Top Players
          </h3>
          {loading ? (
            <p className="text-gray-400 animate-pulse">Loading...</p>
          ) : topPlayers.length > 0 ? (
            <div className="space-y-2">
              {topPlayers.map((entry, i) => (
                <a
                  key={entry.player.id}
                  href={`/players/${entry.player.id}`}
                  className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                >
                  <span className={`w-6 text-center font-bold text-sm ${
                    i === 0 ? 'text-yellow-500' : i === 1 ? 'text-gray-400' : i === 2 ? 'text-amber-700' : 'text-gray-500'
                  }`}>
                    #{i + 1}
                  </span>
                  {entry.player.avatarUrl ? (
                    <img src={entry.player.avatarUrl} alt="" className="w-6 h-6 rounded-full" />
                  ) : (
                    <div className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center text-xs font-bold text-primary-600">
                      {entry.player.displayName.charAt(0)}
                    </div>
                  )}
                  <span className="flex-1 text-sm font-medium truncate">{entry.player.displayName}</span>
                  <span className="text-sm font-bold text-gray-600 dark:text-gray-400">{entry.eloRating}</span>
                </a>
              ))}
              <a href="/leaderboard" className="block text-sm text-primary-600 hover:underline pt-2">
                Full leaderboard &rarr;
              </a>
            </div>
          ) : (
            <p className="text-gray-500 dark:text-gray-400 text-sm">No players yet. Log in with Slack to get started.</p>
          )}
        </div>

        {/* Quick Links */}
        <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6 transition-shadow hover:shadow-md">
          <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
            <span className="text-2xl">⚡</span> Quick Links
          </h3>
          <div className="space-y-2">
            <a href="/tournaments" className="block p-3 rounded-lg bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm font-medium">
              📋 Tournament History
            </a>
            <a href="/leaderboard" className="block p-3 rounded-lg bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm font-medium">
              📊 Leaderboard
            </a>
            <a href="/api/auth/slack" className="block p-3 rounded-lg bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm font-medium">
              🔐 Login with Slack
            </a>
          </div>
        </div>
      </div>
    </Layout>
  )
}
