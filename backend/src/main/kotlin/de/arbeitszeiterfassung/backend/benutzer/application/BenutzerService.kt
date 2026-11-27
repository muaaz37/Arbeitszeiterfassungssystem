package de.arbeitszeiterfassung.backend.benutzer.application

import de.arbeitszeiterfassung.backend.benutzer.domain.Benutzer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerkennungBereitsVergeben
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentitaetNichtGefunden
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentityProvider
import de.arbeitszeiterfassung.backend.benutzer.domain.IdentityProviderFehler
import de.arbeitszeiterfassung.backend.benutzer.domain.RollenRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.util.UUID

@ConfigurationProperties("benutzer.entwicklung")
data class Entwicklungsmitarbeiter(val mitarbeiterIds: Set<UUID> = emptySet())

class MitarbeiterUnbekannt : RuntimeException("Der Mitarbeiter ist für die Anlage nicht freigegeben.")
class MitarbeiterBereitsZugeordnet : RuntimeException("Für den Mitarbeiter besteht bereits ein Konto.")
class AnlageAbgleichErforderlich : RuntimeException("Der Zustand der Benutzeranlage muss geprüft werden.")

@Service
@EnableConfigurationProperties(Entwicklungsmitarbeiter::class)
class BenutzerService(
    private val benutzerRepository: BenutzerRepository,
    private val rollenRepository: RollenRepository,
    private val identitaeten: IdentityProvider,
    private val entwicklungsmitarbeiter: Entwicklungsmitarbeiter
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun listeBenutzer(): List<Benutzer> = benutzerRepository.findeAlle()

    fun legeAn(kennung: String, email: String, mitarbeiterId: UUID): UUID {
        pruefeZuordnung(kennung, mitarbeiterId)
        if (mitarbeiterId !in entwicklungsmitarbeiter.mitarbeiterIds) throw MitarbeiterUnbekannt()

        val rolle = rollenRepository.findeAlle().single { it.bezeichnung == "MITARBEITER" }
        val id = UUID.randomUUID()
        val keycloakId = identitaeten.legeIdentitaetAn(kennung, email)
        val konto = Benutzer(id, kennung, email, aktiv = true, keycloakId, mitarbeiterId, setOf(rolle))

        // Der Repository-Aufruf umfasst auch den Commit der lokalen Transaktion.
        try {
            benutzerRepository.speichere(konto)
        } catch (fehler: DataIntegrityViolationException) {
            try {
                identitaeten.loescheIdentitaet(keycloakId)
            } catch (_: IdentitaetNichtGefunden) {
                // Die anzulegende Identität fehlt bereits.
            } catch (_: IdentityProviderFehler) {
                abgleichErforderlich(id, keycloakId)
            }
            pruefeZuordnung(kennung, mitarbeiterId)
            throw fehler
        } catch (_: RuntimeException) {
            // Bei unklarem Commit könnte PostgreSQL bereits auf diese Identität verweisen.
            abgleichErforderlich(id, keycloakId)
        }
        return id
    }

    private fun pruefeZuordnung(kennung: String, mitarbeiterId: UUID) {
        if (benutzerRepository.existiertKennung(kennung)) throw BenutzerkennungBereitsVergeben()
        if (benutzerRepository.existiertMitarbeiterzuordnung(mitarbeiterId)) throw MitarbeiterBereitsZugeordnet()
    }

    private fun abgleichErforderlich(id: UUID, keycloakId: String): Nothing {
        log.error("Benutzeranlage abgleichen: benutzerId={}, keycloakId={}", id, keycloakId)
        throw AnlageAbgleichErforderlich()
    }
}
