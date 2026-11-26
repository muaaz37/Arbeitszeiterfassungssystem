package de.arbeitszeiterfassung.backend.benutzer.persistence.jpa

import de.arbeitszeiterfassung.backend.benutzer.domain.Rolle
import de.arbeitszeiterfassung.backend.benutzer.domain.RollenRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional(readOnly = true)
class JpaRollenAdapter(
    private val rollenJpaRepository: SpringDataRolleJpaRepository
) : RollenRepository {

    override fun findeNachId(rolleId: UUID): Rolle? {
        return rollenJpaRepository.findById(rolleId)
            .orElse(null)
            ?.toDomain()
    }

    override fun findeAlle(): Set<Rolle> {
        return rollenJpaRepository.findAll()
            .map { it.toDomain() }
            .toSet()
    }

    private fun RolleJpaEntity.toDomain(): Rolle {
        return Rolle(
            id = id,
            bezeichnung = bezeichnung
        )
    }
}
