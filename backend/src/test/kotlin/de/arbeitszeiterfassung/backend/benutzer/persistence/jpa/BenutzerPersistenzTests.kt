package de.arbeitszeiterfassung.backend.benutzer.persistence.jpa

import de.arbeitszeiterfassung.backend.PostgreSqlTestConfiguration
import de.arbeitszeiterfassung.backend.benutzer.domain.Benutzer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import de.arbeitszeiterfassung.backend.benutzer.domain.Rolle
import de.arbeitszeiterfassung.backend.benutzer.domain.RollenRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest
@Import(PostgreSqlTestConfiguration::class)
class BenutzerPersistenzTests @Autowired constructor(
    private val benutzerRepository: BenutzerRepository,
    private val rollenRepository: RollenRepository,
    private val jdbcTemplate: JdbcTemplate
) {

    @Test
    fun `Migrationen initialisieren die vier Anwendungsrollen`() {
        val rollen = rollenRepository.findeAlle()

        assertEquals(
            setOf("MITARBEITER", "VORGESETZTER", "PERSONALVERWALTUNG", "ADMINISTRATION"),
            rollen.map { it.bezeichnung }.toSet()
        )
        assertEquals(4, rollen.size)
        val rolle = rollen.single { it.bezeichnung == "MITARBEITER" }
        assertEquals(rolle, rollenRepository.findeNachId(rolle.id))
    }

    @Test
    fun `Benutzer mit zwei Rollen wird gespeichert und erneut geladen`() {
        val benutzer = neuerBenutzer()

        val gespeichert = benutzerRepository.speichere(benutzer)

        assertEquals(benutzer, gespeichert)
        assertEquals(benutzer, benutzerRepository.findeNachId(benutzer.id))
        assertEquals(benutzer, benutzerRepository.findeNachKeycloakId(benutzer.keycloakId))
        assertEquals(setOf("MITARBEITER", "VORGESETZTER"), gespeichert.rollen.map { it.bezeichnung }.toSet())
    }

    @Test
    fun `Doppelte Benutzerkennung wird abgewiesen`() {
        val vorhanden = neuerBenutzer()
        benutzerRepository.speichere(vorhanden)
        val doppelt = neuerBenutzer().copy(kennung = vorhanden.kennung)

        assertFailsWith<DataIntegrityViolationException> {
            benutzerRepository.speichere(doppelt)
        }

        assertNull(benutzerRepository.findeNachId(doppelt.id))
        assertEquals(vorhanden, benutzerRepository.findeNachId(vorhanden.id))
    }

    @Test
    fun `Doppelte Keycloak-ID wird abgewiesen`() {
        val vorhanden = neuerBenutzer()
        benutzerRepository.speichere(vorhanden)
        val doppelt = neuerBenutzer().copy(keycloakId = vorhanden.keycloakId)

        assertFailsWith<DataIntegrityViolationException> {
            benutzerRepository.speichere(doppelt)
        }

        assertNull(benutzerRepository.findeNachId(doppelt.id))
        assertEquals(vorhanden, benutzerRepository.findeNachKeycloakId(vorhanden.keycloakId))
    }

    @Test
    fun `Doppelte Mitarbeiterzuordnung wird abgewiesen`() {
        val vorhanden = neuerBenutzer()
        benutzerRepository.speichere(vorhanden)
        val doppelt = neuerBenutzer().copy(mitarbeiterId = vorhanden.mitarbeiterId)

        assertFailsWith<DataIntegrityViolationException> {
            benutzerRepository.speichere(doppelt)
        }

        assertNull(benutzerRepository.findeNachId(doppelt.id))
        assertEquals(vorhanden, benutzerRepository.findeNachId(vorhanden.id))
    }

    @Test
    fun `Doppelte Rollenzuordnung wird durch PostgreSQL abgewiesen`() {
        val benutzer = neuerBenutzer()
        benutzerRepository.speichere(benutzer)
        val rolle = benutzer.rollen.first()

        // Der direkte SQL-Aufruf erreicht die Datenbankregel auch bei einer Set-Beziehung.
        assertFailsWith<DataIntegrityViolationException> {
            jdbcTemplate.update(
                "INSERT INTO benutzerrolle (benutzer_id, rolle_id) VALUES (?, ?)",
                benutzer.id,
                rolle.id
            )
        }

        assertEquals(benutzer, benutzerRepository.findeNachId(benutzer.id))
    }

    @Test
    fun `Deaktivierter Kontostatus bleibt nach erneutem Laden erhalten`() {
        val benutzer = neuerBenutzer()
        benutzerRepository.speichere(benutzer)
        val deaktiviert = benutzer.copy(aktiv = false)

        benutzerRepository.speichere(deaktiviert)

        assertEquals(deaktiviert, benutzerRepository.findeNachId(benutzer.id))
        assertEquals(deaktiviert, benutzerRepository.findeNachKeycloakId(benutzer.keycloakId))
    }

    @Test
    fun `Suche nach unbekannten Kennungen liefert null`() {
        assertNull(benutzerRepository.findeNachId(UUID.randomUUID()))
        assertNull(benutzerRepository.findeNachKeycloakId(UUID.randomUUID().toString()))
        assertNull(rollenRepository.findeNachId(UUID.randomUUID()))
    }

    @Test
    fun `Unbekannte Rolle verhindert eine teilweise Speicherung`() {
        val benutzer = neuerBenutzer()
        val unbekannteRolle = Rolle(UUID.randomUUID(), "UNBEKANNT")

        assertFailsWith<InvalidDataAccessApiUsageException> {
            benutzerRepository.speichere(benutzer.copy(rollen = benutzer.rollen + unbekannteRolle))
        }

        assertNull(benutzerRepository.findeNachId(benutzer.id))
    }

    private fun neuerBenutzer(): Benutzer {
        val id = UUID.randomUUID()
        val rollen = rollenRepository.findeAlle()
            .filter { it.bezeichnung in setOf("MITARBEITER", "VORGESETZTER") }
            .toSet()

        return Benutzer(
            id = id,
            kennung = "test-$id",
            email = "$id@example.test",
            aktiv = true,
            keycloakId = UUID.randomUUID().toString(),
            mitarbeiterId = UUID.randomUUID(),
            rollen = rollen
        )
    }
}
