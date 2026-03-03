import { useEffect, useState } from 'react'

interface LeaderboardEntry {
  rank: number
  player: {
    id: number
    displayName: string
    avatarUrl: string | null
    eloRating: number
  }
  eloRating: number
  totalMatches: number
  totalWins: number
  winRate: number
  eloTrend: number
}

export default function Leaderboard() {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([])
  const [type, setType] = useState<'elo' | 'points'>('elo')
  const [period, setPeriod] = useState<'4w' | '12w' | 'all'>('all')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    fetch(`/api/leaderboard?type=${type}&period=${period}`)
      .then(res => res.json())
      .then(data => {
        setEntries(data)
        setLoading(false)
      })
      .catch(() => setLoading(false))
  }, [type, period])

  return (
    <div className="min-h-screen">
      <header className="bg-white dark:bg-gray-900 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-primary-600">Rakket</h1>
          <nav className="flex gap-6 text-sm font-medium text-gray-600 dark:text-gray-300">
            <a href="/" className="hover:text-primary-600">Dashboard</a>
            <a href="/leaderboard" className="text-primary-600 font-semibold">Leaderboard</a>
            <a href="/tournaments" className="hover:text-primary-600">Tournaments</a>
          </nav>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <h2 className="text-2xl font-bold">Leaderboard</h2>

          <div className="flex gap-4">
            <div className="flex rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
              <button
                onClick={() => setType('elo')}
                className={`px-4 py-2 text-sm font-medium ${
                  type === 'elo'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300'
                }`}
              >
                ELO Rating
              </button>
              <button
                onClick={() => setType('points')}
                className={`px-4 py-2 text-sm font-medium ${
                  type === 'points'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300'
                }`}
              >
                Tournament Points
              </button>
            </div>

            <select
              value={period}
              onChange={e => setPeriod(e.target.value as '4w' | '12w' | 'all')}
              className="rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm"
            >
              <option value="4w">Last 4 weeks</option>
              <option value="12w">Last 12 weeks</option>
              <option value="all">All time</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div className="text-center py-12 text-gray-500">Loading...</div>
        ) : entries.length === 0 ? (
          <div className="text-center py-12 text-gray-500">No players yet.</div>
        ) : (
          <div className="bg-white dark:bg-gray-900 rounded-xl shadow overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-800">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Rank
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Player
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    {type === 'elo' ? 'ELO' : 'Points'}
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden sm:table-cell">
                    Trend
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">
                    Matches
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider hidden md:table-cell">
                    Win Rate
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {entries.map(entry => (
                  <tr
                    key={entry.player.id}
                    className="hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`text-lg font-bold ${
                        entry.rank === 1 ? 'text-yellow-500' :
                        entry.rank === 2 ? 'text-gray-400' :
                        entry.rank === 3 ? 'text-amber-700' : 'text-gray-600 dark:text-gray-400'
                      }`}>
                        #{entry.rank}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <a href={`/players/${entry.player.id}`} className="flex items-center gap-3 hover:underline">
                        {entry.player.avatarUrl ? (
                          <img
                            src={entry.player.avatarUrl}
                            alt=""
                            className="w-8 h-8 rounded-full"
                          />
                        ) : (
                          <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center text-sm font-bold text-primary-600">
                            {entry.player.displayName.charAt(0).toUpperCase()}
                          </div>
                        )}
                        <span className="font-medium">{entry.player.displayName}</span>
                      </a>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right font-bold text-lg">
                      {entry.eloRating}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right hidden sm:table-cell">
                      {entry.eloTrend > 0 ? (
                        <span className="text-green-600">+{entry.eloTrend}</span>
                      ) : entry.eloTrend < 0 ? (
                        <span className="text-red-600">{entry.eloTrend}</span>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-gray-600 dark:text-gray-400 hidden md:table-cell">
                      {entry.totalMatches}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-gray-600 dark:text-gray-400 hidden md:table-cell">
                      {(entry.winRate * 100).toFixed(0)}%
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  )
}
