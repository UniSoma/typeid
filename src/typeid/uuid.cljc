(ns typeid.uuid
  "UUID utility functions for TypeID library.

   Cross-platform utilities for working with UUID objects and byte arrays,
   plus UUIDv7 generation for timestamp-ordered identifiers.

   ## Overview

   This namespace provides utilities for:
   - Converting between platform-native UUID objects and byte arrays
   - Generating UUIDv7 bytes with timestamp-based ordering
   - Low-level UUID manipulation for binary protocols and storage

   All functions work with platform-native UUID types:
   - **JVM**: `java.util.UUID`
   - **ClojureScript**: `cljs.core/UUID`

   ## Main Functions

   - [[uuid->bytes]] - Convert UUID object to 16-byte array
   - [[bytes->uuid]] - Convert 16-byte array to UUID object
   - [[generate-uuidv7]] - Generate UUIDv7 bytes with timestamp ordering

   ## Quick Example

   ```clojure
   (require '[typeid.uuid :as uuid]
            '[typeid.core :as typeid])

   ;; Convert UUID to bytes
   (uuid/uuid->bytes #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\")
   ;;=> #object[\"[B\" ...] (16-byte array)

   ;; Convert bytes back to UUID
   (uuid/bytes->uuid (uuid/uuid->bytes my-uuid))
   ;;=> #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"

   ;; Generate UUIDv7 bytes and create TypeID
   (let [uuid-bytes (uuid/generate-uuidv7)
         uuid-obj (uuid/bytes->uuid uuid-bytes)]
     (typeid/create \"order\" uuid-obj))
   ;;=> \"order_01h455vb4pex5vsknk084sn02q\"
   ```"
  #?(:clj (:import [java.security SecureRandom])))

#?(:clj (set! *warn-on-reflection* true))

;; T020: UUIDv7 generation
(defn- current-timestamp-ms
  "Get current Unix timestamp in milliseconds."
  ^long []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/Date)))

(defn- random-bytes
  "Generate n random bytes using platform-specific secure random source."
  [^long n]
  #?(:clj (let [rng (SecureRandom.)
                rand-byte-arr (byte-array n)]
            (.nextBytes rng rand-byte-arr)
            rand-byte-arr)
     :cljs (let [arr (js/Uint8Array. n)]
             (if (and js/globalThis
                   (.-crypto js/globalThis))
               (.getRandomValues (.-crypto js/globalThis) arr)
               ;; Fallback for Node.js without global crypto
               (let [crypto (js/require "crypto")]
                 (.randomFillSync crypto arr)))
             arr)))

