package de.arbeitszeiterfassung.backend.benutzer.domain

interface IdentityProvider {

    fun legeIdentitaetAn(kennung: String, email: String): String

    fun aendereEmail(keycloakId: String, email: String)

    fun aktiviereIdentitaet(keycloakId: String)

    fun deaktiviereIdentitaet(keycloakId: String)
}
