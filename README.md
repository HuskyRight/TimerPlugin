# ⏱️ WorldTimer - Minecraft Spigot/Paper Plugin

**WorldTimer** ist ein leistungsstarkes Minecraft-Plugin (optimiert für **Paper 1.21+**), entwickelt für Challenges, Speedruns und Hardcore-Projekte. Es kombiniert einen präzisen Welt-Timer mit einem vollständigen **Welt-Reset- und Rollback-System** für alle Dimensionen (Overworld, Nether und End) inklusive Spieler-Inventaren.

---

## 🚀 Hauptfunktionen

* **⏱️ Challenge Timer**:
  * Gut sichtbare Anzeige per **BossBar** oben im Bildschirm.
  * Automatisches Pausieren der Welt (Einfrieren von Zeit, Mob-KI und Block-Ticks), wenn der Timer gestoppt ist oder nicht genügend Spieler online sind.
  * Automatischer Stopp und Pausierung bei Spielertod.
  * **Timer-Persistenz**: Der Timer übersteht Server-Neustarts und wird mit Presets/Rollbacks verknüpft gespeichert.

* **🌍 Welt-Reset (`/world reset`)**:
  * Setzt alle drei Dimensionen (**Overworld**, **Nether** und **The End**) komplett zurück.
  * Löscht die Inventare aller Spieler (`playerdata`), sodass jeder bei Null startet.
  * **Automatisches Rollback-Backup**: Bevor die Welt gelöscht wird, wird der aktuelle Spielstand automatisch gesichert.
  * Resettet den Timer auf `00:00:00`.

* **⏮️ Welt-Rollback (`/world rollback`)**:
  * Stellt die Welt vor dem letzten Reset 1:1 wieder her.
  * Stellt alle **Spieler-Inventare, Enderkisten, Erfahrungslevel, Positionen** und den exakten **Timer-Stand** wieder her.

* **💾 Presets & Speicherstände (`/world save` / `/world load`)**:
  * Manuelles Speichern beliebiger Stände mit eigenem Namen.
  * Laden von Presets inkl. Welten, Inventaren und Timer.
  * Übersichtliche Auflistung aller Presets sortiert nach **Speicherdatum** (`/world list`).

---

## 📋 Befehle & Berechtigungen

### Berechtigungen
* `worldtimer.admin` — Erlaubt die Nutzung aller Timer- und Welt-Befehle (Standard: **OP**).

---

### ⏱️ Timer-Befehle (`/timer` & `/timermode`)

| Befehl | Beschreibung |
|---|---|
| `/timer on` | Blendet die Timer-BossBar für alle Spieler ein. |
| `/timer off` | Blendet den Timer aus, stoppt ihn und setzt die Zeit auf 0 zurück. |
| `/timer start` | Startet den Timer. Die Welt läuft weiter. |
| `/timer stop` | Hält den Timer an. Die Welt wird pausiert. |
| `/timer reset` | Setzt die abgelaufene Zeit auf `00:00:00` zurück. |
| `/timer exclude <Spieler>` | Schließt einen Spieler vom Timer aus (Toggle). Ausgeschlossene Spieler werden bei pausierter Welt nicht eingefroren (Locked) und zählen nicht zu den aktiven Spielern. |
| `/timer exclude list` | Listet alle aktuell vom Timer ausgeschlossenen Spieler auf. |
| `/timer exclude <add\|remove> <Spieler>` | Fügt einen Spieler explizit zur Ausschlussliste hinzu oder entfernt ihn. |
| `/timermode set <Anzahl>` | Legt fest, wie viele Spieler mindestens online sein müssen, damit der Timer läuft. |

---

### 🌍 Welt-Befehle (`/world`)

