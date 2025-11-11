(ns typeid.validation
    "Validation predicates for TypeID components.

   Manual validation predicates with zero external dependencies.")

(set! *warn-on-reflection* true)

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

   Valid prefixes:
   - Empty string
   - 1-63 lowercase characters matching pattern: [a-z]([a-z_]{0,61}[a-z])?"
  [s]
  (and (string? s)
       (<= (count s) 63)
       (boolean (re-matches prefix-pattern s))))

(defn validate-prefix
  "Validate prefix, return {:ok prefix} or {:error error-map}."
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
  "Check if suffix is valid:
   - Exactly 26 characters
   - All characters in base32 alphabet
   - First character <= 7 (prevents overflow)"
  [s]
  (and (string? s)
       (= 26 (count s))
       (<= (int (first s)) (int \7))
       (every? base32-chars s)))

(defn valid-typeid-string?
  "Check if string is a valid TypeID format.
   - Length between 26 and 90 characters (26 for suffix only, up to 63+1+26 with prefix)
   - All lowercase"
  [s]
  (and (string? s)
       (<= 26 (count s) 90)
       (= s (clojure.string/lower-case s))))

;; T019: UUID bytes validation predicates
(defn valid-uuid-bytes?
  "Check if bytes represent a valid UUID (exactly 16 bytes)."
  [b]
  #?(:clj (and (bytes? b) (= 16 (alength ^bytes b)))
     :cljs (and (instance? js/Uint8Array b) (= 16 (.-length b)))))

(defn valid-uuidv7-bytes?
  "Stricter validation for generated UUIDs (version 7, variant 10).
   Checks:
   - Exactly 16 bytes
   - Version bits (48-51) = 0111 (7)
   - Variant bits (64-65) = 10"
  [b]
  (and (valid-uuid-bytes? b)
       #?(:clj (let [^bytes bytes b
                     ;; Mask to unsigned to avoid sign extension issues
                     byte6 (bit-and (aget bytes 6) 0xFF)
                     byte8 (bit-and (aget bytes 8) 0xFF)]
                    (and (= 7 (bit-and (bit-shift-right byte6 4) 0x0F))
                         (= 2 (bit-and (bit-shift-right byte8 6) 0x03))))
          :cljs (and (= 7 (bit-and (bit-shift-right (aget b 6) 4) 0x0F))
                     (= 2 (bit-and (bit-shift-right (aget b 8) 6) 0x03))))))
