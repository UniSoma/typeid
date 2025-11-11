(ns typeid.core-test
  (:require [clojure.test :refer [deftest is testing]]
    [typeid.core :as t]
    [typeid.validation :as v]))

;; T023: Unit tests for generate function
(deftest generate-with-prefix-test
  (testing "Generate TypeID with valid prefix"
    (let [typeid (t/generate "user")]
      (is (string? typeid))
      (is (= 31 (count typeid))) ; "user" (4) + "_" (1) + suffix (26)
      (is (.startsWith typeid "user_"))
      (is (= 26 (count (subs typeid 5)))))) ; suffix is 26 chars

  (testing "Generate TypeID with empty prefix"
    (let [typeid (t/generate "")]
      (is (string? typeid))
      (is (= 26 (count typeid))) ; just suffix, no prefix
      (is (not (.contains typeid "_"))))) ; no separator

  (testing "Generate TypeID with various valid prefixes"
    (doseq [prefix ["a" "abc" "my_type" "user_account"]]
      (let [typeid (t/generate prefix)]
        (is (.startsWith typeid (str prefix "_")))
        (is (= (+ (count prefix) 1 26) (count typeid))))))

  (testing "Generated TypeIDs are unique"
    (let [id1 (t/generate "user")
          id2 (t/generate "user")]
      (is (not= id1 id2))
      (is (.startsWith id1 "user_"))
      (is (.startsWith id2 "user_"))))

  (testing "Generated TypeID suffix starts with 0-7"
    (dotimes [_ 20] ; Test multiple times due to randomness
      (let [typeid (t/generate "test")
            suffix (subs typeid 5) ; Skip "test_"
            first-char (first suffix)]
        (is (contains? #{\0 \1 \2 \3 \4 \5 \6 \7} first-char)
          (str "First character of suffix must be 0-7, got: " first-char))))))

(deftest generate-with-invalid-prefix-test
  (testing "Reject prefix with uppercase"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern|lowercase"
          (t/generate "User"))))

  (testing "Reject prefix with numbers"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern"
          (t/generate "user123"))))

  (testing "Reject prefix that's too long"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*63|too long"
          (t/generate (apply str (repeat 64 "a"))))))

  (testing "Reject prefix starting with underscore"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
          #"(?i)prefix.*pattern"
          (t/generate "_user")))))

;; T024: Unit tests for parse function
(deftest parse-valid-typeid-test
  (testing "Parse TypeID with prefix"
    (let [typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"
          {:keys [ok error]} (t/parse typeid)]
      (is (nil? error))
      (is (map? ok))
      (is (= "user" (:prefix ok)))
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix ok)))
      (is (= typeid (:typeid ok)))
      (is (some? (:uuid ok)))
      (is (v/valid-uuid-bytes? (:uuid ok)))))

  (testing "Parse TypeID without prefix"
    (let [typeid "01h5fskfsk4fpeqwnsyz5hj55t"
          {:keys [ok error]} (t/parse typeid)]
      (is (nil? error))
      (is (= "" (:prefix ok)))
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix ok)))
      (is (= typeid (:typeid ok)))
      (is (v/valid-uuid-bytes? (:uuid ok)))))

  (testing "Parse TypeID with underscores in prefix"
    (let [typeid "my_user_type_01h5fskfsk4fpeqwnsyz5hj55t"
          {:keys [ok error]} (t/parse typeid)]
      (is (nil? error))
      (is (= "my_user_type" (:prefix ok)))
      (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix ok))))))

(deftest parse-invalid-typeid-test
  (testing "Reject TypeID with uppercase"
    (let [{:keys [ok error]} (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-case (:type error)))))

  (testing "Reject TypeID that's too short"
    (let [{:keys [ok error]} (t/parse "user_tooshort")]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-length (:type error)))))

  (testing "Reject TypeID that's too long"
    (let [{:keys [ok error]} (t/parse (str "user_" (apply str (repeat 100 "a"))))]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-length (:type error)))))

  (testing "Reject TypeID with invalid base32 character"
    (let [{:keys [ok error]} (t/parse "user_01h5fskfsk4fpeqwnsyz5hj5il")] ; 'i' and 'l' are invalid
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-suffix (:type error)))))

  (testing "Reject TypeID with suffix starting with 8-z"
    (let [{:keys [ok error]} (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-suffix (:type error)))))

  (testing "Reject non-string input"
    (let [{:keys [ok error]} (t/parse 12345)]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-typeid-type (:type error))))))

