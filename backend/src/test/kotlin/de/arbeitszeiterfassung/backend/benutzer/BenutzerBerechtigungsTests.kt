package de.arbeitszeiterfassung.backend.benutzer

import de.arbeitszeiterfassung.backend.PostgreSqlTestConfiguration
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerBerechtigungsAuskunft
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import de.arbeitszeiterfassung.backend.benutzer.domain.RollenRepository
import de.arbeitszeiterfassung.backend.speichereAktivesTestkonto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Import(PostgreSqlTestConfiguration::class)
class BenutzerBerechtigungsTests @Autowired constructor(
    private val auskunft: BenutzerBerechtigungsAuskunft,
    private val benutzerRepository: BenutzerRepository,
    private val rollenRepository: RollenRepository
) {
    @Test
    fun `Rollenwechsel gilt sofort und Mehrfachrollen kombinieren ihre Rechte`() {
        val konto = benutzerRepository.speichereAktivesTestkonto()
        val rollen = rollenRepository.findeAlle().associateBy { it.bezeichnung }
        val admin = rollen.getValue("ADMINISTRATION")
        val personal = rollen.getValue("PERSONALVERWALTUNG")

        fun rechteFuer(vararg namen: String): Set<String> {
            benutzerRepository.speichere(konto.copy(rollen = namen.map { rollen.getValue(it) }.toSet()))
            return auskunft.ermittleBerechtigungen(konto.id)
        }

        assertTrue(auskunft.istAktiv(konto.id))
        assertEquals(emptySet(), auskunft.ermittleBerechtigungen(konto.id))
        val adminRechte = rechteFuer(admin.bezeichnung)
        assertEquals(setOf("benutzer.verwalten", "rollen.zuweisen_entziehen", "audit.lesen"), adminRechte)
        val personalRechte = rechteFuer(personal.bezeichnung)
        assertTrue("stammdaten.verwalten" in personalRechte)
        assertTrue(adminRechte.intersect(personalRechte).isEmpty())
        assertEquals(adminRechte + personalRechte, rechteFuer(admin.bezeichnung, personal.bezeichnung))

        val mitarbeiterRechte = rechteFuer("MITARBEITER")
        assertTrue("urlaub.eigen.beantragen" in mitarbeiterRechte)
        assertFalse("urlaub.abteilung.entscheiden" in mitarbeiterRechte)
        val vorgesetztenRechte = rechteFuer("VORGESETZTER")
        assertTrue("urlaub.abteilung.entscheiden" in vorgesetztenRechte)
        assertFalse("benutzer.verwalten" in vorgesetztenRechte)
    }

    @Test
    fun `Deaktivierte und unbekannte Konten haben keine Berechtigungen`() {
        val konto = benutzerRepository.speichereAktivesTestkonto()
        val admin = rollenRepository.findeAlle().single { it.bezeichnung == "ADMINISTRATION" }
        benutzerRepository.speichere(konto.copy(aktiv = false, rollen = setOf(admin)))

        for (id in listOf(konto.id, UUID.randomUUID())) {
            assertFalse(auskunft.istAktiv(id))
            assertEquals(emptySet(), auskunft.ermittleBerechtigungen(id))
        }
    }
}
