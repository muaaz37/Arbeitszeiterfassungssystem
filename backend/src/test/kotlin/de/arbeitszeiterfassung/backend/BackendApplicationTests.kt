package de.arbeitszeiterfassung.backend

import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import de.arbeitszeiterfassung.backend.benutzer.domain.RollenRepository
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest
@AutoConfigureMockMvc
@Import(
    BackendApplicationTests.TestEndpoint::class,
    PostgreSqlTestConfiguration::class
)
class BackendApplicationTests {
    @RestController
    class TestEndpoint {
        @GetMapping("/api/test")
        fun get() = "OK"

        @GetMapping("/api/v1/benutzer/testzugriff")
        fun verwaltungszugriff() = "OK"
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var benutzerRepository: BenutzerRepository

    @Autowired
    lateinit var rollenRepository: RollenRepository

    @Test
    fun `Eigene Berechtigungen folgen dem aktiven Konto aus dem Token`() {
        val konto = benutzerRepository.speichereAktivesTestkonto()
        val admin = rollenRepository.findeAlle().single { it.bezeichnung == "ADMINISTRATION" }
        benutzerRepository.speichere(konto.copy(rollen = setOf(admin)))
        val identitaet = jwt().jwt { it.subject(konto.keycloakId) }

        mockMvc.get("/api/v1/me/permissions") { with(identitaet) }.andExpect {
            status { isOk() }
            jsonPath("$.aktiv") { value(true) }
            jsonPath("$.berechtigungen") {
                value(containsInAnyOrder("benutzer.verwalten", "rollen.zuweisen_entziehen", "audit.lesen"))
            }
        }

        benutzerRepository.speichere(konto.copy(aktiv = false, rollen = setOf(admin)))
        mockMvc.get("/api/v1/me/permissions") { with(identitaet) }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `Berechtigungsabruf ohne Konto oder ohne Token wird abgewiesen`() {
        mockMvc.get("/api/v1/me/permissions") {
            with(jwt().jwt { it.subject("unbekanntes-subject") })
        }.andExpect { status { isForbidden() } }

        mockMvc.get("/api/v1/me/permissions").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `Benutzerverwaltung prueft die aktuelle Berechtigung bei jedem Aufruf`() {
        val konto = benutzerRepository.speichereAktivesTestkonto()
        val rollen = rollenRepository.findeAlle().associateBy { it.bezeichnung }
        val mitarbeiter = konto.copy(rollen = setOf(rollen.getValue("MITARBEITER")))
        val admin = konto.copy(rollen = setOf(rollen.getValue("ADMINISTRATION")))
        val token = jwt().jwt { it.subject(konto.keycloakId) }
        val pfad = "/api/v1/benutzer/testzugriff"

        mockMvc.get(pfad).andExpect { status { isUnauthorized() } }
        benutzerRepository.speichere(mitarbeiter)
        for (url in listOf("/api/v1/benutzer", pfad)) {
            mockMvc.get(url) { with(token) }.andExpect { status { isForbidden() } }
        }

        benutzerRepository.speichere(admin)
        mockMvc.get(pfad) { with(token) }.andExpect {
            status { isOk() }
            content { string("OK") }
        }
        benutzerRepository.speichere(mitarbeiter)
        mockMvc.get(pfad) { with(token) }.andExpect { status { isForbidden() } }

        benutzerRepository.speichere(admin.copy(aktiv = false))
        mockMvc.get(pfad) { with(token) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun contextLoads() {
    }

    @Test
    fun `GET ohne Zugriffstoken liefert 401`() {
        mockMvc.get("/api/test").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `GET mit authentifizierter Testidentitaet erreicht den Testendpunkt`() {
        val konto = benutzerRepository.speichereAktivesTestkonto()
        mockMvc.get("/api/test") { with(jwt().jwt { it.subject(konto.keycloakId) }) }.andExpect {
            status { isOk() }
            content { string("OK") }
        }
    }

    @Test
    fun `Healthcheck ist ohne Zugriffstoken erreichbar`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `GET auth me liefert Subject und Benutzername`() {
        val konto = benutzerRepository.speichereAktivesTestkonto()
        mockMvc.get("/api/v1/auth/me") {
            with(jwt().jwt { token ->
                token.subject(konto.keycloakId)
                token.claim("preferred_username", "test-user")
            })
        }.andExpect {
            status { isOk() }
            jsonPath("$.subject") { value(konto.keycloakId) }
            jsonPath("$.username") { value("test-user") }
        }
    }

    @Test
    fun `GET auth me ohne Anwendungskonto liefert 403`() {
        mockMvc.get("/api/v1/auth/me") {
            with(jwt().jwt { token ->
                token.subject("keycloak-id-ohne-anwendungskonto")
                token.claim("preferred_username", "test-user")
            })
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET auth me ohne preferred_username liefert 401`() {
        val konto = benutzerRepository.speichereAktivesTestkonto()
        mockMvc.get("/api/v1/auth/me") {
            with(jwt().jwt { token -> token.subject(konto.keycloakId) })
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `GET auth me ohne Zugriffstoken liefert 401`() {
        mockMvc.get("/api/v1/auth/me").andExpect {
            status { isUnauthorized() }
        }
    }
}
