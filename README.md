# Personal-, Urlaubs- und Arbeitszeitmanagementsystem

Webanwendung zur Verwaltung von Personal- und Abteilungsdaten, Urlaubsanträgen, Arbeitszeiten, Abwesenheiten, Dienstplänen, Auswertungen und Benachrichtigungen.

> **Projektstatus:** Repository-Grundkonfiguration und lauffähiges Spring-Boot-Grundgerüst vorhanden. Frontend, Datenbankanbindung und Authentifizierung befinden sich noch in Vorbereitung.

## Vorgesehene Architektur

- **Frontend:** Angular mit TypeScript
- **Backend:** Kotlin und Spring Boot als modularer Monolith
- **API:** REST / JSON
- **Datenbank:** PostgreSQL, Spring Data JPA und versionierte Migrationen
- **Authentifizierung:** Keycloak / OpenID Connect
- **Betrieb:** Docker Compose und HTTPS-Reverse-Proxy

Die fachlichen Backend-Module sind Benutzer/Rollen, Personal/Abteilungen, Urlaub, Zeit/Pausen, Abwesenheiten, Dienstplanung, Audit, Auswertungen und Benachrichtigungen. Die Datenverantwortung bleibt je Fachmodul getrennt.

## Geplante Projektstruktur

| Verzeichnis | Inhalt |
| --- | --- |
| frontend/ | Angular-Anwendung |
| backend/ | Kotlin-/Spring-Boot-Anwendung |
| infra/ | Docker Compose, Deployment und Beispielkonfiguration |
| docs/ | Architektur, API- und Projektdokumentation |
| .github/ | Pull-Request-Vorlage, Issue-Vorlagen und später CI |

Die Verzeichnisse werden erst mit den jeweiligen ersten Implementierungsschritten angelegt.


## Lokale Entwicklungsumgebung

Voraussetzung ist Docker mit Docker Compose. Führe die Befehle im Projektverzeichnis aus.

### Infrastruktur starten

1. Kopiere `.env.example` nach `.env`.
2. Setze in `.env` die lokalen Passwörter:
  - `POSTGRES_PASSWORD` für die Anwendungsdatenbank
  - `KC_DB_PASSWORD` für die Keycloak-Datenbank
  - `KC_BOOTSTRAP_ADMIN_PASSWORD` für die Keycloak Admin Console
3. Starte die Infrastruktur:

   ```sh
   docker compose up -d --build
   ```

Beim ersten Start mit einer frischen Keycloak-Datenbank legt Keycloak das Administratorkonto automatisch an. Öffne die [Admin Console](http://localhost:8081/admin/) und melde dich mit `KC_BOOTSTRAP_ADMIN_USERNAME` (Standard `admin`) und `KC_BOOTSTRAP_ADMIN_PASSWORD` an.

Öffne für den Login-Test die [Kontoseite des Projektrealms](http://localhost:8081/realms/arbeitszeiterfassung/account/) und melde dich als `test-user` an. Das Passwort steht in der Realm-Datei und ist ausschließlich für lokale Tests vorgesehen.

`docker compose down` stoppt die Infrastruktur. Die Daten bleiben in den PostgreSQL-Volumes erhalten.

### Keycloak und Spring Security

| Einstellung | Lokaler Wert |
| --- | --- |
| Spring Security `issuer-uri` | `http://localhost:8081/realms/arbeitszeiterfassung` |
| Öffentliche Angular-Client-ID | `arbeitszeiterfassung` |
| Anmeldeverfahren | Authorization Code Flow mit PKCE `S256` |
| Redirect-URI | `http://localhost:4200/` |
| Web Origin | `http://localhost:4200` |

Sobald die Angular-Callback-Route feststeht, wird ihre vollständige URL als Redirect-URI in Angular und im Keycloak-Realm hinterlegt. Die Web Origin wird aktualisiert, wenn sich das Protokoll, der Host oder der Port des Frontends ändert.

### Administration

`admin` verwaltet Keycloak über die Admin Console. `test-app-admin` ist das lokale Testkonto für die fachliche Administration in der Anwendung. Die fachliche Rolle und den Kontostatus verwaltet später Modul A in der Anwendungsdatenbank.

## Zusammenarbeit

1. Eine Aufgabe als GitHub Issue erfassen.
2. Einen kurzlebigen Branch vom aktuellen main erstellen, z. B. feat/12-urlaubsantrag.
3. Änderungen lokal testen und mit aussagekräftigen Commits speichern.
4. Einen Pull Request nach main erstellen und die Vorlage ausfüllen.
5. Gegenseitig reviewen und nach erfolgreicher Prüfung mit **Create a merge commit** zusammenführen. So bleiben die einzelnen Commits und ihre Autoren nachvollziehbar.

Details stehen in [CONTRIBUTING.md](CONTRIBUTING.md).

## Sicherheit

Keine Passwörter, Tokens, privaten Schlüssel, echten Personaldaten oder produktiven Konfigurationswerte in Git speichern. Lokale Geheimnisse bleiben in ignorierten Umgebungsdateien; für notwendige Konfigurationsnamen werden später separate Beispiel-Dateien bereitgestellt.

## Lizenz

Dieses Repository enthält eine [MIT-Lizenz](LICENSE).
