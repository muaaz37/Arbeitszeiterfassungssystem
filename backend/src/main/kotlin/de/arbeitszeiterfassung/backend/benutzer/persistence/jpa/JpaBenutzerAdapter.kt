package de.arbeitszeiterfassung.backend.benutzer.persistence.jpa

import de.arbeitszeiterfassung.backend.benutzer.domain.Benutzer
import de.arbeitszeiterfassung.backend.benutzer.domain.BenutzerRepository
import de.arbeitszeiterfassung.backend.benutzer.domain.Rolle
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class JpaBenutzerAdapter(
    private val benutzerJpaRepository: SpringDataBenutzerJpaRepository,
    private val rolleJpaRepository: SpringDataRolleJpaRepository
) : BenutzerRepository {

    @Transactional
    override fun speichere(benutzer: Benutzer): Benutzer {
        val rollenIds = benutzer.rollen.map { it.id }.toSet()
        val rollen = rolleJpaRepository.findAllById(rollenIds)

        require(rollen.size == rollenIds.size) {
            "Mindestens eine zugeordnete Rolle existiert nicht"
        }

        val entity = BenutzerJpaEntity(
            id = benutzer.id,
            kennung = benutzer.kennung,
            email = benutzer.email,
            aktiv = benutzer.aktiv,
            keycloakId = benutzer.keycloakId,
            mitarbeiterId = benutzer.mitarbeiterId,
            rollen = rollen.toMutableSet()
        )

        return benutzerJpaRepository.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findeNachId(benutzerId: UUID): Benutzer? {
        return benutzerJpaRepository.findById(benutzerId)
            .orElse(null)
            ?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findeNachKeycloakId(keycloakId: String): Benutzer? {
        return benutzerJpaRepository.findByKeycloakId(keycloakId)
            ?.toDomain()
    }

    private fun BenutzerJpaEntity.toDomain(): Benutzer {
        return Benutzer(
            id = id,
            kennung = kennung,
            email = email,
            aktiv = aktiv,
            keycloakId = keycloakId,
            mitarbeiterId = mitarbeiterId,
            rollen = rollen.map { rolle ->
                Rolle(
                    id = rolle.id,
                    bezeichnung = rolle.bezeichnung
                )
            }.toSet()
        )
    }
}
