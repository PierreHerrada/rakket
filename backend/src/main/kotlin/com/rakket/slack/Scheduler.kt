package com.rakket.slack

import com.rakket.config.AppConfig
import com.rakket.tournament.TournamentEngine
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.*
import java.time.format.DateTimeFormatter

private val logger = LoggerFactory.getLogger("com.rakket.slack.Scheduler")

/**
 * Coroutine-based scheduler for recurring tasks: registration messages,
 * tournament starts, daily summaries, and weekly recaps.
 */
class Scheduler(private val config: AppConfig, private val bot: SlackBot) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun start() {
        logger.info("Scheduler starting with timezone=${config.timezone}, " +
            "tournamentDay=${config.tournamentDay}, " +
            "registrationTime=${config.registrationTime}, " +
            "tournamentTime=${config.tournamentTime}")

        // Schedule tournament registration message
        scheduleWeekly(
            dayOfWeek = DayOfWeek.valueOf(config.tournamentDay),
            time = LocalTime.parse(config.registrationTime, timeFormatter),
            name = "tournament-registration",
        ) {
            val today = LocalDate.now(ZoneId.of(config.timezone))
            val tournamentId = TournamentEngine.createTournament(today)
            bot.postRegistrationMessage(tournamentId)
        }

        // Schedule tournament start (close registration, generate brackets)
        scheduleWeekly(
            dayOfWeek = DayOfWeek.valueOf(config.tournamentDay),
            time = LocalTime.parse(config.tournamentTime, timeFormatter),
            name = "tournament-start",
        ) {
            // Find today's tournament in registration state and start it
            val today = LocalDate.now(ZoneId.of(config.timezone))
            val tournamentId = org.jetbrains.exposed.sql.transactions.transaction {
                com.rakket.db.Tournaments.select {
                    (com.rakket.db.Tournaments.date eq today) and
                        (com.rakket.db.Tournaments.status eq "registration")
                }.firstOrNull()?.get(com.rakket.db.Tournaments.id)
            } ?: return@scheduleWeekly

            val started = TournamentEngine.startTournament(tournamentId)
            if (started) {
                bot.postRoundMatchups(tournamentId, 1)
            } else {
                bot.postMessage(":x: Tournament cancelled — not enough players (minimum 3 required).")
            }
        }

        // Schedule daily summary at 09:00 every day
        scheduleDaily(
            time = LocalTime.of(9, 0),
            name = "daily-summary",
        ) {
            bot.postDailySummary()
        }

        // Schedule weekly recap on Fridays at 17:00
        scheduleWeekly(
            dayOfWeek = DayOfWeek.FRIDAY,
            time = LocalTime.of(17, 0),
            name = "weekly-recap",
        ) {
            bot.postWeeklyRecap()
        }

        logger.info("Scheduler started successfully")
    }

    fun stop() {
        scope.cancel()
        logger.info("Scheduler stopped")
    }

    private fun scheduleWeekly(
        dayOfWeek: DayOfWeek,
        time: LocalTime,
        name: String,
        action: suspend () -> Unit,
    ) {
        scope.launch {
            while (isActive) {
                val now = ZonedDateTime.now(ZoneId.of(config.timezone))
                var nextRun = now.with(dayOfWeek).with(time)

                if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
                    nextRun = nextRun.plusWeeks(1)
                }

                val delayMs = Duration.between(now, nextRun).toMillis()
                logger.info("[$name] Next run at $nextRun (in ${delayMs / 1000}s)")

                delay(delayMs)

                try {
                    action()
                    logger.info("[$name] Executed successfully")
                } catch (e: Exception) {
                    logger.error("[$name] Failed", e)
                }
            }
        }
    }

    private fun scheduleDaily(
        time: LocalTime,
        name: String,
        action: suspend () -> Unit,
    ) {
        scope.launch {
            while (isActive) {
                val now = ZonedDateTime.now(ZoneId.of(config.timezone))
                var nextRun = now.with(time)

                if (nextRun.isBefore(now) || nextRun.isEqual(now)) {
                    nextRun = nextRun.plusDays(1)
                }

                val delayMs = Duration.between(now, nextRun).toMillis()
                logger.info("[$name] Next run at $nextRun (in ${delayMs / 1000}s)")

                delay(delayMs)

                try {
                    action()
                    logger.info("[$name] Executed successfully")
                } catch (e: Exception) {
                    logger.error("[$name] Failed", e)
                }
            }
        }
    }
}