(deftest generate-parse-round-trip-test
  (testing "Generate and parse round-trip preserves data"
    (let [prefixes ["user" "order" "session" "" "a" "my_type"]]
      (doseq [prefix prefixes]
        (let [typeid (t/generate prefix)
              {parsed :ok} (t/parse typeid)]
          (is (= prefix (:prefix parsed)))
          (is (= typeid (:typeid parsed)))
          (is (= 26 (count (:suffix parsed))))
          (is (v/valid-uuid-bytes? (:uuid parsed)))))))

  (testing "Parse and re-generate produces different UUID but same prefix"
    (let [typeid1 (t/generate "user")
          {parsed1 :ok} (t/parse typeid1)
          typeid2 (t/generate "user")
          {parsed2 :ok} (t/parse typeid2)]
      (is (= "user" (:prefix parsed1) (:prefix parsed2)))
      (is (not= typeid1 typeid2)) ; Different UUIDs
      (is (not= (:suffix parsed1) (:suffix parsed2))))))

;; T034: Unit tests for validate function (User Story 2)
(deftest validate-valid-typeid-test
  (testing "Validate correct TypeID with prefix"
    (let [{:keys [ok error]} (t/validate "user_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (nil? error))
      (is (= true ok))))

  (testing "Validate correct TypeID without prefix"
    (let [{:keys [ok error]} (t/validate "01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (nil? error))
      (is (= true ok))))

  (testing "Validate TypeID with underscores in prefix"
    (let [{:keys [ok error]} (t/validate "my_user_type_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (nil? error))
      (is (= true ok)))))

(deftest validate-invalid-typeid-test
  (testing "Reject TypeID with uppercase"
    (let [{:keys [ok error]} (t/validate "User_01h5fskfsk4fpeqwnsyz5hj55t")]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-case (:type error)))))

  (testing "Reject TypeID that's too short"
    (let [{:keys [ok error]} (t/validate "user_tooshort")]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-length (:type error)))))

  (testing "Reject TypeID that's too long"
    (let [{:keys [ok error]} (t/validate (str "user_" (apply str (repeat 100 "a"))))]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-length (:type error)))))

  (testing "Reject TypeID with invalid base32 character"
    (let [{:keys [ok error]} (t/validate "user_01h5fskfsk4fpeqwnsyz5hj5il")] ; 'i' and 'l' are invalid
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-suffix (:type error)))))

  (testing "Reject TypeID with suffix starting with 8-z"
    (let [{:keys [ok error]} (t/validate "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-suffix (:type error)))))

  (testing "Reject non-string input"
    (let [{:keys [ok error]} (t/validate 12345)]
      (is (nil? ok))
      (is (some? error))
      (is (= :invalid-typeid-type (:type error))))))

(deftest validate-vs-parse-test
  (testing "Validate should accept same inputs as parse"
    (let [typeids ["user_01h5fskfsk4fpeqwnsyz5hj55t"
                   "01h5fskfsk4fpeqwnsyz5hj55t"
                   "my_type_01h5fskfsk4fpeqwnsyz5hj55t"]]
      (doseq [typeid typeids]
        (let [validate-result (t/validate typeid)
              parse-result (t/parse typeid)]
          (is (nil? (:error validate-result)))
          (is (nil? (:error parse-result)))
          (is (= true (:ok validate-result)))
          (is (some? (:ok parse-result))))))))

;; T036: Edge case tests
(deftest edge-case-prefix-tests
  (testing "Maximum length prefix (63 characters)"
    (let [max-prefix (str "a" (apply str (repeat 61 "b")) "z") ; 63 chars: a + 61 b's + z
          typeid (t/generate max-prefix)]
      (is (string? typeid))
      (is (= (+ 63 1 26) (count typeid))) ; 63 prefix + 1 separator + 26 suffix
      (is (.startsWith typeid max-prefix))

      ;; Should parse successfully
      (let [{:keys [ok error]} (t/parse typeid)]
        (is (nil? error))
        (is (= max-prefix (:prefix ok))))

      ;; Should validate successfully
      (let [{:keys [ok error]} (t/validate typeid)]
        (is (nil? error))
        (is (= true ok)))))

  (testing "Consecutive underscores in prefix"
    (let [prefix "my__type__name" ; Multiple consecutive underscores
          typeid (t/generate prefix)]
      (is (string? typeid))
      (is (.startsWith typeid prefix))

      ;; Should parse successfully
      (let [{:keys [ok error]} (t/parse typeid)]
        (is (nil? error))
        (is (= prefix (:prefix ok))))

      ;; Should validate successfully
      (let [{:keys [ok error]} (t/validate typeid)]
        (is (nil? error))
        (is (= true ok)))))

  (testing "Prefix boundary characters"
    ;; Single character prefix (minimum non-empty)
    (let [typeid-a (t/generate "a")]
      (is (= 28 (count typeid-a))) ; 1 + 1 + 26
      (let [{:keys [ok]} (t/parse typeid-a)]
        (is (= "a" (:prefix ok)))))

    ;; Two character prefix
    (let [typeid-ab (t/generate "ab")]
      (is (= 29 (count typeid-ab))) ; 2 + 1 + 26
      (let [{:keys [ok]} (t/parse typeid-ab)]
        (is (= "ab" (:prefix ok)))))

    ;; Three character prefix (recommended minimum)
    (let [typeid-abc (t/generate "abc")]
      (is (= 30 (count typeid-abc))) ; 3 + 1 + 26
      (let [{:keys [ok]} (t/parse typeid-abc)]
        (is (= "abc" (:prefix ok))))))

  (testing "Prefix must start and end with letter (not underscore)"
    ;; Valid: starts and ends with letter
    (let [valid-prefixes ["a" "ab" "a_b" "abc_def" "user_account_type"]]
      (doseq [prefix valid-prefixes]
        (let [typeid (t/generate prefix)]
          (is (string? typeid) (str "Should generate TypeID for prefix: " prefix))
          (let [{:keys [ok error]} (t/parse typeid)]
            (is (nil? error) (str "Should parse successfully for prefix: " prefix))
            (is (= prefix (:prefix ok)))))))

    ;; Invalid: starts with underscore
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/generate "_invalid")))

    ;; Invalid: ends with underscore
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (t/generate "invalid_")))))

