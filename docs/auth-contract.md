# Identitäts- und Berechtigungsvertrag

Entwurf für Issue #6 zur gemeinsamen Prüfung.

## Lokale Dienstadressen

Diese Adressen gelten auf dem Entwicklungsrechner, wenn Angular und Spring Boot lokal laufen und Docker Compose Keycloak und PostgreSQL bereitstellt.

| Dienst | Adresse | Verwendung |
| --- | --- | --- |
| Angular | `http://localhost:4200/` | Einstieg in die Webanwendung im Browser |
| Spring Boot | `http://localhost:8080` | Basis-URL der REST-API |
| Keycloak | `http://localhost:8081` | Basis-URL für die Anmeldung im Projektrealm |
| PostgreSQL (Anwendungsdatenbank) | `jdbc:postgresql://localhost:5433/arbeitszeiterfassung` | JDBC-Verbindung des lokal gestarteten Backends bei Verwendung des Standard-Datenbanknamens |

Docker Compose leitet den lokalen Port `5433` an `app-db:5432` weiter. Keycloak verwendet im Compose-Netz seine eigene Datenbank unter `keycloak-db:5432`.

## Lokale OIDC-Konfiguration

| Einstellung | Vereinbarter Wert |
| --- | --- |
| Realm | `arbeitszeiterfassung` |
| Öffentliche Angular-Client-ID | `arbeitszeiterfassung` |
| Issuer | `http://localhost:8081/realms/arbeitszeiterfassung` |
| Erwartete Audience der Backend-API | `arbeitszeiterfassung-api` |

## Anmeldung

Der öffentliche Angular-Client verwendet den OpenID Connect Authorization Code Flow mit PKCE (`S256`).

| Einstellung im lokalen Keycloak-Realm | Wert |
| --- | --- |
| Erlaubte Redirect-URI nach dem Login | `http://localhost:4200/` |
| Erlaubte Web Origin für Angular | `http://localhost:4200` |

1. Angular erzeugt für den Anmeldeversuch einen zufälligen `code_verifier`. Es leitet den Browser mit der daraus abgeleiteten `code_challenge` und der vereinbarten Redirect-URI zu Keycloak.
2. Der Benutzer meldet sich bei Keycloak an. Keycloak leitet den Browser mit einem kurz gültigen Autorisierungscode an die Redirect-URI zurück.
3. Angular sendet den Code und den ursprünglichen `code_verifier` an den Token-Endpunkt von Keycloak. Keycloak vergleicht den daraus berechneten Prüfwert mit der ursprünglichen Challenge und stellt bei erfolgreicher Prüfung das Zugriffstoken aus.
4. Angular sendet das Zugriffstoken bei geschützten Aufrufen der Spring-Boot-API im Header `Authorization: Bearer <access-token>`.

## Abmeldung

Die lokale Logout-Rücksprungadresse des Angular-Clients ist `http://localhost:4200/`. Bei der Abmeldung beendet Angular die lokale Anmeldung und veranlasst die Abmeldung bei Keycloak. Nach Ende der Keycloak-Sitzung kehrt der Browser zur vereinbarten Adresse zurück.

**Abnahmekriterium (AT-B-01-4):** Ruft der Benutzer danach `/dashboard` erneut auf, fordert die Anwendung eine neue Anmeldung an.

## Identität des angemeldeten Benutzers

Angular sendet bei `GET /api/v1/auth/me` das Keycloak-Zugriffstoken als Bearer-Token. Spring Security prüft Signatur, Aussteller, Audience und zeitliche Gültigkeit. Der Endpunkt aus Issue #9 liest die Identität aus dem geprüften Token.

| JSON-Feld bei HTTP 200 | Claim im Zugriffstoken | Bedeutung |
| --- | --- | --- |
| `subject` | `sub` | Stabile Kennung des Kontos beim vereinbarten Aussteller. Modul A verwendet sie zur Zuordnung über `benutzer.keycloak_id`. |
| `username` | `preferred_username` | Aktueller Benutzername für die Anzeige in Angular. Er kann sich ändern. |

Bei fehlendem oder ungültigem Zugriffstoken antwortet das Backend mit HTTP 401. Issue #9 überprüft die vereinbarten Felder mit einem tatsächlich von Keycloak ausgestellten Zugriffstoken.

