(ns typeid.impl.uuid
  "UUIDv7 generation with platform-specific time and random sources."
  #?(:clj (:import [java.security SecureRandom])))

(set! *warn-on-reflection* true)

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
                bytes (byte-array n)]
            (.nextBytes rng bytes)
            bytes)
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
  []
  (let [timestamp (current-timestamp-ms)
        rand-bytes (random-bytes 10) ;; 10 random bytes (80 bits)
        uuid #?(:clj (byte-array 16)
                :cljs (js/Uint8Array. 16))]

    ;; Set timestamp (bytes 0-5): 48 bits
    #?(:clj (do
              (aset uuid 0 (unchecked-byte (bit-shift-right timestamp 40)))
              (aset uuid 1 (unchecked-byte (bit-shift-right timestamp 32)))
              (aset uuid 2 (unchecked-byte (bit-shift-right timestamp 24)))
              (aset uuid 3 (unchecked-byte (bit-shift-right timestamp 16)))
              (aset uuid 4 (unchecked-byte (bit-shift-right timestamp 8)))
              (aset uuid 5 (unchecked-byte timestamp)))
       :cljs (do
               (aset uuid 0 (bit-shift-right timestamp 40))
               (aset uuid 1 (bit-and (bit-shift-right timestamp 32) 0xFF))
               (aset uuid 2 (bit-and (bit-shift-right timestamp 24) 0xFF))
               (aset uuid 3 (bit-and (bit-shift-right timestamp 16) 0xFF))
               (aset uuid 4 (bit-and (bit-shift-right timestamp 8) 0xFF))
               (aset uuid 5 (bit-and timestamp 0xFF))))

    ;; Byte 6: Random (12 bits in low nibble) + version (4 bits = 0111 in high nibble)
    #?(:clj (aset uuid 6 (unchecked-byte (bit-or (bit-and (aget rand-bytes 0) 0x0F) 0x70)))
       :cljs (aset uuid 6 (bit-or (bit-and (aget rand-bytes 0) 0x0F) 0x70)))

    ;; Byte 7: Random (8 bits)
    #?(:clj (aset uuid 7 (aget rand-bytes 1))
       :cljs (aset uuid 7 (aget rand-bytes 1)))

    ;; Byte 8: Variant (2 bits = 10 in high 2 bits) + random (6 bits in low 6 bits)
    #?(:clj (aset uuid 8 (unchecked-byte (bit-or (bit-and (aget rand-bytes 2) 0x3F) 0x80)))
       :cljs (aset uuid 8 (bit-or (bit-and (aget rand-bytes 2) 0x3F) 0x80)))

    ;; Bytes 9-15: Random (56 bits)
    #?(:clj (System/arraycopy rand-bytes 3 uuid 9 7)
       :cljs (dotimes [i 7]
               (aset uuid (+ 9 i) (aget rand-bytes (+ 3 i)))))

    uuid))
