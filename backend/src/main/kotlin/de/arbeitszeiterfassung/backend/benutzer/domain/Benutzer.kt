package de.arbeitszeiterfassung.backend.benutzer.domain

import java.util.UUID

data class Benutzer(
    val id: UUID,
    val kennung: String,
    val email: String,
    val aktiv: Boolean,
    val keycloakId: String,
    val mitarbeiterId: UUID,
    val rollen: Set<Rolle>
)
