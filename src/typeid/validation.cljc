(ns typeid.validation
  "Validation predicates for TypeID components.

   Manual validation predicates with zero external dependencies.

   ## Overview

   This namespace provides low-level validation functions used internally
   by `typeid.core` and `typeid.codec`. Most users should use the high-level
   API in [[typeid.core]] instead.

   ## Main Functions

   - [[valid-prefix?]] - Check if a prefix matches the TypeID pattern
   - [[validate-prefix]] - Validate prefix with detailed error reporting
   - [[valid-base32-suffix?]] - Check if a suffix is valid base32
   - [[valid-typeid-string?]] - Check if a string has valid TypeID format
   - [[valid-uuid-bytes?]] - Check if bytes represent a valid 16-byte UUID"
  (:require [clojure.string :as str]))

#?(:clj (set! *warn-on-reflection* true))

;; T017: Prefix validation predicates
(def ^:private prefix-pattern
  "Regex pattern for valid TypeID prefixes.
   - Empty string OR
   - Starts with lowercase letter, ends with lowercase letter
   - Middle can have lowercase letters or underscores
   - Maximum 63 characters total"
  #"^([a-z]([a-z_]{0,61}[a-z])?)?$")

(defn valid-prefix?
  "Check if prefix matches the TypeID prefix pattern.

   ## Valid Prefixes

   - Empty string
   - 1-63 lowercase characters matching pattern: `[a-z]([a-z_]{0,61}[a-z])?`

   ## Examples

   ```clojure
   (valid-prefix? \"user\")
   ;;=> true

   (valid-prefix? \"user_account\")
   ;;=> true

   (valid-prefix? \"\")
   ;;=> true

   (valid-prefix? \"User\")
   ;;=> false ; uppercase not allowed

   (valid-prefix? \"user_\")
   ;;=> false ; cannot end with underscore
   ```"
  [s]
  (and (string? s)
    (<= (count s) 63)
    (boolean (re-matches prefix-pattern s))))

(defn validate-prefix
  "Validate prefix and return detailed result.

   ## Returns

   - `{:ok prefix}` if valid
   - `{:error error-map}` if invalid

   The error map contains `:type`, `:message`, and `:data` keys.

   ## Examples

   ```clojure
   (validate-prefix \"user\")
   ;;=> {:ok \"user\"}

   (validate-prefix \"User\")
   ;;=> {:error {:type :invalid-prefix-format, :message \"...\", ...}}

   (validate-prefix (str/join (repeat 64 \"a\")))
   ;;=> {:error {:type :prefix-too-long, :message \"...\", ...}}
   ```"
  [prefix]
  (cond
    (not (string? prefix))
    {:error {:type :invalid-prefix-type
             :message "Prefix must be a string"
             :data {:prefix prefix :type (type prefix)}}}

    (> (count prefix) 63)
    {:error {:type :prefix-too-long
             :message "Prefix must be at most 63 characters"
             :data {:prefix prefix :length (count prefix)}}}

    (not (re-matches prefix-pattern prefix))
    {:error {:type :invalid-prefix-format
             :message "Prefix must match pattern [a-z]([a-z_]{0,61}[a-z])? or be empty"
             :data {:prefix prefix :pattern (str prefix-pattern)}}}

    :else
    {:ok prefix}))

;; T018: TypeID string validation predicates
(def ^:private base32-chars
  "Set of valid Crockford base32 characters."
  (set "0123456789abcdefghjkmnpqrstvwxyz"))

(defn valid-base32-suffix?
  "Check if suffix is valid base32 TypeID suffix.

   ## Requirements

   - Exactly 26 characters
   - All characters in Crockford base32 alphabet (`0-9a-hjkmnp-tv-z`)
   - First character must be `0-7` (prevents 128-bit overflow)

   ## Examples

   ```clojure
   (valid-base32-suffix? \"01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> true

   (valid-base32-suffix? \"81h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> false ; first char > 7

   (valid-base32-suffix? \"01h5fskfsk4fpeqwnsyz5hj55\")
   ;;=> false ; too short
   ```"
  [s]
  (and (string? s)
    (= 26 (count s))
    #?(:clj  (<= (int (first s)) (int \7))
       :cljs (let [first-char (.charAt s 0)]
               (<= (.charCodeAt first-char 0) (.charCodeAt "7" 0))))
    (every? base32-chars s)))

(defn valid-typeid-string?
  "Check if string has valid TypeID format (basic checks).

   ## Requirements

   - Length between 26 and 90 characters
     - 26 characters: suffix only
     - Up to 90 characters: 63 (max prefix) + 1 (underscore) + 26 (suffix)
   - All lowercase

   **Note:** This only checks basic format. Use [[typeid.core/explain]] for complete validation.

   ## Examples

   ```clojure
   (valid-typeid-string? \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> true

   (valid-typeid-string? \"01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> true

   (valid-typeid-string? \"User_01h5fskfsk4fpeqwnsyz5hj55t\")
   ;;=> false ; uppercase
   ```"
  [s]
  (and (string? s)
    (<= 26 (count s) 90)
    (= s (str/lower-case s))))

;; T019: UUID bytes validation predicates
(defn valid-uuid-bytes?
  "Check if bytes represent a valid UUID.

   ## Requirements

   - Exactly 16 bytes
   - Platform-specific byte array type:
     - JVM: byte array (`bytes?`)
     - ClojureScript: `js/Uint8Array`

   ## Examples

   ```clojure
   (def uuid-bytes (byte-array 16))
   (valid-uuid-bytes? uuid-bytes)
   ;;=> true

   (valid-uuid-bytes? (byte-array 15))
   ;;=> false ; too short
   ```"
  [b]
  #?(:clj (and (bytes? b) (= 16 (alength ^bytes b)))
     :cljs (and (instance? js/Uint8Array b) (= 16 (.-length b)))))

(defn valid-uuidv7-bytes?
  "Stricter validation for UUIDv7 format.

   Checks that bytes conform to UUIDv7 specification:
   - Exactly 16 bytes
   - Version bits (bits 48-51) = `0111` (version 7)
   - Variant bits (bits 64-65) = `10` (RFC 4122 variant)

   ## Examples

   ```clojure
   (require '[typeid.impl.uuid :as uuid])

   (def v7-bytes (uuid/generate-uuidv7))
   (valid-uuidv7-bytes? v7-bytes)
   ;;=> true

   ;; Random bytes won't pass version check
   (valid-uuidv7-bytes? (byte-array 16))
   ;;=> false ; version/variant bits not set correctly
   ```"
  [b]
  (and (valid-uuid-bytes? b)
    #?(:clj (let [^bytes uuid-bytes b
                     ;; Mask to unsigned to avoid sign extension issues
                  byte6 (bit-and (aget uuid-bytes 6) 0xFF)
                  byte8 (bit-and (aget uuid-bytes 8) 0xFF)]
              (and (= 7 (bit-and (bit-shift-right byte6 4) 0x0F))
                (= 2 (bit-and (bit-shift-right byte8 6) 0x03))))
       :cljs (and (= 7 (bit-and (bit-shift-right (aget b 6) 4) 0x0F))
               (= 2 (bit-and (bit-shift-right (aget b 8) 6) 0x03))))))
