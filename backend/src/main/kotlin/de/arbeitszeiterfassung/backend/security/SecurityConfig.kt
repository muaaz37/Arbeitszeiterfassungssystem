package de.arbeitszeiterfassung.backend.security

import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
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
class SecurityConfig(private val benutzerRepository: BenutzerRepository) {
    private val aktivesKonto = AuthorizationManager<RequestAuthorizationContext> { authentication, _ ->
        val jwt = authentication.get() as? JwtAuthenticationToken
        if (jwt == null || !jwt.isAuthenticated) return@AuthorizationManager AuthorizationDecision(false)

        val subject = jwt.token.subject
        if (subject.isNullOrBlank()) throw InvalidBearerTokenException("Subject fehlt")
        AuthorizationDecision(benutzerRepository.findeNachKeycloakId(subject)?.aktiv == true)
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            csrf { disable() }
            authorizeHttpRequests {
                authorize(HttpMethod.GET, "/actuator/health", permitAll)
                authorize(anyRequest, aktivesKonto)
            }
            oauth2ResourceServer { jwt { } }
        }
        return http.build()
    }
}
