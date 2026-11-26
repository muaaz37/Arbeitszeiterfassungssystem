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
| Spring Security `issuer-uri` (Umgebungsvariable `KEYCLOAK_ISSUER_URI`) | `http://localhost:8081/realms/arbeitszeiterfassung` |
| Erwartete API-Audience (Umgebungsvariable `KEYCLOAK_AUDIENCE`) | `arbeitszeiterfassung-api` |
| Öffentliche Angular-Client-ID | `arbeitszeiterfassung` |
| Anmeldeverfahren | Authorization Code Flow mit PKCE `S256` |
| Redirect-URI | `http://localhost:4200/` |
| Web Origin | `http://localhost:4200` |

Der Realm-Import setzt für Zugriffstoken des Angular-Clients die Audience `arbeitszeiterfassung-api`. Bestehende Realms werden beim Start übersprungen. Für eine bereits eingerichtete lokale Umgebung im dedizierten Client Scope des Clients `arbeitszeiterfassung` einen Mapper vom Typ `Audience` mit der Custom Audience `arbeitszeiterfassung-api` anlegen und die Ausgabe im Access Token aktivieren. Danach ein neues Zugriffstoken beziehen.

Sobald die Angular-Callback-Route feststeht, wird ihre vollständige URL als Redirect-URI in Angular und im Keycloak-Realm hinterlegt. Die Web Origin wird aktualisiert, wenn sich das Protokoll, der Host oder der Port des Frontends ändert.

### Administration

`admin` verwaltet Keycloak über die Admin Console. `test-app-admin` ist das lokale Testkonto für die fachliche Administration in der Anwendung. Die fachliche Rolle und den Kontostatus verwaltet später Modul A in der Anwendungsdatenbank.

## Benutzer- und Rollenpersistenz

Modul A speichert Anwendungskonten, Kontostatus und Rollenzuordnungen in PostgreSQL. Die fachlichen Repository-Interfaces liegen unter `benutzer.domain`. Die JPA-Adapter und Spring-Data-Repositories liegen unter `benutzer.persistence.jpa`. Andere Fachmodule beziehen Auskünfte über die Fachschnittstellen von Modul A.

### Datenbankverbindung und lokaler Start

Die folgenden Umgebungsvariablen konfigurieren das lokal gestartete Backend.

| Variable | Standardwert | Verwendung |
| --- | --- | --- |
| `APP_DB_HOST` | `localhost` | Host der Anwendungsdatenbank |
| `APP_DB_PORT` | `5433` | Veröffentlichter PostgreSQL-Port auf dem Entwicklungsrechner |
| `APP_DB_NAME` | `arbeitszeiterfassung` | Datenbankname |
| `APP_DB_USER` | `arbeitszeiterfassung` | Datenbankbenutzer |
| `POSTGRES_PASSWORD` | Erforderlicher externer Wert | Passwort des Datenbankbenutzers |

Nach der Einrichtung der lokalen `.env` gemäß dem Abschnitt zur Infrastruktur startet dieser Befehl die Anwendungsdatenbank im Projektverzeichnis.

```sh
docker compose up -d app-db
```

Docker Compose liest die `.env` im Projektverzeichnis. Für den lokal gestarteten Backend-Prozess müssen die Variablen ebenfalls in dessen Umgebung gesetzt sein, beispielsweise in der IntelliJ-Startkonfiguration. Benutzername, Datenbankname und Passwort müssen mit der eingerichteten Datenbank übereinstimmen.

Das Backend wird anschließend im Verzeichnis `backend` gestartet.

```sh
./gradlew bootRun
```

Der Port `5433` wird zum PostgreSQL-Port `5432` im Container weitergeleitet. Der Compose-Dienst `app-db` speichert die Daten im Volume `app-db-data`. Die Variablen zur initialen Datenbankanlage werden beim ersten Start mit einem leeren Volume verwendet.

### Versionierte Migrationen