(defn generate-uuidv7
  "Generate UUIDv7 (RFC 9562) bytes with timestamp-based ordering.

   ## Returns

   16-byte array representing a UUIDv7:
   - **JVM**: `byte[]` (primitive byte array)
   - **ClojureScript**: `js/Uint8Array`

   Each call produces unique bytes with chronological sortability.
   Later timestamps result in lexicographically greater byte sequences.

   ## UUIDv7 Structure

   Per RFC 9562 specification:
   - **Bytes 0-5**: Unix timestamp in milliseconds (48 bits)
   - **Byte 6**: Version bits (4 bits = `0111` for v7) + random (12 bits)
   - **Byte 8**: Variant bits (2 bits = `10` RFC 4122) + random (6 bits)
   - **Bytes 9-15**: Cryptographically random data (56 bits)

   ## Examples

   ```clojure
   ;; Generate UUIDv7 bytes
   (generate-uuidv7)
   ;;=> #object[\"[B\" 0x...] (16 bytes)

   ;; Convert to UUID object for use with TypeID
   (bytes->uuid (generate-uuidv7))
   ;;=> #uuid \"018d5e9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"

   ;; Complete workflow: Generate → Convert → Create TypeID
   (require '[typeid.core :as typeid])
   (let [uuid-bytes (generate-uuidv7)
         uuid-obj (bytes->uuid uuid-bytes)]
     (typeid/create \"order\" uuid-obj))
   ;;=> \"order_01h455vb4pex5vsknk084sn02q\"

   ;; Verify chronological ordering
   (let [uuid1 (generate-uuidv7)]
     (Thread/sleep 10)
     (let [uuid2 (generate-uuidv7)]
       (neg? (compare (seq uuid1) (seq uuid2)))))
   ;;=> true ; uuid1 sorts before uuid2
   ```

   See also: [[bytes->uuid]], [[uuid->bytes]], [[typeid.core/create]]"
  ^bytes []
  (let [timestamp (current-timestamp-ms)
        rand-bytes (random-bytes 10) ;; 10 random bytes (80 bits)
        uuid-bytes #?(:clj (byte-array 16)
                      :cljs (js/Uint8Array. 16))]

    ;; Set timestamp (bytes 0-5): 48 bits
    #?(:clj (do
              (aset uuid-bytes 0 (unchecked-byte (bit-shift-right timestamp 40)))
              (aset uuid-bytes 1 (unchecked-byte (bit-shift-right timestamp 32)))
              (aset uuid-bytes 2 (unchecked-byte (bit-shift-right timestamp 24)))
              (aset uuid-bytes 3 (unchecked-byte (bit-shift-right timestamp 16)))
              (aset uuid-bytes 4 (unchecked-byte (bit-shift-right timestamp 8)))
              (aset uuid-bytes 5 (unchecked-byte timestamp)))
       :cljs (do
               ;; JavaScript bitwise ops are 32-bit, so use division for high bytes
               (aset uuid-bytes 0 (js/Math.floor (/ timestamp 1099511627776)))  ; / 2^40
               (aset uuid-bytes 1 (bit-and (js/Math.floor (/ timestamp 4294967296)) 0xFF))  ; / 2^32
               (aset uuid-bytes 2 (bit-and (bit-shift-right timestamp 24) 0xFF))
               (aset uuid-bytes 3 (bit-and (bit-shift-right timestamp 16) 0xFF))
               (aset uuid-bytes 4 (bit-and (bit-shift-right timestamp 8) 0xFF))
               (aset uuid-bytes 5 (bit-and timestamp 0xFF))))

    ;; Byte 6: Random (12 bits in low nibble) + version (4 bits = 0111 in high nibble)
    #?(:clj (aset uuid-bytes 6 (unchecked-byte (bit-or (bit-and (aget ^bytes rand-bytes 0) 0x0F) 0x70)))
       :cljs (aset uuid-bytes 6 (bit-or (bit-and (aget rand-bytes 0) 0x0F) 0x70)))

    ;; Byte 7: Random (8 bits)
    #?(:clj (aset uuid-bytes 7 (aget ^bytes rand-bytes 1))
       :cljs (aset uuid-bytes 7 (aget rand-bytes 1)))

    ;; Byte 8: Variant (2 bits = 10 in high 2 bits) + random (6 bits in low 6 bits)
    #?(:clj (aset uuid-bytes 8 (unchecked-byte (bit-or (bit-and (aget ^bytes rand-bytes 2) 0x3F) 0x80)))
       :cljs (aset uuid-bytes 8 (bit-or (bit-and (aget rand-bytes 2) 0x3F) 0x80)))

    ;; Bytes 9-15: Random (56 bits)
    #?(:clj (System/arraycopy rand-bytes 3 uuid-bytes 9 7)
       :cljs (dotimes [i 7]
               (aset uuid-bytes (+ 9 i) (aget rand-bytes (+ 3 i)))))

    uuid-bytes))

