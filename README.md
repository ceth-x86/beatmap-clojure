# beatmap

A Clojure application for interacting with Apple Music API, managing tokens securely, and exporting user albums to CSV format.

## Features

- **Apple Music Integration**: Fetch user albums and playlists from Apple Music library
- **Secure Token Management**: Layered configuration with environment variables
- **CSV Export**: Export albums and playlists to CSV format
- **Smart Playlist Separation**: Automatically separate editable and non-editable playlists
- **Case-Insensitive Sorting**: Albums and playlists are sorted alphabetically
- **Progress Tracking**: Real-time progress display during data fetching
- **Modular Architecture**: Clean separation of concerns with dedicated modules
- **Modular architecture**: API, CSV export, and business logic are separated into dedicated namespaces
- **Comprehensive unit test coverage for all core modules (see `test/` directory)**

## Project Structure

```
src/beatmap/
├── beatmap.clj                    # Main application entry point
├── apple_music/
│   ├── core.clj                   # Core API request logic and tokens
│   ├── albums.clj                 # Album-related API functions
│   └── playlists.clj              # Playlist-related API functions
├── csv_export/
│   ├── utils.clj                  # Common CSV utilities
│   ├── albums.clj                 # Album CSV export functionality
│   └── playlists.clj              # Playlist CSV export functionality
├── config.clj                     # Configuration management
├── tokens.clj                     # Token validation and management
├── entities.clj                   # Domain entities and sorting logic
└── operations.clj                 # High-level processing operations
```

## Installation

Download from https://github.com/beatmap/beatmap

## Usage

### Quick Start

1. **Setup configuration:**
   ```bash
   make setup-config
   ```

2. **Run the application:**
   ```bash
   make run
   ```

The application will:
- Validate your Apple Music tokens
- Fetch all albums from your library (with pagination)
- Save them to `resources/catalog/albums.csv` with columns: Artist, Year, Album (sorted case-insensitively)
- Display sample albums in the console

### Available Commands

```bash
# Export albums to CSV
make run-cmd CMD=albums
# or
clojure -M -m beatmap.beatmap albums

# Export playlists AND tracks from editable playlists
make run-cmd CMD=playlists
# or
clojure -M -m beatmap.beatmap playlists

# This command will:
# - Export all playlists to two CSV files (editable and non-editable)
# - Export tracks from all editable playlists to separate CSV files (one per playlist)

# Generate derived data from existing files
make run-cmd CMD="generate artists"
# or
clojure -M -m beatmap.beatmap generate artists

# Show help
make run-cmd CMD=help
# or
clojure -M -m beatmap.beatmap help
```

### Configuration

For detailed configuration instructions, see [Configuration Guide](doc/configuration.md).

**Quick setup:**
- Copy `config/local.edn.example` to `config/local.edn`
- Edit `config/local.edn` with your actual tokens and secrets
- Optionally create `.env` file from `env.example` for environment variables

### Tokens

The application uses three tokens:
- `developer-token` - Developer authentication token
- `user-token` - User authentication token
- `openai-api-key` - OpenAI API key

For detailed token usage, see [Tokens Usage Guide](doc/tokens-usage.md).

### Running the Application

Run the application with default greeting:
```bash
make run
# or
clojure -X:run-x
```

Run with specific command:
```bash
make run-cmd CMD=albums      # Export albums
make run-cmd CMD=playlists   # Export playlists and tracks
make run-cmd CMD=help        # Show help
```

Setup configuration:
```bash
make setup-config
```

Run tests:
```bash
make test
# or
clojure -T:build test
```

Build uberjar:
```bash
make build
# or
clojure -T:build ci
java -jar target/beatmap-0.1.0-SNAPSHOT.jar
```

## Examples

### Sample Output

When you run the application, you'll see output like:

**For albums:**
```
✅ All tokens are configured
🎵 Fetching albums from your Apple Music library...
📄 Fetching albums page 1 (offset: 0)...
✅ Page 1 loaded: 25 albums
📊 Total albums so far: 25
...
💾 Successfully saved albums to: resources/catalog/albums.csv
📊 Sample of your albums:
   1. IDLES (2019) - A Beautiful Thing: IDLES Live at Le Bataclan
   2. Depeche Mode (1982) - A Broken Frame (Deluxe)
   3. Biffy Clyro (2020) - A Celebration of Endings
```

**For playlists:**
```
✅ All tokens are configured
🎵 Fetching playlists from your Apple Music library...
📄 Fetching playlists page 1 (offset: 0)...
✅ Page 1 loaded: 25 playlists
📊 Total playlists so far: 25
...
✅ Written 54 playlists to resources/catalog/playlists_personal.csv
✅ Written 45 playlists to resources/catalog/playlists_apple_music.csv
📊 Summary:
   Editable playlists: 54 -> resources/catalog/playlists_personal.csv
   Non-editable playlists: 45 -> resources/catalog/playlists_apple_music.csv
💾 Successfully saved playlists to separate files
```

### Output Formats

#### CSV Output Format (Albums)

The generated `resources/catalog/albums.csv` file contains:
- **Artist**: Artist name
- **Year**: Release year (or "Unknown" if not available)
- **Album**: Album name

#### CSV Output Format (Playlists)

The application generates two separate CSV files:

**`resources/catalog/playlists_personal.csv`** - Editable playlists (canEdit: true):
- **Name**: Playlist name
- **Track Count**: Number of tracks
- **Curator**: Playlist curator (if available)
- **Description**: Playlist description (if available)
- **Last Modified**: Last modification date (if available)

**`resources/catalog/playlists_apple_music.csv`** - Non-editable playlists (canEdit: false):
- **Name**: Playlist name
- **Track Count**: Number of tracks
- **Curator**: Playlist curator (if available)
- **Description**: Playlist description (if available)
- **Last Modified**: Last modification date (if available)

### API Usage Note

When calling functions like `get-user-albums`, use keyword arguments and numbers (e.g., `:limit 5`, `:offset 20`), not string keys or values.

## License

Copyright © 2025 Ceth

_EPLv1.0 is just the default for projects generated by `clj-new`: you are not_
_required to open source this project, nor are you required to use EPLv1.0!_
_Feel free to remove or change the `LICENSE` file and remove or update this_
_section of the `README.md` file!_

Distributed under the Eclipse Public License version 1.0.

## Testing

The project includes unit tests for all major modules and functions.
To run all tests:

```bash
make test
# or
clojure -T:build test
```
