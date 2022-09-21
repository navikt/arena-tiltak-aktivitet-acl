package no.nav.arena_tiltak_aktivitet_acl.configuration

import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.featuretoggle.UnleashClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
open class FeatureToggleConfig {
	@Bean
	open fun isEnabled(@Value("\${app.unleashUrl}") unleashUrl: String): UnleashClient {
		return UnleashClientImpl(
			unleashUrl,
			"aktivitet-arena-acl"
		)
	}
}
