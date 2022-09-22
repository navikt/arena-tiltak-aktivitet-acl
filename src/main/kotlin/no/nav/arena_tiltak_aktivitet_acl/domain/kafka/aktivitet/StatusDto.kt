package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

//Subsett av arenastatuser
enum class DeltakerStatus {
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
