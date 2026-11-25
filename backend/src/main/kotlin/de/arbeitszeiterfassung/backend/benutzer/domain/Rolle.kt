package de.arbeitszeiterfassung.backend.benutzer.domain

import java.util.UUID

data class Rolle(
    val id: UUID,
    val bezeichnung: String
)
