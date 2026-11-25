package de.arbeitszeiterfassung.backend

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.net.InetSocketAddress
import java.time.Instant
import java.util.Date

@SpringBootTest(properties = ["KEYCLOAK_AUDIENCE=arbeitszeiterfassung-api"])
@AutoConfigureMockMvc
class AuthMeJwtDecoderTests @Autowired constructor(private val mockMvc: MockMvc) {
    companion object {
        private val key = RSAKeyGenerator(2048).keyID("test-key").generate()
        private val issuerServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/keys") { exchange ->
                val body = JWKSet(key.toPublicJWK()).toString().toByteArray()
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        private val issuer = "http://127.0.0.1:${issuerServer.address.port}"

        @JvmStatic
        @DynamicPropertySource
        fun configureIssuer(registry: DynamicPropertyRegistry) {
            registry.add("KEYCLOAK_ISSUER_URI") { issuer }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "$issuer/keys" }
        }

        @JvmStatic
        @AfterAll
        fun stopIssuer() {
            issuerServer.stop(0)
        }
    }

    private fun signedToken(
        issuedAt: Instant,
        expiresAt: Instant,
        audience: String = "arbeitszeiterfassung-api",
        tokenIssuer: String = issuer,
        signingKey: RSAKey = key
    ): String {
        val claims = JWTClaimsSet.Builder()
            .issuer(tokenIssuer)
            .audience(audience)
            .subject("konto-1")
            .claim("preferred_username", "test-user")
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt))
            .build()
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build()
        val token = SignedJWT(header, claims)
        token.sign(RSASSASigner(signingKey))
        return token.serialize()
    }

    @Test
    fun `Decoder akzeptiert ein gültiges signiertes Zugriffstoken`() {
        val now = Instant.now()
        val token = signedToken(now.minusSeconds(600), now.plusSeconds(300))
        mockMvc.get("/api/v1/auth/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.subject") { value("konto-1") }
            jsonPath("$.username") { value("test-user") }
        }
    }

    @Test
    fun `Decoder weist ein seit fünf Minuten abgelaufenes Zugriffstoken ab`() {
        val now = Instant.now()
        val token = signedToken(now.minusSeconds(600), now.minusSeconds(300))
        mockMvc.get("/api/v1/auth/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `Decoder weist ein Token für eine andere API ab`() {
        val now = Instant.now()
        val token = signedToken(
            issuedAt = now.minusSeconds(600),
            expiresAt = now.plusSeconds(300),
            audience = "andere-api"
        )
        mockMvc.get("/api/v1/auth/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `Decoder weist ein Token mit abweichendem Aussteller ab`() {
        val now = Instant.now()
        val token = signedToken(
            issuedAt = now.minusSeconds(600),
            expiresAt = now.plusSeconds(300),
            tokenIssuer = "$issuer/anderer-realm"
        )
        mockMvc.get("/api/v1/auth/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `Decoder weist ein Token mit ungültiger Signatur ab`() {
        val now = Instant.now()
        val token = signedToken(
            issuedAt = now.minusSeconds(600),
            expiresAt = now.plusSeconds(300),
            signingKey = RSAKeyGenerator(2048).generate()
        )
        mockMvc.get("/api/v1/auth/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `Decoder weist ein unlesbares Zugriffstoken ab`() {
        mockMvc.get("/api/v1/auth/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer kein-jwt")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
