package de.arbeitszeiterfassung.backend.benutzer.application

import de.arbeitszeiterfassung.backend.benutzer.domain.Benutzer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerBerechtigungsAuskunft
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BenutzerBerechtigungsService(
    private val benutzerRepository: BenutzerRepository
) : BenutzerBerechtigungsAuskunft {
    override fun istAktiv(benutzerId: UUID): Boolean =
        benutzerRepository.findeNachId(benutzerId)?.aktiv == true

    override fun ermittleBerechtigungen(benutzerId: UUID): Set<String> =
        benutzerRepository.findeNachId(benutzerId)?.takeIf { it.aktiv }?.let(::rechteAusRollen).orEmpty()

    fun berechtigungenFuerAktivesSubject(subject: String): Set<String>? =
        benutzerRepository.findeNachKeycloakId(subject)?.takeIf { it.aktiv }?.let(::rechteAusRollen)

    private fun rechteAusRollen(benutzer: Benutzer): Set<String> =
        benutzer.rollen.flatMapTo(mutableSetOf()) { rolle ->
            berechtigungenProRolle[rolle.bezeichnung].orEmpty()
        }

    companion object {
        const val BENUTZER_VERWALTEN = "benutzer.verwalten"

        private val berechtigungenProRolle = mapOf(
            "MITARBEITER" to setOf(
                "stammdaten.eigene.lesen",
                "urlaub.eigen.beantragen",
                "urlaub.eigen.lesen",
                "urlaub.eigen.stornieren",
                "arbeitszeit.eigene.erfassen",
                "pause.eigene.erfassen",
                "arbeitszeit.eigene.lesen",
                "krankmeldung.eigene.erfassen",
                "dienstplan.eigen.veroeffentlicht.lesen",
                "kennzahlen.eigene.lesen",
                "benachrichtigung.eigene.lesen"
            ),
            "VORGESETZTER" to setOf(
                "urlaub.abteilung.entscheiden",
                "stammdaten.abteilung.lesen",
                "abwesenheit.abteilung.lesen",
                "schicht.abteilung.zuweisen",
                "dienstplan.abteilung.bearbeiten",
                "dienstplan.abteilung.veroeffentlichen",
                "kennzahlen.abteilung.lesen",
                "arbeitszeit.abteilung.korrigieren"
            ),
            "PERSONALVERWALTUNG" to setOf(
                "stammdaten.verwalten",
                "abteilung.verwalten",
                "arbeitszeitregel.verwalten",
                "urlaubsanspruch.verwalten",
                "feiertag.verwalten",
                "abwesenheit.personal.verwalten",
                "arbeitszeit.personal.korrigieren",
                "schichtvorlage.verwalten",
                "schicht.personal.zuweisen",
                "dienstplan.personal.bearbeiten",
                "dienstplan.personal.veroeffentlichen",
                "urlaub.personal.entscheiden",
                "kennzahlen.personal.lesen"
            ),
            "ADMINISTRATION" to setOf(
                BENUTZER_VERWALTEN,
                "rollen.zuweisen_entziehen",
                "audit.lesen"
            )
        )
    }
}
