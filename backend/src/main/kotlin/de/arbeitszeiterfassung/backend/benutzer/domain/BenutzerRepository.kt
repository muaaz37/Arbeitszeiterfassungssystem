package de.arbeitszeiterfassung.backend.benutzer.domain

import java.util.UUID

interface BenutzerRepository {

    fun speichere(benutzer: Benutzer): Benutzer

    fun findeNachId(benutzerId: UUID): Benutzer?

    fun findeNachKeycloakId(keycloakId: String): Benutzer?
}
