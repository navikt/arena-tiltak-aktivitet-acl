package no.nav.arena_tiltak_aktivitet_acl

import io.getunleash.Unleash
import io.mockk.every
import io.mockk.mockk
import no.nav.arena_tiltak_aktivitet_acl.utils.token_provider.ScopedTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class LocalApplicationConfig {

	@Bean
	open fun unlead(): Unleash  {
		val mock = mockk<Unleash>()
		every { mock.isEnabled(any()) } returns false
		return mock
	}

	@Bean
	open fun scopedTokenProvider(): ScopedTokenProvider {
		return object : ScopedTokenProvider {
			override fun getToken(scope: String): String {
				return "MOCK_TOKEN"
			}
		}
	}

}
