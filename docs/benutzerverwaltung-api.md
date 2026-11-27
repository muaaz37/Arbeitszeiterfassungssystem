# Administrative Benutzerverwaltung

Vertragsentwurf für die gemeinsame Abstimmung von Backend (#17) und Angular (#21). Alle Aufrufe erfordern ein gültiges Zugriffstoken, ein aktives Anwendungskonto und die PostgreSQL-Berechtigung `benutzer.verwalten`. Die Pfadvariable `{id}` bezeichnet die interne UUID des Anwendungskontos.

| Methode und Pfad | JSON-Anfrage | Erfolg |
| --- | --- | --- |
| `GET /api/v1/benutzer` | – | `200 OK` mit einem Array. Jeder Eintrag enthält `id`, `kennung`, `email`, `aktiv`, `mitarbeiterId` und `rollen` als Array von Rollenbezeichnern. Ein leeres Ergebnis ist `[]`. |
| `POST /api/v1/benutzer` | `kennung`, `email` und `mitarbeiterId` (UUID), alle erforderlich | `201 Created` mit `{"id":"<UUID>"}`. Das neue Konto erhält die Rolle `MITARBEITER`. |
| `PATCH /api/v1/benutzer/{id}` | `{"email":"neu@example.test"}` | `204 No Content` |
| `PATCH /api/v1/benutzer/{id}/status` | `{"aktiv":false}` oder `{"aktiv":true}` | `204 No Content` |

Die Benutzeranlage verwendet vor der Integration von Modul B ausschließlich vereinbarte Entwicklungsmitarbeiter. Persönliche Zugangsdaten werden über Keycloak eingerichtet.

| HTTP-Status | Fall |
| --- | --- |
| 400 | Ein Pflichtfeld fehlt oder ein Feldwert ist ungültig. |
| 401 | Das Zugriffstoken fehlt, ist ungültig oder abgelaufen. |
| 403 | Das Anwendungskonto fehlt, ist deaktiviert oder besitzt die Verwaltungsberechtigung nicht. |
| 404 | Beim Bearbeiten oder Ändern des Status existiert die angegebene Benutzer-ID nicht. |
| 409 | Die Benutzerkennung oder Mitarbeiterzuordnung ist bereits vergeben, oder die beantragte Deaktivierung betrifft ein Administratorkonto. Die Sperre für Administratorkonten gilt bis zur Umsetzung von US-03. |

Fachliche Fehler enthalten `code` (maschinenlesbare Kennung), `message` (Meldung für die Oberfläche) und `status` (numerischer HTTP-Status). Vor der Frontend-Anbindung sind die konkreten Fehlercodes sowie die Feldnamen `rollen` und `id` in der Anlageantwort mit dem Frontend abzustimmen.

## Benutzeranlage lokal prüfen

Für den Backend-Start das Spring-Profil `lokal` aktivieren und den technischen Keycloak-Client wie im README einrichten. Die freien Testmitarbeiter für die Anlage sind `6ed50520-94db-418c-b26e-6a6323025095` und `ffc537c6-4571-4760-b68c-04ea459278e4`. Die Standardkonfiguration gibt keine Mitarbeiter-IDs frei. Die Rechte des aufrufenden Administrators stammen aus PostgreSQL.

Beispiel für den JSON-Body von `POST /api/v1/benutzer`:

```json
{
  "kennung": "beispiel",
  "email": "beispiel@example.test",
  "mitarbeiterId": "6ed50520-94db-418c-b26e-6a6323025095"
}
```

Mit der ID aus der Antwort `201 Created` lässt sich das neue aktive Konto in der Benutzerliste wiederfinden. Es hat die Anfangsrolle `MITARBEITER`. Das persönliche Passwort wird anschließend in Keycloak eingerichtet.

| Fehler bei der Anlage | HTTP | Code |
| --- | --- | --- |
| Fehlerhafter JSON-Body, ungültige UUID oder ungültige Eingabefelder | 400 | `EINGABE_UNGUELTIG` |
| Mitarbeiter-ID ist für die Entwicklung nicht freigegeben | 400 | `MITARBEITER_UNBEKANNT` |
| Kennung, E-Mail oder Mitarbeiterzuordnung bereits belegt | 409 | `KENNUNG_VERGEBEN`, `EMAIL_VERGEBEN` oder `MITARBEITER_ZUGEORDNET` |
| Keycloak meldet einen nicht näher bestimmten Konflikt | 409 | `IDENTITAETSKONFLIKT` |
| Keycloak bestätigt die Anlage nicht | 502 | `IDENTITAETSANBIETER_FEHLER` |
| Datenbankoperation fehlgeschlagen | 500 | `DATENBANKFEHLER` |
| Unklarer Datenbank-Commit oder fehlgeschlagene Kompensation | 500 | `ABGLEICH_ERFORDERLICH` |

### Abgleich nach einem Teilfehler

Die Datenbank erzwingt eindeutige Kennungen und Mitarbeiterzuordnungen auch bei gleichzeitigen Anfragen. Scheitert die Speicherung an einer Integritätsbedingung, löscht der Service die gerade von Keycloak zurückgegebene Identität. Bei unklarem Commit-Ergebnis erhält er die Identität für den Abgleich. Das Backend protokolliert dann die interne Benutzer-ID und die Keycloak-ID. Vor einem erneuten Versuch beide IDs in PostgreSQL und Keycloak vergleichen. Bei übereinstimmender Zuordnung das vorhandene Konto prüfen. Eine verwaiste Identität erst nach bestätigtem Fehlen der Zuordnung bereinigen.

Bei einem Timeout während der Identitätsanlage kann Keycloak den Benutzer bereits angelegt haben. Der Service wiederholt den Aufruf nicht. Vor einem neuen Versuch die angefragte Kennung in Keycloak prüfen und eine vorhandene Keycloak-ID mit PostgreSQL abgleichen.

Im lokalen Integrationstest mit isoliertem Keycloak 26.8.2 und PostgreSQL 16 lieferte die Anlage `201`. Die gespeicherte Keycloak-ID stimmte mit der neu angelegten Identität überein. Die Benutzerliste enthielt das aktive Konto mit der angeforderten Mitarbeiter-ID und ausschließlich der Rolle `MITARBEITER`. Der wiederholte POST lieferte `409 KENNUNG_VERGEBEN`.
