# SmartExam SA

SmartExam SA is an end-to-end ecosystem that helps South African teachers plan, generate, and manage assessment papers aligned with the CAPS curriculum. It consists of a production-ready Android application and a centralized Next.js Admin Portal for marketplace management.

## 🚀 Current Status: **Production Ready**
✅ **Firebase Integration Complete** - Real-time sync, authentication, and comprehensive testing suite
✅ **Payment Infrastructure Ready** - Stripe SDK integrated, Paystack architecture documented
✅ **Offline-First Architecture** - Room database with Firestore sync bridge
✅ **CAPS-Aligned Content System** - Complete question authoring and assessment generation

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
- **Subscription-based Marketplace** for browsing and downloading premium question packs with individual teacher subscriptions (R50/month).
- **Admin Management Portal** for creating, validating, and publishing curriculum-compliant content to the marketplace.
- **Question bank** that filters by subject, grade, or search query to reuse questions efficiently.
- **Assessment generator** to compose tests, enforce target marks, and export both test & memo PDFs.
- **Teacher settings** for school profile, branding (logo upload), and subscription management.
- **Production-Ready Firebase Sync** using real-time Firestore integration with comprehensive testing suite.
- **Firebase Connection Testing** with dedicated test activity for connectivity verification and debugging.

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
4. **Firebase Sync** → `SyncService` provides real-time Firestore integration with comprehensive test coverage via `FirebaseConnectionTestActivity`.
5. **Marketplace Integration** → Ready for Paystack payment processing with documented backend architecture.

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
   - Test **Firebase Connection** with quick connectivity verification.
   - Run **Firebase Full Test Suite** for comprehensive integration testing.

## Sample Data & Firebase Integration
- **Sample data button** seeds representative questions/subjects using `SampleDataGenerator` and updates dashboard metrics.
- **Firebase Connection Testing**: 
  - Quick test button in MainActivity for basic connectivity verification
  - Comprehensive test suite via `FirebaseConnectionTestActivity` covering:
    - Anonymous authentication
    - Firestore read/write operations
    - Real-time data synchronization
    - Error handling and logging
- **Real-time Sync**: `SyncService` provides production-ready Firestore integration with automatic local database updates.
- **Firebase Project**: Connected to `smartexam-sa` with collections for users, question_packs, purchased_packs, and test data.

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
- `subscriptions`: individual teacher subscription status and billing information.
- `transactions`: payment records and receipts for subscription billing.

## Testing
- **Unit tests:** JUnit 4 is configured; add tests under `app/src/test/java` for Room DAO logic, utilities, and repositories.
- **Instrumentation tests:** Espresso & AndroidX Test are available for UI flows (`app/src/androidTest`).
- **Firebase Integration Testing:** 
  - `FirebaseConnectionTestActivity` provides comprehensive test coverage for:
    - Authentication flows (anonymous sign-in)
    - Firestore connectivity and operations
    - Data persistence and synchronization
    - Error handling and network resilience
  - Quick connectivity test available directly from MainActivity
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
1. **Paystack Payment Integration** - Complete individual teacher subscription billing (R50/month) using documented backend architecture.
2. **Production Marketplace Launch** - Hook Marketplace to real Firestore collections + Paystack Checkout.
3. **Advanced Features**:
   - Pack purchase history and license checks
   - Analytics on question usage and learner performance exports
   - Extended PDF styling (cover pages, invigilator slips, answer sheets)
   - Offline-first sync queue for classrooms without stable internet
   - Jetpack Compose migration for modern UI
4. **Admin Portal Enhancement** - Complete Next.js management dashboard with content validation tools.

## License
MIT License - Feel free to use this project for educational and commercial purposes.

---

## 📱 Quick Start Guide

### For Immediate Testing
1. **Open Android Studio** and run the app
2. **Test Firebase Connection** - Tap the button on MainActivity for quick verification
3. **Run Full Test Suite** - Access comprehensive Firebase integration tests
4. **Explore Features** - Load sample data and test question authoring/assessment generation

### Firebase Console Access
- **Project ID**: `smartexam-sa`
- **Console**: https://console.firebase.google.com/project/smartexam-sa
- **Collections**: `connection_tests`, `sync_tests`, `users`, `question_packs`

### Documentation
- **Firestore Schema**: `docs/FirestoreSchema.md`
- **Paystack Integration**: `docs/PaystackBackendIntegration.md`
- **Subscription Architecture**: `docs/SubscriptionArchitectureSummary.md`