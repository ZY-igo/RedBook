# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-16

### Added

- **Home Feed System**
  - Dual-tab discovery interface (Following / For You)
  - Channel-based content organization with category tabs
  - Skeleton loading states for improved perceived performance
  - Pull-to-refresh functionality

- **Search Module**
  - Real-time search suggestions
  - Search history with chip-based display
  - Guess-based recommendations

- **Content Publishing**
  - Text note creation (`PublishTextActivity`)
  - Long-form content publishing (`PublishLongFormActivity`)
  - Image attachment support
  - Draft auto-save functionality

- **Note Detail Page**
  - Multi-image gallery with `PhotoView` zoom support
  - Video playback via Media3 ExoPlayer
  - Comment interaction UI
  - Payload-based efficient item refresh

- **User Profile**
  - Profile viewing and editing (`EditProfileActivity`)
  - Avatar preview with full-screen zoom (`ImagePreviewActivity`)
  - Personal information management

- **Messaging System**
  - Message notification list
  - Conversation management

- **Logging System**
  - File-based log persistence (`LogFileStore`)
  - Log level configuration
  - 7-day log retention with automatic cleanup
  - Crash information serialization

### Changed

- **Image Handling**
  - Integrated PhotoView 2.0.0 for pinch-to-zoom functionality
  - Smooth transition animations for full-screen mode
  - Efficient RecyclerView item refresh with payloads

- **Network Layer**
  - Enhanced logging interceptor with file output
  - Improved error handling and retry mechanisms

### Fixed

- Image preview gesture conflicts with parent horizontal paging
- Full-screen transition visual artifacts

## [0.1.0] - 2026-06-01

### Added

- Initial project setup
- Basic Clean Architecture structure
- Hilt dependency injection configuration
- Room database setup with initial entities
- Retrofit networking infrastructure
- Core UI components foundation