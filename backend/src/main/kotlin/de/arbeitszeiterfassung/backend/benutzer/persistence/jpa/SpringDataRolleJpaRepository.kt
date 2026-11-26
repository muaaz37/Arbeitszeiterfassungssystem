package de.arbeitszeiterfassung.backend.benutzer.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataRolleJpaRepository : JpaRepository<RolleJpaEntity, UUID>
