# Notewise Clone

A production-grade, local-first Android note-taking application inspired by Notewise, featuring hardware-accelerated low-latency vector inking, Bézier curve smoothing, templates, multi-layer canvas rendering, PDF import/export, and a cloud synchronization backend powered by **Fastify**, **Prisma ORM**, and **PostgreSQL**.

---

## Architecture Overview

```
notewise-clone/
├── backend/                  # Cloud Sync Server
│   ├── prisma/
│   │   └── schema.prisma     # PostgreSQL schema (Users, Notebooks, Pages, Devices)
│   ├── src/
│   │   ├── routes/
│   │   │   ├── auth.ts       # Authentication (Register, Login, JWT)
│   │   │   ├── sync.ts       # Delta Sync Engine (Push/Pull with conflict resolution)
│   │   │   └── notebooks.ts  # Notebook CRUD
│   │   ├── prisma.ts         # Prisma client singleton
│   │   └── server.ts         # Fastify application entrypoint
│   └── package.json
│
└── android/                  # Android Native App (Kotlin + Compose)
    ├── app/
    │   ├── src/main/java/com/notewise/clone/
    │   │   ├── core/
    │   │   │   ├── canvas/   # Vector Drawing Engine (Bézier curves, Pen, Highlighter, Eraser)
    │   │   │   ├── database/ # Local Room SQLite database (0ms latency local cache)
    │   │   │   ├── template/ # Page background templates (Ruled, Grid, Dot, Cornell)
    │   │   │   ├── pdf/      # Android PDF vector exporter & renderer
    │   │   │   └── sync/     # Background SyncManager pushing deltas to Prisma
    │   │   └── ui/
    │   │       ├── home/     # Notebook shelf dashboard & search
    │   │       ├── editor/   # Canvas view with floating toolbox & color palette
    │   │       └── theme/    # Material 3 theme & color schemes
    │   └── build.gradle.kts
    └── settings.gradle.kts
```

---

## 1. Quick Start: Cloud Backend (PostgreSQL + Prisma)

### Prerequisites
- Node.js 20+
- PostgreSQL database

### Setup & Run
1. Navigate to the backend directory:
   ```bash
   cd notewise-clone/backend
   ```
2. Configure `.env` with your PostgreSQL connection string:
   ```env
   DATABASE_URL="postgresql://postgres:password@localhost:5432/notewise_db?schema=public"
   JWT_SECRET="notewise_super_secret_jwt_key"
   PORT=3000
   ```
3. Run database migrations and generate the Prisma Client:
   ```bash
   npx prisma migrate dev --name init
   npx prisma generate
   ```
4. Start the development server:
   ```bash
   npm run dev
   ```
   The sync server will be active at `http://localhost:3000`.

---

## 2. Quick Start: Android Application

### Prerequisites
- Android Studio Ladybug / Meerkat or Android SDK 35+
- Kotlin 2.0+

### Key Features Implemented:
* **Vector Drawing Engine:** Zero-latency drawing canvas with quadratic Bézier smoothing and pressure sensitivity for styluses and fingers.
* **Dual-Layer Rendering:** Highlighters blend beneath pen strokes using `PorterDuff.Mode.MULTIPLY`.
* **Templates:** Ruled lines, Grid, Dot Grid, Cornell Notes, and Blank canvas.
* **Undo/Redo History:** Stack-based memory-bounded action history.
* **Local Persistence:** Room SQLite database saves strokes instantly with 0ms lag.
* **Cloud Sync:** `SyncManager` pushes local modifications and pulls remote updates from the Prisma backend.
* **PDF Exporter:** High-resolution multi-page PDF generation via `android.graphics.pdf.PdfDocument`.