Die eigentliche Identität ergibt sich aus Aussteller und Subject. Der Benutzername dient der Anzeige. [OpenID Connect Core, Abschnitt 5.7](https://openid.net/specs/openid-connect-core-1_0.html#ClaimStability)

## Zuordnung von Identität, Benutzerkonto und Mitarbeiter

| Kennung | Zuständigkeit | Verwendung |
| --- | --- | --- |
| `sub` | Keycloak | Modul A sucht den Wert aus dem geprüften Zugriffstoken des vereinbarten Issuers in der eindeutigen Spalte `benutzer.keycloak_id`. |
| `benutzer_id` | Modul A – Benutzer und Rollen | Die interne UUID identifiziert das gefundene Anwendungskonto. Zu diesem Konto gehören Kontostatus und Rollenzuordnungen. |
| `mitarbeiter_id` | Modul B – Personal und Abteilungen | Die UUID identifiziert den Mitarbeiterdatensatz. Modul A speichert den Verweis in `benutzer.mitarbeiter_id`. |

Jedes Anwendungskonto gehört zu genau einem Mitarbeiter. Ein Mitarbeiter kann bereits vor der Kontoanlage existieren und höchstens einem Anwendungskonto zugeordnet sein. Bei einer Änderung der Benutzerkennung bleibt die Verknüpfung über `keycloak_id` erhalten.

Grundlage sind das Datenmodell und die Integritätsbedingungen in Kapitel 7 des Architekturentwurfs.

## Fachliche Anwendungsrollen

Modul A verwaltet die Rollenzuordnungen in PostgreSQL. Für die vier vordefinierten Rollen verwenden Backend und API die folgenden stabilen Bezeichner. Angular kann die fachlichen Namen als Beschriftung anzeigen.

| Fachlicher Name | Technischer Bezeichner | Zuständigkeit gemäß Architekturentwurf |
| --- | --- | --- |
| Mitarbeiter | `MITARBEITER` | Zugriff auf die für den Mitarbeiter freigegebenen eigenen Daten und Funktionen. |
| Vorgesetzter | `VORGESETZTER` | Zusätzliche Befugnisse für die zugeordneten Abteilungen. |
| Personalverwaltung | `PERSONALVERWALTUNG` | Zugriff auf die für die Personalverwaltung freigegebenen Personal- und Verwaltungsfunktionen. |
| Administration | `ADMINISTRATION` | Verwaltung von Benutzern und Rollen sowie exklusiver Lesezugriff auf Audit-Protokolle. |

Ein Benutzer kann mehrere Rollen besitzen. Die Berechtigungen ergeben sich aus den aktuell zugewiesenen Rollen. Der zuständige Fachdienst prüft zusätzlich, ob der Zugriff auf den konkreten Mitarbeiter oder die konkrete Abteilung zulässig ist.

## Eigene Berechtigungen und Kontostatus

Angular ruft `GET /api/v1/ich/berechtigungen` mit einem Bearer-Zugriffstoken auf. Der Endpunkt nimmt keine Benutzer-ID entgegen. Nach der Tokenprüfung ordnet Modul A das Subject dem Anwendungskonto zu und liest dessen aktuellen Status und Berechtigungen aus PostgreSQL. Jedes aktive Anwendungskonto darf die eigenen Berechtigungen abrufen.

| JSON-Feld bei HTTP 200 | Typ | Bedeutung |
| --- | --- | --- |
| `aktiv` | Boolean | Aktueller Kontostatus. Bei erfolgreichem Abruf ist der Wert `true`. |
| `berechtigungen` | Array von Strings | Effektive Berechtigungen aus allen zugewiesenen Rollen. Jeder Berechtigungsbezeichner kommt höchstens einmal vor. Die Reihenfolge spielt keine Rolle. |

Angular verwendet die Berechtigungen für die Sichtbarkeit von Funktionen. Das Backend prüft bei jedem fachlichen Aufruf die erforderliche Berechtigung und den Zugriff auf das konkrete Objekt erneut.

| Fall | Antwort |
| --- | --- |
| Das Zugriffstoken fehlt, ist ungültig oder abgelaufen. | `401 Unauthorized` |
| Das Token ist gültig, aber zu seinem Subject existiert kein Anwendungskonto. | `403 Forbidden` |
| Das Token ist gültig, aber das Anwendungskonto ist deaktiviert. | `403 Forbidden` |
| Ein aktiver Benutzer hat die Berechtigung für eine bestimmte Fachfunktion nicht. | Der Abruf der eigenen Berechtigungen liefert `200 OK`. Der Aufruf der geschützten Fachfunktion liefert `403 Forbidden`. |

Ein fachlicher Zustandskonflikt bei einer Änderung führt zu `409 Conflict`, etwa wenn ein bereits entschiedener Urlaubsantrag erneut genehmigt werden soll. Der lesende Berechtigungsabruf hat keinen solchen Konfliktfall.

Innerhalb des Backends stellt Modul A die Fachschnittstelle `BenutzerBerechtigungsAuskunft` bereit. Sie bietet gemäß Architekturentwurf, Abschnitt 6.1.1, die Operationen `istAktiv(benutzerId: UUID): Boolean` und `ermittleBerechtigungen(benutzerId: UUID): Set<String>`. Beide verwenden die interne Benutzer-ID. Andere Fachmodule beziehen die Auskunft über diese Schnittstelle.
