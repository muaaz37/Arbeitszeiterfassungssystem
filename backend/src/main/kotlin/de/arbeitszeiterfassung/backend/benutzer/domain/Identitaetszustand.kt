package de.arbeitszeiterfassung.backend.benutzer.domain

data class Identitaetszustand(
    val keycloakId: String,
    val email: String?,
    val aktiv: Boolean,
)
