package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DatavaskTest : FunSpec({

	test("replaceSpecialChars") {
		"+.!-_,;:-*&-- /\\'`´-,$".nullifyStringWithOnlySpecialChars() shouldBe null
		"+.!- Noen spesialkarakterer -----,$".nullifyStringWithOnlySpecialChars()  shouldBe "+.!- Noen spesialkarakterer -----,$"
	}

	test("redactNorwegianSSNs") {
		"12345678901 Navn Navnesen".redactNorwegianSSNs() shouldBe "[FNR] Navn Navnesen"
		"Navn Navnesen 12345678901".redactNorwegianSSNs() shouldBe "Navn Navnesen [FNR]"
		"Fødselsnummer 123456 78901 Tel: 99999999".redactNorwegianSSNs() shouldBe "Fødselsnummer [FNR] Tel: 99999999"
		"123456789012345".redactNorwegianSSNs() shouldBe "123456789012345"
	}
})
