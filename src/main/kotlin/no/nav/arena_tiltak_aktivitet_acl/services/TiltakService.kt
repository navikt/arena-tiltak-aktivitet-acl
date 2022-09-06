package no.nav.arena_tiltak_aktivitet_acl.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak
import no.nav.arena_tiltak_aktivitet_acl.repositories.TiltakRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.CacheUtils.tryCacheFirstNullable
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
open class TiltakService(
	private val tiltakRepository: TiltakRepository
) {

	private val cache: Cache<String, Tiltak> = Caffeine.newBuilder()
		.maximumSize(200)
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.recordStats()
		.build()

	fun upsert(id: UUID, kode: String, navn: String, administrasjonskode: String) {
		cache.invalidate(kode)
		tiltakRepository.upsert(id, kode, navn, administrasjonskode)
	}

	fun getByKode(kode: String): Tiltak? {
		return tryCacheFirstNullable(cache, kode) {
			tiltakRepository.getByKode(kode)?.let {
				Tiltak(
					id = it.id,
					kode = it.kode,
					navn = it.navn,
					administrasjonskode = Tiltak.Administrasjonskode.valueOf(it.administrasjonskode))
			}
		}
	}

	fun invalidateTiltakByKodeCache() {
		cache.invalidateAll()
	}

}

