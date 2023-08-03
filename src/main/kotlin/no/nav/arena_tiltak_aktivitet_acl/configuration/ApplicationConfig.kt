package no.nav.arena_tiltak_aktivitet_acl.configuration

import no.nav.common.auth.oidc.filter.AzureAdUserRoleResolver
import no.nav.common.auth.oidc.filter.OidcAuthenticationFilter
import no.nav.common.auth.oidc.filter.OidcAuthenticator
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig
import no.nav.common.rest.filter.LogRequestFilter
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.utils.EnvironmentUtils
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("default")
@EnableJwtTokenValidation
@Configuration
open class ApplicationConfig {

	companion object {
		const val APPLICATION_NAME = "aktivitet-arena-acl"
	}

	@Bean
	open fun machineToMachineTokenClient(): MachineToMachineTokenClient {
		return AzureAdTokenClientBuilder.builder()
			.withNaisDefaults()
			.buildMachineToMachineTokenClient()
	}

	@Bean
	open fun logRequestFilterRegistrationBean(): FilterRegistrationBean<LogRequestFilter> {
		val registration = FilterRegistrationBean<LogRequestFilter>()
		registration.filter = LogRequestFilter(
			APPLICATION_NAME, EnvironmentUtils.isDevelopment().orElse(false)
		)
		registration.order = 1
		registration.addUrlPatterns("/*")
		return registration
	}

	@Bean
	open fun authenticationFilterRegistrationBean(): FilterRegistrationBean<OidcAuthenticationFilter> {
		val azureAdAuthConfig = OidcAuthenticatorConfig()
			.withDiscoveryUrl(getEnvOrProperty("AZURE_APP_WELL_KNOWN_URL"))
//			.withClientId(getEnvOrProperty("AZURE_APP_CLIENT_ID"))
			.withUserRoleResolver(AzureAdUserRoleResolver())

		val registration = FilterRegistrationBean<OidcAuthenticationFilter>()
		val authenticationFilter = OidcAuthenticationFilter(OidcAuthenticator.fromConfigs(azureAdAuthConfig))
		registration.filter = authenticationFilter
		registration.addUrlPatterns("/api/*")
		return registration
	}

}

fun getEnvOrProperty(key: String): String {
	return System.getenv(key) ?: System.getProperty(key)
}
