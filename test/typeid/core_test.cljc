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
          result (t/parse typeid)]
         (is (map? result))
         (is (= "user" (:prefix result)))
         (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix result)))
         (is (= typeid (:typeid result)))
         (is (some? (:uuid result)))
         (is (v/valid-uuid-bytes? (:uuid result)))))

  (testing "Parse TypeID without prefix"
    (let [typeid "01h5fskfsk4fpeqwnsyz5hj55t"
          result (t/parse typeid)]
         (is (= "" (:prefix result)))
         (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix result)))
         (is (= typeid (:typeid result)))
         (is (v/valid-uuid-bytes? (:uuid result)))))

  (testing "Parse TypeID with underscores in prefix"
    (let [typeid "my_user_type_01h5fskfsk4fpeqwnsyz5hj55t"
          result (t/parse typeid)]
         (is (= "my_user_type" (:prefix result)))
         (is (= "01h5fskfsk4fpeqwnsyz5hj55t" (:suffix result))))))

(deftest parse-invalid-typeid-test
  (testing "Reject TypeID with uppercase"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                          #"(?i)lowercase"
                          (t/parse "User_01h5fskfsk4fpeqwnsyz5hj55t"))))

  (testing "Reject TypeID that's too short"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                          #"(?i)26.*90|length"
                          (t/parse "user_tooshort"))))

  (testing "Reject TypeID that's too long"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                          #"(?i)26.*90|length"
                          (t/parse (str "user_" (apply str (repeat 100 "a")))))))

  (testing "Reject TypeID with invalid base32 character"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                          #"(?i)invalid.*suffix|base32|character"
                          (t/parse "user_01h5fskfsk4fpeqwnsyz5hj5il")))) ; 'i' and 'l' are invalid

  (testing "Reject TypeID with suffix starting with 8-z"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                          #"(?i)invalid.*suffix|overflow|0-7"
                          (t/parse "user_8zzzzzzzzzzzzzzzzzzzzzzzzz"))))

  (testing "Reject non-string input"
    (is (thrown-with-msg? #?(:clj Exception :cljs js/Error)
                          #"(?i)string"
                          (t/parse 12345)))))

(deftest generate-parse-round-trip-test
  (testing "Generate and parse round-trip preserves data"
    (let [prefixes ["user" "order" "session" "" "a" "my_type"]]
         (doseq [prefix prefixes]
                (let [typeid (t/generate prefix)
                      parsed (t/parse typeid)]
                     (is (= prefix (:prefix parsed)))
                     (is (= typeid (:typeid parsed)))
                     (is (= 26 (count (:suffix parsed))))
                     (is (v/valid-uuid-bytes? (:uuid parsed)))))))

  (testing "Parse and re-generate produces different UUID but same prefix"
    (let [typeid1 (t/generate "user")
          parsed1 (t/parse typeid1)
          typeid2 (t/generate "user")
          parsed2 (t/parse typeid2)]
         (is (= "user" (:prefix parsed1) (:prefix parsed2)))
         (is (not= typeid1 typeid2)) ; Different UUIDs
         (is (not= (:suffix parsed1) (:suffix parsed2))))))
