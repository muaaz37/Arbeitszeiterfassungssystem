package de.arbeitszeiterfassung.backend.benutzer.domain

open class IdentityProviderFehler(meldung: String) : RuntimeException(meldung)

class BenutzerkennungBereitsVergeben :
    IdentityProviderFehler("Die Benutzerkennung ist bereits vergeben.")

class EmailBereitsVergeben :
    IdentityProviderFehler("Die E-Mail-Adresse ist bereits vergeben.")

class Identitaetskonflikt :
    IdentityProviderFehler("Die Benutzerkennung oder E-Mail-Adresse ist bereits vergeben.")

class TechnischerZugriffAbgelehnt :
    IdentityProviderFehler("Der Identitätsanbieter hat den technischen Zugriff abgelehnt.")

class IdentitaetsanbieterNichtErreichbar :
    IdentityProviderFehler("Der Identitätsanbieter ist nicht erreichbar.")

class IdentitaetNichtGefunden :
    IdentityProviderFehler("Die Identität wurde nicht gefunden.")
