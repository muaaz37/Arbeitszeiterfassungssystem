# Personal-, Urlaubs- und Arbeitszeitmanagementsystem

Webanwendung zur Verwaltung von Personal- und Abteilungsdaten, Urlaubsanträgen, Arbeitszeiten, Abwesenheiten, Dienstplänen, Auswertungen und Benachrichtigungen.

> **Projektstatus:** Angular-Grundgerüst mit Keycloak-Login, JWT-geschützte Spring-Boot-API sowie Benutzer- und Rollenpersistenz in PostgreSQL mit Flyway-Migrationen sind vorhanden. Die fachliche Kontostatus- und Berechtigungsprüfung sowie die administrative Benutzerverwaltung sind noch offen.

## Vorgesehene Architektur

- **Frontend:** Angular mit TypeScript
- **Backend:** Kotlin und Spring Boot als modularer Monolith
- **API:** REST / JSON
- **Datenbank:** PostgreSQL, Spring Data JPA und versionierte Migrationen
- **Authentifizierung:** Keycloak / OpenID Connect
- **Betrieb:** Docker Compose und HTTPS-Reverse-Proxy

Die fachlichen Backend-Module sind Benutzer/Rollen, Personal/Abteilungen, Urlaub, Zeit/Pausen, Abwesenheiten, Dienstplanung, Audit, Auswertungen und Benachrichtigungen. Die Datenverantwortung bleibt je Fachmodul getrennt.

## Projektstruktur

| Verzeichnis | Inhalt |
| --- | --- |
| frontend/ | Angular-Anwendung |
| backend/ | Kotlin-/Spring-Boot-Anwendung |
| infra/ (geplant) | Künftige Bündelung der Infrastruktur- und Deployment-Dateien |
| docs/ | Architektur, API- und Projektdokumentation |
| .github/ | Pull-Request-Vorlage, Issue-Vorlagen und später CI |

Docker Compose und die Beispielkonfiguration liegen derzeit als `docker-compose.yml` und `.env.example` im Projektverzeichnis. Die Keycloak-Konfiguration liegt unter `keycloak/`. Ein separates `infra/`-Verzeichnis ist vorgesehen.


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

### Frontend lokal starten

Voraussetzungen sind Docker Compose, Java 21 und die folgenden Frontend-Werkzeuge:

| Werkzeug | Version / unterstützter Bereich |
| --- | --- |
| Angular | 21.2.x; aktuell 21.2.26 im Lockfile, CLI und Build-Werkzeuge 21.2.31 |
| Node.js | 24.x (`>=24.0.0 <25.0.0`); lokal mit 24.13.1 geprüft |
| npm | 11.x; `packageManager` legt 11.11.0 fest, lokal zusätzlich mit 11.8.0 geprüft |

Der dokumentierte Node.js-Bereich erfüllt die Versionsanforderungen der eingecheckten Angular- und Testabhängigkeiten. Die konkreten Paketstände sind in `frontend/package-lock.json` versioniert; `npm ci` installiert diese Stände. Nach Aktualisierungen des Lockfiles müssen die Versionsanforderungen erneut geprüft werden.

Richte die lokale `.env` wie im Abschnitt „Infrastruktur starten“ beschrieben ein.
Starte Keycloak und die Anwendungsdatenbank im Projektverzeichnis:

```bash
docker compose up -d keycloak app-db
```

Setze für den Backend-Prozess die Datenbank-Umgebungsvariablen aus dem Abschnitt „Datenbankverbindung und lokaler Start“, insbesondere `POSTGRES_PASSWORD`, beispielsweise in der IntelliJ-Startkonfiguration. Docker Compose liest die lokale `.env` automatisch; das lokal gestartete Backend übernimmt sie nicht automatisch.

Starte das Backend in einem zweiten Terminal im Projektverzeichnis:

```bash
cd backend
./gradlew bootRun
```

Starte Angular in einem weiteren Terminal im Projektverzeichnis:

```bash
cd frontend
npm ci
npm start
```

