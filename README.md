# F1 Race Report Tool — Backend

> Spring Boot REST API that fetches Formula 1 race data from the Jolpica/Ergast F1 API,
> persists it to PostgreSQL, and generates AI-powered journalist-style race reports using the Groq API.

---

## 📐 Architecture Overview

```
React Frontend (Port 5173)
        │
        │ HTTP / JSON
        ▼
Spring Boot REST API (Port 8080)
        │
        ├──► PostgreSQL (Port 5432)      ← Persist races, drivers, results, reports
        ├──► Jolpica F1 API              ← Fetch race data, lap times, standings
        │    https://api.jolpi.ca/ergast/f1
        └──► Groq AI API                 ← Generate journalist race reports
             https://api.groq.com/openai/v1
```

### Request Flow

```
GET /api/race-data?season=2024&round=5
        │
        ▼
RaceController.getRaceData()
        │
        ▼
RaceDataService.getRaceData()
        │
        ├─ Check PostgreSQL ──► Hit → return from DB (fast)
        │                       Miss ↓
        └─ ErgastService.fetchRaceResults()   → Jolpica API call
           ErgastService.fetchLapData()       → Jolpica API call
           ErgastService.fetchDriverStandings()→ Jolpica API call
                │
                └─ Persist to PostgreSQL (async)
                │
                └─ Assemble RaceDataDTO → return JSON
```

---

## 🗂️ Project Structure

```
src/main/java/com/f1report/
├── F1ReportApplication.java       ← Entry point
├── config/
│   ├── AppConfig.java             ← RestTemplate beans, thread pool
│   ├── CacheConfig.java           ← Caffeine cache with per-cache TTLs
│   └── CorsConfig.java            ← CORS for React frontend
├── controller/
│   ├── SeasonController.java      ← GET /api/seasons, GET /api/races
│   ├── RaceController.java        ← GET /api/race-data
│   ├── ReportController.java      ← POST /api/generate-report, PDF export
│   └── GlobalExceptionHandler.java← Catches all exceptions → JSON errors
├── service/
│   ├── ErgastService.java         ← Jolpica API integration + JSON parsing
│   ├── RaceDataService.java       ← Orchestrates fetch + DB + DTO assembly
│   ├── GroqService.java           ← Groq AI API + prompt engineering
│   ├── ReportService.java         ← Report lifecycle (check → generate → store)
│   └── PdfExportService.java      ← OpenPDF report generation
├── repository/
│   ├── DriverRepository.java
│   ├── RaceRepository.java
│   ├── RaceResultRepository.java
│   └── ReportRepository.java
├── model/
│   ├── Driver.java                ← @Entity → drivers table
│   ├── Race.java                  ← @Entity → races table
│   ├── RaceResult.java            ← @Entity → race_results table
│   └── Report.java                ← @Entity → reports table
└── dto/
    ├── ApiResponseDTO.java        ← Generic { success, message, data } wrapper
    ├── SeasonDTO.java
    ├── RaceDTO.java
    ├── DriverResultDTO.java
    ├── LapDataDTO.java
    ├── RaceDataDTO.java           ← Master race payload (all data combined)
    ├── ReportRequestDTO.java
    └── ReportResponseDTO.java
```

---

## ⚙️ Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 21 (LTS) | Runtime |
| Gradle | 8.7 (via wrapper) | Build tool |
| PostgreSQL | 15+ | Database |
| Groq API Key | Free | AI report generation |

---

## 🚀 Setup & Run

### 1. Clone the project

```bash
git clone <your-repo-url>
cd f1-race-report-backend
```

### 2. Create PostgreSQL database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create the database
CREATE DATABASE f1reportdb;
\q
```

### 3. Configure environment variables

```bash
# Copy the example env file
cp .env.example .env

# Edit .env with your values
nano .env
```

Fill in:
- `DB_PASSWORD` – your PostgreSQL password
- `GROQ_API_KEY` – get free at https://console.groq.com

### 4. Set environment variables in your shell

**macOS/Linux:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/f1reportdb
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export GROQ_API_KEY=gsk_your_key_here
```

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/f1reportdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:GROQ_API_KEY="gsk_your_key_here"
```

### 5. Run the application

```bash
# Grant execute permission to Gradle wrapper (macOS/Linux only)
chmod +x gradlew

# Start the Spring Boot application
./gradlew bootRun
```

You should see:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.5)

...
[main] c.f1report.F1ReportApplication  : Started F1ReportApplication in 3.2 seconds
```

### 6. Build a production JAR

```bash
./gradlew bootJar
java -jar build/libs/f1-race-report-backend.jar
```

---

## 🌐 API Reference

All endpoints return:
```json
{
  "success": true,
  "message": "OK",
  "data": { ... },
  "statusCode": 200
}
```

### GET `/api/seasons`
Returns all available F1 seasons (1950–present), newest first.

```bash
curl http://localhost:8080/api/seasons
```

### GET `/api/races?season=2024`
Returns all races for a given season.

```bash
curl "http://localhost:8080/api/races?season=2024"
```

### GET `/api/race-data?season=2024&round=5`
Returns the full race payload (results + lap data + standings).

