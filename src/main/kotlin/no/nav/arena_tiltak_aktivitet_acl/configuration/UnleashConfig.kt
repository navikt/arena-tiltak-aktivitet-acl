package no.nav.arena_tiltak_aktivitet_acl.configuration

import io.getunleash.util.UnleashConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unleash")
data class UnleashConfig(
	val appName: String,
    val url: String,
    val token: String,
    val instanceId: String,
    val environment: String,
) {
    fun toUnleashConfig(): UnleashConfig {
        return UnleashConfig.builder()
            .appName(appName)
            .instanceId(instanceId)
            .unleashAPI("$url/api")
            .apiKey(token)
            .environment(environment)
            .build()
    }
}
