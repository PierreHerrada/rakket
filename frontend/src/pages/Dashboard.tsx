export default function Dashboard() {
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
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
            <h2 className="text-lg font-semibold mb-2">Next Tournament</h2>
            <p className="text-gray-500 dark:text-gray-400">No upcoming tournament scheduled.</p>
          </div>

          <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
            <h2 className="text-lg font-semibold mb-2">Top Players</h2>
            <p className="text-gray-500 dark:text-gray-400">No players yet.</p>
          </div>

          <div className="bg-white dark:bg-gray-900 rounded-xl shadow p-6">
            <h2 className="text-lg font-semibold mb-2">Recent Results</h2>
            <p className="text-gray-500 dark:text-gray-400">No matches played yet.</p>
          </div>
        </div>
      </main>
    </div>
  )
}
