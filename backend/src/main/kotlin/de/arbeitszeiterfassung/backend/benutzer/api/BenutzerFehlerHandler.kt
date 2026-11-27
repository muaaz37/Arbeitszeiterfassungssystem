package de.arbeitszeiterfassung.backend.benutzer.api

import de.arbeitszeiterfassung.backend.benutzer.application.AnlageAbgleichErforderlich
import de.arbeitszeiterfassung.backend.benutzer.application.MitarbeiterBereitsZugeordnet
import de.arbeitszeiterfassung.backend.benutzer.application.MitarbeiterUnbekannt
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerkennungBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.EmailBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.Identitaetskonflikt
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentityProviderFehler
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class BenutzerFehlerResponse(val code: String, val message: String, val status: Int)

@RestControllerAdvice(assignableTypes = [BenutzerController::class])
class BenutzerFehlerHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun ungueltigeAnfrage() = antwort(HttpStatus.BAD_REQUEST, "EINGABE_UNGUELTIG", "Bitte prüfe die übermittelten Benutzerdaten.")

    @ExceptionHandler(MitarbeiterUnbekannt::class)
    fun unbekannt(e: MitarbeiterUnbekannt) = antwort(HttpStatus.BAD_REQUEST, "MITARBEITER_UNBEKANNT", e.message!!)

    @ExceptionHandler(MitarbeiterBereitsZugeordnet::class)
    fun zugeordnet(e: MitarbeiterBereitsZugeordnet) = antwort(HttpStatus.CONFLICT, "MITARBEITER_ZUGEORDNET", e.message!!)

    @ExceptionHandler(IdentityProviderFehler::class)
    fun identitaetsfehler(e: IdentityProviderFehler) = when (e) {
        is BenutzerkennungBereitsVergeben -> antwort(HttpStatus.CONFLICT, "KENNUNG_VERGEBEN", e.message!!)
        is EmailBereitsVergeben -> antwort(HttpStatus.CONFLICT, "EMAIL_VERGEBEN", e.message!!)
        is Identitaetskonflikt -> antwort(HttpStatus.CONFLICT, "IDENTITAETSKONFLIKT", e.message!!)
        else -> antwort(HttpStatus.BAD_GATEWAY, "IDENTITAETSANBIETER_FEHLER", "Keycloak hat die Anlage nicht bestätigt.")
    }

    @ExceptionHandler(AnlageAbgleichErforderlich::class)
    fun abgleich(e: AnlageAbgleichErforderlich) = antwort(HttpStatus.INTERNAL_SERVER_ERROR, "ABGLEICH_ERFORDERLICH", e.message!!)

    @ExceptionHandler(DataAccessException::class)
    fun datenbankfehler() = antwort(HttpStatus.INTERNAL_SERVER_ERROR, "DATENBANKFEHLER", "Die Datenbankoperation ist fehlgeschlagen.")

    private fun antwort(status: HttpStatus, code: String, message: String) =
        ResponseEntity.status(status).body(BenutzerFehlerResponse(code, message, status.value()))
}
