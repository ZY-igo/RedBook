# Contributing to RedBook

Thank you for your interest in contributing to RedBook!

## Development Setup

1. Clone the repository
2. Open the project in Android Studio
3. Ensure you have the following SDKs installed:
   - Android SDK 36
   - Build Tools 36.x.x
4. Copy `google-services.json` to `app/` directory if you need Firebase features
5. Sync Gradle and build the project

## Coding Standards

### Kotlin Style Guide

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful names for variables and functions
- Add KDoc comments for public APIs and complex logic

### Architecture

- Follow Clean Architecture principles
- Separate concerns: UI → UseCase → Repository → DataSource
- Use dependency injection for all dependencies
- Prefer composition over inheritance

### Code Organization

```
ui/          # Activities, Fragments, ViewModels, Adapters
usecase/     # Business logic
data/        # Repositories, DataSources, Models
di/          # Dependency Injection modules
```

## Pull Request Process

1. Fork the repository and create your branch from `main`
2. If you've added code that should be tested, add tests
3. Ensure the build passes with `./gradlew assembleDebug`
4. Update documentation if you've changed significant functionality
5. Your PR will be reviewed by maintainers

## Bug Reports

Please include:
- A clear description of the bug
- Steps to reproduce
- Expected vs actual behavior
- Device/Android version if applicable
- Logcat output if available

## Feature Requests

Open an issue with:
- Clear description of the feature
- Use case justification
- Any mockups or examples if applicable