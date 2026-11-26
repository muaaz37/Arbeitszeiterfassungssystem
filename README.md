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

### Frontend lokal starten

Voraussetzungen sind Docker Compose, Java 21, Node.js und npm.

Richte die lokale `.env` wie im Abschnitt „Infrastruktur starten“ beschrieben ein.
Starte Keycloak im Projektverzeichnis:

```bash
docker compose up -d keycloak
```

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

Führe Tests und Build im Verzeichnis `frontend/` aus:

```bash
npm test -- --watch=false
npm run build
```


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
