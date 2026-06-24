# Acadex 🎓

Acadex is an **Academic Operating System** designed for students to manage notes, track assignments, schedule study goals, monitor upcoming exams, and access shared academic resources. 

Built as a mobile-first, production-ready Android application, Acadex utilizes modern Android development guidelines, combining clean architecture layers with premium Material 3 design and real-time offline-first sync.

---

## 🛠️ Tech Stack & Library System

- **Core**: 100% [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design System**: [Material 3](https://m3.material.io/) with custom glassmorphism components
- **Architecture**: MVVM + Clean Architecture principles
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Local Database**: [Room Database](https://developer.android.com/training/data-storage/room) (with offline-first caching)
- **Remote Integration & Cloud Services**:
  - **Firebase Authentication**: User session management and persistence
  - **Cloud Firestore**: Real-time multi-device cloud data synchronization
  - **Firebase Storage**: PDF uploads and profile pictures
  - **Firebase Cloud Messaging (FCM)**: Deadline warnings and schedule alerts
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/) (for quotes and notifications)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (for profile avatars)
- **Asynchrony**: Kotlin Coroutines & `StateFlow`

---

## 🏛️ Application Architecture

Acadex is structured using **Clean Architecture** guidelines, splitting the codebase into three main layers to guarantee testability and decoupling:

```
app/src/main/java/com/acadex/app/
│
├── domain/                  # Business Logic Layer (Pure Kotlin)
│   ├── model/               # Domain Models (User, Note, Assignment, PlannerTask, Exam, Resource)
│   ├── repository/          # Repository contracts / interfaces
│   └── usecase/             # CleanUseCase containers (Auth, Notes, Assignment, Planner, Exam, Resource)
│
├── data/                    # Frameworks, Storage, and Implementations
│   ├── local/               # Room DB schema, Entity definition, NoteDao cache
│   ├── remote/              # Retrofit API interface, Firebase wrapper, FCM Messaging Service
│   ├── mapper/              # Data Entity -> Domain model mappers
│   └── repository/          # Implementation of Domain Repository contracts
│
└── presentation/            # User Interface Layer (Jetpack Compose)
    ├── theme/               # Material 3 colors, shapes, typography scales
    ├── components/          # Reusable components (GlassyCard, EmptyState, LoadingState)
    ├── navigation/          # Compose Navigation Graph controller and routes
    ├── auth/                # Sign In, SignUp, and recovery ViewModels & Screens
    ├── home/                # Stats, announcement sliders, quick-actions ViewModels & Screens
    ├── notes/               # Study notes, favoriting lists, detail sheets, and attachments editors
    ├── planner/             # Weekly calendar scheduler, task checklists, and assignment editors
    ├── resources/           # Category chips and download buttons
    └── profile/             # Preferences toggles (Dark Mode, Notifications) and profile updates
```

---

## 🎨 Design System Guidelines

- **Primary Color**: `#4F46E5` (Indigo)
- **Secondary Color**: `#8B5CF6` (Violet)
- **Accent Color**: `#38BDF8` (Sky)
- **Gradient**: Indigo → Violet → Sky Gradient (used in login, landing, and headers)
- **Light Theme Background**: `#FAFAFC`
- **Dark Theme Background**: `#0F172A`
- **Widgets**: Acrylic Glassmorphism borders and cards (`GlassyCard`) with a default corner radius of `20dp`.

---

## ⚡ Setup & Build Instructions

### Prerequisites
- **Java**: JDK 17 installed
- **Android SDK**: Install SDK Platform 34 (Android 14) and Android SDK Build-Tools 34.0.0

### Firebase Configuration
The repository includes a template config file `app/google-services.json` to allow the compiler to build the application. To connect the application to your real Firebase project:
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a project and register a new Android application with the package name: `com.acadex.app`.
3. Download the `google-services.json` config file.
4. Replace the template file at `app/google-services.json` with the downloaded file.
5. In Firebase, enable:
   - **Authentication** (Email/Password Sign-In provider)
   - **Cloud Firestore Database** (Start in test mode or write security rules)
   - **Cloud Storage** (For document uploads)

### Compilation
To compile the application and build the debug APK:
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

### Running Unit Tests
To run usecase business tests and verify the code:
```bash
# Windows
.\gradlew.bat testDebugUnitTest

# macOS/Linux
./gradlew testDebugUnitTest
```
