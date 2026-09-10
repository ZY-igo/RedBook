# RedBook

A social media Android application inspired by Xiaohongshu (小红书), featuring content discovery, user interactions, and multimedia publishing capabilities.

## Features

### Core Features
- **Home Feed** - Dual-tab content discovery with "Following" and "For You" sections
- **Search** - Full-text search with history, suggestions, and results
- **Content Publishing** - Create and publish text notes and long-form content with images
- **Note Details** - Rich media viewing with image zoom and video playback
- **Messaging** - Notification and message management system
- **User Profile** - Personal profile management with editable information
- **Push Notifications** - Firebase Cloud Messaging integration

### Technical Highlights
- Image zoom and pan support via PhotoView
- Video playback with ExoPlayer and Media3
- Offline-first architecture with local caching
- Comprehensive logging system with file persistence
- Crash reporting and recovery mechanisms

## Tech Stack

### Framework & Language
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **UI Framework**: Jetpack Compose + View Binding hybrid

### Architecture
- **Pattern**: Clean Architecture + MVVM
- **Dependency Injection**: Hilt
- **State Management**: StateFlow + ViewModel

### Key Libraries
| Category | Library | Version |
|----------|---------|---------|
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Local Database | Room | 2.7.0 |
| Image Loading | Coil | 2.7.0 |
| Video Playback | Media3 ExoPlayer | 1.10.1 |
| Image Zoom | PhotoView | 2.0.0 |
| Push Notification | Firebase Cloud Messaging | 25.0.1 |
| Async | Kotlin Coroutines | 1.10.2 |
| DI | Hilt | 2.56.2 |

## Project Structure

```
app/src/main/java/com/zhengyang/redbook/
├── data/                          # Data layer
│   ├── auth/                      # Authentication models and repository
│   ├── local/                     # Room database, DAOs
│   ├── mapper/                    # Data mappers between layers
│   ├── model/                     # Domain entities
│   ├── remote/                    # API services, interceptors, DTOs
│   └── repository/                # Repository implementations
├── di/                            # Hilt dependency injection modules
├── media/                         # Media playback management
├── push/                          # Firebase push notification handling
├── service/                       # Background services
├── ui/                            # Presentation layer
│   ├── collect/                   # Collections feature
│   ├── common/                    # Shared UI components
│   ├── home/                      # Home feed feature
│   ├── message/                   # Messaging feature
│   ├── my/                        # Profile feature
│   ├── note/                      # Note detail feature
│   ├── placeholder/               # Placeholder pages
│   ├── publish/                   # Content publishing feature
│   ├── search/                    # Search feature
│   └── widget/                    # Custom UI widgets
├── usecase/                       # Business logic use cases
├── utils/                         # Utilities and extensions
├── worker/                        # WorkManager workers
├── MainActivity.kt               # Main entry activity
├── RedBookApplication.kt         # Application class
└── SplashActivity.kt              # Splash screen
```

### Layer Responsibilities

- **UI Layer** (`ui/`) - Activities, Fragments, ViewModels, Adapters. Handles UI rendering and user input.
- **Domain Layer** (`usecase/`) - Business logic encapsulated in use cases.
- **Data Layer** (`data/`) - Repository implementations, data sources, API services, local database.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
│  │Activity │  │Fragment │  │ ViewModel│  │ Adapter │        │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘        │
└───────┼────────────┼────────────┼────────────┼──────────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Use Case Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │LoadHomeFeed  │  │PublishNote   │  │SearchContent │       │
│  │UseCase      │  │UseCase       │  │UseCase       │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │HomeRepository│  │NoteRepository│ │UserRepository│       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
└─────────┼─────────────────┼─────────────────┼───────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data Source Layer                        │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │   Remote Data Source │  │   Local Data Source  │        │
│  │   (Retrofit + API)   │  │   (Room + Cache)     │        │
│  └──────────────────────┘  └──────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2024.1.1) or later
- JDK 17+
- Android SDK 36
- Gradle 9.4.1+

### Build

```bash
# Sync and build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run lint
./gradlew lint
```

### Environment Variables

You can configure the API base URL via Gradle properties or environment variables:

```bash
# Via Gradle property
./gradlew assembleDebug -PbaseApiUrl="http://your-api-server:8080/"

# Via environment variable
BASE_API_URL="http://your-api-server:8080/" ./gradlew assembleDebug
```

Default base URL is `http://10.0.2.2:8080/` (localhost for emulator).

### Firebase Configuration

Place your `google-services.json` file in `app/` directory to enable Firebase services (push notifications).

## Configuration

### API Configuration

- **Connect Timeout**: 15 seconds
- **Read Timeout**: 15 seconds
- **Write Timeout**: 15 seconds
- **Retry Policy**: 3 retries with exponential backoff

### Room Database

- Database name: `redbook_database`
- Schema location: `app/schemas/`
- Automatic migration support

### Logging

- Log files location: `files/app_logs/`
- Log format: `redbook-yyyy-MM-dd.log`
- Retention: 7 days
- Log levels: VERBOSE, DEBUG, INFO, WARN, ERROR

## Key Modules

### Home Module (`ui/home/`)

Implements the main content feed with:
- Tab-based navigation (Following / For You)
- Channel-based content organization
- Skeleton loading states
- Pull-to-refresh functionality

### Search Module (`ui/search/`)

Provides search capabilities:
- Real-time search suggestions
- Search history management
- Multiple content type results

### Publishing Module (`ui/publish/`)

Handles content creation:
- Text note publishing
- Long-form content with images
- Draft auto-save functionality

### Note Detail Module (`ui/note/`)

Displays note content:
- Multi-image gallery with zoom
- Video playback support
- Comment interaction

## License

This project is for educational and demonstration purposes.