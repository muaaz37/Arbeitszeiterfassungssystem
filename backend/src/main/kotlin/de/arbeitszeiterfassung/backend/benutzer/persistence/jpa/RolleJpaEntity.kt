package de.arbeitszeiterfassung.backend.benutzer.persistence.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "rolle")
class RolleJpaEntity(
    @Id
    @Column(name = "rolle_id")
    var id: UUID,

    @Column(
        name = "bezeichnung",
        nullable = false,
        unique = true,
        length = 255
    )
    var bezeichnung: String
)
