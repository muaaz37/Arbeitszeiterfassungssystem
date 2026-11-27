package de.arbeitszeiterfassung.backend.benutzer.identity.keycloak

import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerkennungBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.EmailBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentitaetsanbieterNichtErreichbar
import de.arbeitszeiterfassung.backend.benutzer.domain.Identitaetskonflikt
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentityProvider
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentityProviderFehler
import de.arbeitszeiterfassung.backend.benutzer.domain.TechnischerZugriffAbgelehnt
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentitaetNichtGefunden
import de.arbeitszeiterfassung.backend.benutzer.domain.Identitaetszustand
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient
import java.time.Duration

@Component
class KeycloakIdentityAdapter(
    @Value("\${identity.keycloak.basis-url}") basisUrl: String,
    @Value("\${identity.keycloak.realm}") private val realm: String,
    @Value("\${identity.keycloak.client-id}") private val clientId: String,
    @Value("\${identity.keycloak.client-secret}") private val clientSecret: String,
) : IdentityProvider {

    private val jsonMapper = JsonMapper.builder().build()

    private val restClient = RestClient.builder()
        .baseUrl(basisUrl.trimEnd('/'))
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build()
            ).apply {
                setReadTimeout(Duration.ofSeconds(5))
            }
        )
        .build()

    override fun legeIdentitaetAn(kennung: String, email: String): String {
        val token = holeZugriffstoken()

        val antwort = keycloakAufruf(beiKonflikt = ::benutzerkonflikt) {
            restClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .headers { it.setBearerAuth(token) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("username" to kennung, "email" to email, "enabled" to true))
                .retrieve()
                .toBodilessEntity()
        }

        if (antwort.statusCode != HttpStatus.CREATED) {
            throw IdentityProviderFehler("Keycloak hat die Anlage der Identität nicht bestätigt.")
        }

        val pfad = antwort.headers.location?.path
        val benutzerPfad = "/admin/realms/$realm/users/"
        val keycloakId = pfad
            ?.substringAfterLast(benutzerPfad, "")
            ?.takeIf { it.isNotBlank() && '/' !in it }

        return keycloakId
            ?: throw IdentityProviderFehler("Keycloak hat keine Benutzer-ID zurückgegeben.")
    }

    private fun holeZugriffstoken(): String {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw TechnischerZugriffAbgelehnt()
        }

        val formular = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
        }

        val antwort = keycloakAufruf {
            restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .headers { it.setBasicAuth(clientId, clientSecret) }
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formular)
                .retrieve()
                .body(Map::class.java)
        }

        return (antwort?.get("access_token") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: throw IdentityProviderFehler("Keycloak hat kein Zugriffstoken zurückgegeben.")
    }

    private fun benutzerkonflikt(fehler: RestClientResponseException): IdentityProviderFehler {
        val meldung = runCatching {
            jsonMapper.readValue(fehler.responseBodyAsString, Map::class.java)
                ?.get("errorMessage") as? String
        }.getOrNull()

        return when (meldung) {
            "User exists with same username" -> BenutzerkennungBereitsVergeben()
            "User exists with same email" -> EmailBereitsVergeben()
            else -> Identitaetskonflikt()
        }
    }

    private fun <T> keycloakAufruf(
        identitaetsbezogen: Boolean = false,
        beiKonflikt: (RestClientResponseException) -> IdentityProviderFehler = {
            IdentityProviderFehler("Keycloak hat einen Konflikt gemeldet.")
        },
        aufruf: () -> T,
    ): T {
        try {
            return aufruf()
        } catch (fehler: ResourceAccessException) {
            throw IdentitaetsanbieterNichtErreichbar()
        } catch (fehler: RestClientResponseException) {
            throw when (fehler.statusCode.value()) {
                401, 403 -> TechnischerZugriffAbgelehnt()
                404 -> if (identitaetsbezogen) {
                    IdentitaetNichtGefunden()
                } else {
                    IdentityProviderFehler("Der angefragte Keycloak-Endpunkt wurde nicht gefunden.")
                }
                409 -> beiKonflikt(fehler)
                else -> IdentityProviderFehler(
                    "Keycloak hat die Anfrage mit HTTP ${fehler.statusCode.value()} abgelehnt."
                )
            }
        } catch (fehler: RestClientException) {
            throw IdentityProviderFehler("Die Antwort von Keycloak konnte nicht verarbeitet werden.")
        }
    }

    override fun aendereEmail(keycloakId: String, email: String) {
        aktualisiereIdentitaet(keycloakId, mapOf("email" to email))
    }

    override fun aktiviereIdentitaet(keycloakId: String) {
        aktualisiereIdentitaet(keycloakId, mapOf("enabled" to true))
    }

    override fun deaktiviereIdentitaet(keycloakId: String) {
        aktualisiereIdentitaet(keycloakId, mapOf("enabled" to false))
    }

    override fun leseIdentitaetszustand(keycloakId: String): Identitaetszustand {
        val token = holeZugriffstoken()

        val antwort = keycloakAufruf(identitaetsbezogen = true) {
            restClient.get()
                .uri("/admin/realms/{realm}/users/{keycloakId}", realm, keycloakId)
                .headers { it.setBearerAuth(token) }
                .retrieve()
                .toEntity(Map::class.java)
        }

        val daten = antwort.body
        val id = daten?.get("id") as? String
        val aktiv = daten?.get("enabled") as? Boolean

        if (antwort.statusCode != HttpStatus.OK || id != keycloakId || aktiv == null) {
            throw IdentityProviderFehler("Keycloak hat keinen gültigen Identitätszustand zurückgegeben.")
        }

        return Identitaetszustand(
            keycloakId = keycloakId,
            email = daten?.get("email") as? String,
            aktiv = aktiv,
        )
    }

    override fun loescheIdentitaet(keycloakId: String) {
        val token = holeZugriffstoken()

        val antwort = keycloakAufruf(identitaetsbezogen = true) {
            restClient.delete()
                .uri("/admin/realms/{realm}/users/{keycloakId}", realm, keycloakId)
                .headers { it.setBearerAuth(token) }
                .retrieve()
                .toBodilessEntity()
        }

        if (antwort.statusCode != HttpStatus.NO_CONTENT) {
            throw IdentityProviderFehler("Keycloak hat das Löschen der Identität nicht bestätigt.")
        }
    }

    private fun aktualisiereIdentitaet(keycloakId: String, aenderungen: Map<String, Any>) {
        val token = holeZugriffstoken()

        val antwort = keycloakAufruf(
            identitaetsbezogen = true,
            beiKonflikt = ::benutzerkonflikt,
        ) {
            restClient.put()
                .uri("/admin/realms/{realm}/users/{keycloakId}", realm, keycloakId)
                .headers { it.setBearerAuth(token) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(aenderungen)
                .retrieve()
                .toBodilessEntity()
        }

        if (antwort.statusCode != HttpStatus.NO_CONTENT) {
            throw IdentityProviderFehler("Keycloak hat die Änderung der Identität nicht bestätigt.")
        }
    }

}
