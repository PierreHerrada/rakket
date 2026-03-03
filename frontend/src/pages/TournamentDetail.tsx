import { useEffect, useState } from 'react'

interface PlayerSummary {
  id: number
  displayName: string
  avatarUrl: string | null
  eloRating: number
}

interface SetScore {
  setNumber: number
  player1Score: number
  player2Score: number
}

interface Match {
  id: number
  player1: PlayerSummary | null
  player2: PlayerSummary | null
  winner: PlayerSummary | null
  status: string
  sets: SetScore[]
  completedAt: string | null
}

interface Round {
  id: number
  roundNumber: number
  status: string
  matches: Match[]
}

interface Participant {
  playerId: number
  displayName: string
  avatarUrl: string | null
  finalPlacement: number | null
  pointsAwarded: number
  roundsWon: number
}

interface TournamentDetail {
  tournament: {
    id: number
    date: string
    status: string
    participantCount: number
    totalRounds: number
  }
  participants: Participant[]
  rounds: Round[]
}

export default function TournamentDetail() {
  const [detail, setDetail] = useState<TournamentDetail | null>(null)
  const [loading, setLoading] = useState(true)

  const id = window.location.pathname.split('/').pop()

  useEffect(() => {
    fetch(`/api/tournaments/${id}`)
      .then(res => res.json())
      .then(data => {
        setDetail(data)
        setLoading(false)
      })
      .catch(() => setLoading(false))
  }, [id])

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center text-gray-500">Loading...</div>
  }

  if (!detail) {
    return <div className="min-h-screen flex items-center justify-center text-gray-500">Tournament not found</div>
  }

  const { tournament, participants, rounds } = detail

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
        <div className="mb-6">
          <a href="/tournaments" className="text-primary-600 hover:underline text-sm">&larr; Back to tournaments</a>
        </div>

        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 mb-8">
          <div>
            <h2 className="text-2xl font-bold">Tournament #{tournament.id}</h2>
            <p className="text-gray-500 dark:text-gray-400">
              {new Date(tournament.date).toLocaleDateString('en-US', {
                weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
              })}
              {' '}&middot; {tournament.participantCount} players &middot; {tournament.totalRounds} rounds
            </p>
          </div>
          <span className={`px-3 py-1 rounded-full text-xs font-medium ${
            tournament.status === 'active' ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300' :
            tournament.status === 'completed' ? 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300' :
            'bg-blue-100 text-blue-800'
          }`}>
            {tournament.status}
          </span>
        </div>

        {/* Standings */}
        {participants.some(p => p.finalPlacement != null) && (
          <div className="mb-8">
            <h3 className="text-lg font-semibold mb-4">Final Standings</h3>
            <div className="bg-white dark:bg-gray-900 rounded-xl shadow overflow-hidden">
              <table className="w-full">
                <thead className="bg-gray-50 dark:bg-gray-800">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">#</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Player</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Points</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                  {[...participants]
                    .filter(p => p.finalPlacement != null)
                    .sort((a, b) => (a.finalPlacement ?? 99) - (b.finalPlacement ?? 99))
                    .map(p => (
                      <tr key={p.playerId} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                        <td className="px-4 py-3 font-bold text-gray-600 dark:text-gray-400">
                          {p.finalPlacement}
                        </td>
                        <td className="px-4 py-3">
                          <a href={`/players/${p.playerId}`} className="hover:underline font-medium">
                            {p.displayName}
                          </a>
                        </td>
                        <td className="px-4 py-3 text-right">{p.pointsAwarded} pts</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Rounds */}
        <h3 className="text-lg font-semibold mb-4">Rounds</h3>
        <div className="space-y-6">
          {rounds.map(round => (
            <div key={round.id} className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
              <div className="flex items-center justify-between mb-4">
                <h4 className="font-semibold">Round {round.roundNumber}</h4>
                <span className={`px-2 py-1 rounded text-xs ${
                  round.status === 'completed' ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300' :
                  round.status === 'active' ? 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300' :
                  'bg-gray-100 text-gray-700'
                }`}>
                  {round.status}
                </span>
              </div>
              <div className="space-y-3">
                {round.matches.map(match => (
                  <div key={match.id} className="flex items-center gap-4 p-3 rounded-lg bg-gray-50 dark:bg-gray-800">
                    <div className="flex-1 text-right">
                      <span className={`font-medium ${match.winner?.id === match.player1?.id ? 'text-green-600 font-bold' : ''}`}>
                        {match.player1?.displayName ?? 'BYE'}
                      </span>
                    </div>
                    <div className="px-4 text-center min-w-[120px]">
                      {match.status === 'bye' ? (
                        <span className="text-gray-400 text-sm">BYE</span>
                      ) : match.sets.length > 0 ? (
                        <span className="text-sm font-mono">
                          {match.sets.map(s => `${s.player1Score}-${s.player2Score}`).join(' / ')}
                        </span>
                      ) : (
                        <span className="text-gray-400 text-sm">vs</span>
                      )}
                    </div>
                    <div className="flex-1">
                      <span className={`font-medium ${match.winner?.id === match.player2?.id ? 'text-green-600 font-bold' : ''}`}>
                        {match.player2?.displayName ?? 'BYE'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}
