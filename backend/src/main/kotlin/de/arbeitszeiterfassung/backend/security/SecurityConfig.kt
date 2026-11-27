package de.arbeitszeiterfassung.backend.security

import de.arbeitszeiterfassung.backend.benutzer.application.BenutzerBerechtigungsService
import de.arbeitszeiterfassung.backend.benutzer.application.BenutzerBerechtigungsService.Companion.BENUTZER_VERWALTEN
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext

@Configuration
class SecurityConfig(private val berechtigungsService: BenutzerBerechtigungsService) {
    private fun kontozugriff(berechtigung: String? = null) =
        AuthorizationManager<RequestAuthorizationContext> { authentication, _ ->
            val jwt = authentication.get() as? JwtAuthenticationToken
            if (jwt == null || !jwt.isAuthenticated) return@AuthorizationManager AuthorizationDecision(false)

            val subject = jwt.token.subject
            if (subject.isNullOrBlank()) throw InvalidBearerTokenException("Subject fehlt")
            val rechte = berechtigungsService.berechtigungenFuerAktivesSubject(subject)
            AuthorizationDecision(rechte != null && (berechtigung == null || berechtigung in rechte))
        }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            csrf { disable() }
            authorizeHttpRequests {
                authorize(HttpMethod.GET, "/actuator/health", permitAll)
                authorize("/api/v1/benutzer/**", kontozugriff(BENUTZER_VERWALTEN))
                authorize(anyRequest, kontozugriff())
            }
            oauth2ResourceServer { jwt { } }
        }
        return http.build()
    }
}
