(ns typeid.impl.base32
  "Base32 encoding/decoding using Crockford alphabet for TypeID suffixes.

   The Crockford base32 alphabet excludes ambiguous characters (i, l, o, u)
   and uses 0-9a-z (32 characters total)."
  #?(:clj (:import [java.math BigInteger])))

#?(:clj (set! *warn-on-reflection* true))

;; T013: Encode alphabet (index → character)
(def ^:private encode-alphabet
  "Crockford base32 alphabet for encoding 5-bit values to characters.
   Maps index [0-31] to character ['0'-'9', 'a'-'z' excluding i,l,o,u]."
  "0123456789abcdefghjkmnpqrstvwxyz")

;; T014: Decode map (character → index)
(def ^:private decode-map
  "Reverse lookup map for decoding base32 characters to 5-bit values."
  {\0  0, \1  1, \2  2, \3  3, \4  4, \5  5, \6  6, \7  7,
   \8  8, \9  9, \a 10, \b 11, \c 12, \d 13, \e 14, \f 15,
   \g 16, \h 17, \j 18, \k 19, \m 20, \n 21, \p 22, \q 23,
   \r 24, \s 25, \t 26, \v 27, \w 28, \x 29, \y 30, \z 31})

;; T015: Encode function (UUID bytes → 26-char base32 string)
(defn encode
  "Encode 16 UUID bytes as a 26-character base32 string.

   Uses Crockford base32 alphabet (0-9a-z excluding i,l,o,u).
   The 128-bit UUID is encoded as 26 base32 characters."
  ^String [uuid-bytes]
  #?(:clj
     (let [;; Convert bytes to BigInteger (unsigned)
           big-int (BigInteger. 1 ^bytes uuid-bytes)
           sb (StringBuilder. 26)]
       ;; Extract each 5-bit group from right to left
       (loop [n big-int
              chars-left 26]
         (when (pos? chars-left)
           (let [remainder (.remainder n (BigInteger/valueOf 32))
                 idx (.intValue remainder)
                 ch (.charAt encode-alphabet idx)]
             (.append sb ch)
             (recur (.divide n (BigInteger/valueOf 32))
               (dec chars-left)))))
       ;; Reverse to get correct order
       (.reverse sb)
       (.toString sb))
     :cljs
     ;; ClojureScript version: simpler bit manipulation
     (let [result (atom [])]
       (loop [i 0]
         (when (< i 26)
           (let [bit-start (* i 5)
                 byte-idx (quot bit-start 8)
                 bit-offset (rem bit-start 8)
                 byte1 (if (< byte-idx 16) (aget uuid-bytes byte-idx) 0)
                 byte2 (if (< (inc byte-idx) 16) (aget uuid-bytes (inc byte-idx)) 0)
                 ;; Extract 5 bits
                 bits (bit-and 0x1F
                        (bit-or (bit-shift-right byte1 (- 8 bit-offset 5))
                          (bit-shift-right (bit-shift-left byte2 8) (- 8 bit-offset 5))))
                 ch (.charAt encode-alphabet bits)]
             (swap! result conj ch)
             (recur (inc i)))))
       (apply str @result))))

;; T016: Decode function (26-char base32 string → UUID bytes)
(defn decode
  "Decode a 26-character base32 string to 16 UUID bytes.

   The first character must be 0-7 to ensure the decoded value fits in 128 bits.

   Throws ex-info if invalid format."
  [^String base32-str]
  (when-not (= 26 (count base32-str))
    (throw (ex-info "Base32 suffix must be exactly 26 characters"
             {:type :invalid-suffix-length
              :suffix base32-str
              :length (count base32-str)})))

  ;; Check first character for overflow
  (let [first-ch (.charAt base32-str 0)
        first-val (get decode-map first-ch)]
    (when-not first-val
      (throw (ex-info "Invalid base32 character"
               {:type :invalid-base32-char
                :char first-ch
                :position 0})))
    (when (> first-val 7)
      (throw (ex-info "First character of suffix must be 0-7 to prevent 128-bit overflow"
               {:type :suffix-overflow
                :suffix base32-str
                :first-char first-ch
                :max-allowed-char \7}))))

  #?(:clj
     ;; JVM: Use BigInteger for decoding
     (let [;; Convert base32 string to BigInteger
           big-int (loop [i 0
                          acc (BigInteger/valueOf 0)]
                     (if (< i 26)
                       (let [ch (.charAt base32-str i)
                             digit-val (or (get decode-map ch)
                                         (throw (ex-info "Invalid base32 character"
                                                  {:type :invalid-base32-char
                                                   :char ch
                                                   :position i})))]
                         (recur (inc i)
                           (.add (.multiply acc (BigInteger/valueOf 32))
                             (BigInteger/valueOf digit-val))))
                       acc))
           ;; Convert to byte array (16 bytes)
           byte-arr (.toByteArray big-int)]
       ;; Handle padding: BigInteger may add a sign byte or have fewer than 16 bytes
       (cond
         (= 16 (alength byte-arr)) byte-arr
         (< (alength byte-arr) 16) (let [result (byte-array 16)]
                                     (System/arraycopy byte-arr 0 result (- 16 (alength byte-arr)) (alength byte-arr))
                                     result)
         :else (let [result (byte-array 16)]
                 (System/arraycopy byte-arr 1 result 0 16)
                 result)))
     :cljs
     ;; ClojureScript: Simple implementation
     (let [uuid-bytes (js/Uint8Array. 16)]
       (loop [char-idx 0
              acc 0
              acc-bits 0]
         (if (< char-idx 26)
           (let [ch (.charAt base32-str char-idx)
                 digit-val (or (get decode-map ch)
                             (throw (ex-info "Invalid base32 character"
                                      {:type :invalid-base32-char
                                       :char ch
                                       :position char-idx})))
                 new-acc (bit-or (bit-shift-left acc 5) digit-val)
                 new-bits (+ acc-bits 5)]
             (if (>= new-bits 8)
               (let [byte-val (bit-and (bit-shift-right new-acc (- new-bits 8)) 0xFF)
                     byte-idx (quot (- (* 26 5) new-bits) 8)]
                 (when (< byte-idx 16)
                   (aset uuid-bytes byte-idx byte-val))
                 (recur (inc char-idx)
                   (bit-and new-acc (dec (bit-shift-left 1 (- new-bits 8))))
                   (- new-bits 8)))
               (recur (inc char-idx) new-acc new-bits)))
           uuid-bytes)))))