Flyway führt beim Backend-Start die noch ausstehenden SQL-Dateien unter `backend/src/main/resources/db/migration/` aus.

| Migration | Änderung |
| --- | --- |
| `V1__benutzer_anlegen.sql` | Tabelle `benutzer` mit eindeutiger Kennung, Keycloak-ID und Mitarbeiterzuordnung |
| `V2__rollen_anlegen.sql` | Tabellen `rolle` und `benutzerrolle` mit zusammengesetztem Primärschlüssel und Fremdschlüsseln |
| `V3__rollen_initialisieren.sql` | Initialisierung von `MITARBEITER`, `VORGESETZTER`, `PERSONALVERWALTUNG` und `ADMINISTRATION` |

Hibernate prüft mit `spring.jpa.hibernate.ddl-auto=validate`, ob das Schema zu den JPA-Entities passt. Schemaänderungen erfolgen durch weitere Flyway-Migrationen. Bereits ausgeführte versionierte Migrationen werden beibehalten.

### Rollen und fachliche Berechtigungen

Die Tabellen `rolle` und `benutzerrolle` speichern die vier Anwendungsrollen und ihre Zuordnung zu Benutzern.

Für die spätere Berechtigungsprüfung wird eine zentrale Zuordnung der vier vordefinierten Rollen zu fachlichen Berechtigungen im Code von Modul A vorgesehen. Die effektiven Berechtigungen eines Benutzers ergeben sich aus der Vereinigung der Berechtigungen seiner zugewiesenen Rollen.

Die Berechtigungsprüfung verwendet den aktuellen Kontostatus und die Rollenzuordnungen aus PostgreSQL. Der zuständige Fachdienst prüft zusätzlich den Zugriff auf konkrete Mitarbeiter oder Abteilungen.

Issue #10 stellt die Persistenz bereit. Die Ableitung und Durchsetzung der Berechtigungen wird im folgenden Berechtigungsarbeitspaket umgesetzt. Die reguläre Kontoanlage mit der Anfangsrolle Mitarbeiter und die Initialisierung des ersten Anwendungskontos mit der Rolle Administration erfolgen in der weiteren Benutzerverwaltung mit zugehöriger Keycloak-Identität und Mitarbeiterzuordnung.

### Abhängigkeit zu Modul B

`benutzer.mitarbeiter_id` ist verpflichtend und eindeutig. Sobald Modul B die Tabelle `mitarbeiter` angelegt hat, ergänzt eine neue Flyway-Migration den Fremdschlüssel auf `mitarbeiter(mitarbeiter_id)`.

Vor dieser Migration müssen alle vorhandenen Mitarbeiter-IDs auf existierende Mitarbeiter verweisen. Eventuell vorhandene ungültige Zuordnungen müssen vorher geklärt werden. Die Migration wird nach der Tabellenerstellung von Modul B eingeordnet und erhält eine freie Versionsnummer in der gemeinsamen Migrationsfolge.

Die UUIDs in den Persistenztests dienen als Testdaten für diese Zuordnung. Die vollständige referenzielle Integrität wird mit der beschriebenen späteren Migration hergestellt.

### Automatisierte Tests

Voraussetzung ist eine laufende Docker-Umgebung. Im Verzeichnis `backend` führt dieser Befehl Build und Tests aus.

```sh
./gradlew clean build
```

Die Anwendungstests und `BenutzerPersistenzTests` verwenden PostgreSQL über Testcontainers. Spring übernimmt die Verbindungsdaten über `@ServiceConnection`. Flyway führt die Migrationen auf den frischen Testdatenbanken aus.

Die Persistenztests prüfen die initialen Rollen, das Speichern und erneute Laden eines Benutzers mit mehreren Rollen, eindeutige Kennungen und Zuordnungen sowie den deaktivierten Kontostatus. Weitere Fälle prüfen unbekannte Kennungen und die Abweisung einer unbekannten Rolle vor der Speicherung.

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
