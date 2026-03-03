import { useEffect, useState } from 'react'

interface Tournament {
  id: number
  date: string
  status: string
  participantCount: number
  totalRounds: number
}

export default function TournamentHistory() {
  const [tournaments, setTournaments] = useState<Tournament[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('/api/tournaments')
      .then(res => res.json())
      .then(data => {
        setTournaments(data)
        setLoading(false)
      })
      .catch(() => setLoading(false))
  }, [])

  const statusColor = (status: string) => {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300'
      case 'registration': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300'
      case 'completed': return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300'
      case 'cancelled': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  return (
    <div className="min-h-screen">
      <header className="bg-white dark:bg-gray-900 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-primary-600">Rakket</h1>
          <nav className="flex gap-6 text-sm font-medium text-gray-600 dark:text-gray-300">
            <a href="/" className="hover:text-primary-600">Dashboard</a>
            <a href="/leaderboard" className="hover:text-primary-600">Leaderboard</a>
            <a href="/tournaments" className="text-primary-600 font-semibold">Tournaments</a>
          </nav>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h2 className="text-2xl font-bold mb-6">Tournament History</h2>

        {loading ? (
          <div className="text-center py-12 text-gray-500">Loading...</div>
        ) : tournaments.length === 0 ? (
          <div className="text-center py-12 text-gray-500">No tournaments yet.</div>
        ) : (
          <div className="space-y-4">
            {tournaments.map(t => (
              <a
                key={t.id}
                href={`/tournaments/${t.id}`}
                className="block bg-white dark:bg-gray-900 rounded-xl shadow p-6 hover:shadow-md transition-shadow"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <h3 className="text-lg font-semibold">
                      Tournament #{t.id}
                    </h3>
                    <p className="text-gray-500 dark:text-gray-400 text-sm">
                      {new Date(t.date).toLocaleDateString('en-US', {
                        weekday: 'long',
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                      })}
                    </p>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      {t.participantCount} players
                    </span>
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      {t.totalRounds} rounds
                    </span>
                    <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusColor(t.status)}`}>
                      {t.status}
                    </span>
                  </div>
                </div>
              </a>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
