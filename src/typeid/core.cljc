(ns typeid.core
  "TypeID generation, parsing, and validation API.

   TypeIDs are type-safe, K-sortable, globally unique identifiers that combine
   a type prefix with a UUIDv7 suffix encoded in base32.

   Example: user_01h5fskfsk4fpeqwnsyz5hj55t"
  (:require [typeid.impl.base32 :as base32]
    [typeid.impl.util :as util]
    [typeid.impl.uuid :as uuid]
    [typeid.validation :as v]))

(set! *warn-on-reflection* true)

;; T029: Generate function
(defn generate
  "Generate a new TypeID with the given prefix.

   The prefix must be 0-63 lowercase characters matching [a-z]([a-z_]{0,61}[a-z])?,
   or an empty string for prefix-less TypeIDs.

   Generates a UUIDv7 (timestamp-based UUID) and encodes it as a base32 suffix.

   Returns TypeID string like \"user_01h5fskfsk4fpeqwnsyz5hj55t\".

   Example:
     (generate \"user\")
     ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

     (generate \"\")
     ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

   Throws ex-info if prefix is invalid.

   See also: `parse`, `validate`"
  [prefix]
  ;; Validate prefix
  (let [prefix-validation (v/validate-prefix prefix)]
    (if (:error prefix-validation)
      (throw (ex-info (:message (:error prefix-validation))
               (:error prefix-validation)))
      ;; Generate UUIDv7
      (let [uuid-bytes (uuid/generate-uuidv7)
            ;; Encode to base32
            suffix (base32/encode uuid-bytes)
            ;; Combine prefix + suffix
            typeid-str (util/join-typeid prefix suffix)]
        typeid-str))))

;; T030: Parse function
(defn parse
  "Parse a TypeID string into its components.

   Returns a map with:
   - :prefix   Type prefix (empty string if none)
   - :suffix   26-character base32 suffix
   - :uuid     16-byte UUID (decoded from suffix)
   - :typeid   Original TypeID string

   Example:
     (parse \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:prefix \"user\"
     ;;    :suffix \"01h5fskfsk4fpeqwnsyz5hj55t\"
     ;;    :uuid #bytes[...]
     ;;    :typeid \"user_01h5fskfsk4fpeqwnsyz5hj55t\"}

     (parse \"01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:prefix \"\"
     ;;    :suffix \"01h5fskfsk4fpeqwnsyz5hj55t\"
     ;;    :uuid #bytes[...]
     ;;    :typeid \"01h5fskfsk4fpeqwnsyz5hj55t\"}

   Throws ex-info if TypeID string is invalid.

   See also: `generate`, `validate`"
  [typeid-str]
  ;; Basic string validation
  (when-not (string? typeid-str)
    (throw (ex-info "TypeID must be a string"
             {:type :invalid-typeid-type
              :value typeid-str
              :value-type (type typeid-str)})))

  ;; Check length
  (when-not (<= 26 (count typeid-str) 90)
    (throw (ex-info "TypeID must be 26-90 characters"
             {:type :invalid-length
              :typeid typeid-str
              :length (count typeid-str)})))

  ;; Check lowercase
  (when-not (= typeid-str (clojure.string/lower-case typeid-str))
    (throw (ex-info "TypeID must be all lowercase"
             {:type :invalid-case
              :typeid typeid-str})))

  ;; Split into prefix and suffix
  (let [[prefix suffix] (util/split-typeid typeid-str)]
    ;; Validate prefix
    (let [prefix-validation (v/validate-prefix prefix)]
      (when (:error prefix-validation)
        (throw (ex-info (:message (:error prefix-validation))
                 (:error prefix-validation)))))

    ;; Validate suffix format
    (when-not (v/valid-base32-suffix? suffix)
      (throw (ex-info "Invalid TypeID suffix"
               {:type :invalid-suffix
                :suffix suffix})))

    ;; Decode suffix to UUID
    (let [uuid-bytes (base32/decode suffix)]
      {:prefix prefix
       :suffix suffix
       :uuid uuid-bytes
       :typeid typeid-str})))
