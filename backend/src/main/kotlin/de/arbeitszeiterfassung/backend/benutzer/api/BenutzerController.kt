package de.arbeitszeiterfassung.backend.benutzer.api

import de.arbeitszeiterfassung.backend.benutzer.application.BenutzerService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class BenutzerAnlageRequest(
    @field:NotBlank @field:Size(max = 255) val kennung: String,
    @field:NotBlank @field:Email @field:Size(max = 255) val email: String,
    val mitarbeiterId: UUID
)

data class BenutzerAnlageResponse(val id: UUID)

data class BenutzerListenEintrag(
    val id: UUID,
    val kennung: String,
    val email: String,
    val aktiv: Boolean,
    val mitarbeiterId: UUID,
    val rollen: List<String>
)

@RestController
@RequestMapping("/api/v1/benutzer")
class BenutzerController(private val benutzerService: BenutzerService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun anlegen(@Valid @RequestBody anfrage: BenutzerAnlageRequest): BenutzerAnlageResponse =
        BenutzerAnlageResponse(benutzerService.legeAn(anfrage.kennung.trim(), anfrage.email, anfrage.mitarbeiterId))

    @GetMapping
    fun liste(): List<BenutzerListenEintrag> = benutzerService.listeBenutzer().map { konto ->
        BenutzerListenEintrag(
            id = konto.id,
            kennung = konto.kennung,
            email = konto.email,
            aktiv = konto.aktiv,
            mitarbeiterId = konto.mitarbeiterId,
            rollen = konto.rollen.map { it.bezeichnung }.sorted()
        )
    }
}
