package de.arbeitszeiterfassung.backend.auth
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class AuthMeResponse(
    val subject: String,
    val username: String
)

@RestController
class AuthController {

    @GetMapping("/api/v1/auth/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): AuthMeResponse {
        val subject = jwt.subject
        val username = jwt.getClaimAsString("preferred_username")

        if (subject.isNullOrBlank() || username.isNullOrBlank()) {
            throw InvalidBearerTokenException("Erforderliche Benutzerkennung fehlt")
        }
        return AuthMeResponse(subject = subject, username = username)
    }
}
