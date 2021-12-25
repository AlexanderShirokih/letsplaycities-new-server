package ru.quandastudio.lpsserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quandastudio.lpsserver.data.mappers.country.CountryGroupMapper
import ru.quandastudio.lpsserver.data.mappers.country.CountryMapper

@Configuration
open class MappersConfig {

    @Bean
    open fun provideCountryMapper(): CountryMapper {
        return CountryMapper()
    }

    @Bean
    open fun provideCountryGroupMapper(countryMapper: CountryMapper): CountryGroupMapper {
        return CountryGroupMapper(countryMapper)
    }
}