(deftest edge-case-suffix-tests
  (testing "Suffix boundary: first character must be 0-7"
    ;; Valid: first char in 0-7
    (doseq [first-char [\0 \1 \2 \3 \4 \5 \6 \7]]
      (let [valid-suffix (str first-char (apply str (repeat 25 "z")))
            valid-typeid (str "test_" valid-suffix)
            {:keys [ok error]} (t/parse valid-typeid)]
        (is (nil? error) (str "Should accept suffix starting with: " first-char))
        (is (some? ok))))

    ;; Invalid: first char 8-z
    (doseq [first-char [\8 \9 \a \b \c \d \e \f \g \h \j \k \m \n \p \q \r \s \t \v \w \x \y \z]]
      (let [invalid-suffix (str first-char (apply str (repeat 25 "z")))
            invalid-typeid (str "test_" invalid-suffix)
            {:keys [ok error]} (t/parse invalid-typeid)]
        (is (nil? ok) (str "Should reject suffix starting with: " first-char))
        (is (= :invalid-suffix (:type error))))))

  (testing "Suffix must be exactly 26 characters"
    ;; Too short (25 chars)
    (let [{:keys [ok error]} (t/parse "test_01h5fskfsk4fpeqwnsyz5hj5")]
      (is (nil? ok))
      (is (= :invalid-suffix (:type error))))

    ;; Too long (28 chars)
    (let [{:keys [ok error]} (t/parse "test_01h5fskfsk4fpeqwnsyz5hj55tt")]
      (is (nil? ok))
      (is (= :invalid-suffix (:type error))))))

(deftest edge-case-typeid-length-tests
  (testing "Minimum TypeID length (26 chars - no prefix)"
    (let [min-typeid "01h5fskfsk4fpeqwnsyz5hj55t"
          {:keys [ok error]} (t/parse min-typeid)]
      (is (nil? error))
      (is (= "" (:prefix ok)))
      (is (= 26 (count min-typeid)))))

  (testing "Maximum TypeID length (90 chars - 63 prefix + 1 sep + 26 suffix)"
    (let [max-prefix (str "a" (apply str (repeat 61 "b")) "z") ; 63 chars
          max-typeid (t/generate max-prefix)
          {:keys [ok error]} (t/parse max-typeid)]
      (is (nil? error))
      (is (= max-prefix (:prefix ok)))
      (is (= 90 (count max-typeid)))))

  (testing "Length boundaries"
    ;; 25 chars: too short
    (let [{:keys [ok error]} (t/parse "0h5fskfsk4fpeqwnsyz5hj55t")]
      (is (nil? ok))
      (is (= :invalid-length (:type error))))

    ;; 91 chars: too long (64-char prefix + sep + 26 suffix)
    (let [too-long-prefix (apply str (repeat 64 "a"))
          too-long-typeid (str too-long-prefix "_01h5fskfsk4fpeqwnsyz5hj55t")
          {:keys [ok error]} (t/parse too-long-typeid)]
      (is (nil? ok))
      (is (= :invalid-length (:type error))))))
