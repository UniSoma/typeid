(ns ^:no-doc typeid.impl.uuid
  "UUIDv7 generation with platform-specific time and random sources."
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
  "Generate a UUIDv7 (RFC 9562) as 16 bytes.

   UUIDv7 structure:
   - Bytes 0-5: Unix timestamp in milliseconds (48 bits)
   - Byte 6: Sub-millisecond precision + version bits (4 bits = 0111 for v7)
   - Byte 7: Variant bits (2 bits = 10) + random (6 bits)
   - Bytes 8-15: Random data (64 bits)

   Returns a byte array of exactly 16 bytes."
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

   Accepts:
   - JVM: java.util.UUID
   - ClojureScript: cljs.core/UUID

   Returns 16-byte array representation of the UUID.
   Throws if input is not a valid UUID object."
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
