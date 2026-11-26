package de.arbeitszeiterfassung.backend.benutzer.identity.keycloak

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerkennungBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.EmailBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.Identitaetskonflikt
import de.arbeitszeiterfassung.backend.benutzer.domain.TechnischerZugriffAbgelehnt
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
