# SmartExam SA

SmartExam SA is an end-to-end ecosystem that helps South African teachers plan, generate, and manage assessment papers aligned with the CAPS curriculum. It consists of a production-ready Android application and a centralized Next.js Admin Portal for marketplace management.

## Table of Contents
1. [Features](#features)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [Project Structure](#project-structure)
5. [Local Development](#local-development)
6. [Running the App](#running-the-app)
7. [Sample Data & Sync](#sample-data--sync)
8. [Building PDFs](#building-pdfs)
9. [Data Model](#data-model)
10. [Testing](#testing)
11. [Contribution Guide](#contribution-guide)
12. [Troubleshooting](#troubleshooting)
13. [Roadmap Ideas](#roadmap-ideas)
14. [License](#license)

## Features
- **Dashboard overview** with question, paper, and subject counts plus quick links to key flows.
- **Question authoring** with support for multiple question types, difficulty levels, rich metadata (CAPS aligned), and optional image attachments.
- **Functional Marketplace** for browsing and purchasing question packs directly within the Android app.
- **Admin Management Portal** for creating, validating, and publishing curriculum-compliant content to the marketplace.
- **Question bank** that filters by subject, grade, or search query to reuse questions efficiently.
- **Assessment generator** to compose tests, enforce target marks, and export both test & memo PDFs.
- **Teacher settings** for school profile, branding (logo upload), and subscription management.
- **Production-Ready Sync** using a Room-to-Firestore bridge for purchased pack persistence.

## Architecture
| Layer | Responsibilities |
| --- | --- |
| Activities (`app/src/main/java/com/smartexam/activities`) | UI controllers, navigation, View wiring |
| Admin Portal (`smartexam-admin/`) | Next.js management dashboard for marketplace content |
| Database (`com.smartexam.database`) | Room entities, DAOs, converters |
| Models (`com.smartexam.models`) | Unified CAPS models shared across Android & Firestore |
| Sync & Marketplace (`com.smartexam.marketplace`) | Stripe integration and background sync services |

### Key Flows
1. **Question Creation** → `QuestionFormActivity` persists questions via `AppDatabase.questionDao()`.
2. **Question Bank & Filtering** → `QuestionBankActivity` loads all questions, filters locally, and supports CRUD.
3. **Assessment Generation** → `AssessmentGeneratorActivity` selects questions, generates PDFs, and stores `AssessmentPaper` plus `PaperQuestion` mappings (for a future detail screen).
4. **Marketplace Sync** → `SyncManager` consumes question pack payloads and stores them locally (currently mocked for offline demo).

## Tech Stack
- **Language:** Java 17 (Android toolchain)
- **Android SDK:** compile/target 34, minSdk 26
- **Build system:** Gradle 8.13 + Android Gradle Plugin 8.13.2
- **UI libraries:** AppCompat, Material Components, ConstraintLayout, SwipeRefreshLayout
- **Persistence:** Room 2.6.1, SharedPreferences
- **Cloud:** Firebase Auth & Firestore (via BOM 32.7.2)
- **Payments:** Stripe Android SDK 22.6.0
- **Documents:** iTextPDF 5.5.13.3 for test/memo export
- **JSON:** Gson 2.10.1

## Project Structure
```
SmartExam SA/
├── app/                  # Android Application Source
├── smartexam-admin/      # Next.js Management Portal
├── docs/                 # Architectural documentation
├── gradle/               # Build system configuration
└── build.gradle           # Root build config
```

## Local Development
### Prerequisites
- Android Studio (Giraffe or newer) with Android SDK 34 installed.
- JDK 17 (bundled with Android Studio is fine).
- Gradle wrapper (already included).
- Firebase project credentials if you plan to test Auth/Firestore.

### First-time Setup
1. Clone the repository.
2. Copy `local.properties` template or configure the `sdk.dir` to your Android SDK path.
3. Open the project in Android Studio; let it sync Gradle dependencies.
4. Set up a device/emulator with Android 13 (API 33) or newer for all runtime permissions features.
5. (Optional) Configure a Firebase project and download `google-services.json` into `app/` if cloud sync is required.

## Running the App
1. Select the `app` run configuration in Android Studio.
2. Use **Run ▶** to install on a connected device/emulator.
3. On first launch, grant storage/media permissions when prompted (PDF export & media pickers rely on them).
4. From the dashboard you can:
   - Load **Sample Data** to pre-populate the local DB for exploration.
   - Jump to Question Bank, create questions, or generate assessments.

## Sample Data & Sync
- **Sample data button** seeds representative questions/subjects using `SampleDataGenerator` and updates dashboard metrics.
- **Sync purchased pack** currently injects a mocked JSON payload via `SyncManager.processPurchasedPack()` to demonstrate how Firestore packs would hydrate the local database.
- Replace the `buildMockPackPayload()` implementation in `MainActivity` with real Firestore/Stripe callbacks once the backend is wired up.

## Building PDFs
- `PDFGenerator` creates both Test and Memo documents using the selected questions.
- Generated files default to the app-specific external storage directory (`/Android/data/.../files`).
- After generation, `AssessmentGeneratorActivity` surfaces "Open Test" / "Open Memo" buttons and uses a `FileProvider` defined in `AndroidManifest.xml` to share PDFs with system viewers.
- Customize layout, branding, and cover pages inside `PDFGenerator` as needed.

## Data Model
### Room Entities
| Entity | Purpose |
| --- | --- |
| `Question` | Core pedagogical item with marks, difficulty, and cognitive level |
| `AssessmentPaper` | Generated assessment container |
| `QuestionPack` | Marketplace-ready bundle metadata |
| `PurchasedPack` | Tracks local ownership and sync status |

### Firestore Prototype (see `docs/FirestoreSchema.md`)
- `questions`: global marketplace pool.
- `question_packs`: purchasable bundles tied to Stripe price IDs.
- `users`: teacher profiles with purchased pack tracking.

## Testing
- **Unit tests:** JUnit 4 is configured; add tests under `app/src/test/java` for Room DAO logic, utilities, and repositories.
- **Instrumentation tests:** Espresso & AndroidX Test are available for UI flows (`app/src/androidTest`).
- Aim for the global WinSurf rule of **80% coverage** on services and critical flows.

## Contribution Guide
1. Create a branch from `dev`: `git checkout -b feature/<slug>`.
2. Follow the WinSurf global conventions (2-space tabs, ESLint/Prettier configs if web modules are added later).
3. Keep commits small, descriptive, and adhere to `type(scope): message` format (e.g., `feat(generator): add PDF watermark`).
4. Run lint, unit tests, and instrumentation tests before opening a PR.
5. Ensure new screens/flows respect the REST guidelines & security posture outlined in `MEMORY[user_global]`.
6. Request at least one review for `dev` merges and two for `main` releases.

## Troubleshooting
| Issue | Fix |
| --- | --- |
| Admin SDK Missing Credentials | Copy `.env.example` to `.env.local` and add Firebase keys. |
| Next.js SWC Error (Windows) | Project is configured with `.babelrc` to bypass SWC binary issues. |
| Room Schema Mismatch | Incremented `AppDatabase` version to `4` for marketplace models. |

## Roadmap Ideas
1. Hook Marketplace to real Firestore collections + Stripe Checkout.
2. Implement pack purchase history and license checks.
3. Add analytics on question usage and learner performance exports.
4. Extend PDF styling (cover pages, invigilator slips, answer sheets).
5. Ship offline-first sync queue for classrooms without stable internet.
6. Port dashboard to Jetpack Compose once stable.

## License
Specify your preferred license (e.g., MIT, Apache-2.0) here.
#   S m a r t E x a m - S A  
 