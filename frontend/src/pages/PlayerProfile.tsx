import { useEffect, useState } from 'react'

interface PlayerStats {
  player: {
    id: number
    slackUserId: string
    displayName: string
    avatarUrl: string | null
    eloRating: number
    totalMatches: number
    totalWins: number
    createdAt: string
  }
  tournamentsPlayed: number
  tournamentWins: number
  bestPlacement: number | null
  averagePlacement: number | null
  currentWinStreak: number
  longestWinStreak: number
  badges: {
    badgeType: string
    name: string
    description: string
    emoji: string
    earnedAt: string
  }[]
  recentMatches: {
    id: number
    player1: { id: number; displayName: string } | null
    player2: { id: number; displayName: string } | null
    winner: { id: number; displayName: string } | null
    status: string
    sets: { setNumber: number; player1Score: number; player2Score: number }[]
  }[]
  headToHead: {
    opponent: { id: number; displayName: string; avatarUrl: string | null }
    wins: number
    losses: number
    totalMatches: number
  }[]
}

interface EloPoint {
  recordedAt: string
  eloAfter: number
}

export default function PlayerProfile() {
  const [stats, setStats] = useState<PlayerStats | null>(null)
  const [eloHistory, setEloHistory] = useState<EloPoint[]>([])
  const [loading, setLoading] = useState(true)

  const id = window.location.pathname.split('/').pop()

  useEffect(() => {
    Promise.all([
      fetch(`/api/players/${id}/stats`).then(r => r.json()),
      fetch(`/api/players/${id}/elo-history`).then(r => r.json()),
    ])
      .then(([statsData, eloData]) => {
        setStats(statsData)
        setEloHistory(eloData)
        setLoading(false)
      })
      .catch(() => setLoading(false))
  }, [id])

  if (loading) return <div className="min-h-screen flex items-center justify-center text-gray-500">Loading...</div>
  if (!stats) return <div className="min-h-screen flex items-center justify-center text-gray-500">Player not found</div>

  const { player } = stats
  const winRate = player.totalMatches > 0 ? ((player.totalWins / player.totalMatches) * 100).toFixed(0) : '0'

  return (
    <div className="min-h-screen">
      <header className="bg-white dark:bg-gray-900 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-primary-600">Rakket</h1>
          <nav className="flex gap-6 text-sm font-medium text-gray-600 dark:text-gray-300">
            <a href="/" className="hover:text-primary-600">Dashboard</a>
            <a href="/leaderboard" className="hover:text-primary-600">Leaderboard</a>
            <a href="/tournaments" className="hover:text-primary-600">Tournaments</a>
          </nav>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Profile header */}
        <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6 mb-6">
          <div className="flex items-center gap-4 mb-6">
            {player.avatarUrl ? (
              <img src={player.avatarUrl} alt="" className="w-16 h-16 rounded-full" />
            ) : (
              <div className="w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center text-2xl font-bold text-primary-600">
                {player.displayName.charAt(0).toUpperCase()}
              </div>
            )}
            <div>
              <h2 className="text-2xl font-bold">{player.displayName}</h2>
              <p className="text-gray-500 dark:text-gray-400 text-sm">
                Joined {new Date(player.createdAt).toLocaleDateString()}
              </p>
            </div>
            <div className="ml-auto text-right">
              <div className="text-3xl font-bold text-primary-600">{player.eloRating}</div>
              <div className="text-sm text-gray-500">ELO Rating</div>
            </div>
          </div>

          {/* Stats grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <StatCard label="Matches" value={player.totalMatches} />
            <StatCard label="Win Rate" value={`${winRate}%`} />
            <StatCard label="Tournaments" value={stats.tournamentsPlayed} />
            <StatCard label="Tournament Wins" value={stats.tournamentWins} />
            <StatCard label="Best Placement" value={stats.bestPlacement ? `#${stats.bestPlacement}` : '-'} />
            <StatCard label="Avg Placement" value={stats.averagePlacement ? `#${stats.averagePlacement.toFixed(1)}` : '-'} />
            <StatCard label="Current Streak" value={`${stats.currentWinStreak}W`} />
            <StatCard label="Best Streak" value={`${stats.longestWinStreak}W`} />
          </div>
        </div>

        {/* ELO Chart placeholder */}
        {eloHistory.length > 0 && (
          <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6 mb-6">
            <h3 className="text-lg font-semibold mb-4">ELO History</h3>
            <div className="h-48 flex items-end gap-1">
              {(() => {
                const ratings = eloHistory.map(p => p.eloAfter)
                const min = Math.min(...ratings) - 20
                const max = Math.max(...ratings) + 20
                const range = max - min || 1
                return eloHistory.map((point, i) => (
                  <div
                    key={i}
                    className="flex-1 bg-primary-500 rounded-t opacity-80 hover:opacity-100 transition-opacity"
                    style={{ height: `${((point.eloAfter - min) / range) * 100}%` }}
                    title={`${point.eloAfter} ELO`}
                  />
                ))
              })()}
            </div>
            <div className="flex justify-between text-xs text-gray-400 mt-2">
              <span>{eloHistory[0]?.eloAfter}</span>
              <span>{eloHistory[eloHistory.length - 1]?.eloAfter}</span>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Badges */}
          {stats.badges.length > 0 && (
            <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
              <h3 className="text-lg font-semibold mb-4">Badges</h3>
              <div className="grid grid-cols-2 gap-3">
                {stats.badges.map(badge => (
                  <div key={badge.badgeType} className="flex items-center gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-800">
                    <span className="text-2xl">{badge.emoji}</span>
                    <div>
                      <div className="font-medium text-sm">{badge.name}</div>
                      <div className="text-xs text-gray-500">{badge.description}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Head-to-head */}
          {stats.headToHead.length > 0 && (
            <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
              <h3 className="text-lg font-semibold mb-4">Head-to-Head</h3>
              <div className="space-y-2">
                {stats.headToHead.slice(0, 10).map(h2h => (
                  <div key={h2h.opponent.id} className="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-gray-800">
                    <a href={`/players/${h2h.opponent.id}`} className="font-medium hover:underline">
                      {h2h.opponent.displayName}
                    </a>
                    <div className="flex items-center gap-2">
                      <span className="text-green-600 font-bold">{h2h.wins}</span>
                      <span className="text-gray-400">-</span>
                      <span className="text-red-600 font-bold">{h2h.losses}</span>
                      <span className="text-xs text-gray-400">({h2h.totalMatches})</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Recent matches */}
        {stats.recentMatches.length > 0 && (
          <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6 mt-6">
            <h3 className="text-lg font-semibold mb-4">Recent Matches</h3>
            <div className="space-y-2">
              {stats.recentMatches.map(match => {
                const won = match.winner?.id === parseInt(id!)
                return (
                  <div key={match.id} className="flex items-center gap-4 p-3 rounded-lg bg-gray-50 dark:bg-gray-800">
                    <span className={`w-6 text-center font-bold ${won ? 'text-green-600' : 'text-red-600'}`}>
                      {won ? 'W' : 'L'}
                    </span>
                    <span className="flex-1">
                      vs{' '}
                      {match.player1?.id === parseInt(id!)
                        ? match.player2?.displayName
                        : match.player1?.displayName}
                    </span>
                    <span className="text-sm font-mono text-gray-600 dark:text-gray-400">
                      {match.sets.map(s => `${s.player1Score}-${s.player2Score}`).join(' / ')}
                    </span>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </main>
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="p-3 rounded-lg bg-gray-50 dark:bg-gray-800 text-center">
      <div className="text-xl font-bold">{value}</div>
      <div className="text-xs text-gray-500 dark:text-gray-400">{label}</div>
    </div>
  )
}
