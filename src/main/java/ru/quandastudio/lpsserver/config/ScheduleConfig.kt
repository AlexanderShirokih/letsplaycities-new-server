package ru.quandastudio.lpsserver.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import ru.quandastudio.lpsserver.core.ServerContext
import ru.quandastudio.lpsserver.util.StringUtil
import ru.quandastudio.lpsserver.websocket.SocketHandler
import java.time.Duration

@Configuration
@EnableScheduling
open class ScheduleConfig(
    private val serverContext: ServerContext,
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    private var serverUptime: Long = 0L
    private var lastTime: Long = System.currentTimeMillis()

    @Scheduled(initialDelay = SCHEDULE_DELAY, fixedDelay = SCHEDULE_PERIOD)
    private fun scheduleSystemTasks() {
        val now = System.currentTimeMillis()

        serverUptime += (now - lastTime)
        lastTime = now

        SocketHandler.logstate()
        serverContext.log()
        serverContext.taskLooper.log()
        serverContext.botManager.log()
        log.info("UPTIME: {}", StringUtil.formatDuration(Duration.ofMillis(serverUptime)))
    }

    companion object {
        private const val SCHEDULE_DELAY = 5000L
        private const val SCHEDULE_PERIOD = 120_000L
    }
}