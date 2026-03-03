import { useEffect, useState } from 'react'

interface Settings {
  appUrl: string
  timezone: string
  tournamentDay: string
  registrationTime: string
  tournamentTime: string
  matchFormat: string
}

export default function Admin() {
  const [settings, setSettings] = useState<Settings | null>(null)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('/api/admin/settings')
      .then(res => {
        if (res.status === 403) throw new Error('Not authorized')
        return res.json()
      })
      .then(data => {
        setSettings(data)
        setLoading(false)
      })
      .catch(err => {
        setMessage(err.message)
        setLoading(false)
      })
  }, [])

  const createTournament = async () => {
    const res = await fetch('/api/admin/tournament/create', { method: 'POST' })
    const data = await res.json()
    setMessage(data.message || data.error)
  }

  if (loading) return <div className="min-h-screen flex items-center justify-center text-gray-500">Loading...</div>

  if (!settings) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-red-600 mb-2">Access Denied</h2>
          <p className="text-gray-500">{message || 'You must be an admin to access this page.'}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen">
      <header className="bg-white dark:bg-gray-900 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-primary-600">Rakket</h1>
          <nav className="flex gap-6 text-sm font-medium text-gray-600 dark:text-gray-300">
            <a href="/" className="hover:text-primary-600">Dashboard</a>
            <a href="/leaderboard" className="hover:text-primary-600">Leaderboard</a>
            <a href="/admin" className="text-primary-600 font-semibold">Admin</a>
          </nav>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h2 className="text-2xl font-bold mb-6">Admin Panel</h2>

        {message && (
          <div className="mb-6 p-4 rounded-lg bg-blue-50 dark:bg-blue-900 text-blue-800 dark:text-blue-200">
            {message}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Actions */}
          <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
            <h3 className="text-lg font-semibold mb-4">Actions</h3>
            <div className="space-y-3">
              <button
                onClick={createTournament}
                className="w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
              >
                Create Tournament (Today)
              </button>
            </div>
          </div>

          {/* Current Settings */}
          <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
            <h3 className="text-lg font-semibold mb-4">Current Settings</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Timezone</span>
                <span className="font-medium">{settings.timezone}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Tournament Day</span>
                <span className="font-medium">{settings.tournamentDay}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Registration Time</span>
                <span className="font-medium">{settings.registrationTime}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Tournament Time</span>
                <span className="font-medium">{settings.tournamentTime}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Match Format</span>
                <span className="font-medium">{settings.matchFormat}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">App URL</span>
                <span className="font-medium">{settings.appUrl}</span>
              </div>
              <p className="text-xs text-gray-400 pt-2">
                Settings are configured via environment variables. Restart the app after changes.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
