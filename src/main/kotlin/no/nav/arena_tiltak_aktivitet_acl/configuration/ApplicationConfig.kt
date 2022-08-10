package no.nav.arena_tiltak_aktivitet_acl.configuration

import no.nav.arena_tiltak_aktivitet_acl.utils.token_provider.ScopedTokenProvider
import no.nav.arena_tiltak_aktivitet_acl.utils.token_provider.azure_ad.AzureAdScopedTokenProviderBuilder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("default")
@EnableJwtTokenValidation
@Configuration
open class ApplicationConfig {

	@Bean
	open fun scopedTokenProvider(): ScopedTokenProvider {
		return AzureAdScopedTokenProviderBuilder.builder().withEnvironmentDefaults().build()
	}

}
