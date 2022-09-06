package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

data class StatusDto (
	val type: AktivitetStatus,
	val aarsak: AarsakType? //TODO: Denne må vi snakke mer om
){
	//Subsett av arenastatuser
	enum class AarsakType {
		SOKT_INN, //Aktuell
		AVSLAG,
		IKKE_AKTUELL, //IKKAKTUELL
		IKKE_MOETT, //IKKEMK
		INFOMOETE,
		TAKKET_JA, //JATAKK
		TAKKET_NEI, //NEITAKK
		FATT_PLASS, //TILBUD
		VENTELISTE
	}
}
