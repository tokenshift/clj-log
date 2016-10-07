(ns com.tokenshift.log
  "Structured logging to STDOUT for 12-factor apps."
  (:require [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.format :as fmt])
  (:import [java.util UUID]))

;; # Formatters

(def edn-formatter pr-str)
(def json-formatter json/generate-string)

(def formatters
  "List of built-in log event formatters."
  {:edn  edn-formatter
   :json json-formatter})

;; # Configuration

(def ^:dynamic *formatter*
  "The default formatter outputs events as JSON, but can be overridden."
  json-formatter)

(def ^:dynamic *output-stream*
  "By default, events are output to STDOUT."
  *out*)

;; # Request IDs

(def ^:dynamic *request-id*
  "An optional request ID to be included with every logged event (useful for
  web apps that want to track a single request through the system)."
  nil)

(defmacro with-new-request-id
  ;"Evaluates body with a randomly generated request ID.
  [& body]
  (let [id (str (UUID/randomUUID))]
    `(binding [*request-id* ~id]
       ~@body)))

;; # Logging Functions

(defn- timestamp
  []
  (fmt/unparse (fmt/formatters :date-time) (t/now)))

(defn- with-request-id
  [event]
  (if *request-id*
    (assoc event :_request-id *request-id*)
    event))

(defn- with-timestamp
  [event]
  (assoc event :_time (timestamp)))

(defn write
  [event]
  (binding [*out* *output-stream*]
    (-> event
        (with-request-id)
        (with-timestamp)
        (*formatter* event)
        (println))))

;; # Log Levels

(def log-levels
  {:all   0
   :debug 1
   :info  2
   :warn  3
   :error 4
   :fatal 5})

(def ^:dynamic *log-level* :all)

(doseq [[log-level-name log-level-num] log-levels]
  (intern *ns* (symbol (name log-level-name))
          (fn [event]
            (when (>= log-level-num (log-levels *log-level* 0))
              (write (assoc event :_level log-level-name))))))
