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

;; T046: Encode function (User Story 3)
(defn encode
  "Encode UUID bytes with a prefix into a TypeID string.

   Returns {:ok typeid-string} on success, {:error error-map} on failure.

   The uuid-bytes must be exactly 16 bytes (big-endian).
   The prefix follows the same rules as `generate`.

   Example:
     (def uuid-bytes (byte-array [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                   0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
     (encode uuid-bytes \"user\")
     ;;=> {:ok \"user_01h5fskfsk4fpeqwnsyz5hj55t\"}

   See also: `decode`, `from-uuid`"
  [uuid-bytes prefix]
  ;; Validate UUID bytes
  (cond
    (not (v/valid-uuid-bytes? uuid-bytes))
    {:error {:type :invalid-uuid-length
             :message "UUID must be exactly 16 bytes"
             :data {:uuid-bytes uuid-bytes
                    :length (if (bytes? uuid-bytes) (alength uuid-bytes) nil)}}}

    :else
    ;; Validate prefix
    (let [prefix-validation (v/validate-prefix prefix)]
      (if (:error prefix-validation)
        prefix-validation
        ;; Encode to base32 and combine
        (let [suffix (base32/encode uuid-bytes)
              typeid-str (util/join-typeid prefix suffix)]
          {:ok typeid-str})))))

;; T047: Decode function (User Story 3)
(defn decode
  "Decode a TypeID string to extract UUID bytes.

   Returns {:ok uuid-bytes} on success, {:error error-map} on failure.

   The returned uuid-bytes is a 16-byte array (big-endian).

   Example:
     (decode \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:ok #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
     ;;               0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]}

   See also: `encode`, `parse`"
  [typeid-str]
  ;; Use parse to validate and extract UUID
  (let [parse-result (parse typeid-str)]
    (if (:ok parse-result)
      {:ok (:uuid (:ok parse-result))}
      parse-result)))

;; T048: uuid->hex function (User Story 3)
(defn uuid->hex
  "Convert UUID bytes to hexadecimal string.

   Returns {:ok hex-string} on success, {:error error-map} on failure.

   The hex-string is 32 characters of lowercase hexadecimal.

   Example:
     (def uuid-bytes (byte-array [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                   0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
     (uuid->hex uuid-bytes)
     ;;=> {:ok \"0188e5f5f34a7b3d9f2a1c5de67fa8c1\"}

   See also: `hex->uuid`"
  [uuid-bytes]
  (if-not (v/valid-uuid-bytes? uuid-bytes)
    {:error {:type :invalid-uuid-length
             :message "UUID must be exactly 16 bytes"
             :data {:uuid-bytes uuid-bytes
                    :length (if (bytes? uuid-bytes) (alength uuid-bytes) nil)}}}
    (let [hex-str (apply str (map #(format "%02x" (bit-and % 0xff)) uuid-bytes))]
      {:ok hex-str})))

;; T048: hex->uuid function (User Story 3)
(defn hex->uuid
  "Convert hexadecimal string to UUID bytes.

   Returns {:ok uuid-bytes} on success, {:error error-map} on failure.

   The hex-string must be exactly 32 characters of [0-9a-f].

   Example:
     (hex->uuid \"0188e5f5f34a7b3d9f2a1c5de67fa8c1\")
     ;;=> {:ok #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
     ;;               0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]}

   See also: `uuid->hex`"
  [hex-string]
  (cond
    (not (string? hex-string))
    {:error {:type :invalid-hex-type
             :message "Hex string must be a string"
             :data {:value hex-string
                    :value-type (type hex-string)}}}

    (not (= 32 (count hex-string)))
    {:error {:type :invalid-hex-length
             :message "Hex string must be exactly 32 characters"
             :data {:hex-string hex-string
                    :length (count hex-string)}}}

    (not (re-matches #"^[0-9a-f]{32}$" hex-string))
    {:error {:type :invalid-hex-char
             :message "Hex string must contain only [0-9a-f] characters"
             :data {:hex-string hex-string}}}

    :else
    (try
      (let [uuid-bytes (byte-array 16)]
        (dotimes [i 16]
          (let [hex-pair (subs hex-string (* i 2) (* (inc i) 2))
                byte-val (Integer/parseInt hex-pair 16)]
            (aset uuid-bytes i (unchecked-byte byte-val))))
        {:ok uuid-bytes})
      (catch #?(:clj Exception :cljs js/Error) e
        {:error {:type :hex-parse-error
                 :message (str "Failed to parse hex string: " #?(:clj (.getMessage e) :cljs (.-message e)))
                 :data {:hex-string hex-string}}}))))

;; T051: typeid->map function (User Story 3)
(defn typeid->map
  "Convert TypeID string to a map (unwrapped parse).

   Returns parsed map directly or throws exception on error.

   This function is provided for REPL/debugging convenience.
   Production code should use `parse` which returns {:ok ...} or {:error ...}.

   Example:
     (typeid->map \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> {:prefix \"user\"
     ;;     :suffix \"01h5fskfsk4fpeqwnsyz5hj55t\"
     ;;     :uuid #bytes[...]
     ;;     :typeid \"user_01h5fskfsk4fpeqwnsyz5hj55t\"}

   See also: `parse`"
  [typeid-str]
  (let [{:keys [ok error]} (parse typeid-str)]
    (if ok
      ok
      (throw (ex-info (:message error) error)))))
