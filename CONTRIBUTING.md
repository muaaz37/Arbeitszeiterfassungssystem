# Entwicklungsrichtlinien

Die folgenden Regeln gelten für die gemeinsame Entwicklung des Personal-, Urlaubs- und Arbeitszeitmanagementsystems.

## 1. Aufgabenplanung

- Jedes Arbeitspaket wird vor Entwicklungsbeginn als GitHub Issue angelegt und einer verantwortlichen Person zugewiesen.
- Jedes Issue enthält ein konkretes Ziel und überprüfbare Akzeptanzkriterien. Bei fachlichen Funktionen werden die zugehörigen LF-/LQ-Anforderungen aus dem Pflichtenheft angegeben.
- Umfangreiche Funktionen werden in eigenständig prüfbare Arbeitspakete aufgeteilt. Ein Pull Request behandelt ein abgegrenztes Arbeitspaket.

## 2. Branches und Integration

- `main` ist der geschützte Integrationsbranch. Direkte Änderungen an `main` sind ausgeschlossen.
- Für jedes Arbeitspaket wird vom aktuellen `main` ein eigener Branch erstellt.
- Die Branch-Namen folgen dem Schema `feat/<issue>-<thema>`, `fix/<issue>-<thema>`, `docs/<issue>-<thema>` oder `chore/<issue>-<thema>`.
- Innerhalb eines Arbeitsbranches können mehrere Commits und Pushes erfolgen. Nach dem Merge beginnt das nächste Arbeitspaket auf einem neuen Branch vom aktuellen `main`.
- Die Integration in `main` erfolgt ausschließlich über einen genehmigten Pull Request und **Create a merge commit**.

## 3. Commits

- Jeder Commit ist anhand seiner Git-Autorenidentität dem tatsächlichen Verfasser zuordenbar.
- Commits enthalten zusammengehörige Änderungen. Die Nachricht verwendet `typ(bereich): beschreibung` mit einem der Typen `feat`, `fix`, `test`, `docs`, `refactor`, `chore` oder `ci`.
- Beispiele: `feat(urlaub): antragstellung implementieren`, `fix(zeit): pausenberechnung korrigieren`, `test(auth): rollenprüfung ergänzen`.

## 4. Pull Requests und Reviews

- Jeder Pull Request benennt das zugehörige Issue, beschreibt die Änderungen und dokumentiert die durchgeführten Prüfungen.
- Vor der Review-Anfrage prüft der Autor den vollständigen Diff auf unbeabsichtigte Änderungen und vertrauliche Inhalte.
- Die Freigabe erfolgt durch das jeweils andere Teammitglied. Ein Pull Request wird erst nach **mindestens einer Genehmigung** und der Klärung sämtlicher Review-Diskussionen gemergt.
- Neue Änderungen nach einer Freigabe erfordern gemäß Branch-Schutz eine erneute Prüfung.
- Der Reviewer prüft fachliches Verhalten, Sicherheit, Architekturgrenzen, Tests und mögliche Auswirkungen auf bestehende Funktionen.
- Nach erfolgreichem Merge wird der zugehörige Remote-Branch gelöscht.

## 5. Abschlusskriterien

Ein Arbeitspaket gilt als abgeschlossen, wenn alle folgenden Bedingungen erfüllt sind:

1. Die Akzeptanzkriterien des Issues sind umgesetzt.
2. Änderungen am Anwendungscode lassen sich lokal bauen beziehungsweise starten und sind durch geeignete Tests überprüft. Die Testergebnisse stehen im Pull Request.
3. Neue oder geänderte Fachregeln werden durch automatisierte Tests abgedeckt. Für reine Dokumentations- und Konfigurationsänderungen wird die betroffene Funktion beziehungsweise Darstellung geprüft.
4. Änderungen an REST-Verträgen, Datenbankschemata oder Betriebsparametern sind in den zugehörigen Artefakten dokumentiert; Schemaänderungen erfolgen über versionierte Migrationen.
5. Es sind keine Zugangsdaten, Tokens, privaten Schlüssel, echten Personaldaten oder generierten Build-Dateien eingecheckt.
6. Alle im Repository verpflichtenden CI-Prüfungen sind erfolgreich abgeschlossen.
7. Die erforderliche Code-Review-Freigabe liegt vor.

## 6. Architekturregeln

- Das Backend wird als modularer Monolith mit Kotlin und Spring Boot umgesetzt; das Angular-Frontend greift über die definierte REST-API darauf zu.
- Fachmodule kommunizieren über ihre vorgesehenen Fachschnittstellen. Der direkte Zugriff auf Repositories anderer Module ist untersagt.
- Fachliche Repository-Interfaces sind von konkreten JPA-Persistenzadaptern getrennt.
- Jahresurlaubskonten werden ausschließlich durch Modul C, Arbeitszeitkonten ausschließlich durch Modul D verwaltet.
- Die Genehmigung eines Urlaubsantrags speichert Statusänderung, betroffene Jahreskonten, Audit-Eintrag und In-App-Benachrichtigung in einer gemeinsamen Transaktion.
- Authentifizierung und Berechtigungen werden im Backend geprüft; eine Anzeige- oder Sperrlogik im Frontend ersetzt keine serverseitige Autorisierung.
- Zeitabhängige Geschäftslogik nutzt die definierte abstrahierte Zeitquelle. Optionaler E-Mail-Versand erfolgt unabhängig vom Erfolg der fachlichen Datenbanktransaktion.