Öffne [das Dashboard](http://localhost:4200/dashboard). Ohne Anmeldung leitet Angular zu Keycloak weiter. Nach erfolgreicher Anmeldung zeigt das Dashboard die Benutzerkennung an.

| Einstellung | Lokaler Wert | Bedeutung |
| --- | --- | --- |
| Angular | `http://localhost:4200` | Adresse des Frontends im Browser. |
| Keycloak-URL | `http://localhost:8081` | Basisadresse des Keycloak-Servers, der die Anmeldung durchführt. |
| Realm und Public Client-ID | jeweils `arbeitszeiterfassung` | Der Realm enthält die Benutzer und Anmeldeeinstellungen. Die Client-ID identifiziert das Angular-Frontend bei Keycloak. |
| Login-Rücksprungadresse | `http://localhost:4200/` | Adresse, zu der Keycloak den Browser nach erfolgreicher Anmeldung zurückleitet. |
| Anmeldeverfahren | Authorization Code Flow mit PKCE `S256` | Angular tauscht den Autorisierungscode gegen ein Zugriffstoken. PKCE bindet den Code an den Client, der die Anmeldung gestartet hat. |
| Erwartete Backend-Audience | `arbeitszeiterfassung-api` | Wert, den das Backend im `aud`-Claim des Zugriffstokens erwartet. |
| Lokaler API-Proxy | `/api/**` → `http://localhost:8080` | Der Angular-Entwicklungsserver leitet API-Anfragen an das lokale Spring-Boot-Backend weiter. |

Melde dich für die Integrationsprüfung als `test-user` an und prüfe im Netzwerk-Tab des Browsers, ob `GET /api/v1/auth/me` den Status `200 OK` liefert. Die Antwort enthält `username: "test-user"` und die Keycloak-Benutzer-ID als `subject`.

Angular verwendet `/api` als zentrale Backend-Adresse. Der HTTP-Interceptor ergänzt das Bearer-Token nur bei Aufrufen der eigenen API. Keycloak prüft vor dem Aufruf die Gültigkeit des Tokens und erneuert es bei Bedarf.

Bei HTTP 401 bietet das Dashboard eine erneute Anmeldung an. Bei HTTP 403 zeigt es eine Meldung zur fehlenden Berechtigung. Bei einem Netzwerkfehler meldet es, dass die Backend-API momentan nicht erreichbar ist. Fehlgeschlagene API-Aufrufe werden nicht automatisch wiederholt.


Führe Tests und Build im Verzeichnis `frontend/` aus:

```bash
npm test -- --watch=false
npm run build
```

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

### Keycloak-Identity-Adapter

Der Adapter legt Benutzeridentitäten im Keycloak-Realm `arbeitszeiterfassung` an. Er kann ihre E-Mail-Adresse ändern, sie aktivieren oder deaktivieren und ihren aktuellen Zustand anhand der Keycloak-ID abfragen. Wenn die Benutzeranlage fehlschlägt, kann der Adapter das bereits erstellte Keycloak-Konto löschen.

Das Backend verwendet dafür den vertraulichen OpenID-Connect-Client `arbeitszeiterfassung-identity` mit aktiviertem Service Account und dem Client Credentials Flow. Der Service Account erhält im Realm die Admin-Berechtigungen **Users → manage** für die Benutzerverwaltung und **Users → view** für die Zustandsabfrage. Dem Client wird `kc-admin-api` als Default Client Scope zugewiesen.

| Umgebungsvariable | Lokaler Standardwert | Bedeutung |
| --- | --- | --- |
| `KEYCLOAK_IDENTITY_BASE_URL` | `http://localhost:8081` | Basisadresse von Keycloak |
| `KEYCLOAK_IDENTITY_REALM` | `arbeitszeiterfassung` | Realm für die Identitätsverwaltung |
| `KEYCLOAK_IDENTITY_CLIENT_ID` | `arbeitszeiterfassung-identity` | ID des technischen Clients |
| `KEYCLOAK_IDENTITY_CLIENT_SECRET` | keiner | Secret für die Authentifizierung des technischen Clients |

Das Client-Secret wird dem Backend als lokale Umgebungsvariable übergeben, zum Beispiel über die IntelliJ-Startkonfiguration.

#### Persönliche Zugangsdaten einrichten

Neue Identitäten werden aktiv angelegt. Ein Administrator öffnet das Konto in der Keycloak Admin Console unter **Users → Credentials** und setzt ein temporäres Passwort. Bei der ersten Anmeldung fordert Keycloak den Benutzer auf, ein persönliches Passwort zu setzen. Persönliche Zugangsdaten werden ausschließlich in Keycloak verwaltet.

Nach der Deaktivierung verweigert Keycloak neue Anmeldungen. Nach der Reaktivierung kann sich der Benutzer mit seinen vorhandenen Zugangsdaten wieder anmelden. Die Keycloak-ID bleibt bei beiden Vorgängen erhalten.

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
