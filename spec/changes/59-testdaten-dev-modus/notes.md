# Notizen #59
- Dev-SQL gegen frisches PostgreSQL 17 (Migrationen V1–V9) zweimal geladen: kein Fehler, keine Duplikate (3 Teams, 11 Features, 12 Kapazitäten, 2 Abhängigkeiten, 2 Milestones).
- PI 27.1 (2027-01-11 bis 2027-03-19, 2 Wochen) hat 4 Entwicklungs-Iterationen plus IP; Kapazität und Einplanung nutzen nur 1–4.
- Nicht von Hand geprüft: `quarkus:dev` selbst (Quinoa/Angular-Start); belegt sind nur der Kontext `default` durch `gate.sh` (CucumberIT, SmokeIT grün) und das SQL gegen das Schema.
- `ON CONFLICT DO NOTHING` zieht geänderte Werte bestehender Zeilen nicht nach; dann Dev-DB leeren (im SQL-Kopf vermerkt).
