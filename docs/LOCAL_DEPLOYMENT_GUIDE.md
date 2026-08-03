# Local Deployment Guide — Opus Dispute Hub (Complete System)

This guide covers setting up and running the **full system** — the Spring Boot backend API, the React+Vite frontend with its proxy layer, and PostgreSQL — on your local laptop for development and testing.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Step 1: Install Java 19](#2-step-1-install-java-19)
3. [Step 2: Install Node.js and pnpm](#3-step-2-install-nodejs-and-pnpm)
4. [Step 3: Install and Configure PostgreSQL](#4-step-3-install-and-configure-postgresql)
5. [Step 4: Clone the Repositories](#5-step-4-clone-the-repositories)
6. [Step 5: Configure Backend Environment Variables](#6-step-5-configure-backend-environment-variables)
7. [Step 6: Set Up the Mastercard Keystore](#7-step-6-set-up-the-mastercard-keystore)
8. [Step 7: Build and Run the Backend](#8-step-7-build-and-run-the-backend)
9. [Step 8: Build and Run the Frontend](#9-step-8-build-and-run-the-frontend)
10. [Step 9: Verify the Full System](#10-step-9-verify-the-full-system)
11. [Running with Docker Compose (Alternative)](#11-running-with-docker-compose-alternative)
12. [How the Frontend Connects to the Backend](#12-how-the-frontend-connects-to-the-backend)
13. [Configuration Reference](#13-configuration-reference)
14. [Common Development Tasks](#14-common-development-tasks)
15. [Troubleshooting](#15-troubleshooting)
16. [Quick Start (5 Minutes)](#16-quick-start-5-minutes)

---

## 1. Prerequisites

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| Java (GraalVM or OpenJDK) | 19 | `java -version` |
| Maven | 3.8+ (included via `mvnw` wrapper) | `./mvnw -version` |
| Node.js | 20+ | `node --version` |
| pnpm | 9+ | `pnpm --version` |
| PostgreSQL | 14+ | `psql --version` |
| Git | Any recent | `git --version` |
| Docker + Docker Compose | Latest (optional — for Docker approach) | `docker --version` |

### API Keys You'll Need

| Key | Required? | Purpose |
|-----|-----------|---------|
| `GEMINI_API_KEY` | Yes (for AI agents) | Powers all 7 AI agents via Google Gemini 2.5 Flash |
| `STRIPE_SECRET_KEY` | Optional | Stripe evidence enrichment during Agent 2 |
| `ETHOCA_API_KEY_ID` + `ETHOCA_API_SECRET` | Optional | Ethoca Consumer Clarity integration |
| Mastercard `.p12` keystore | Optional | Mastercard Mastercom API calls (ingestion, chargebacks, etc.) |

The backend will start without any of these keys — local CRUD operations and evidence file management work without them. AI agents require the Gemini key. Mastercard API calls require the `.p12` keystore.

---

## 2. Step 1: Install Java 19

### macOS

```bash
# Using SDKMAN (recommended)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 19.0.2-graalce

# Or using Homebrew
brew install --cask graalvm-jdk19
export JAVA_HOME=$(/usr/libexec/java_home -v 19)
```

### Windows

1. Download GraalVM JDK 19 from https://www.graalvm.org/downloads/
2. Extract to `C:\Program Files\Java\graalvm-jdk-19`
3. Set environment variables:
   - `JAVA_HOME` = `C:\Program Files\Java\graalvm-jdk-19`
   - Add `%JAVA_HOME%\bin` to `PATH`

### Linux (Ubuntu/Debian)

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 19.0.2-graalce
```

### Verify

```bash
java -version
# Should show: java version "19.x.x" or similar
```

---

## 3. Step 2: Install Node.js and pnpm

The frontend requires Node.js 20+ and pnpm 9+.

### macOS

```bash
# Using Homebrew
brew install node@20
npm install -g pnpm@9

# Or using nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc
nvm install 20
nvm use 20
npm install -g pnpm@9
```

### Windows

1. Download Node.js 20 LTS from https://nodejs.org/
2. Run the installer
3. Open a new terminal and run:

```cmd
npm install -g pnpm@9
```

### Linux (Ubuntu/Debian)

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
npm install -g pnpm@9
```

### Verify

```bash
node --version    # Should be 20.x.x or higher
pnpm --version    # Should be 9.x.x or higher
```

---

## 4. Step 3: Install and Configure PostgreSQL

### macOS

```bash
brew install postgresql@16
brew services start postgresql@16

psql postgres -c "CREATE USER dispute_admin WITH PASSWORD 'localdevpassword';"
psql postgres -c "CREATE DATABASE dispute_db OWNER dispute_admin;"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE dispute_db TO dispute_admin;"
```

### Windows

1. Download PostgreSQL 16 from https://www.postgresql.org/download/windows/
2. Run the installer — remember the password you set for the `postgres` user
3. Open pgAdmin or `psql` shell and run:

```sql
CREATE USER dispute_admin WITH PASSWORD 'localdevpassword';
CREATE DATABASE dispute_db OWNER dispute_admin;
GRANT ALL PRIVILEGES ON DATABASE dispute_db TO dispute_admin;
```

### Linux (Ubuntu/Debian)

```bash
sudo apt-get update
sudo apt-get install -y postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql

sudo -u postgres psql -c "CREATE USER dispute_admin WITH PASSWORD 'localdevpassword';"
sudo -u postgres psql -c "CREATE DATABASE dispute_db OWNER dispute_admin;"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE dispute_db TO dispute_admin;"
```

### Verify

```bash
psql -h localhost -U dispute_admin -d dispute_db -c "SELECT 1;"
# Should return: 1
```

---

## 5. Step 4: Clone the Repositories

You need two repositories — the backend (Spring Boot) and the frontend (React+Vite).

```bash
# Create a workspace directory
mkdir opus-dispute-hub && cd opus-dispute-hub

# Clone the backend
git clone <backend-repo-url> backend
# This is the Spring Boot API (Java project with pom.xml)

# Clone the frontend
git clone <frontend-repo-url> frontend
# This contains the dispute-dashboard artifact (the React+Vite app)
```

### Backend Project Structure

```
backend/
├── src/
│   ├── main/java/com/opus/dispute/management/   # Java source code
│   └── resources/application.properties          # App configuration
├── src/data/
│   ├── reason-code-rules.json                    # Reason code → evidence rules
│   └── sources/                                  # Evidence files (issuer + acquirer)
├── pom.xml                                       # Maven dependencies
├── mvnw / mvnw.cmd                               # Maven wrapper
└── docker-compose.yml                            # Docker Compose (backend + PostgreSQL)
```

### Frontend Project Structure

```
frontend/
├── artifacts/dispute-dashboard/
│   ├── src/                                      # React components and pages
│   │   ├── pages/chargebacks/                    # Main dispute management UI
│   │   ├── pages/settings/                       # Settings and config page
│   │   └── lib/                                  # API clients, utilities
│   ├── server/
│   │   └── sb-proxy-plugin.ts                    # The proxy layer (critical)
│   ├── vite.config.ts                            # Vite configuration
│   ├── package.json                              # Dependencies
│   └── dist/                                     # Build output (generated)
├── package.json                                  # Root workspace config
└── pnpm-workspace.yaml                           # pnpm workspace definition
```

---

## 6. Step 5: Configure Backend Environment Variables

### Option A: Shell Profile (Persistent)

Add to `~/.bashrc`, `~/.zshrc`, or `~/.bash_profile`:

```bash
# Required for AI agents
export GEMINI_API_KEY="your-gemini-api-key-here"

# Optional — for Stripe evidence enrichment
export STRIPE_SECRET_KEY="your-stripe-secret-key-here"

# Optional — for Ethoca integration
export ETHOCA_API_KEY_ID="your-ethoca-key-id"
export ETHOCA_API_SECRET="your-ethoca-secret"

# Database (only needed if using non-default values)
export PGHOST="localhost"
export PGPORT="5432"
export PGDATABASE="dispute_db"
export PGUSER="dispute_admin"
export PGPASSWORD="localdevpassword"
```

Then reload: `source ~/.bashrc` (or `~/.zshrc`)

**Windows (PowerShell — session only):**

```powershell
$env:GEMINI_API_KEY = "your-gemini-api-key-here"
$env:STRIPE_SECRET_KEY = "your-stripe-secret-key-here"
$env:ETHOCA_API_KEY_ID = "your-ethoca-key-id"
$env:ETHOCA_API_SECRET = "your-ethoca-secret"
$env:PGHOST = "localhost"
$env:PGPORT = "5432"
$env:PGDATABASE = "dispute_db"
$env:PGUSER = "dispute_admin"
$env:PGPASSWORD = "localdevpassword"
```

**Windows (permanent):** Open **Settings → System → About → Advanced system settings → Environment Variables** and add each variable under User variables.

### Option B: `.env` File (Project-Level)

Create a `.env` file in the backend project root (gitignored):

```bash
GEMINI_API_KEY=your-gemini-api-key-here
STRIPE_SECRET_KEY=your-stripe-secret-key-here
ETHOCA_API_KEY_ID=your-ethoca-key-id
ETHOCA_API_SECRET=your-ethoca-secret
PGHOST=localhost
PGPORT=5432
PGDATABASE=dispute_db
PGUSER=dispute_admin
PGPASSWORD=localdevpassword
```

Then source it before running:

```bash
# macOS/Linux
export $(cat .env | xargs)
./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
Get-Content .env | ForEach-Object {
  if ($_ -match '^([^=]+)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
  }
}
mvnw.cmd spring-boot:run
```

### Using Default Database Settings

If you name your database `dispute_db` and use the user `postgres` with password `password`, you don't need to set any `PG*` environment variables at all — the defaults in `application.properties` will work:

```properties
spring.datasource.url=jdbc:postgresql://${PGHOST:localhost}:${PGPORT:5432}/${PGDATABASE:dispute_db}
spring.datasource.username=${PGUSER:postgres}
spring.datasource.password=${PGPASSWORD:password}
```

---

## 7. Step 6: Set Up the Mastercard Keystore

The Mastercard `.p12` signing keystore is required only for live Mastercard API calls (ingestion, chargebacks, transaction search, etc.). Without it, the backend starts normally — you'll see a warning in the logs, and all local features (dispute CRUD, AI agents, evidence management) work fine.

### If You Have the Keystore

```bash
# macOS/Linux
cp "/path/to/Opus Dispute Management System-sandbox-signing.p12" \
   backend/src/main/resources/
```

```powershell
# Windows (PowerShell)
Copy-Item "C:\path\to\Opus Dispute Management System-sandbox-signing.p12" `
  -Destination backend\src\main\resources\
```

Verify the path in `application.properties` matches:

```properties
mastercard.keystore-path=src/main/resources/Opus Dispute Management System-sandbox-signing.p12
```

### If You Don't Have the Keystore

No action needed. The backend will start with a harmless warning:

```
WARN: Mastercard keystore not found at configured path. Mastercard API features will be unavailable.
```

---

## 8. Step 7: Build and Run the Backend

```bash
# macOS/Linux
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
cd backend
mvnw.cmd spring-boot:run
```

The first run will download all Maven dependencies (2–5 minutes). Subsequent starts are much faster.

### What to Expect on Startup

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

:: Spring Boot ::                (v4.0.3)

... Tomcat started on port 5000 (http) ...
... Started MastercardDisputeManagementApplication in X.XX seconds ...
```

### Quick Verify

```bash
# macOS/Linux
curl -s http://localhost:5000/api/disputes | python3 -m json.tool
```

```powershell
# Windows (PowerShell) — use curl.exe (not curl, which is a PowerShell alias)
curl.exe -s http://localhost:5000/api/disputes | python -m json.tool
```

Should return `[]` on first run, or a list of disputes if data exists.

> **Windows note**: Throughout this guide, `curl` commands should be run as `curl.exe` in PowerShell, and `python3` should be run as `python`. PowerShell aliases `curl` to `Invoke-WebRequest` which behaves differently, and Windows installs Python as `python` (not `python3`).

**Leave the backend running** and open a new terminal for the frontend.

---

## 9. Step 8: Build and Run the Frontend

### 9.1 Configure the Backend URL

The frontend proxy needs to know where the backend is. Edit the proxy plugin file to point to your local backend.

Open `frontend/artifacts/dispute-dashboard/server/sb-proxy-plugin.ts` and find this line near the top:

```typescript
const SB_URL =
  process.env.SPRINGBOOT_URL ||
  "https://14c4d4fe-3a81-4b0e-a199-f1f4d5147f31-00-bhxfmiflhthj.pike.replit.dev";
```

For local development, you have two options:

**Option A: Set an environment variable** (no code change needed)

```bash
export SPRINGBOOT_URL=http://localhost:5000
```

**Option B: Change the fallback URL** (permanent)

```typescript
const SB_URL =
  process.env.SPRINGBOOT_URL ||
  "http://localhost:5000";
```

### 9.2 Remove Replit-Specific Plugins

The Vite config references Replit-specific plugins that won't exist on your laptop. Open `frontend/artifacts/dispute-dashboard/vite.config.ts` and simplify it:

**Before** (Replit version):

```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";
import runtimeErrorOverlay from "@replit/vite-plugin-runtime-error-modal";
import { sbProxyPlugin } from "./server/sb-proxy-plugin";

const rawPort = process.env.PORT;
if (!rawPort) {
  throw new Error("PORT environment variable is required...");
}
const port = Number(rawPort);
// ... basePath checks ...

export default defineConfig({
  base: basePath,
  plugins: [
    sbProxyPlugin(),
    react(),
    tailwindcss(),
    runtimeErrorOverlay(),
    ...(process.env.NODE_ENV !== "production" && process.env.REPL_ID !== undefined
      ? [/* cartographer, devBanner */]
      : []),
  ],
  // ...
});
```

**After** (local version):

```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";
import { sbProxyPlugin } from "./server/sb-proxy-plugin";

const port = Number(process.env.PORT || "5173");
const basePath = process.env.BASE_PATH || "/";

export default defineConfig({
  base: basePath,
  plugins: [
    sbProxyPlugin(),
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "src"),
    },
    dedupe: ["react", "react-dom"],
  },
  root: path.resolve(import.meta.dirname),
  build: {
    outDir: path.resolve(import.meta.dirname, "dist/public"),
    emptyOutDir: true,
  },
  server: {
    port,
    host: "0.0.0.0",
    allowedHosts: true,
  },
  preview: {
    port,
    host: "0.0.0.0",
    allowedHosts: true,
  },
});
```

### 9.3 Install Dependencies

```bash
cd frontend

# Install all workspace dependencies
pnpm install
```

> **Note**: If the workspace has other packages (e.g., `@workspace/api-client-react`, `@workspace/api-zod`), pnpm will install those too. If you get errors about missing workspace packages, you can install just the dashboard's deps:
> ```bash
> cd artifacts/dispute-dashboard
> pnpm install
> ```

### 9.4 Run the Frontend in Dev Mode

```bash
# macOS/Linux — from the frontend root directory
SPRINGBOOT_URL=http://localhost:5000 PORT=5173 BASE_PATH="/" pnpm --filter @workspace/dispute-dashboard run dev
```

```powershell
# Windows (PowerShell)
$env:SPRINGBOOT_URL = "http://localhost:5000"
$env:PORT = "5173"
$env:BASE_PATH = "/"
pnpm --filter @workspace/dispute-dashboard run dev
```

### What to Expect

```
  VITE v6.x.x  ready in XXX ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: http://0.0.0.0:5173/

[SB Proxy] Proxying /api/* → http://localhost:5000
[SB Proxy] Warmup complete: XX disputes
```

### 9.5 Open in Browser

Navigate to **http://localhost:5173/** — you should see the Opus Dispute Hub dashboard with live data from your local backend.

### 9.6 Running in Production Preview Mode

To test the production build locally (same mode used in AWS deployment):

```bash
# macOS/Linux
PORT=4173 BASE_PATH="/" pnpm --filter @workspace/dispute-dashboard run build
SPRINGBOOT_URL=http://localhost:5000 PORT=4173 BASE_PATH="/" pnpm --filter @workspace/dispute-dashboard run serve
```

```powershell
# Windows (PowerShell)
$env:PORT = "4173"
$env:BASE_PATH = "/"
pnpm --filter @workspace/dispute-dashboard run build

$env:SPRINGBOOT_URL = "http://localhost:5000"
pnpm --filter @workspace/dispute-dashboard run serve
```

Then open http://localhost:4173/

---

## 10. Step 9: Verify the Full System

With both backend (port 5000) and frontend (port 5173) running:

### Verify Backend Directly

```bash
# macOS/Linux
curl -s http://localhost:5000/api/mastercard/test
curl -s http://localhost:5000/api/disputes | python3 -m json.tool
curl -s http://localhost:5000/api/agents/status | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s http://localhost:5000/api/mastercard/test
curl.exe -s http://localhost:5000/api/disputes | python -m json.tool
curl.exe -s http://localhost:5000/api/agents/status | python -m json.tool
```

### Verify Frontend Proxy

```bash
# macOS/Linux

# Disputes through the proxy (normalized, paginated)
curl -s http://localhost:5173/api/disputes | python3 -m json.tool
# Should return: { "data": [...], "total": N, "page": 1, "pageSize": 50, "totalPages": N }

# Stats (computed by the proxy)
curl -s http://localhost:5173/api/disputes/stats | python3 -m json.tool

# Agent endpoint through proxy
curl -s http://localhost:5173/api/agents/status | python3 -m json.tool

# Pipeline config (local to frontend)
curl -s http://localhost:5173/api/config/pipeline | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s http://localhost:5173/api/disputes | python -m json.tool
curl.exe -s http://localhost:5173/api/disputes/stats | python -m json.tool
curl.exe -s http://localhost:5173/api/agents/status | python -m json.tool
curl.exe -s http://localhost:5173/api/config/pipeline | python -m json.tool
```

### Verify in the Browser

1. Open http://localhost:5173/
2. You should see the **Chargeback Management** page with:
   - Summary cards (Total disputes, Missing Data, Enriched, Flagged Negative ROI)
   - Tabs: All Chargebacks | Opened | Represented | Not Represented
   - A table of disputes with Claim ID, Type, Amount, Due Date, Reason, Enrichment %, Status
3. Click on any dispute to open the detail drawer
4. Click through the agent pipeline sections (Agent 1 through Agent 4)
5. Navigate to **Settings** in the sidebar to see configuration options

### Create a Test Dispute and Run Agents

```bash
# macOS/Linux
curl -s -X POST http://localhost:5000/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "TEST-001",
    "transactionId": "TXN-001",
    "reasonCode": "4834",
    "amount": 150.00,
    "currency": "USD",
    "merchantName": "Test Merchant",
    "status": "NEW"
  }' | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s -X POST http://localhost:5000/api/disputes `
  -H "Content-Type: application/json" `
  -d '{\"claimId\":\"TEST-001\",\"transactionId\":\"TXN-001\",\"reasonCode\":\"4834\",\"amount\":150.00,\"currency\":\"USD\",\"merchantName\":\"Test Merchant\",\"status\":\"NEW\"}' | python -m json.tool
```

Note the returned `id` value (e.g., 1), then run agents:

```bash
# macOS/Linux — requires GEMINI_API_KEY
curl -s -X POST http://localhost:5000/api/agents/summarize/1 | python3 -m json.tool
curl -s -X POST http://localhost:5000/api/agents/enrich/1 | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s -X POST http://localhost:5000/api/agents/summarize/1 | python -m json.tool
curl.exe -s -X POST http://localhost:5000/api/agents/enrich/1 | python -m json.tool
```

Now refresh the frontend — the dispute should show updated enrichment %.

---

## 11. Running with Docker Compose (Alternative)

If you don't want to install Java, Node.js, and PostgreSQL locally, use Docker Compose to run the entire stack.

### 11.1 Create `docker-compose.yml`

In your workspace root directory, create:

```yaml
version: "3.8"

services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: dispute_db
      POSTGRES_USER: dispute_admin
      POSTGRES_PASSWORD: localdevpassword
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dispute_admin -d dispute_db"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "5000:5000"
    environment:
      PGHOST: db
      PGPORT: "5432"
      PGDATABASE: dispute_db
      PGUSER: dispute_admin
      PGPASSWORD: localdevpassword
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY:-}
      ETHOCA_API_KEY_ID: ${ETHOCA_API_KEY_ID:-}
      ETHOCA_API_SECRET: ${ETHOCA_API_SECRET:-}
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - ./backend/src/data:/app/data

  frontend:
    build:
      context: ./frontend/artifacts/dispute-dashboard
      dockerfile: Dockerfile.frontend
    ports:
      - "5173:5173"
    environment:
      SPRINGBOOT_URL: http://backend:5000
      PORT: "5173"
      BASE_PATH: /
      NODE_ENV: development
    depends_on:
      - backend

volumes:
  pgdata:
```

### 11.2 Create the Frontend Dockerfile

Create `frontend/artifacts/dispute-dashboard/Dockerfile.frontend`:

```dockerfile
FROM node:20-slim
WORKDIR /app
RUN npm install -g pnpm@9

COPY package.json ./
RUN pnpm install

COPY . .

ENV PORT=5173
ENV BASE_PATH=/

EXPOSE 5173

CMD ["npx", "vite", "--config", "vite.config.ts", "--host", "0.0.0.0"]
```

### 11.3 Set API Keys

Create a `.env` file in the workspace root:

```bash
GEMINI_API_KEY=your-gemini-api-key-here
STRIPE_SECRET_KEY=your-stripe-secret-key-here
```

### 11.4 Run Everything

```bash
docker-compose up -d

# Check logs
docker-compose logs -f backend    # Backend logs
docker-compose logs -f frontend   # Frontend logs

# Test (use curl.exe on Windows PowerShell)
curl -s http://localhost:5000/api/disputes   # Backend directly
curl -s http://localhost:5173/api/disputes   # Through frontend proxy
```

Open http://localhost:5173/ in your browser.

### 11.5 Stopping

```bash
docker-compose down

# To also delete the database data:
docker-compose down -v
```

### 11.6 Rebuilding After Code Changes

```bash
# Rebuild backend only
docker-compose up -d --build backend

# Rebuild frontend only
docker-compose up -d --build frontend

# Rebuild everything
docker-compose up -d --build
```

---

## 12. How the Frontend Connects to the Backend

Understanding this connection is important for debugging.

### The Request Flow

```
Browser (http://localhost:5173)
    │
    │  All page navigation → Vite serves React SPA (HTML/JS/CSS)
    │  All /api/* requests → Handled by Vite proxy plugin
    │
    ▼
Vite Dev Server (port 5173)
    │
    ├── GET /api/disputes
    │   → Proxy fetches ALL disputes from backend
    │   → Normalizes statuses (ENRICHED→open, SECOND_PRESENTMENT_SUBMITTED→represented, etc.)
    │   → Hydrates enrichment % from evidence maps
    │   → Caches result for 30 seconds
    │   → Applies tab filter, search, sort, pagination
    │   → Returns { data, total, page, pageSize, totalPages }
    │
    ├── GET /api/disputes/stats
    │   → Computes totals, averages, counts from cached dispute list
    │   → Returns { totalDisputes, enriched, averageEnrichmentPercent, ... }
    │
    ├── GET /api/disputes/:id
    │   → Returns single dispute from cache
    │
    ├── GET/PUT /api/config/pipeline
    │   → Reads/writes pipeline-config.json (local file, not sent to backend)
    │
    ├── POST /api/agents/*
    │   → Forwards to backend with keep-alive heartbeats (every 10s)
    │   → Clears dispute cache on completion
    │
    └── Everything else (/api/*)
        → Proxied directly to backend (http://localhost:5000)
```

### Why Not Call the Backend Directly?

The frontend could call `http://localhost:5000/api/*` directly, but the proxy layer adds:

1. **Status normalization** — backend uses ~12 statuses, frontend uses 5
2. **Enrichment hydration** — fills in missing enrichment percentages
3. **Caching** — reduces backend load (especially for list views)
4. **Server-side pagination** — backend returns all disputes; proxy paginates
5. **Keep-alive** — prevents timeout on long agent calls (60-180 seconds)
6. **Stats computation** — aggregates computed from cached data
7. **Local config** — pipeline automation rules stored locally

### Key Environment Variable

The single most important config for the frontend-backend connection:

```bash
SPRINGBOOT_URL=http://localhost:5000
```

This tells the proxy where to forward `/api/*` requests. If this is wrong or the backend is down, you'll see "Backend server is temporarily unavailable" errors in the UI.

---

## 13. Configuration Reference

### Backend Configuration (`application.properties`)

#### Database

| Property | Default | Description |
|----------|---------|-------------|
| `PGHOST` | `localhost` | PostgreSQL hostname |
| `PGPORT` | `5432` | PostgreSQL port |
| `PGDATABASE` | `dispute_db` | Database name |
| `PGUSER` | `postgres` | Database username |
| `PGPASSWORD` | `password` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-creates/updates tables on startup |

#### Server

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `5000` | HTTP port the backend listens on |

#### Mastercard API

| Property | Description |
|----------|-------------|
| `mastercard.base-url` | Sandbox: `https://sandbox.api.mastercard.com` |
| `mastercard.consumer-key` | OAuth 1.0a consumer key |
| `mastercard.keystore-path` | Path to the `.p12` signing keystore |
| `mastercard.keystore-password` | Keystore password |

#### AI / External APIs

| Property | Env Variable | Description |
|----------|-------------|-------------|
| `gemini.api-key` | `GEMINI_API_KEY` | Google Gemini 2.5 Flash API key |
| `stripe.secret-key` | `STRIPE_SECRET_KEY` | Stripe API secret key |
| `ethoca.api-key-id` | `ETHOCA_API_KEY_ID` | Ethoca API key ID |
| `ethoca.api-secret` | `ETHOCA_API_SECRET` | Ethoca HMAC signing secret |

#### Urgency Settings

| Property | Default | Description |
|----------|---------|-------------|
| `urgency.buffer-days` | `0` | Buffer days added to due date calculation |
| `urgency.critical-days` | `2` | Days remaining to flag as critical |
| `urgency.warning-days` | `5` | Days remaining to flag as warning |

#### Ingestion

| Property | Default | Description |
|----------|---------|-------------|
| `ingestion.scheduled.enabled` | `false` | Enable/disable scheduled Mastercard polling |
| `ingestion.scheduled.interval-ms` | `3600000` | Polling interval (1 hour) |
| `ingestion.max-new-claims` | `3` | Max new disputes per ingestion run |
| `claim-detail.local-fallback.enabled` | `true` | Use local data when Mastercard API unavailable |

#### File Uploads

| Property | Default | Description |
|----------|---------|-------------|
| `spring.servlet.multipart.max-file-size` | `10MB` | Max single file upload size |
| `spring.servlet.multipart.max-request-size` | `50MB` | Max total request size (batch uploads) |

### Frontend Configuration (Environment Variables)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRINGBOOT_URL` | (hardcoded fallback URL) | Backend API URL — set to `http://localhost:5000` for local dev |
| `PORT` | Required | Port the Vite server binds to (use `5173` for dev) |
| `BASE_PATH` | Required | URL base path (use `/` for local dev) |
| `NODE_ENV` | `development` | Set to `production` for preview mode |

### Frontend Proxy Configuration (in `sb-proxy-plugin.ts`)

| Setting | Value | Description |
|---------|-------|-------------|
| Cache TTL | 30 seconds | How long the dispute list is cached |
| Keep-alive interval | 10 seconds | Heartbeat frequency for long agent calls |
| Proxy timeout | 180 seconds | Max time to wait for backend response |
| Fetch timeout | 60 seconds | Timeout for dispute list fetch |

---

## 14. Common Development Tasks

### Restarting After Backend Code Changes

```bash
# macOS
lsof -ti :5000 | xargs kill
cd backend && ./mvnw spring-boot:run

# Linux
fuser -k 5000/tcp
cd backend && ./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
Stop-Process -Id (Get-NetTCPConnection -LocalPort 5000).OwningProcess -Force
cd backend; mvnw.cmd spring-boot:run
```

The frontend does NOT need to restart when the backend changes — the proxy will automatically forward requests to the restarted backend.

### Restarting After Frontend Code Changes

In dev mode (vite dev), the frontend hot-reloads automatically — no restart needed.

For the proxy plugin (`sb-proxy-plugin.ts`), you need to restart the Vite dev server:

```bash
# macOS/Linux — stop with Ctrl+C, then restart:
SPRINGBOOT_URL=http://localhost:5000 PORT=5173 BASE_PATH="/" pnpm --filter @workspace/dispute-dashboard run dev
```

```powershell
# Windows (PowerShell) — stop with Ctrl+C, then restart:
$env:SPRINGBOOT_URL = "http://localhost:5000"
$env:PORT = "5173"
$env:BASE_PATH = "/"
pnpm --filter @workspace/dispute-dashboard run dev
```

### Reset the Database

```bash
psql -h localhost -U dispute_admin -d dispute_db -c "
  DROP SCHEMA public CASCADE;
  CREATE SCHEMA public;
  GRANT ALL ON SCHEMA public TO dispute_admin;
"

# Restart the backend — tables will be recreated automatically
cd backend && ./mvnw spring-boot:run
```

### View Database Tables

```bash
psql -h localhost -U dispute_admin -d dispute_db

# List all tables
\dt

# View disputes
SELECT id, claim_id, status, reason_code, amount FROM dispute LIMIT 10;

# View agent conversations
SELECT id, dispute_id, agent_type, created_at FROM agent_conversations ORDER BY created_at DESC LIMIT 10;

# View app settings
SELECT * FROM app_settings;

# Exit
\q
```

### Upload Evidence for a Dispute

```bash
# macOS/Linux — upload a single file
curl -X POST http://localhost:5000/api/agents/upload-evidence/{disputeId} \
  -F "file=@/path/to/receipt.pdf" \
  -F "category=merchant" \
  -F "evidenceName=purchase_receipt" \
  -F "reRunAgents=true"

# Upload multiple files at once
curl -X POST http://localhost:5000/api/agents/upload-evidence-batch/{disputeId} \
  -F "files=@/path/to/receipt.pdf" \
  -F "files=@/path/to/shipping_proof.jpg" \
  -F "category=merchant" \
  -F "reRunAgents=true"
```

```powershell
# Windows (PowerShell)
curl.exe -X POST http://localhost:5000/api/agents/upload-evidence/{disputeId} `
  -F "file=@C:\path\to\receipt.pdf" `
  -F "category=merchant" `
  -F "evidenceName=purchase_receipt" `
  -F "reRunAgents=true"
```

### Run the Full Agent Pipeline

```bash
# macOS/Linux — runs Agents 1 → 2 → 3a → 4 in sequence
curl -s -X POST http://localhost:5000/api/agents/full-pipeline/{disputeId} | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s -X POST http://localhost:5000/api/agents/full-pipeline/{disputeId} | python -m json.tool
```

### Trigger Manual Ingestion

Requires the Mastercard `.p12` keystore to be configured.

```bash
# macOS/Linux
curl -s -X POST http://localhost:5000/api/ingestion/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "queueName": "Pending",
    "lastModifiedDateFrom": "2026-03-28T00:00",
    "lastModifiedDateTo": "2026-04-08T12:00"
  }' | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s -X POST http://localhost:5000/api/ingestion/ingest `
  -H "Content-Type: application/json" `
  -d '{\"queueName\":\"Pending\",\"lastModifiedDateFrom\":\"2026-03-28T00:00\",\"lastModifiedDateTo\":\"2026-04-08T12:00\"}' | python -m json.tool
```

### Upload a Policy Document

```bash
# macOS/Linux
curl -X POST http://localhost:5000/api/agents/policy/merchant/upload \
  -F "file=@/path/to/merchant-policy.pdf"

curl -X POST http://localhost:5000/api/agents/policy/network/upload \
  -F "file=@/path/to/mastercard-rules.pdf" \
  -F "networkName=Mastercard"
```

```powershell
# Windows (PowerShell)
curl.exe -X POST http://localhost:5000/api/agents/policy/merchant/upload `
  -F "file=@C:\path\to\merchant-policy.pdf"

curl.exe -X POST http://localhost:5000/api/agents/policy/network/upload `
  -F "file=@C:\path\to\mastercard-rules.pdf" `
  -F "networkName=Mastercard"
```

### Change Backend Settings

```bash
# macOS/Linux
curl -s http://localhost:5000/api/config | python3 -m json.tool

curl -s -X PUT http://localhost:5000/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "pipeline.auto-enrich": "true",
    "agents.response-length": "short",
    "urgency.critical-days": "2",
    "urgency.warning-days": "5"
  }' | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s http://localhost:5000/api/config | python -m json.tool

curl.exe -s -X PUT http://localhost:5000/api/config `
  -H "Content-Type: application/json" `
  -d '{\"pipeline.auto-enrich\":\"true\",\"agents.response-length\":\"short\",\"urgency.critical-days\":\"2\",\"urgency.warning-days\":\"5\"}' | python -m json.tool
```

### Update Pipeline Automation (Frontend-Only Config)

```bash
# macOS/Linux
curl -s http://localhost:5173/api/config/pipeline | python3 -m json.tool

curl -s -X PUT http://localhost:5173/api/config/pipeline \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "rules": [
      {
        "id": "rule-1",
        "name": "Auto-enrich new disputes",
        "enabled": true,
        "conditions": { "status": "NEW" },
        "action": "enrich"
      }
    ]
  }' | python3 -m json.tool
```

```powershell
# Windows (PowerShell)
curl.exe -s http://localhost:5173/api/config/pipeline | python -m json.tool

curl.exe -s -X PUT http://localhost:5173/api/config/pipeline `
  -H "Content-Type: application/json" `
  -d '{\"enabled\":true,\"rules\":[{\"id\":\"rule-1\",\"name\":\"Auto-enrich new disputes\",\"enabled\":true,\"conditions\":{\"status\":\"NEW\"},\"action\":\"enrich\"}]}' | python -m json.tool
```

---

## 15. Troubleshooting

### "Port 5000 already in use" (Backend)

```bash
# macOS
lsof -i :5000
lsof -ti :5000 | xargs kill

# Linux
fuser 5000/tcp
fuser -k 5000/tcp
```

```powershell
# Windows (PowerShell)
netstat -ano | findstr :5000
Stop-Process -Id (Get-NetTCPConnection -LocalPort 5000).OwningProcess -Force
```

On macOS, AirPlay Receiver uses port 5000 by default. Disable it in **System Settings → General → AirDrop & Handoff → AirPlay Receiver**.

### "Port 5173 already in use" (Frontend)

```bash
# macOS
lsof -ti :5173 | xargs kill

# Linux
fuser -k 5173/tcp
```

```powershell
# Windows (PowerShell)
Stop-Process -Id (Get-NetTCPConnection -LocalPort 5173).OwningProcess -Force
```

### Frontend shows "Backend server is temporarily unavailable"

The proxy can't reach the backend. Check:

1. Is the backend running? Test with `curl -s http://localhost:5000/api/disputes` (or `curl.exe` on Windows)
2. Is `SPRINGBOOT_URL` set correctly? Should be `http://localhost:5000`
3. Did you change the fallback URL in `sb-proxy-plugin.ts`?

### Frontend loads but shows 0 disputes

1. Check the proxy warmup in the terminal: `[SB Proxy] Warmup complete: XX disputes`
2. If it says `Warmup failed`, the proxy can't reach the backend
3. Try: `curl -s http://localhost:5173/api/disputes` (or `curl.exe` on Windows) and check the response

### Agent calls hang or timeout

Agent calls (enrichment, rebuttal) can take 60-180 seconds. This is normal — the backend is calling the Gemini API and processing evidence. The proxy sends keep-alive heartbeats to prevent browser timeout.

If they fail entirely:
- Check `GEMINI_API_KEY` is set: `echo $GEMINI_API_KEY`
- Check agent status: `curl -s http://localhost:5000/api/agents/status`
- Large evidence sets may be truncated — the backend trims context to 800,000 characters before sending to Gemini

### "Connection refused" to PostgreSQL

```bash
# macOS/Linux
pg_isready -h localhost -p 5432

# macOS — start it
brew services start postgresql@16

# Linux — start it
sudo systemctl start postgresql

# Verify connection
psql -h localhost -U dispute_admin -d dispute_db -c "SELECT 1;"
```

```powershell
# Windows (PowerShell) — check if PostgreSQL service is running
Get-Service -Name "postgresql*"

# Start it if stopped
Start-Service -Name "postgresql-x64-16"

# Verify connection
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -U dispute_admin -d dispute_db -c "SELECT 1;"
```

### "FATAL: password authentication failed"

```bash
# macOS/Linux
sudo -u postgres psql -c "ALTER USER dispute_admin WITH PASSWORD 'localdevpassword';"

echo $PGUSER      # Should be: dispute_admin
echo $PGPASSWORD  # Should be: localdevpassword
```

```powershell
# Windows (PowerShell) — open psql as postgres user
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "ALTER USER dispute_admin WITH PASSWORD 'localdevpassword';"

echo $env:PGUSER      # Should be: dispute_admin
echo $env:PGPASSWORD  # Should be: localdevpassword
```

### "java: command not found" or wrong Java version

```bash
java -version

# macOS — set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 19)

# With SDKMAN:
sdk use java 19.0.2-graalce
```

```powershell
# Windows (PowerShell) — check and set JAVA_HOME
java -version
$env:JAVA_HOME = "C:\Program Files\Java\graalvm-jdk-19"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### Maven build fails

```bash
# macOS/Linux
./mvnw clean
chmod +x mvnw
./mvnw clean package -DskipTests
./mvnw dependency:purge-local-repository
./mvnw clean install -DskipTests
```

```powershell
# Windows (PowerShell)
mvnw.cmd clean
mvnw.cmd clean package -DskipTests
mvnw.cmd dependency:purge-local-repository
mvnw.cmd clean install -DskipTests
```

### Frontend won't install dependencies (pnpm errors)

If you get errors about missing workspace packages like `@workspace/api-client-react`:

```bash
# Option 1: Install from the workspace root
cd frontend && pnpm install

# Option 2: If workspace packages are missing, install just the dashboard
cd frontend/artifacts/dispute-dashboard
pnpm install --no-workspace
```

### Replit-specific plugin errors

If you see errors about `@replit/vite-plugin-runtime-error-modal` or `@replit/vite-plugin-cartographer`:

1. Remove those imports from `vite.config.ts` (see Step 8, Section 9.2)
2. Or uninstall them: `pnpm remove @replit/vite-plugin-runtime-error-modal @replit/vite-plugin-cartographer @replit/vite-plugin-dev-banner`

### "Keystore not found" warning

This is expected if you don't have the Mastercard `.p12` file. The warning is harmless — all non-Mastercard features work fine.

### Database tables not created

The backend uses `spring.jpa.hibernate.ddl-auto=update`, which auto-creates tables. If tables are missing:

```bash
# Check the user has CREATE TABLE permission
psql -h localhost -U dispute_admin -d dispute_db -c \
  "SELECT has_database_privilege('dispute_admin', 'dispute_db', 'CREATE');"

# Grant if needed
sudo -u postgres psql -c "ALTER DATABASE dispute_db OWNER TO dispute_admin;"
```

---

## 16. Quick Start (5 Minutes)

For experienced developers who just want both pieces running:

### macOS / Linux

```bash
# ── Terminal 1: Backend ──

cd backend

# Set API key
export GEMINI_API_KEY="your-key-here"
export PGUSER="dispute_admin"
export PGPASSWORD="localdevpassword"

# Create database (first time only)
psql postgres -c "CREATE USER dispute_admin WITH PASSWORD 'localdevpassword';" 2>/dev/null
psql postgres -c "CREATE DATABASE dispute_db OWNER dispute_admin;" 2>/dev/null

# Run backend
chmod +x mvnw && ./mvnw spring-boot:run

# ── Terminal 2: Frontend ──

cd frontend

# Install deps (first time only)
pnpm install

# Run frontend pointed at local backend
SPRINGBOOT_URL=http://localhost:5000 PORT=5173 BASE_PATH="/" \
  pnpm --filter @workspace/dispute-dashboard run dev

# ── Verify ──
# Backend: curl http://localhost:5000/api/disputes
# Frontend: open http://localhost:5173 in your browser
# Proxy:    curl http://localhost:5173/api/disputes
```

### Windows (PowerShell)

```powershell
# ── Terminal 1: Backend ──

cd backend

# Set API key
$env:GEMINI_API_KEY = "your-key-here"
$env:PGUSER = "dispute_admin"
$env:PGPASSWORD = "localdevpassword"

# Create database (first time only — use pgAdmin or psql from PostgreSQL install)
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "CREATE USER dispute_admin WITH PASSWORD 'localdevpassword';"
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "CREATE DATABASE dispute_db OWNER dispute_admin;"

# Run backend
mvnw.cmd spring-boot:run

# ── Terminal 2: Frontend ──

cd frontend

# Install deps (first time only)
pnpm install

# Run frontend pointed at local backend
$env:SPRINGBOOT_URL = "http://localhost:5000"
$env:PORT = "5173"
$env:BASE_PATH = "/"
pnpm --filter @workspace/dispute-dashboard run dev

# ── Verify ──
# Backend: curl.exe http://localhost:5000/api/disputes
# Frontend: open http://localhost:5173 in your browser
# Proxy:    curl.exe http://localhost:5173/api/disputes
```
