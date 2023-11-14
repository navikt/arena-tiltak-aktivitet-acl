package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigInteger

class OperationPosTest : StringSpec({

	"Should pad to 20 characters" {
		OperationPos.of("000232432052").value shouldBe "00000000000232432052"
		OperationPos.of("000232432052").value.length shouldBe 20
	}

	"Should have same numeric value" {
		OperationPos.of("00034235236235").value.toBigInteger() shouldBe BigInteger.valueOf(34235236235L)
	}

	"Should throw on non-numeric value" {
		shouldThrowExactly<IllegalArgumentException> { OperationPos.of("ABC") }
	}

	"Should throw when more than 20 characters" {
		shouldThrowExactly<IllegalArgumentException> { OperationPos.of("123456789012345678901")}
	}

})
