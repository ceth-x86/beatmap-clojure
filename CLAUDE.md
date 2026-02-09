# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Clojure application that integrates with the Apple Music API to export user's music library data to CSV format. The application supports exporting albums and playlists from Apple Music. It uses secure token management and supports both development and production environments.

## Common Development Commands

### Running the Application
```bash
# Setup configuration (creates config/local.edn from example)
make setup-config

# Run with default greeting
make run

# Export albums to CSV (Apple Music)
make run-cmd CMD=albums

# Export playlists and tracks from editable playlists (Apple Music)
make run-cmd CMD=playlists

# Generate derived data from existing files
make run-cmd CMD="generate artists"

# Show help
make run-cmd CMD=help
```

### Testing and Building
```bash
# Run all tests
make test
# or
clojure -T:build test

# Build uberjar
make build
# or
clojure -T:build ci

# Run built JAR
make run-jar
```

### Development Tools
```bash
# Start REPL
make repl

# Format code (if cljfmt is installed)
make format

# Lint code (if clj-kondo is installed)
make lint

# Check token configuration
make check-tokens

# Show current configuration (without secrets)
make show-config

# Clean build artifacts
make clean
```

## Code Architecture

### Core Architecture
- **Entry Point**: `src/beatmap/beatmap.clj` - Main application entry with CLI command handling
- **Configuration**: Layered config system using `resources/config.edn` + `config/local.edn` + environment variables
- **Token Management**: Secure token validation and management via `src/beatmap/tokens.clj`

### Module Organization
```
src/beatmap/
├── beatmap.clj          # Main entry point and CLI commands
├── config.clj           # Configuration loading and merging
├── tokens.clj           # Token validation and management
├── entities.clj         # Domain entities and sorting logic
├── operations.clj       # High-level processing operations
├── generate.clj         # Generate command handlers
├── enrich.clj           # Enrich command handlers
├── apple_music/         # Apple Music API integration
│   ├── core.clj         # Core API request logic with retry handling
│   ├── albums.clj       # Album-specific API functions
│   └── playlists.clj    # Playlist-specific API functions
└── csv/                 # CSV export functionality
    ├── utils.clj        # Common CSV utilities
    ├── albums.clj       # Album CSV export
    ├── playlists.clj    # Playlist CSV export
    ├── tracks.clj       # Track CSV export
    └── artists.clj      # Artist CSV export
```

### Key Design Patterns
- **API Layer**: `apple_music/core.clj` provides a robust HTTP client with automatic retries and proper error handling
- **Separation of Concerns**: API calls, business logic, and CSV export are in separate namespaces
- **Configuration Management**: Multi-layer config system supports local development and production deployment
- **Error Handling**: Comprehensive error handling with user-friendly messages and retry logic

## Configuration Requirements

The application requires API tokens configured in `config/local.edn`:
```clojure
{:secrets
 {:developer-token "your-apple-music-developer-token"  ; Required for Apple Music
  :user-token "your-apple-music-user-token"            ; Required for Apple Music
  :openai-api-key "your-openai-api-key"}}              ; Optional, for enrichment
```

Use `make setup-config` to create the initial configuration file from the example.

## Testing

- Comprehensive test coverage for all core modules in `test/` directory
- Tests use the `:test` alias which includes test.check for property-based testing
- Run tests with `make test` or `clojure -T:build test`

## Build System

- Uses `tools.build` for building (see `build.clj`)
- Uses `deps.edn` for dependency management
- Makefile provides convenient commands for common tasks
- Builds to `target/beatmap-0.1.0-SNAPSHOT.jar`