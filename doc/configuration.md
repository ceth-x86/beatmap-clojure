# Configuration Guide

This document explains how to configure the beatmap application, including how to handle tokens and secrets.

## Configuration Structure

The application uses a layered configuration approach:

1. **Base Configuration** (`resources/config.edn`) - Non-sensitive settings
2. **Local Configuration** (`config/local.edn`) - Secrets and local overrides
3. **Environment Variables** - Runtime configuration

## Setting Up Configuration

### 1. Initial Setup

```bash
make setup-config
```

This will create `config/local.edn` from the example file.

### 2. Edit Local Configuration

Edit `config/local.edn` with your actual values:

```clojure
{:secrets
 {:developer-token "your-actual-developer-token"
  :user-token "your-actual-user-token"
  :openai-api-key "your-actual-openai-api-key"}
 
 :development
 {:debug true
  :mock-external-services true}}
```

### 3. Environment Variables (Optional)

Create a `.env` file for environment variables:

```bash
cp env.example .env
```

Then edit `.env` with your values:

```bash
DEVELOPER_TOKEN=your-developer-token-here
USER_TOKEN=your-user-token-here
OPENAI_API_KEY=your-openai-api-key-here
```

## Using Configuration in Code

```clojure
(ns your-namespace
  (:require [beatmap.config :as config]))

;; Load configuration
(def app-config (config/merge-configs))

;; Get regular config values
(def api-url (config/get-config app-config :api :base-url))

;; Get secrets
(def developer-token (config/get-secret app-config :developer-token))
(def user-token (config/get-secret app-config :user-token))
(def openai-api-key (config/get-secret app-config :openai-api-key))
```

## Security Best Practices

1. **Never commit secrets to git** - `config/local.edn` and `.env` are in `.gitignore`
2. **Use environment variables in production** - Set them in your deployment environment
3. **Rotate tokens regularly** - Update your tokens periodically
4. **Use different tokens for different environments** - Development, staging, production

## Available Configuration Keys

### Base Configuration (`resources/config.edn`)

- `:app/name` - Application name
- `:app/version` - Application version
- `:app/environment` - Environment (development, staging, production)
- `:api/base-url` - Base URL for API calls
- `:api/timeout` - API timeout in milliseconds
- `:logging/level` - Log level (debug, info, warn, error)
- `:logging/format` - Log format (json, plain)

### Secrets (`config/local.edn`)

- `:secrets/developer-token` - Developer authentication token
- `:secrets/user-token` - User authentication token
- `:secrets/openai-api-key` - OpenAI API key for AI services

## Makefile Commands

- `make setup-config` - Create local config from example
- `make show-config` - Display current configuration (without secrets)
- `make run-with-env` - Run with environment variables loaded
- `make load-env` - Load environment variables from .env file

## Troubleshooting

### Configuration not loading

1. Check that `config/local.edn` exists
2. Verify the EDN syntax is correct
3. Ensure environment variables are set (if using them)

### Secrets not found

1. Verify the secret key exists in your configuration
2. Check that `config/local.edn` is properly formatted
3. Ensure the file is not being ignored by git 