#_{:clj-kondo/ignore [:shadowed-var]}
(defn uuid->bytes
  "Convert a platform-native UUID object to a 16-byte array.

   ## Parameters

   **uuid** - Platform-native UUID object:
   - **JVM**: `java.util.UUID`
   - **ClojureScript**: `cljs.core/UUID`

   ## Returns

   16-byte array (big-endian order):
   - **JVM**: `byte[]` (primitive byte array)
   - **ClojureScript**: `js/Uint8Array`

   ## Exceptions

   Throws `ex-info` with type `:typeid/invalid-uuid` if input is not a valid UUID object.

   ## Examples

   ```clojure
   ;; Convert UUID to bytes
   (uuid->bytes #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\")
   ;;=> #object[\"[B\" 0x...] (16-byte array)

   ;; Extract UUID from parsed TypeID and convert to bytes
   (require '[typeid.core :as typeid])
   (let [tid (typeid/create \"user\")
         parsed (typeid/parse tid)
         uuid (:uuid parsed)]
     (uuid->bytes uuid))
   ;;=> #object[\"[B\" ...] (16 bytes)

   ;; Use for binary storage or protocols
   (let [uuid #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"
         bytes (uuid->bytes uuid)]
     (alength bytes))
   ;;=> 16
   ```

   See also: [[bytes->uuid]], [[generate-uuidv7]], [[typeid.core/parse]]"
  [uuid]
  #?(:clj
     (if (instance? java.util.UUID uuid)
       (let [uuid-bytes (byte-array 16)
             msb (.getMostSignificantBits ^java.util.UUID uuid)
             lsb (.getLeastSignificantBits ^java.util.UUID uuid)]
         ;; Most significant bits (bytes 0-7)
         (aset uuid-bytes 0 (unchecked-byte (bit-shift-right msb 56)))
         (aset uuid-bytes 1 (unchecked-byte (bit-shift-right msb 48)))
         (aset uuid-bytes 2 (unchecked-byte (bit-shift-right msb 40)))
         (aset uuid-bytes 3 (unchecked-byte (bit-shift-right msb 32)))
         (aset uuid-bytes 4 (unchecked-byte (bit-shift-right msb 24)))
         (aset uuid-bytes 5 (unchecked-byte (bit-shift-right msb 16)))
         (aset uuid-bytes 6 (unchecked-byte (bit-shift-right msb 8)))
         (aset uuid-bytes 7 (unchecked-byte msb))
         ;; Least significant bits (bytes 8-15)
         (aset uuid-bytes 8 (unchecked-byte (bit-shift-right lsb 56)))
         (aset uuid-bytes 9 (unchecked-byte (bit-shift-right lsb 48)))
         (aset uuid-bytes 10 (unchecked-byte (bit-shift-right lsb 40)))
         (aset uuid-bytes 11 (unchecked-byte (bit-shift-right lsb 32)))
         (aset uuid-bytes 12 (unchecked-byte (bit-shift-right lsb 24)))
         (aset uuid-bytes 13 (unchecked-byte (bit-shift-right lsb 16)))
         (aset uuid-bytes 14 (unchecked-byte (bit-shift-right lsb 8)))
         (aset uuid-bytes 15 (unchecked-byte lsb))
         uuid-bytes)
       (throw (ex-info "Invalid UUID: expected platform-native UUID object"
                {:type :typeid/invalid-uuid
                 :message "Invalid UUID type: expected java.util.UUID"
                 :input uuid
                 :expected "java.util.UUID"
                 :actual (str (type uuid))})))
     :cljs
     (if (uuid? uuid)
       ;; In ClojureScript, UUID objects are stored as strings internally
       ;; We need to parse the hex string representation
       (let [hex-str (str uuid)
             ;; Remove hyphens from UUID string (e.g., "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a" -> "018c3f9e9e4e7a8a8b2a7e8e9e4e7a8a")
             clean-hex (.replace hex-str (js/RegExp. "-" "g") "")
             uuid-bytes (js/Uint8Array. 16)]
         ;; Parse hex string to bytes
         (dotimes [i 16]
           (let [hex-byte (.substr clean-hex (* i 2) 2)
                 byte-val (js/parseInt hex-byte 16)]
             (aset uuid-bytes i byte-val)))
         uuid-bytes)
       (throw (ex-info "Invalid UUID: expected platform-native UUID object"
                {:type :typeid/invalid-uuid
                 :message "Invalid UUID type: expected cljs.core/UUID"
                 :input uuid
                 :expected "cljs.core/UUID"
                 :actual (str (type uuid))})))))

