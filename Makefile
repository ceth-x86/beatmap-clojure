.PHONY: help run run-custom test build clean repl deps

# Default target
help:
	@echo "Available commands:"
	@echo "  run        - Run the project with default greeting"
	@echo "  run-cmd    - Run with command (albums, playlists, generate, help)"
	@echo "  test       - Run tests"
	@echo "  build      - Build uberjar"
	@echo "  run-jar    - Run the built JAR file"
	@echo "  clean      - Clean build artifacts"
	@echo "  repl       - Start REPL"
	@echo "  deps       - Check dependencies"
	@echo "  setup-config - Setup configuration files"
	@echo "  check-tokens - Check if all tokens are configured"
	@echo "  test-apple-music - Test Apple Music API"
	@echo "  show-config - Show current configuration"
	@echo "  help       - Show this help"

# Run the project with default greeting
run:
	clojure -X:run-x

# Run with command argument (albums, playlists, help)
run-cmd:
	@if [ -z "$(CMD)" ]; then \
		echo "Usage: make run-cmd CMD=\"albums|playlists|generate [subcommand]|enrich [subcommand]|help\""; \
		echo "Examples:"; \
		echo "  make run-cmd CMD=albums"; \
		echo "  make run-cmd CMD=playlists"; \
		echo "  make run-cmd CMD=\"generate artists\""; \
		echo "  make run-cmd CMD=\"enrich artist_by_countries\""; \
		echo "  make run-cmd CMD=help"; \
		exit 1; \
	fi
	clojure -M -m beatmap.beatmap $(CMD)

# Run with custom name
run-custom:
	@if [ -z "$(NAME)" ]; then \
		echo "Usage: make run-custom NAME=YourName"; \
		exit 1; \
	fi
	clojure -X:run-x :name '"$(NAME)"'

# Run via main function
run-main:
	clojure -M:run-m

# Run via main with custom name
run-main-custom:
	@if [ -z "$(NAME)" ]; then \
		echo "Usage: make run-main-custom NAME=YourName"; \
		exit 1; \
	fi
	clojure -M:run-m $(NAME)

# Run tests
test:
	clojure -T:build test

# Build uberjar
build:
	clojure -T:build ci

# Run the built JAR file
run-jar:
	@if [ ! -f "target/beatmap-0.1.0-SNAPSHOT.jar" ]; then \
		echo "JAR file not found. Run 'make build' first."; \
		exit 1; \
	fi
	java -jar target/beatmap-0.1.0-SNAPSHOT.jar

# Clean build artifacts
clean:
	rm -rf target/
	rm -rf .cpcache/
	rm -rf .clj-kondo/.cache/

# Start REPL
repl:
	clojure

# Check dependencies
deps:
	clojure -Spath

# Install dependencies (if needed)
install-deps:
	clojure -P

# Format code (if cljfmt is available)
format:
	@if command -v cljfmt >/dev/null 2>&1; then \
		cljfmt fix; \
	else \
		echo "cljfmt not found. Install with: clojure -Ttools install io.github.cognitect-labs/cljfmt '{:git/tag \"v0.11.0\"}' :as cljfmt"; \
	fi

# Lint code (if clj-kondo is available)
lint:
	@if command -v clj-kondo >/dev/null 2>&1; then \
		clj-kondo --lint src test; \
	else \
		echo "clj-kondo not found. Install from: https://github.com/clj-kondo/clj-kondo"; \
	fi

# Full development workflow
dev: clean install-deps test run

# Quick development cycle
quick: test run

# Setup configuration
setup-config:
	@if [ ! -f "config/local.edn" ]; then \
		echo "Creating local config from example..."; \
		cp config/local.edn.example config/local.edn; \
		echo "Please edit config/local.edn with your actual values"; \
	else \
		echo "Local config already exists"; \
	fi

# Load environment variables from .env file (if exists)
load-env:
	@if [ -f ".env" ]; then \
		echo "Loading environment from .env file..."; \
		export $$(cat .env | xargs); \
	fi

# Run with environment variables loaded
run-with-env: load-env run

# Show current configuration (without secrets)
show-config:
	@echo "Current configuration:"
	@clojure -M -e "(require '[beatmap.config :as config]) (println (config/load-config))"

# Check token configuration
check-tokens:
	@echo "Checking token configuration..."
	@clojure -M -e "(require '[beatmap.tokens :as tokens]) (if (tokens/validate-tokens) (println \"✅ All tokens are configured\") (do (println \"⚠️  Missing tokens:\") (doseq [[k v] (tokens/missing-tokens)] (println (str \"   - \" k)))))"

# Test Apple Music API
test-apple-music:
	@echo "Testing Apple Music API..."
	@clojure -M -e "(require '[beatmap.apple-music :as apple-music]) (try (let [album (apple-music/get-user-albums :limit 1)] (println \"✅ Apple Music API test successful\") (println (str \"Album data: \" (pr-str album)))) (catch Exception e (println (str \"❌ Apple Music API test failed: \" (.getMessage e)))))"

 