| Befehl | Beschreibung |
|---|---|
| `/world reset` | Generiert Overworld, Nether und End neu. Resettet das Inventar aller Spieler. Erstellt vorab automatisch ein Rollback-Backup. |
| `/world rollback` *(oder `/world Rollback`)* | Stellt die Welt vor dem letzten Reset wieder her (inkl. aller Items und des Timer-Stands). |
| `/world save <Name>` | Speichert die aktuellen Welten, Spielerdaten und den Timer unter dem angegebenen Namen. *(Läuft im Spiel ohne Neustart)* |
| `/world load <Name>` | Lädt ein gespeichertes Preset wieder hoch. *(Erstellt vorab zur Sicherheit ein Rollback)* |
| `/world delete <Name>` | Löscht ein gespeichertes Preset von der Festplatte. |
| `/world list` | Zählt alle gespeicherten Presets auf, **sortiert nach Speicherdatum (neueste zuerst)** mit Datum, Uhrzeit, Timer-Wert und Ersteller. |

> 💡 **Tipp:** Alle Befehle unterstützen Tab-Vervollständigung (z. B. schlägt `/world load <TAB>` automatisch vorhandene Presets vor).

---

## 🛠️ Technische Funktionsweise

1. **Lifecycle über `load: STARTUP`:**
   In Minecraft Paper/Spigot kann die Hauptwelt (`world`) im laufenden Betrieb nicht gefahrlos entladen werden. Daher nutzt WorldTimer den Lifecycle-Modus `load: STARTUP`. Beim Reset oder Laden wird eine anstehende Aktion hinterlegt, Spieler werden mit Info-Nachricht gekickt und der Server startet neu. In `onLoad()` werden die Weltenbereiche ausgetauscht oder gelöscht, **bevor** Minecraft die Welten initialisiert und Dateisperren anlegt.
2. **Sauberes Dateihandling:**
   * Beim Speichern und Laden werden `session.lock` und `uid.dat` ignoriert bzw. bereinigt, um UUID-Konflikte und `ExceptionWorldConflict` zu vermeiden.
   * Sämtliche Spielerdaten (`<uuid>.dat`) aus dem `playerdata/`-Verzeichnis werden synchronisiert (`savePlayers()`) und als Teil des Presets bzw. Rollbacks archiviert.
3. **Dateistruktur im Plugin-Ordner (`plugins/WorldTimer/`):**
   ```text
   plugins/WorldTimer/
   ├── timer.yml             # Aktueller Timer-Zustand (übersteht Serverstopps)
   ├── pending_action.yml    # Temporäre Steuerdatei für Reset/Rollback/Load beim Booten
   └── presets/
       ├── _rollback/        # Automatischer Rollback-Punkt des letzten Stands
       │   ├── world/
       │   ├── world_nether/
       │   ├── world_the_end/
       │   └── meta.yml      # Metadaten (Zeitstempel, Timer, Ersteller)
       └── mein_preset/      # Benutzer-Presets aus /world save
           ├── ...
           └── meta.yml
   ```

---

## 📦 Installation & Kompilierung

### Voraussetzungen
* **Java 17** oder höher
* **Paper 1.21+** (oder kompatible Spigot-Forks)
* **Maven** (zum Bauen aus dem Quellcode)

### Plugin kompilieren
Im Projektverzeichnis folgenden Befehl ausführen:
```bash
mvn clean package
```
Die fertige `.jar`-Datei befindet sich anschließend in:
```text
target/worldtimer-1.1.jar
```

### Installation auf dem Server
1. Kopiere `worldtimer-1.1.jar` in den `plugins/`-Ordner deines Paper-Servers.
2. Stelle sicher, dass in deiner Startdatei (`start.bat` bzw. `start.sh`) eine Neustart-Schleife vorhanden ist, damit der Server nach einem `/world reset` oder `/world rollback` automatisch wieder hochfährt:
   * **Windows (`start.bat`):**
     ```bat
     :loop
     java -Xms4G -Xmx4G -jar paper.jar nogui
     goto loop
     ```
   * **Linux (`start.sh`):**
     ```bash
     while true; do
         java -Xms4G -Xmx4G -jar paper.jar nogui
         echo "Server startet neu in 3 Sekunden..."
         sleep 3
     done
     ```
3. Starte den Server. WorldTimer ist nun einsatzbereit!

