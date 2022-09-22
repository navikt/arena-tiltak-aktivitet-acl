package no.nav.arena_tiltak_aktivitet_acl.configuration

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.arena_tiltak_aktivitet_acl.utils.token_provider.ScopedTokenProvider
import no.nav.arena_tiltak_aktivitet_acl.utils.token_provider.azure_ad.AzureAdScopedTokenProviderBuilder
import no.nav.common.json.JsonUtils
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("default")
@EnableJwtTokenValidation
@Configuration
open class ApplicationConfig {

	init {
		JsonUtils.getMapper().registerKotlinModule()
	}

	@Bean
	open fun machineToMachineTokenClient(): MachineToMachineTokenClient {
		return AzureAdTokenClientBuilder.builder()
			.withNaisDefaults()
			.buildMachineToMachineTokenClient()
	}

}
