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

   Throws ex-info if prefix is invalid.

   Example:
     (generate \"user\")
     ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

     (generate \"\")
     ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

   See also: `parse`, `validate`"
  [prefix]
  ;; Validate prefix
  (let [prefix-validation (v/validate-prefix prefix)]
    (when (:error prefix-validation)
      (throw (ex-info (:message (:error prefix-validation))
               (:error prefix-validation))))
    ;; Generate UUIDv7
    (let [uuid-bytes (uuid/generate-uuidv7)
          ;; Encode to base32
          suffix (base32/encode uuid-bytes)
          ;; Combine prefix + suffix
          typeid-str (util/join-typeid prefix suffix)]
      typeid-str)))

;; T030: Parse function
(defn parse
  "Parse a TypeID string into its components.

   Returns {:ok parsed-map} on success, {:error error-map} on failure.

   The parsed-map contains:
   - :prefix   Type prefix (empty string if none)
   - :suffix   26-character base32 suffix
   - :uuid     16-byte UUID (decoded from suffix)
   - :typeid   Original TypeID string

   Example:
     (parse \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:ok {:prefix \"user\"
     ;;         :suffix \"01h5fskfsk4fpeqwnsyz5hj55t\"
     ;;         :uuid #bytes[...]
     ;;         :typeid \"user_01h5fskfsk4fpeqwnsyz5hj55t\"}}

     (parse \"User_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:error {:type :invalid-case
     ;;            :message \"...\"
     ;;            :data {...}}}

   See also: `generate`, `validate`"
  [typeid-str]
  ;; Basic string validation
  (cond
    (not (string? typeid-str))
    {:error {:type :invalid-typeid-type
             :message "TypeID must be a string"
             :data {:value typeid-str
                    :value-type (type typeid-str)}}}

    (not (<= 26 (count typeid-str) 90))
    {:error {:type :invalid-length
             :message "TypeID must be 26-90 characters"
             :data {:typeid typeid-str
                    :length (count typeid-str)}}}

    (not (= typeid-str (clojure.string/lower-case typeid-str)))
    {:error {:type :invalid-case
             :message "TypeID must be all lowercase"
             :data {:typeid typeid-str}}}

    :else
    ;; Split into prefix and suffix
    (let [[prefix suffix] (util/split-typeid typeid-str)]
      ;; Validate prefix
      (let [prefix-validation (v/validate-prefix prefix)]
        (if (:error prefix-validation)
          prefix-validation
          ;; Validate suffix format
          (if-not (v/valid-base32-suffix? suffix)
            {:error {:type :invalid-suffix
                     :message "Invalid TypeID suffix format"
                     :data {:suffix suffix}}}
            ;; Decode suffix to UUID
            (try
              (let [uuid-bytes (base32/decode suffix)]
                {:ok {:prefix prefix
                      :suffix suffix
                      :uuid uuid-bytes
                      :typeid typeid-str}})
              (catch #?(:clj Exception :cljs js/Error) e
                {:error {:type :decode-error
                         :message (str "Failed to decode suffix: " #?(:clj (.getMessage e) :cljs (.-message e)))
                         :data {:suffix suffix}}}))))))))

;; T037: Validate function (User Story 2)
(defn validate
  "Validate a TypeID string without parsing it (faster check).

   Returns {:ok true} on success, {:error error-map} on failure.

   This function validates format without decoding the UUID, making it
   lighter weight than `parse`.

   Example:
     (validate \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:ok true}

     (validate \"User_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:error {:type :invalid-case
     ;;            :message \"...\"
     ;;            :data {...}}}

   See also: `parse`, `generate`"
  [typeid-str]
  ;; Basic string validation - same as parse but without decoding
  (cond
    (not (string? typeid-str))
    {:error {:type :invalid-typeid-type
             :message "TypeID must be a string"
             :data {:value typeid-str
                    :value-type (type typeid-str)}}}

    (not (<= 26 (count typeid-str) 90))
    {:error {:type :invalid-length
             :message "TypeID must be 26-90 characters"
             :data {:typeid typeid-str
                    :length (count typeid-str)}}}

    (not (= typeid-str (clojure.string/lower-case typeid-str)))
    {:error {:type :invalid-case
             :message "TypeID must be all lowercase"
             :data {:typeid typeid-str}}}

    :else
    ;; Split into prefix and suffix
    (let [[prefix suffix] (util/split-typeid typeid-str)]
      ;; Validate prefix
      (let [prefix-validation (v/validate-prefix prefix)]
        (if (:error prefix-validation)
          prefix-validation
          ;; Validate suffix format (but don't decode)
          (if-not (v/valid-base32-suffix? suffix)
            {:error {:type :invalid-suffix
                     :message "Invalid TypeID suffix format"
                     :data {:suffix suffix}}}
            {:ok true}))))))
