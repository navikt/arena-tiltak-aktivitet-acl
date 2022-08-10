package no.nav.arena_tiltak_aktivitet_acl

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
    val application = SpringApplication(Application::class.java)
    application.setAdditionalProfiles("local")
    application.run(*args)
}