```bash
curl "http://localhost:8080/api/race-data?season=2024&round=5"
```

### POST `/api/generate-report`
Generates an AI race report using Groq.

```bash
curl -X POST http://localhost:8080/api/generate-report \
  -H "Content-Type: application/json" \
  -d '{"season": 2024, "round": 5, "forceRegenerate": false}'
```

### GET `/api/reports`
Returns the 10 most recently generated reports.

```bash
curl http://localhost:8080/api/reports
```

### GET `/api/export-pdf/{reportId}`
Downloads the report as a PDF file.

```bash
curl "http://localhost:8080/api/export-pdf/1" --output race-report.pdf
```

### GET `/api/export-pdf/race?season=2024&round=5`
Downloads PDF by season+round (requires report to exist).

```bash
curl "http://localhost:8080/api/export-pdf/race?season=2024&round=5" --output race-report.pdf
```

### GET `/actuator/health`
Application health check (for load balancers / monitoring).

```bash
curl http://localhost:8080/actuator/health
```

---

## 🗄️ Database Schema

Hibernate auto-creates these tables on startup (`JPA_DDL_AUTO=update`):

```sql
-- drivers: one row per unique F1 driver
CREATE TABLE drivers (
    id               BIGSERIAL PRIMARY KEY,
    driver_id        VARCHAR(100) UNIQUE NOT NULL,  -- "max_verstappen"
    given_name       VARCHAR(100) NOT NULL,
    family_name      VARCHAR(100) NOT NULL,
    date_of_birth    VARCHAR(20),
    nationality      VARCHAR(100),
    permanent_number VARCHAR(5),
    code             VARCHAR(5),                     -- "VER"
    url              VARCHAR(500)
);

-- races: one row per Grand Prix event
CREATE TABLE races (
    id           BIGSERIAL PRIMARY KEY,
    season       INT NOT NULL,
    round        INT NOT NULL,
    race_name    VARCHAR(200) NOT NULL,
    circuit_id   VARCHAR(100),
    circuit_name VARCHAR(200),
    country      VARCHAR(100),
    locality     VARCHAR(100),
    race_date    DATE,
    race_time    VARCHAR(20),
    UNIQUE (season, round)
);

-- race_results: one row per driver per race
CREATE TABLE race_results (
    id                    BIGSERIAL PRIMARY KEY,
    race_id               BIGINT REFERENCES races(id),
    driver_id             BIGINT REFERENCES drivers(id),
    finishing_position    INT,
    grid_position         INT,
    points                DOUBLE PRECISION,
    laps_completed        INT,
    status                VARCHAR(50),
    finish_time           VARCHAR(50),
    fastest_lap_time      VARCHAR(50),
    fastest_lap_rank      INT,
    fastest_lap_avg_speed DOUBLE PRECISION,
    constructor_name      VARCHAR(100),
    car_number            VARCHAR(5),
    UNIQUE (race_id, driver_id)
);

-- reports: AI-generated race reports
CREATE TABLE reports (
    id                BIGSERIAL PRIMARY KEY,
    season            INT NOT NULL,
    round             INT NOT NULL,
    race_name         VARCHAR(200),
    report_content    TEXT NOT NULL,
    model_used        VARCHAR(100),
    prompt_tokens     INT,
    completion_tokens INT,
    created_at        TIMESTAMP NOT NULL,
    winner_name       VARCHAR(200),
    winner_constructor VARCHAR(100)
);
CREATE INDEX idx_reports_season_round ON reports(season, round);
```

---

## 🔧 Caching Strategy

| Cache | TTL | Max Entries | Notes |
|-------|-----|-------------|-------|
| `seasons` | 24 hours | 20 | Rarely changes |
| `races` | 6 hours | 500 | May update mid-season |
| `raceResults` | 12 hours | 200 | Immutable after race |
| `lapData` | 12 hours | 100 | Immutable after race |
| `standings` | 6 hours | 500 | Updates each race |

---

## 🐳 Docker (Optional)

```bash
# Build the JAR first
./gradlew bootJar

# Run with Docker Compose (includes PostgreSQL)
docker-compose up
```

`docker-compose.yml` (create this):
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: f1reportdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/f1reportdb
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      GROQ_API_KEY: ${GROQ_API_KEY}
    depends_on:
      - postgres
```

---

## 🐛 Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| `Connection refused` on DB | PostgreSQL not running | `brew services start postgresql` or `pg_ctl start` |
| `401 Unauthorized` from Groq | Wrong/missing API key | Check `GROQ_API_KEY` env var |
| Empty lap data | Old races (pre-2012) | Expected – Ergast has limited lap data for older races |
| `No race data found` | Future race | Race hasn't happened yet |
| Slow first request | Cache miss → Ergast API | Subsequent requests use cache |

---

## 📦 Gradle Tasks Reference

```bash
./gradlew bootRun       # Run in development mode (auto-restart on change)
./gradlew bootJar       # Build production fat JAR
./gradlew test          # Run all tests
./gradlew dependencies  # Show dependency tree
./gradlew clean         # Clean build directory
```
