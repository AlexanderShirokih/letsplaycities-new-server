package ru.quandastudio.lpsserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quandastudio.lpsserver.core.game.id.GameIdFactory
import ru.quandastudio.lpsserver.core.game.id.SimpleGameIdFactory

@Configuration
open class GameCoreConfig {

    @Bean
    open fun provideGameIdFactory(): GameIdFactory {
        return SimpleGameIdFactory()
    }
}