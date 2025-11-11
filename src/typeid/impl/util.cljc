(ns typeid.impl.util
    "Shared utility functions for TypeID implementation."
    (:require [clojure.string :as str]))

(set! *warn-on-reflection* true)

;; T021: Utility functions
(defn split-typeid
  "Split TypeID string into prefix and suffix.
   Returns a vector [prefix suffix].
   If no separator found, treats entire string as suffix with empty prefix."
  [^String typeid-str]
  (let [last-underscore (.lastIndexOf typeid-str "_")]
       (if (= -1 last-underscore)
           ["" typeid-str]
           [(.substring typeid-str 0 last-underscore)
            (.substring typeid-str (inc last-underscore))])))

(defn join-typeid
  "Join prefix and suffix into TypeID string.
   Adds separator underscore only if prefix is non-empty."
  ^String [^String prefix ^String suffix]
  (if (empty? prefix)
      suffix
      (str prefix "_" suffix)))
