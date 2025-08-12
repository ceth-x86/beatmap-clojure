(ns beatmap.chatgpt.core
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [clojure.string :as str]
            [beatmap.tokens :as tokens]))

(def ^:private openai-api-url "https://api.openai.com/v1/chat/completions")

(defn- validate-api-key
  "Validate that OpenAI API key is configured."
  []
  (when-not (tokens/get-openai-api-key)
    (throw (ex-info "OpenAI API key not configured. Please add :openai-api-key to your config secrets." 
                    {:error :missing-api-key}))))

(defn create-chat-request
  "Create a ChatGPT API request payload.
   
   Args:
     messages: Vector of message maps with :role and :content
     model: OpenAI model to use (default: gpt-3.5-turbo)
     temperature: Creativity level 0.0-2.0 (default: 0.1 for factual responses)
     max-tokens: Maximum tokens in response (default: 1000)
   
   Returns:
     Request payload map"
  [messages & {:keys [model temperature max-tokens]
               :or {model "gpt-4.1"
                    temperature 0.1
                    max-tokens 1000}}]
  {:model model
   :messages messages
   :temperature temperature
   :max_tokens max-tokens})

(defn send-chat-request
  "Send a request to ChatGPT API.
   
   Args:
     request-payload: Request payload from create-chat-request
   
   Returns:
     Response map with :success, :content, :error keys"
  [request-payload]
  (validate-api-key)
  (try
    (let [response (http/post openai-api-url
                             {:headers {"Authorization" (str "Bearer " (tokens/get-openai-api-key))
                                       "Content-Type" "application/json"}
                              :body (json/write-str request-payload)
                              :socket-timeout 60000  ; 60 seconds
                              :connection-timeout 30000  ; 30 seconds
                              :as :json})]
      (if (= (:status response) 200)
        (let [response-body (:body response)
              content (get-in response-body [:choices 0 :message :content])]
          {:success true
           :content (str/trim content)
           :tokens-used (get-in response-body [:usage :total_tokens])})
        {:success false
         :error (str "API request failed with status: " (:status response))
         :details (:body response)}))
    (catch Exception e
      {:success false
       :error (str "Request failed: " (.getMessage e))
       :exception e})))

(defn chat-completion
  "Simple interface for ChatGPT completions.
   
   Args:
     prompt: The user prompt/question
     system-message: Optional system message to set context
     options: Optional map with :model, :temperature, :max-tokens
   
   Returns:
     Response map with :success, :content, :error keys
   
   Example:
     (chat-completion \"What is the capital of France?\")"
  [prompt & {:keys [system-message model temperature max-tokens]
             :or {system-message "You are a helpful assistant."
                  model "gpt-4.1"
                  temperature 0.1
                  max-tokens 1000}}]
  (let [messages (if system-message
                   [{:role "system" :content system-message}
                    {:role "user" :content prompt}]
                   [{:role "user" :content prompt}])
        request (create-chat-request messages
                                    :model model
                                    :temperature temperature
                                    :max-tokens max-tokens)]
    (send-chat-request request)))

(defn parse-json-response
  "Parse JSON response from ChatGPT, handling common formatting issues.
   
   Args:
     content: Response content string
   
   Returns:
     Parsed JSON data or nil if parsing fails"
  [content]
  (try
    ;; Try to extract JSON from markdown code blocks if present
    (let [cleaned-content (-> content
                             (str/replace #"```json\n" "")
                             (str/replace #"```" "")
                             (str/trim))]
      (json/read-str cleaned-content))
    (catch Exception e
      (println (str "⚠️ Failed to parse JSON response: " (.getMessage e)))
      nil)))