package de.arbeitszeiterfassung.backend.benutzer.persistence.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "benutzer")
class BenutzerJpaEntity(
    @Id
    @Column(name = "benutzer_id")
    var id: UUID,

    @Column(name = "kennung", nullable = false, unique = true, length = 255)
    var kennung: String,

    @Column(name = "email", nullable = false, length = 255)
    var email: String,

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean,

    @Column(name = "keycloak_id", nullable = false, unique = true, length = 255)
    var keycloakId: String,

    @Column(name = "mitarbeiter_id", nullable = false, unique = true)
    var mitarbeiterId: UUID,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "benutzerrolle",
        joinColumns = [JoinColumn(name = "benutzer_id")],
        inverseJoinColumns = [JoinColumn(name = "rolle_id")]
    )
    var rollen: MutableSet<RolleJpaEntity> = mutableSetOf()
)
