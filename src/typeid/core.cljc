(ns typeid.core
  "TypeID generation, parsing, and validation API.

   TypeIDs are type-safe, K-sortable, globally unique identifiers that combine
   a type prefix with a UUIDv7 suffix encoded in base32.

   Example: user_01h5fskfsk4fpeqwnsyz5hj55t"
  (:require [clojure.string :as str]
    [typeid.codec :as codec]
    [typeid.impl.uuid :as uuid]
    [typeid.validation :as v]))

#?(:clj (set! *warn-on-reflection* true))

;; T029: Generate function
(defn generate
  "Generate a new TypeID with the given prefix.

   The prefix can be:
   - A string (0-63 lowercase characters matching [a-z]([a-z_]{0,61}[a-z])?)
   - A keyword (its name will be used as the prefix)
   - nil or omitted (generates a prefix-less TypeID)

   Generates a UUIDv7 (timestamp-based UUID) and encodes it as a base32 suffix.

   Returns TypeID string like \"user_01h5fskfsk4fpeqwnsyz5hj55t\".

   Throws ex-info if prefix is invalid.

   Examples:
     (generate \"user\")
     ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

     (generate :user)
     ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

     (generate nil)
     ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

     (generate)
     ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

     (generate \"\")
     ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

   See also: `parse`, `validate`"
  ([] (generate ""))
  ([prefix]
   ;; Generate UUIDv7 and encode using codec
    (let [uuid-bytes (uuid/generate-uuidv7)]
      (codec/encode uuid-bytes prefix))))

;; T030: Parse function
(defn parse
  "Parse a TypeID string into its components.

   Returns a map with decomposed parts:
   - :prefix   Type prefix (empty string if none)
   - :suffix   26-character base32 suffix
   - :uuid     16-byte UUID (decoded from suffix)
   - :typeid   Original TypeID string

   Throws ExceptionInfo with structured error data if the input is invalid.

   Examples:
     (parse \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:prefix \"user\"
     ;;     :suffix \"01h5fskfsk4fpeqwnsyz5hj55t\"
     ;;     :uuid #bytes[...]
     ;;     :typeid \"user_01h5fskfsk4fpeqwnsyz5hj55t\"}

     (parse \"User_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> ExceptionInfo: {:type :typeid/invalid-format, :message \"...\", ...}

   See also: `explain` (for validation without exceptions), `generate`"
  [typeid-str]
  ;; Use codec/decode to validate and extract UUID bytes
  (let [uuid-bytes (codec/decode typeid-str)
        ;; Split to extract prefix and suffix
        [prefix suffix] (if (str/includes? typeid-str "_")
                          (let [idx (str/last-index-of typeid-str "_")]
                            [(subs typeid-str 0 idx) (subs typeid-str (inc idx))])
                          ["" typeid-str])]
    {:prefix prefix
     :suffix suffix
     :uuid uuid-bytes
     :typeid typeid-str}))

;; User Story 1: Explain function (replaces validate)
(defn explain
  "Validate a TypeID and explain errors if invalid.

   Returns nil if the input is a valid TypeID string.
   Returns an error map with details if invalid or non-string.

   The error map includes:
   - :type     Error category (namespaced keyword like :typeid/invalid-prefix)
   - :message  Human-readable error description
   - :input    The original input that caused the error
   - :expected Expected format/value (optional)
   - :actual   Actual problematic value (optional)

   Examples:
     (explain \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> nil  ; Valid

     (explain \"User_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:type :typeid/invalid-format
     ;;     :message \"TypeID must be all lowercase\"
     ;;     :input \"User_01h5fskfsk4fpeqwnsyz5hj55t\"
     ;;     :expected \"lowercase string\"
     ;;     :actual \"contains uppercase characters\"}

     (explain 12345)
     ;;=> {:type :typeid/invalid-input-type
     ;;     :message \"Invalid input type: expected string\"
     ;;     :input 12345
     ;;     :expected \"string\"
     ;;     :actual \"number\"}

   See also: `parse` (throws exception on invalid input), `generate`"
  [input]
  (cond
    ;; Check if input is a string
    (not (string? input))
    {:type :typeid/invalid-input-type
     :message "Invalid input type: expected string"
     :input input
     :expected "string"
     :actual (cond
               (nil? input) "nil"
               (number? input) "number"
               (keyword? input) "keyword"
               (map? input) "map"
               (vector? input) "vector"
               (seq? input) "list"
               :else (str (type input)))}

    ;; Check length
    (not (<= 26 (count input) 90))
    {:type :typeid/invalid-length
     :message "Invalid TypeID length"
     :input input
     :expected "26-90 characters"
     :actual (str (count input) " characters")}

    ;; Check lowercase
    (not (= input (str/lower-case input)))
    {:type :typeid/invalid-format
     :message "TypeID must be all lowercase"
     :input input
     :expected "lowercase string"
     :actual "contains uppercase characters"}

    ;; Check doesn't start with underscore
    (.startsWith input "_")
    {:type :typeid/invalid-format
     :message "TypeID cannot start with underscore"
     :input input}

    ;; Validate prefix and suffix
    :else
    (let [[prefix suffix] (if (str/includes? input "_")
                            (let [idx (str/last-index-of input "_")]
                              [(subs input 0 idx) (subs input (inc idx))])
                            ["" input])
          prefix-validation (v/validate-prefix prefix)]
      (cond
        ;; Prefix invalid
        (:error prefix-validation)
        (let [err (:error prefix-validation)]
          {:type :typeid/invalid-prefix
           :message (:message err)
           :input input
           :expected "valid prefix (0-63 lowercase alphanumeric, matching [a-z]([a-z_]{0,61}[a-z])?)"
           :actual (str "prefix: " prefix)})

        ;; Suffix invalid
        (not (v/valid-base32-suffix? suffix))
        {:type :typeid/invalid-suffix
         :message "Invalid TypeID suffix format"
         :input input
         :expected "26-character base32 suffix starting with 0-7"
         :actual (str "suffix: " suffix)}

        ;; All valid
        :else
        nil))))

;; Legacy compatibility: Keep validate function that delegates to explain
;; This maintains backward compatibility while the old validate function is deprecated
(defn validate
  "DEPRECATED: Use `explain` instead.

   Validate a TypeID string without parsing it.
   Returns {:ok true} on success, {:error error-map} on failure.

   This function is deprecated in favor of `explain` which returns nil for valid
   and error map for invalid (cleaner API).

   See also: `explain`, `parse`"
  [typeid-str]
  (if-let [error (explain typeid-str)]
    {:error error}
    {:ok true}))
