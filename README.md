# Stock Tracker

Spring-Boot-Webapp fuer ein privates Aktien- und Krypto-Portfolio mit Login, Asset-Suche, Kauf/Verkauf, Holdings, Transaktionshistorie, Dashboard und Portfolio-Chart.



## Erster Start

Voraussetzungen:

- Java 17
- Internetzugang fuer Maven-Abhaengigkeiten und Kursdaten
- PostgreSQL-kompatible Datenbank, lokal oder Neon (extern)
- optional: Node.js + npm fuer JavaScript-Tests
- OS: Windows (Startskripte)

> ***!!! Hinweis !!!***  
> ***Nutzer aus Präsentation, mit Beispieldaten.***  
> **username: test@live.com**  
> **password: abcdefgh**  
> **security code: 9C4WT9E3**  



### Start unter Windows:
```bat
mvn clean package
java -jar target\stock-tracker-1.0.0.jar
```

### Alternative ohne JAR:

```bat
mvn spring-boot:run
```

Danach: `http://localhost:8080`

`start.bat` startet eine vorhandene JAR. `start-dev.bat` baut neu, startet danach die JAR und führt die Tests aus.
### Schnellstart mit Build und Tests
```bat
.\start-dev.bat
```
### Schnellstart ohne Build und Tests
```bat
.\start.bat
```



## Konfiguration

Standardprofil: `dev`. Im Dev-Profil ist eine Neon/PostgreSQL-Datenbank vorkonfiguriert. Fuer eine eigene Datenbank diese Umgebungsvariablen setzen:

```text
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/stock_tracker
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

Fuer Produktion `SPRING_PROFILES_ACTIVE=prod` setzen und ebenfalls `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` angeben. Im Prod-Profil gilt `ddl-auto=validate`.



## Tests

```bat
mvn test
npm install
npm.cmd run test:js
```

Optionale API-Smoke-Tests aus dem Projekt-Root:

```powershell
.\test-yahoo-api.ps1
.\test-eurostat-api.ps1
.\test-ecb-api.ps1
```

Hinweis: Die API-Tests brauchen Internetzugang.



## Tech Stack

Java 17, Spring Boot 4.1.0-M4, Spring MVC/Security/Data JPA, Thymeleaf, PostgreSQL/Neon, Vanilla JavaScript, Jest/jsdom.



## Struktur

```text
src/main/java/com/project/stocktracker  Backend
src/main/resources/templates            Thymeleaf
src/main/resources/static               CSS/JavaScript
src/test/java                           Java-Tests
src/test/js                             JavaScript-Tests
```



## Autoren

Bleron Beqiri & Dominik Peschke
