package no.nav.arena_tiltak_aktivitet_acl.kafka

import java.util.*

interface KafkaProperties {

    fun consumer(): Properties

    fun producer(): Properties

}
