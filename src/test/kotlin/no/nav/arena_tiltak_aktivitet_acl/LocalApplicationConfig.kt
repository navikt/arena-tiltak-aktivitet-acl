package no.nav.arena_tiltak_aktivitet_acl

import no.nav.arena_tiltak_aktivitet_acl.utils.token_provider.ScopedTokenProvider
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EnableJwtTokenValidation
@Configuration
open class LocalApplicationConfig {

	@Bean
	open fun scopedTokenProvider(): ScopedTokenProvider {
		return object : ScopedTokenProvider {
			override fun getToken(scope: String): String {
				return "MOCK_TOKEN"
			}
		}
	}

}
