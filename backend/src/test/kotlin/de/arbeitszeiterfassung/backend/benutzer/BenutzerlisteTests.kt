package de.arbeitszeiterfassung.backend.benutzer

import de.arbeitszeiterfassung.backend.PostgreSqlTestConfiguration
import de.arbeitszeiterfassung.backend.benutzer.domain.Benutzer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import de.arbeitszeiterfassung.backend.benutzer.domain.RollenRepository
import de.arbeitszeiterfassung.backend.speichereAktivesTestkonto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgreSqlTestConfiguration::class)
class BenutzerlisteTests @Autowired constructor(
    private val mvc: MockMvc,
    private val benutzerRepository: BenutzerRepository,
    private val rollenRepository: RollenRepository
) {
    private val pfad = "/api/v1/benutzer"

    private fun kontoMitRollen(vararg namen: String, aktiv: Boolean = true): Benutzer {
        val rollen = rollenRepository.findeAlle().associateBy { it.bezeichnung }
        val konto = benutzerRepository.speichereAktivesTestkonto()
        return benutzerRepository.speichere(konto.copy(aktiv = aktiv, rollen = namen.map { rollen.getValue(it) }.toSet()))
    }

    @Test
    fun `Admin sieht aktive und deaktivierte Konten mit genau den vereinbarten Feldern`() {
        val admin = kontoMitRollen("ADMINISTRATION", "MITARBEITER")
        val inaktiv = kontoMitRollen(aktiv = false)

        val antwort = mvc.get(pfad) { with(jwt().jwt { it.subject(admin.keycloakId) }) }
            .andExpect { status { isOk() } }.andReturn().response

        val liste = JsonMapper.builder().build().readTree(antwort.contentAsByteArray)
        assertTrue(liste.isArray)
        for (konto in listOf(admin, inaktiv)) {
            val eintrag = liste.single { it["id"].asString() == konto.id.toString() }
            assertEquals(setOf("id", "kennung", "email", "aktiv", "mitarbeiterId", "rollen"), eintrag.propertyNames().toSet())
            assertEquals(konto.kennung, eintrag["kennung"].asString())
            assertEquals(konto.email, eintrag["email"].asString())
            assertEquals(konto.aktiv, eintrag["aktiv"].asBoolean())
            assertEquals(konto.mitarbeiterId.toString(), eintrag["mitarbeiterId"].asString())
            val rollen = eintrag["rollen"]
            assertTrue(rollen.isArray)
            assertEquals(konto.rollen.map { it.bezeichnung }.sorted(), rollen.toList().map { it.asString() })
        }
    }

    @Test
    fun `Benutzerliste erfordert Anmeldung und ein aktives Konto mit Verwaltungsberechtigung`() {
        mvc.get(pfad).andExpect { status { isUnauthorized() } }
        for (konto in listOf(kontoMitRollen("MITARBEITER"), kontoMitRollen("ADMINISTRATION", aktiv = false))) {
            mvc.get(pfad) { with(jwt().jwt { it.subject(konto.keycloakId) }) }
                .andExpect { status { isForbidden() } }
        }
    }
}