(defn bytes->uuid
  "Convert a 16-byte array to a platform-native UUID object.

   This is the inverse operation of [[uuid->bytes]].

   ## Parameters

   **uuid-bytes** - 16-byte array:
   - **JVM**: `byte[]` (primitive byte array)
   - **ClojureScript**: `js/Uint8Array`

   ## Returns

   Platform-native UUID object:
   - **JVM**: `java.util.UUID`
   - **ClojureScript**: `cljs.core/UUID`

   ## Exceptions

   Throws `ex-info` with type `:typeid/invalid-uuid` if input is not exactly 16 bytes.

   ## Examples

   ```clojure
   ;; Convert bytes to UUID
   (bytes->uuid (byte-array [0x01 0x8c 0x3f 0x9e 0x9e 0x4e 0x7a 0x8a
                              0x8b 0x2a 0x7e 0x8e 0x9e 0x4e 0x7a 0x8a]))
   ;;=> #uuid \"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"

   ;; Round-trip conversion (identity property)
   (= my-uuid (-> my-uuid uuid->bytes bytes->uuid))
   ;;=> true

   ;; Generate UUIDv7 and convert to UUID object
   (bytes->uuid (generate-uuidv7))
   ;;=> #uuid \"018d5e9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"

   ;; Complete workflow for TypeID creation
   (require '[typeid.core :as typeid])
   (let [uuid-bytes (generate-uuidv7)
         uuid-obj (bytes->uuid uuid-bytes)]
     (typeid/create \"order\" uuid-obj))
   ;;=> \"order_01h455vb4pex5vsknk084sn02q\"
   ```

   See also: [[uuid->bytes]], [[generate-uuidv7]], [[typeid.core/create]]"
  [uuid-bytes]
  #?(:clj
     (if (and (bytes? uuid-bytes) (= 16 (alength ^bytes uuid-bytes)))
       (let [;; Extract most significant bits (bytes 0-7)
             msb (long (reduce (fn [acc b]
                                 (bit-or (bit-shift-left acc 8)
                                   (bit-and (long b) 0xFF)))
                         0
                         (take 8 uuid-bytes)))
             ;; Extract least significant bits (bytes 8-15)
             lsb (long (reduce (fn [acc b]
                                 (bit-or (bit-shift-left acc 8)
                                   (bit-and (long b) 0xFF)))
                         0
                         (drop 8 uuid-bytes)))]
         (java.util.UUID. msb lsb))
       (throw (ex-info "Invalid UUID bytes: expected exactly 16 bytes"
                {:type :typeid/invalid-uuid
                 :message "UUID must be exactly 16 bytes"
                 :input uuid-bytes
                 :expected "16-byte array"
                 :actual (if (bytes? uuid-bytes)
                           (str (alength ^bytes uuid-bytes) " bytes")
                           "not a byte array")})))
     :cljs
     (if (and (instance? js/Uint8Array uuid-bytes) (= 16 (.-length uuid-bytes)))
       ;; Convert bytes to hex string, then to UUID
       (let [hex-str (apply str (map (fn [b]
                                       (let [hex (.toString b 16)]
                                         (if (= 1 (.-length hex))
                                           (str "0" hex)
                                           hex)))
                                  uuid-bytes))
             ;; Insert hyphens at proper positions for UUID format
             ;; xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
             formatted-uuid (str (.substr hex-str 0 8) "-"
                              (.substr hex-str 8 4) "-"
                              (.substr hex-str 12 4) "-"
                              (.substr hex-str 16 4) "-"
                              (.substr hex-str 20 12))]
         (cljs.core/uuid formatted-uuid))
       (throw (ex-info "Invalid UUID bytes: expected exactly 16 bytes"
                {:type :typeid/invalid-uuid
                 :message "UUID must be exactly 16 bytes"
                 :input uuid-bytes
                 :expected "16-byte Uint8Array"
                 :actual (if (instance? js/Uint8Array uuid-bytes)
                           (str (.-length uuid-bytes) " bytes")
                           "not a Uint8Array")})))))
