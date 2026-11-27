package de.arbeitszeiterfassung.backend.auth

import de.arbeitszeiterfassung.backend.benutzer.application.BenutzerBerechtigungsService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class MeineBerechtigungenResponse(val aktiv: Boolean, val berechtigungen: Set<String>)

@RestController
class MeineBerechtigungenController(private val service: BenutzerBerechtigungsService) {
    @GetMapping("/api/v1/me/permissions")
    fun meineBerechtigungen(@AuthenticationPrincipal jwt: Jwt): MeineBerechtigungenResponse {
        val subject = jwt.subject ?: throw InvalidBearerTokenException("Subject fehlt")
        val rechte = service.berechtigungenFuerAktivesSubject(subject)
            ?: throw AccessDeniedException("Kein aktives Anwendungskonto")
        return MeineBerechtigungenResponse(aktiv = true, berechtigungen = rechte)
    }
}
