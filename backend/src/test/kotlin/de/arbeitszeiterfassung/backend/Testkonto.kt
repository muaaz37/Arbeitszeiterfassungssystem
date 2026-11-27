package de.arbeitszeiterfassung.backend

import de.arbeitszeiterfassung.backend.benutzer.domain.Benutzer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import java.util.UUID

internal fun BenutzerRepository.speichereAktivesTestkonto(): Benutzer {
    val subject = UUID.randomUUID().toString()
    return speichere(Benutzer(
        id = UUID.randomUUID(),
        kennung = "test-$subject",
        email = "$subject@example.test",
        aktiv = true,
        keycloakId = subject,
        mitarbeiterId = UUID.randomUUID(),
        rollen = emptySet()
    ))
}
