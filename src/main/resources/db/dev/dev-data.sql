-- Beispieldaten für den Dev-Modus (Liquibase-Kontext «dev», Issue #59). Keine Schema-Migration: Die Datei darf
-- geändert werden und läuft dann beim nächsten Start neu (runOnChange). Feste UUIDs und ON CONFLICT DO NOTHING
-- machen jeden Lauf idempotent; ein geänderter Wert einer bestehenden Zeile wird nicht nachgezogen (Dev-DB leeren).
-- Die Werte folgen den Domain-Regeln, die SQL nicht prüft: keine Features in der IP-Iteration, Rang >= 1.

INSERT INTO team (id, name) VALUES
    ('00000000-0000-4000-8000-0000000000a1', 'Adler'),
    ('00000000-0000-4000-8000-0000000000a2', 'Falke'),
    ('00000000-0000-4000-8000-0000000000a3', 'Luchs')
ON CONFLICT DO NOTHING;

-- PI 27.1: 2-Wochen-Iterationen, 4 Entwicklungs-Iterationen und die IP-Iteration
INSERT INTO program_increment (id, name, start_date, end_date, iteration_length_weeks, business_context, art_objectives) VALUES
    ('00000000-0000-4000-8000-0000000000b1', 'PI 27.1', '2027-01-11', '2027-03-19', 2,
     'Das Kundenportal löst das alte Antragsformular ab. Parallel läuft die Migration auf die neue Plattform.',
     'Portal-Release 2 produktiv. Migration der Bestandsdaten abgeschlossen. Betriebsbereitschaft des neuen Monitorings.'),
    ('00000000-0000-4000-8000-0000000000b2', 'PI 27.2', '2027-03-22', '2027-05-28', 2, '', '')
ON CONFLICT DO NOTHING;

INSERT INTO pi_link (id, program_increment_id, position, title, url) VALUES
    ('00000000-0000-4000-8000-0000000000c1', '00000000-0000-4000-8000-0000000000b1', 1, 'Roadmap', 'https://example.com/roadmap')
ON CONFLICT DO NOTHING;

INSERT INTO capacity (program_increment_id, team_id, iteration_number, points) VALUES
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a1', 1, 20),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a1', 2, 20),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a1', 3, 16),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a1', 4, 20),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a2', 1, 15),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a2', 2, 15),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a2', 3, 15),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a2', 4, 12),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a3', 1, 10),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a3', 2, 10),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a3', 3, 10),
    ('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a3', 4, 10)
ON CONFLICT DO NOTHING;

-- PI 27.1: vier eingeplante und vier Backlog-Features
INSERT INTO feature (id, program_increment_id, title, description, owner, wsjf_rank, estimate, team_id, iteration_number) VALUES
    ('00000000-0000-4000-8000-0000000000f1', '00000000-0000-4000-8000-0000000000b1',
     'Antragsformular im Portal', 'Kundinnen und Kunden erfassen Anträge online statt auf Papier.', 'Sandra Keller', 1, 13,
     '00000000-0000-4000-8000-0000000000a1', 1),
    ('00000000-0000-4000-8000-0000000000f2', '00000000-0000-4000-8000-0000000000b1',
     'Single Sign-on', 'Anmeldung am Portal über den zentralen Identity Provider.', 'Marco Berger', 2, 8,
     '00000000-0000-4000-8000-0000000000a2', 1),
    ('00000000-0000-4000-8000-0000000000f3', '00000000-0000-4000-8000-0000000000b1',
     'Dokumenten-Upload', 'Belege zum Antrag hochladen und dem Antrag zuordnen.', 'Sandra Keller', 3, 8,
     '00000000-0000-4000-8000-0000000000a1', 3),
    ('00000000-0000-4000-8000-0000000000f4', '00000000-0000-4000-8000-0000000000b1',
     'Migration Bestandsdaten', 'Bestehende Anträge in das neue Datenmodell überführen.', 'Nina Vogel', 4, 21,
     '00000000-0000-4000-8000-0000000000a3', 2),
    ('00000000-0000-4000-8000-0000000000f5', '00000000-0000-4000-8000-0000000000b1',
     'Benachrichtigungen per E-Mail', 'Statusänderungen am Antrag lösen eine E-Mail aus.', 'Marco Berger', 5, 5, NULL, NULL),
    ('00000000-0000-4000-8000-0000000000f6', '00000000-0000-4000-8000-0000000000b1',
     'Monitoring und Alarmierung', 'Dashboards und Alarme für den Betrieb des neuen Portals.', 'Nina Vogel', 6, 13, NULL, NULL),
    ('00000000-0000-4000-8000-0000000000f7', '00000000-0000-4000-8000-0000000000b1',
     'Mehrsprachigkeit', 'Portal auf Deutsch, Französisch und Italienisch.', 'Sandra Keller', NULL, NULL, NULL, NULL),
    ('00000000-0000-4000-8000-0000000000f8', '00000000-0000-4000-8000-0000000000b1',
     'Barrierefreiheit', 'Das Portal erfüllt WCAG 2.1 AA.', 'Marco Berger', 7, 8, NULL, NULL),
    -- PI 27.2: nur Backlog
    ('00000000-0000-4000-8000-0000000000f9', '00000000-0000-4000-8000-0000000000b2',
     'Self-Service für Adressänderungen', 'Adressen im Portal selbst ändern.', 'Sandra Keller', 1, 5, NULL, NULL),
    ('00000000-0000-4000-8000-0000000000fa', '00000000-0000-4000-8000-0000000000b2',
     'Mobile App', 'Ein schlanker Zugang zum Portal als App.', 'Marco Berger', 2, 21, NULL, NULL),
    ('00000000-0000-4000-8000-0000000000fb', '00000000-0000-4000-8000-0000000000b2',
     'Reporting für Sachbearbeitende', 'Auswertungen über offene und erledigte Anträge.', 'Nina Vogel', NULL, NULL, NULL, NULL)
ON CONFLICT DO NOTHING;

-- Dokumenten-Upload braucht den Antrag; der Antrag braucht Single Sign-on (Single Sign-on liegt früher oder gleich)
INSERT INTO feature_dependency (needing_feature_id, needed_feature_id) VALUES
    ('00000000-0000-4000-8000-0000000000f3', '00000000-0000-4000-8000-0000000000f1'),
    ('00000000-0000-4000-8000-0000000000f1', '00000000-0000-4000-8000-0000000000f2')
ON CONFLICT DO NOTHING;

INSERT INTO milestone (id, program_increment_id, name, milestone_date) VALUES
    ('00000000-0000-4000-8000-0000000000d1', '00000000-0000-4000-8000-0000000000b1', 'Beta-Release', '2027-02-12'),
    ('00000000-0000-4000-8000-0000000000d2', '00000000-0000-4000-8000-0000000000b1', 'Go-live Portal', '2027-03-12')
ON CONFLICT DO NOTHING;
