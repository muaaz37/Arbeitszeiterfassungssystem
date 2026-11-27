package de.arbeitszeiterfassung.backend.benutzer.persistence.jpa

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataBenutzerJpaRepository : JpaRepository<BenutzerJpaEntity, UUID> {

    fun existsByKennung(kennung: String): Boolean
    fun existsByMitarbeiterId(mitarbeiterId: UUID): Boolean

    @EntityGraph(attributePaths = ["rollen"])
    fun findAllByOrderByKennungAsc(): List<BenutzerJpaEntity>

    fun findByKeycloakId(keycloakId: String): BenutzerJpaEntity?
}
