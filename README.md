# Personal-, Urlaubs- und Arbeitszeitmanagementsystem

Webanwendung zur Verwaltung von Personal- und Abteilungsdaten, Urlaubsanträgen, Arbeitszeiten, Abwesenheiten, Dienstplänen, Auswertungen und Benachrichtigungen.

> **Projektstatus:** Repository-Grundkonfiguration. Es ist noch keine lauffähige Anwendung enthalten.

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

Dieses Repository enthält eine [MIT-Lizenz](LICENSE). Vor Veröffentlichung von Hochschulmaterialien, personenbezogenen Daten oder fremden Ressourcen sind deren Nutzungsrechte und die jeweiligen Hochschulvorgaben zu beachten.
