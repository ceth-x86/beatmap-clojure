# ChatGPT Integration Catalog

This catalog provides ChatGPT integration functionality for enriching music data with AI-powered insights.

## Structure

- `core.clj` - Core ChatGPT API client with request/response handling
- `artists.clj` - Artist-specific ChatGPT functionality for country lookup

## Setup

To use ChatGPT features, you need to configure the OpenAI API key in your local configuration.

1. **Copy the example config** (if not already done):
   ```bash
   cp config/local.edn.example config/local.edn
   ```

2. **Add your OpenAI API key** to `config/local.edn`:
   ```clojure
   {:secrets
    {:developer-token "your-developer-token-here"
     :user-token "your-user-token-here"
     :openai-api-key "your-openai-api-key-here"}  ; ← Add your key here
    
    :development
    {:debug true
     :mock-external-services true}}
   ```

The application will automatically load the API key from the configuration system.

## Usage

### Artist Country Enrichment

The `enrich artist_by_countries` command uses ChatGPT to determine the country of origin for music artists:

```bash
make run-cmd CMD="enrich artist_by_countries"
```

This command:
1. Reads artists from `resources/catalog/generated/artists.csv`
2. Processes artists in batches of 25 to avoid token limits
3. Uses ChatGPT to determine each artist's country of origin
4. Outputs enriched data to `resources/catalog/enriched/artists_with_countries.csv`

### Features

- **Batch Processing**: Handles large artist lists efficiently
- **Error Handling**: Graceful handling of API failures
- **Token Usage Reporting**: Shows API token consumption
- **JSON Response Parsing**: Robust parsing of ChatGPT responses
- **Progress Tracking**: Real-time batch progress updates

### Example Output

```
🤖 Processing 1260 artists with ChatGPT in batches of 25...
📊 Total batches: 51
🔄 Processing batch 1/51 (25 artists)...
✅ Batch 1 completed: 25 artists processed
💰 Tokens used: 847
📊 ChatGPT processing summary:
   Total artists processed: 1260
   Successfully mapped: 1180
   Unknown/failed: 80
📊 Final country mapping results:
   Known countries: 1180 artists
   Unknown countries: 80 artists
✅ Written 1260 artists with country information to resources/catalog/enriched/artists_with_countries.csv
```

## API Configuration

### Models
- Default: `gpt-3.5-turbo` (cost-effective)
- Alternative: `gpt-4` (higher accuracy, higher cost)

### Parameters
- Temperature: 0.1 (factual responses)
- Max tokens: 2000 per batch
- Timeout: 60 seconds

## Future Extensions

The catalog is designed for extensibility. Planned features:
- Genre classification
- Artist similarity matching
- Album recommendation generation
- Lyrical analysis
- Music trend analysis

## Cost Considerations

ChatGPT API usage incurs costs based on token usage:
- Input tokens: Prompts and context
- Output tokens: Generated responses
- Monitor usage through token reporting in the application logs