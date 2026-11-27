package de.arbeitszeiterfassung.backend.benutzer.identity.keycloak

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerkennungBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.EmailBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.Identitaetskonflikt
import de.arbeitszeiterfassung.backend.benutzer.domain.TechnischerZugriffAbgelehnt
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentitaetNichtGefunden
import de.arbeitszeiterfassung.backend.benutzer.domain.Identitaetszustand
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentityProviderFehler
import tools.jackson.databind.json.JsonMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KeycloakIdentityAdapterTests {

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    @AfterEach
    fun stoppeServer() {
        server.stop(0)
    }

    @Test
    fun `gibt bei erfolgreicher Anlage die Keycloak-ID zurueck`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users") { anfrage ->
            if (anfrage.requestMethod != "POST" ||
                anfrage.requestHeaders.getFirst("Authorization") != "Bearer test-token"
            ) {
                antworte(anfrage, 403)
            } else {
                anfrage.responseHeaders.set(
                    "Location",
                    "/admin/realms/test-realm/users/test-benutzer-id"
                )
                antworte(anfrage, 201)
            }
        }

        val keycloakId = starteAdapter().legeIdentitaetAn("test-benutzer", "test@example.invalid")

        assertEquals("test-benutzer-id", keycloakId)
    }

    @Test
    fun `meldet eine bereits vergebene Benutzerkennung`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users") { anfrage ->
            antworte(anfrage, 409, """{"errorMessage":"User exists with same username"}""")
        }

        val adapter = starteAdapter()

        assertFailsWith<BenutzerkennungBereitsVergeben> {
            adapter.legeIdentitaetAn("test-benutzer", "test@example.invalid")
        }
    }

    @Test
    fun `meldet eine bereits vergebene E-Mail-Adresse`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users") { anfrage ->
            antworte(anfrage, 409, """{"errorMessage":"User exists with same email"}""")
        }

        val adapter = starteAdapter()

        assertFailsWith<EmailBereitsVergeben> {
            adapter.legeIdentitaetAn("test-benutzer", "test@example.invalid")
        }
    }

    @Test
    fun `meldet einen allgemeinen Konflikt bei mehrdeutiger Antwort`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users") { anfrage ->
            antworte(anfrage, 409, """{"errorMessage":"User exists with same username or email"}""")
        }

        val adapter = starteAdapter()

        assertFailsWith<Identitaetskonflikt> {
            adapter.legeIdentitaetAn("test-benutzer", "test@example.invalid")
        }
    }

    @Test
    fun `meldet abgelehnten Zugriff beim Tokenabruf`() {
        server.createContext("/realms/test-realm/protocol/openid-connect/token") { anfrage ->
            antworte(anfrage, 401, """{"error":"invalid_client"}""")
        }

        val adapter = starteAdapter()

        assertFailsWith<TechnischerZugriffAbgelehnt> {
            adapter.legeIdentitaetAn("test-benutzer", "test@example.invalid")
        }
    }

    @Test
    fun `aendert die E-Mail-Adresse der angegebenen Identitaet`() {
        akzeptiereAenderung(mapOf("email" to "neu@example.invalid"))

        starteAdapter().aendereEmail("test-benutzer-id", "neu@example.invalid")
    }

    @Test
    fun `aktiviert die angegebene Identitaet`() {
        akzeptiereAenderung(mapOf("enabled" to true))

        starteAdapter().aktiviereIdentitaet("test-benutzer-id")
    }

    @Test
    fun `deaktiviert die angegebene Identitaet auch bei wiederholtem Aufruf`() {
        akzeptiereAenderung(mapOf("enabled" to false))
        val adapter = starteAdapter()

        repeat(2) {
            adapter.deaktiviereIdentitaet("test-benutzer-id")
        }
    }

    @Test
    fun `meldet eine unbekannte Identitaet`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users/unbekannt") { anfrage ->
            antworte(anfrage, 404)
        }

        val adapter = starteAdapter()

        assertFailsWith<IdentitaetNichtGefunden> {
            adapter.aktiviereIdentitaet("unbekannt")
        }
    }

    @Test
    fun `meldet fehlende Berechtigungen bei einer Aenderung`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users/test-benutzer-id") { anfrage ->
            antworte(anfrage, 403)
        }

        val adapter = starteAdapter()

        assertFailsWith<TechnischerZugriffAbgelehnt> {
            adapter.deaktiviereIdentitaet("test-benutzer-id")
        }
    }

    @Test
    fun `meldet einen fehlgeschlagenen Keycloak-Aufruf`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users/test-benutzer-id") { anfrage ->
            antworte(anfrage, 500)
        }

        val adapter = starteAdapter()

        val fehler = assertFailsWith<IdentityProviderFehler> {
            adapter.aendereEmail("test-benutzer-id", "neu@example.invalid")
        }
        assertEquals("Keycloak hat die Anfrage mit HTTP 500 abgelehnt.", fehler.message)
    }

    @Test
    fun `liest den aktuellen Zustand der Identitaet`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users/test-benutzer-id") { anfrage ->
            if (anfrage.requestMethod == "GET") {
                antworte(
                    anfrage,
                    200,
                    """{"id":"test-benutzer-id","email":"test@example.invalid","enabled":false}"""
                )
            } else {
                antworte(anfrage, 405)
            }
        }

        val zustand = starteAdapter().leseIdentitaetszustand("test-benutzer-id")

        assertEquals(
            Identitaetszustand("test-benutzer-id", "test@example.invalid", false),
            zustand
        )
    }

    @Test
    fun `loescht die Identitaet fuer die Anlagekompensation`() {
        akzeptiereTokenabruf()
        server.createContext("/admin/realms/test-realm/users/test-benutzer-id") { anfrage ->
            val status = if (anfrage.requestMethod == "DELETE") 204 else 405
            antworte(anfrage, status)
        }

        starteAdapter().loescheIdentitaet("test-benutzer-id")
    }

    private fun akzeptiereAenderung(erwarteteAenderungen: Map<String, Any>) {
        akzeptiereTokenabruf()
        val jsonMapper = JsonMapper.builder().build()
        val pfad = "/admin/realms/test-realm/users/test-benutzer-id"

        server.createContext(pfad) { anfrage ->
            val json = anfrage.requestBody.readAllBytes().toString(Charsets.UTF_8)
            val aenderungen = runCatching {
                jsonMapper.readValue(json, Map::class.java)
            }.getOrNull()

            val status = if (
                anfrage.requestMethod == "PUT" &&
                anfrage.requestURI.path == pfad &&
                anfrage.requestHeaders.getFirst("Authorization") == "Bearer test-token" &&
                aenderungen == erwarteteAenderungen
            ) {
                204
            } else {
                400
            }

            antworte(anfrage, status)
        }
    }

    private fun akzeptiereTokenabruf() {
        server.createContext("/realms/test-realm/protocol/openid-connect/token") { anfrage ->
            antworte(anfrage, 200, """{"access_token":"test-token"}""")
        }
    }

    private fun starteAdapter(): KeycloakIdentityAdapter {
        server.start()
        return KeycloakIdentityAdapter(
            basisUrl = "http://127.0.0.1:${server.address.port}",
            realm = "test-realm",
            clientId = "test-client",
            clientSecret = "test-secret"
        )
    }

    private fun antworte(anfrage: HttpExchange, status: Int, json: String? = null) {
        try {
            anfrage.requestBody.readAllBytes()
            if (json == null) {
                anfrage.sendResponseHeaders(status, -1)
            } else {
                val antwort = json.toByteArray(Charsets.UTF_8)
                anfrage.responseHeaders.set("Content-Type", "application/json")
                anfrage.sendResponseHeaders(status, antwort.size.toLong())
                anfrage.responseBody.write(antwort)
            }
        } finally {
            anfrage.close()
        }
    }
}
