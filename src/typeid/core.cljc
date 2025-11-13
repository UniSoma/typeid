(ns typeid.core
  "TypeID generation, parsing, and validation API.

   TypeIDs are type-safe, K-sortable, globally unique identifiers that combine
   a type prefix with a UUIDv7 suffix encoded in base32.

   ## Quick Example

   ```clojure
   (require '[typeid.core :as typeid])

   ;; Generate a new TypeID
   (typeid/create \"user\")
   ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

   ;; Parse and validate
   (typeid/parse \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> {:prefix \"user\", :suffix \"01h5fskfsk4fpeqwnsyz5hj55t\", ...}

   ;; Validate without exceptions
   (typeid/explain \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> nil ; Valid!
   ```

   ## Main Functions

   - [[create]] - Generate new TypeIDs or create from existing UUIDs
   - [[parse]] - Parse TypeID into components (throws on invalid)
   - [[explain]] - Validate TypeID and get error details (no exceptions)"
  (:require [clojure.string :as str]
    [typeid.codec :as codec]
    [typeid.uuid :as uuid]
    [typeid.validation :as v]))

#?(:clj (set! *warn-on-reflection* true))

;; User Story 3: Create function
(defn create
  "Create a TypeID with flexible options.

   ## Arities

   1. **Zero-arity**: Generate new TypeID with no prefix
   2. **One-arity**: Generate new TypeID with given prefix
   3. **Two-arity**: Create TypeID from existing UUID with given prefix

   ## Parameters

   **prefix** (optional) - Can be:
   - A string: 0-63 lowercase characters matching `[a-z]([a-z_]{0,61}[a-z])?`
   - A keyword: its name will be used as the prefix
   - `nil` or empty string: generates a prefix-less TypeID

   **uuid** (two-arity only) - Platform-native UUID object:
   - JVM: `java.util.UUID`
   - ClojureScript: `cljs.core/UUID`

   Accepts any UUID version (v1, v4, v7, etc.) and edge cases (all-zeros, all-ones).

   ## Returns

   TypeID string like `\"user_01h5fskfsk4fpeqwnsyz5hj55t\"`.

   ## Exceptions

   Throws `ExceptionInfo` with structured error data if prefix or UUID is invalid.

   ## Examples

   ```clojure
   ;; Zero-arity (new TypeID, no prefix)
   (create)
   ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

   ;; One-arity (new TypeID with prefix)
   (create \"user\")
   ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

   (create :user)
   ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

   (create nil)
   ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

   ;; Two-arity (from existing UUID)
   (create \"user\" #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\")
   ;;=> \"user_01h455vb4pex5vsknk084sn02q\"

   (create nil #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\")
   ;;=> \"01h455vb4pex5vsknk084sn02q\"

   ;; Invalid inputs throw exceptions
   (create \"User\")
   ;;=> ExceptionInfo: {:type :typeid/invalid-prefix, ...}

   (create \"user\" \"not-a-uuid\")
   ;;=> ExceptionInfo: {:type :typeid/invalid-uuid, ...}
   ```

   See also: [[parse]], [[explain]]"
  (^String [] (create nil))
  (^String [prefix]
   ;; Generate new UUIDv7 and encode
    (let [uuid-bytes (uuid/generate-uuidv7)]
      (codec/encode uuid-bytes prefix)))
  #_{:clj-kondo/ignore [:shadowed-var]}
  (^String [prefix uuid]
   ;; Convert UUID to bytes and encode
    (let [uuid-bytes (uuid/uuid->bytes uuid)]
      (codec/encode uuid-bytes prefix))))

;; T030: Parse function
(defn parse
  "Parse a TypeID string into its components.

   ## Returns

   A map with decomposed parts:
   - `:prefix` - Type prefix (empty string if none)
   - `:suffix` - 26-character base32 suffix
   - `:uuid` - Platform-native UUID object (java.util.UUID on JVM, cljs.core/UUID in ClojureScript)
   - `:typeid` - Original TypeID string

   ## Exceptions

   Throws `ExceptionInfo` with structured error data if the input is invalid.

   ## Examples

   ```clojure
   (parse \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> {:prefix \"user\"
   ;;     :suffix \"01h5fskfsk4fpeqwnsyz5hj55t\"
   ;;     :uuid #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"
   ;;     :typeid \"user_01h5fskfsk4fpeqwnsyz5hj55t\"}

   (parse \"User_01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> ExceptionInfo: {:type :typeid/invalid-format, :message \"...\", ...}

   ;; Round-trip conversion with create
   (let [original-uuid #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"
         typeid (create \"user\" original-uuid)
         recovered-uuid (:uuid (parse typeid))]
     (= original-uuid recovered-uuid))
   ;;=> true
   ```

   See also: [[explain]] (for validation without exceptions), [[create]]"
  [typeid-str]
  ;; Use codec/decode to validate and extract UUID bytes
  (let [uuid-bytes (codec/decode typeid-str)
        ;; Convert bytes to platform-native UUID object
        uuid-obj (uuid/bytes->uuid uuid-bytes)
        ;; Split to extract prefix and suffix
        [prefix suffix] (if (str/includes? typeid-str "_")
                          (let [idx (str/last-index-of typeid-str "_")]
                            [(subs typeid-str 0 idx) (subs typeid-str (inc idx))])
                          ["" typeid-str])]
    {:prefix prefix
     :suffix suffix
     :uuid uuid-obj
     :typeid typeid-str}))

;; User Story 1: Explain function (replaces validate)
(defn explain
  "Validate a TypeID and explain errors if invalid.

   Returns `nil` if the input is a valid TypeID string.
   Returns an error map with details if invalid or non-string.

   ## Error Map Structure

   The error map includes:
   - `:type` - Error category (namespaced keyword like `:typeid/invalid-prefix`)
   - `:message` - Human-readable error description
   - `:input` - The original input that caused the error
   - `:expected` - Expected format/value (optional)
   - `:actual` - Actual problematic value (optional)

   ## Examples

   ```clojure
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
   ```

   See also: [[parse]] (throws exception on invalid input), [[create]]"
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
    (.startsWith ^String input "_")
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

