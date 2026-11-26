package de.arbeitszeiterfassung.backend.benutzer.application

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

    override fun ermittleBerechtigungen(benutzerId: UUID): Set<String> {
        val benutzer = benutzerRepository.findeNachId(benutzerId) ?: return emptySet()
        if (!benutzer.aktiv) return emptySet()

        return benutzer.rollen.flatMapTo(mutableSetOf()) { rolle ->
            berechtigungenProRolle[rolle.bezeichnung].orEmpty()
        }
    }

    private companion object {
        val berechtigungenProRolle = mapOf(
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
                "benutzer.verwalten",
                "rollen.zuweisen_entziehen",
                "audit.lesen"
            )
        )
    }
}
