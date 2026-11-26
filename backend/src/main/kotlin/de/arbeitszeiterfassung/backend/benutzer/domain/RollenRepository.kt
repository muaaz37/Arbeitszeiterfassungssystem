package de.arbeitszeiterfassung.backend.benutzer.domain

import java.util.UUID

interface RollenRepository {
    fun findeNachId(rolleId: UUID): Rolle?
    fun findeAlle(): Set<Rolle>
}
