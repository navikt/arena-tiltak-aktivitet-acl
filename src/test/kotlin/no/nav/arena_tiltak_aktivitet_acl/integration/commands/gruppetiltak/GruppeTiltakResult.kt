package no.nav.arena_tiltak_aktivitet_acl.integration.commands.gruppetiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.AktivitetResult

class GruppeTiltakResult(pos: String, arenaDataDbo: ArenaDataDbo, translationDbo: TranslationDbo) :
	AktivitetResult(pos, arenaDataDbo, translationDbo) {}
