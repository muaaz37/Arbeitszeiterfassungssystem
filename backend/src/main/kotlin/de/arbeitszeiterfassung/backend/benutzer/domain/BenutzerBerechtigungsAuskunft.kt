package de.arbeitszeiterfassung.backend.benutzer.domain

import java.util.UUID

interface BenutzerBerechtigungsAuskunft {
    fun istAktiv(benutzerId: UUID): Boolean
    fun ermittleBerechtigungen(benutzerId: UUID): Set<String>
}
