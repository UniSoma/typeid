(ns typeid.codec
  "Low-level codec operations for TypeID encoding and decoding.

   This namespace provides the building blocks for TypeID encoding/decoding:
   - encode: UUID bytes + prefix → TypeID string
   - decode: TypeID string → UUID bytes
   - uuid->hex: UUID bytes → hex string
   - hex->uuid: Hex string → UUID bytes

   Most users should use the high-level API in typeid.core instead.
   These functions are exposed for advanced use cases and testing."
  (:require [clojure.string :as str]
    [typeid.impl.base32 :as base32]
    [typeid.impl.util :as util]
    [typeid.validation :as v]))

#?(:clj (set! *warn-on-reflection* true))

(defn encode
  "Encode UUID bytes with a prefix into a TypeID string.

   Takes UUID bytes (16-byte array) and an optional prefix, returns a TypeID string.
   Throws ex-info if inputs are invalid.

   The prefix can be:
   - A string (0-63 lowercase alphanumeric matching [a-z]([a-z_]{0,61}[a-z])?)
   - A keyword (its name will be used as the prefix)
   - nil or empty string (generates prefix-less TypeID)

   Examples:
     (def uuid-bytes (byte-array [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                   0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
     (encode uuid-bytes \"user\")
     ;;=> \"user_01h5fskfsk4fpeqwnsyz5hj55t\"

     (encode uuid-bytes nil)
     ;;=> \"01h5fskfsk4fpeqwnsyz5hj55t\"

     (encode uuid-bytes :org)
     ;;=> \"org_01h5fskfsk4fpeqwnsyz5hj55t\"

   See also: `decode`, `typeid.core/create`"
  ^String [uuid-bytes prefix]
  ;; Validate UUID bytes
  (when-not (v/valid-uuid-bytes? uuid-bytes)
    (throw (ex-info "UUID must be exactly 16 bytes"
             {:type :typeid/invalid-uuid
              :message "UUID must be exactly 16 bytes"
              :input uuid-bytes
              :expected "16-byte array"
              :actual (str (if #?(:clj (bytes? uuid-bytes)
                                  :cljs (instance? js/Uint8Array uuid-bytes))
                             #?(:clj (alength ^bytes uuid-bytes)
                                :cljs (alength uuid-bytes))
                             "not a byte array") " bytes")})))

  ;; Normalize and validate prefix
  (let [normalized-prefix (cond
                            (nil? prefix) ""
                            (keyword? prefix) (name prefix)
                            :else prefix)
        prefix-validation (v/validate-prefix normalized-prefix)]
    (when (:error prefix-validation)
      (throw (ex-info (:message (:error prefix-validation))
               (:error prefix-validation))))

    ;; Encode to base32 and combine
    (let [suffix (base32/encode uuid-bytes)]
      (util/join-typeid normalized-prefix suffix))))

(defn decode
  "Decode a TypeID string to extract UUID bytes.

   Takes a TypeID string and returns a 16-byte array (big-endian).
   Throws ex-info if the input is invalid.

   Examples:
     (decode \"user_01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
     ;;           0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]

     (decode \"01h5fskfsk4fpeqwnsyz5hj55t\")
     ;;=> #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
     ;;           0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]

   See also: `encode`, `typeid.core/parse`"
  ^bytes [typeid-str]
  ;; Basic validations
  (when-not (string? typeid-str)
    (throw (ex-info "TypeID must be a string"
             {:type :typeid/invalid-input-type
              :message "TypeID must be a string"
              :input typeid-str
              :expected "string"
              :actual (str (type typeid-str))})))

  (when-not (<= 26 (count typeid-str) 90)
    (throw (ex-info "TypeID must be 26-90 characters"
             {:type :typeid/invalid-length
              :message "TypeID must be 26-90 characters"
              :input typeid-str
              :expected "26-90 characters"
              :actual (str (count typeid-str) " characters")})))

  (when-not (= typeid-str (str/lower-case typeid-str))
    (throw (ex-info "TypeID must be all lowercase"
             {:type :typeid/invalid-format
              :message "TypeID must be all lowercase"
              :input typeid-str
              :expected "lowercase string"
              :actual "contains uppercase characters"})))

  (when (.startsWith ^String typeid-str "_")
    (throw (ex-info "TypeID cannot start with underscore"
             {:type :typeid/invalid-format
              :message "TypeID cannot start with underscore"
              :input typeid-str})))

  ;; Split and validate
  (let [[prefix suffix] (util/split-typeid typeid-str)
        prefix-validation (v/validate-prefix prefix)]
    (when (:error prefix-validation)
      (let [err (:error prefix-validation)]
        (throw (ex-info (:message err) err))))

    (when-not (v/valid-base32-suffix? suffix)
      (throw (ex-info "Invalid TypeID suffix format"
               {:type :typeid/invalid-suffix
                :message "Invalid TypeID suffix format"
                :input typeid-str
                :suffix suffix})))

    ;; Decode suffix to UUID bytes
    (try
      (base32/decode suffix)
      (catch #?(:clj Exception :cljs js/Error) e
        (throw (ex-info (str "Failed to decode suffix: " #?(:clj (.getMessage e) :cljs (.-message e)))
                 {:type :typeid/decode-error
                  :message (str "Failed to decode suffix: " #?(:clj (.getMessage e) :cljs (.-message e)))
                  :input typeid-str
                  :suffix suffix}))))))

(defn uuid->hex
  "Convert UUID bytes to hexadecimal string.

   Takes a 16-byte UUID array and returns a 32-character lowercase hex string
   (no hyphens). Throws ex-info if input is invalid.

   Examples:
     (def uuid-bytes (byte-array [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                                   0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
     (uuid->hex uuid-bytes)
     ;;=> \"0188e5f5f34a7b3d9f2a1c5de67fa8c1\"

   See also: `hex->uuid`"
  [uuid-bytes]
  (when-not (v/valid-uuid-bytes? uuid-bytes)
    (throw (ex-info "UUID must be exactly 16 bytes"
             {:type :typeid/invalid-uuid
              :message "UUID must be exactly 16 bytes"
              :input uuid-bytes
              :expected "16-byte array"
              :actual (str #?(:clj (if (bytes? uuid-bytes) (alength ^bytes uuid-bytes) "not a byte array")
                              :cljs (if (instance? js/Uint8Array uuid-bytes) (.-length uuid-bytes) "not a byte array")))})))

  (apply str (map #?(:clj (fn [b] (format "%02x" (bit-and b 0xff)))
                     :cljs (fn [b] (.padStart (.toString (bit-and b 0xff) 16) 2 "0")))
               uuid-bytes)))

(defn hex->uuid
  "Convert hexadecimal string to UUID bytes.

   Takes a 32-character hex string (with or without hyphens, case-insensitive)
   and returns a 16-byte UUID array. Throws ex-info if input is invalid.

   Examples:
     (hex->uuid \"0188e5f5f34a7b3d9f2a1c5de67fa8c1\")
     ;;=> #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
     ;;           0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]

     (hex->uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\")
     ;;=> #bytes[0x01 0x8c 0x3f 0x9e ...]

     (hex->uuid \"018C3F9E9E4E7A8A8B2A7E8E9E4E7A8A\")
     ;;=> #bytes[0x01 0x8c 0x3f 0x9e ...]  ; Uppercase accepted

   See also: `uuid->hex`"
  ^bytes [hex-string]
  (when-not (string? hex-string)
    (throw (ex-info "Hex string must be a string"
             {:type :typeid/invalid-input-type
              :message "Hex string must be a string"
              :input hex-string
              :expected "string"
              :actual (str (type hex-string))})))

  ;; Remove hyphens and convert to lowercase
  (let [normalized-hex (-> hex-string
                         (str/replace "-" "")
                         (str/lower-case))]

    (when-not (= 32 (count normalized-hex))
      (throw (ex-info "Hex string must be exactly 32 characters (excluding hyphens)"
               {:type :typeid/invalid-uuid
                :message "Hex string must be exactly 32 characters (excluding hyphens)"
                :input hex-string
                :expected "32 hex characters"
                :actual (str (count normalized-hex) " characters")})))

    (when-not (re-matches #"^[0-9a-f]{32}$" normalized-hex)
      (throw (ex-info "Hex string must contain only [0-9a-fA-F] characters"
               {:type :typeid/invalid-uuid
                :message "Hex string must contain only [0-9a-fA-F] characters"
                :input hex-string})))

    (try
      (let [uuid-bytes #?(:clj (byte-array 16)
                          :cljs (js/Uint8Array. 16))]
        (dotimes [i 16]
          (let [hex-pair (subs normalized-hex (* i 2) (* (inc i) 2))
                byte-val #?(:clj (Integer/parseInt hex-pair 16)
                            :cljs (js/parseInt hex-pair 16))]
            (aset uuid-bytes i #?(:clj (unchecked-byte byte-val)
                                  :cljs byte-val))))
        uuid-bytes)
      (catch #?(:clj Exception :cljs js/Error) e
        (throw (ex-info (str "Failed to parse hex string: " #?(:clj (.getMessage e) :cljs (.-message e)))
                 {:type :typeid/invalid-uuid
                  :message (str "Failed to parse hex string: " #?(:clj (.getMessage e) :cljs (.-message e)))
                  :input hex-string}))))))
