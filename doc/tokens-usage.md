# Tokens Usage Guide

This document explains how to use the three tokens configured in the beatmap application.

## Available Tokens

The application is configured to use three tokens:

1. **`developer-token`** - Developer authentication token
2. **`user-token`** - User authentication token  
3. **`openai-api-key`** - OpenAI API key for AI services

## Configuration

### 1. Setup Configuration

```bash
make setup-config
```

### 2. Edit Tokens

Edit `config/local.edn` with your actual tokens:

```clojure
{:secrets
 {:developer-token "your-actual-developer-token"
  :user-token "your-actual-user-token"
  :openai-api-key "your-actual-openai-api-key"}
 
 :development
 {:debug true
  :mock-external-services true}}
```

### 3. Verify Configuration

```bash
make check-tokens
```

## Using Tokens in Code

### Basic Usage

```clojure
(ns your-namespace
  (:require [beatmap.tokens :as tokens]))

;; Get all tokens
(def all-tokens (tokens/get-tokens))

;; Get specific tokens
(def dev-token (tokens/get-developer-token))
(def user-token (tokens/get-user-token))
(def openai-key (tokens/get-openai-api-key))

;; Validate tokens
(if (tokens/validate-tokens)
  (println "All tokens are configured")
  (println "Some tokens are missing"))
```

### Example: API Call with Developer Token

```clojure
(ns your-namespace
  (:require [beatmap.tokens :as tokens]
            [clj-http.client :as http]))

(defn make-api-call []
  (let [dev-token (tokens/get-developer-token)]
    (http/get "https://api.example.com/data"
              {:headers {"Authorization" (str "Bearer " dev-token)}})))
```

### Example: OpenAI API Call

```clojure
(ns your-namespace
  (:require [beatmap.tokens :as tokens]
            [clj-http.client :as http]))

(defn call-openai [prompt]
  (let [api-key (tokens/get-openai-api-key)]
    (http/post "https://api.openai.com/v1/chat/completions"
               {:headers {"Authorization" (str "Bearer " api-key)
                         "Content-Type" "application/json"}
                :body (json/write-str {:model "gpt-3.5-turbo"
                                      :messages [{:role "user" :content prompt}]})})))
```

## REPL Examples

Start the REPL:

```bash
make repl
```

Then try these examples:

```clojure
;; Load the tokens module
(require '[beatmap.tokens :as tokens])

;; Check if all tokens are configured
(tokens/validate-tokens)

;; Get all tokens
(tokens/get-tokens)

;; Get specific token
(tokens/get-developer-token)

;; Check for missing tokens
(tokens/missing-tokens)
```

## Environment Variables

You can also set tokens via environment variables:

```bash
export DEVELOPER_TOKEN="your-dev-token"
export USER_TOKEN="your-user-token"
export OPENAI_API_KEY="your-openai-key"
```

Or create a `.env` file:

```bash
cp env.example .env
# Edit .env with your actual values
```

Then run with environment variables:

```bash
make run-with-env
```

## Security Best Practices

1. **Never commit tokens to git** - They're in `.gitignore`
2. **Use different tokens for different environments**
3. **Rotate tokens regularly**
4. **Use environment variables in production**
5. **Validate tokens before using them**

## Troubleshooting

### Tokens not found

```bash
make check-tokens
```

This will show which tokens are missing.

### Configuration not loading

1. Check that `config/local.edn` exists
2. Verify EDN syntax is correct
3. Ensure environment variables are set (if using them)

### Token validation failing

```clojure
;; In REPL
(require '[beatmap.tokens :as tokens])
(tokens/missing-tokens)
```

This will show exactly which tokens are missing. 