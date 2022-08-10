package no.nav.arena_tiltak_aktivitet_acl.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.amt.AmtTiltak
import no.nav.arena_tiltak_aktivitet_acl.repositories.TiltakRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.CacheUtils.tryCacheFirstNullable
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
open class TiltakService(
	private val tiltakRepository: TiltakRepository
) {

	private val cache: Cache<String, AmtTiltak> = Caffeine.newBuilder()
		.maximumSize(200)
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.recordStats()
		.build()

	fun upsert(id: UUID, kode: String, navn: String) {
		cache.invalidate(kode)
		tiltakRepository.upsert(id, kode, navn)
	}

	fun getByKode(kode: String): AmtTiltak? {
		return tryCacheFirstNullable(cache, kode) { tiltakRepository.getByKode(kode) }
	}

	fun invalidateTiltakByKodeCache() {
		cache.invalidateAll()
	}

}
