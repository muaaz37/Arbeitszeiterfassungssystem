package de.arbeitszeiterfassung.backend.benutzer

import de.arbeitszeiterfassung.backend.PostgreSqlTestConfiguration
import de.arbeitszeiterfassung.backend.benutzer.domain.*
import de.arbeitszeiterfassung.backend.speichereAktivesTestkonto
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ERSTER = "00000000-0000-4000-8000-000000000017"
private const val ZWEITER = "00000000-0000-4000-8000-000000000018"

private const val DRITTER = "00000000-0000-4000-8000-000000000019"

@SpringBootTest(properties = ["benutzer.entwicklung.mitarbeiter-ids=$ERSTER,$ZWEITER,$DRITTER"])
@AutoConfigureMockMvc
@Import(PostgreSqlTestConfiguration::class)
class BenutzeranlageTests @Autowired constructor(
    private val mvc: MockMvc,
    private val konten: BenutzerRepository,
    private val rollen: RollenRepository
) {
    @MockitoBean
    lateinit var identitaeten: IdentityProvider

    private fun admin(): Benutzer {
        val konto = konten.speichereAktivesTestkonto()
        return konten.speichere(konto.copy(rollen = setOf(rollen.findeAlle().single { it.bezeichnung == "ADMINISTRATION" })))
    }

    private fun post(konto: Benutzer, body: String) = mvc.post("/api/v1/benutzer") {
        with(jwt().jwt { it.subject(konto.keycloakId) })
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun anfrage(name: String, mitarbeiter: String) =
        """{"kennung":"$name","email":"$name@example.test","mitarbeiterId":"$mitarbeiter"}"""

    @Test
    fun `Anlage verknuepft Identitaet und Mitarbeiter und weist ausschliesslich MITARBEITER zu`() {
        val admin = admin()
        val name = "neuer-benutzer"
        val email = "$name@example.test"
        val subject = UUID.randomUUID().toString()
        `when`(identitaeten.legeIdentitaetAn(name, email)).thenReturn(subject)
        val result = post(admin, anfrage(name, ERSTER)).andExpect { status { isCreated() } }.andReturn()
        val id = JsonMapper.builder().build().readTree(result.response.contentAsByteArray)["id"].asString()
        val konto = requireNotNull(konten.findeNachId(UUID.fromString(id)))
        assertEquals(subject, konto.keycloakId)
        assertEquals(UUID.fromString(ERSTER), konto.mitarbeiterId)
        assertEquals(name, konto.kennung)
        assertEquals(email, konto.email)
        assertTrue(konto.aktiv)
        assertEquals(setOf("MITARBEITER"), konto.rollen.map { it.bezeichnung }.toSet())

        post(admin, anfrage(name, ZWEITER)).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("KENNUNG_VERGEBEN") }
        }
        post(admin, anfrage("anderer-benutzer", ERSTER)).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("MITARBEITER_ZUGEORDNET") }
        }
        verify(identitaeten).legeIdentitaetAn(name, email)
        verifyNoMoreInteractions(identitaeten)
    }

    @Test
    fun `Validierung und Autorisierung erfolgen vor der Identitaetsanlage`() {
        val admin = admin()
        for (body in listOf("{}", anfrage("   ", ZWEITER), anfrage("test", "keine-uuid"),
            anfrage("test", ZWEITER).replace("test@example.test", "ungueltig"),
            anfrage("test", UUID.randomUUID().toString()))) {
            post(admin, body).andExpect {
                status { isBadRequest() }
                jsonPath("$.status") { value(400) }
                jsonPath("$.message") { isString() }
            }
        }
        post(konten.speichereAktivesTestkonto(), anfrage("test", ZWEITER)).andExpect { status { isForbidden() } }
        verifyNoInteractions(identitaeten)
    }

    @Test
    fun `Fehlende Bestaetigung von Keycloak erzeugt kein Konto und keinen Wiederholungsaufruf`() {
        val name = "timeout"
        val email = "$name@example.test"
        `when`(identitaeten.legeIdentitaetAn(name, email)).thenThrow(IdentitaetsanbieterNichtErreichbar())
        post(admin(), anfrage(name, DRITTER)).andExpect {
            status { isBadGateway() }
            jsonPath("$.code") { value("IDENTITAETSANBIETER_FEHLER") }
        }
        assertFalse(konten.existiertKennung(name))
        verify(identitaeten).legeIdentitaetAn(name, email)
        verifyNoMoreInteractions(identitaeten)
    }

    @Test
    fun `Konkurrierende Mitarbeiterzuordnung rollt die neu erzeugte Identitaet zurueck`() {
        val name = "konkurrenz"
        val email = "$name@example.test"
        val subject = UUID.randomUUID().toString()
        `when`(identitaeten.legeIdentitaetAn(name, email)).thenAnswer {
            val anderesKonto = konten.speichereAktivesTestkonto()
            konten.speichere(anderesKonto.copy(mitarbeiterId = UUID.fromString(ZWEITER)))
            subject
        }
        post(admin(), anfrage(name, ZWEITER)).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("MITARBEITER_ZUGEORDNET") }
        }
        verify(identitaeten).loescheIdentitaet(subject)
        assertFalse(konten.existiertKennung(name))
    }
}
