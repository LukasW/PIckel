# Delta US-16 — Abhängigkeit zwischen Features erfassen (Issue #16)

Story: `spec/user-stories/16-abhaengigkeit-erfassen.md` (neu) · Status: eingepflegt

## Warum

Abhängigkeiten zwischen Teams sind der häufigste Grund, warum ein PI scheitert. Sie müssen im Planning erfasst
werden, nicht im Kopf einzelner Personen.

## Entscheide

- **Richtung:** Feature A **braucht** Feature B; B ist die Lieferung, A der Bedarf. Der Zeitpunkt ergibt sich aus der
  Einplanung beider Features, kein eigenes Datum
- **Gleiches PI:** Beide Features gehören zum selben PI
- **Abgelehnt** werden ein Feature, das sich selbst braucht, eine doppelte Abhängigkeit und die direkte Umkehr
  (A braucht B und B braucht A). Längere Zyklen erkennt PIkel in M1 nicht; der Bedarf ist ohne Praxis spekulativ
- **Löschen eines Features** löscht seine Abhängigkeiten mit (betrifft #10, dort gab es den Fall noch nicht)
- **Anzeige** in M1 als Liste beim Feature und auf der Detailseite; die Linie auf dem Board folgt mit #17

## Neu

### AK: Für ein Feature lässt sich festhalten, dass es ein anderes Feature desselben PI braucht

### AK: Ein Feature kann sich nicht selbst brauchen; doppelte und direkt umgekehrte Abhängigkeiten werden abgelehnt

### AK: Beim Feature sind die Abhängigkeiten in beide Richtungen sichtbar («braucht» und «wird gebraucht von»)

### AK: Eine Abhängigkeit lässt sich entfernen

### AK: Wird ein Feature gelöscht, verschwinden auch seine Abhängigkeiten

### Szenario: Abhängigkeit wird erfasst
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Wenn** ich festhalte, dass "Portal" das Feature "Login" braucht
- **Dann** braucht "Portal" das Feature "Login"
- **Und** wird "Login" von "Portal" gebraucht

### Szenario: Feature, das sich selbst braucht, wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich festhalte, dass "Portal" das Feature "Portal" braucht
- **Dann** wird das Erfassen mit der Meldung "Ein Feature kann nicht von sich selbst abhängen." abgelehnt

### Szenario: Doppelte Abhängigkeit wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** "Portal" braucht "Login"
- **Wenn** ich festhalte, dass "Portal" das Feature "Login" braucht
- **Dann** wird das Erfassen mit der Meldung "Diese Abhängigkeit gibt es schon." abgelehnt

### Szenario: Umgekehrte Abhängigkeit wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** "Portal" braucht "Login"
- **Wenn** ich festhalte, dass "Login" das Feature "Portal" braucht
- **Dann** wird das Erfassen mit der Meldung "Die beiden Features hängen schon umgekehrt voneinander ab." abgelehnt

### Szenario: Abhängigkeit zu einem Feature aus einem anderen PI wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.2" das Feature "Login" ohne WSJF-Rang
- **Wenn** ich festhalte, dass "Portal" das Feature "Login" braucht
- **Dann** wird das Erfassen mit der Meldung "Beide Features müssen zum selben PI gehören." abgelehnt

### Szenario: Abhängigkeit wird entfernt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** "Portal" braucht "Login"
- **Wenn** ich die Abhängigkeit von "Portal" zu "Login" entferne
- **Dann** braucht "Portal" kein Feature

### Szenario: Gelöschtes Feature nimmt seine Abhängigkeiten mit
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Feature "Login" lösche
- **Dann** braucht "Portal" kein Feature

### Szenario: Gelöschtes brauchendes Feature nimmt seine Abhängigkeiten mit
<!-- Im Nachtlauf ergänzt: AK 5 gilt in beide Richtungen, das Szenario oben prüft nur das gebrauchte Feature. -->
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Feature "Portal" lösche
- **Dann** wird "Login" von keinem Feature gebraucht

## Geändert

keine — das Löschen eines Features mit Abhängigkeiten ist ein neuer Fall; die Szenarien von #10 behalten ihr
Ergebnis

## Entfernt

keine
