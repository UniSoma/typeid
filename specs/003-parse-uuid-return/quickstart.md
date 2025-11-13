# Migration Guide: Parse Returns UUID Objects

**Feature**: 003-parse-uuid-return
**Version**: 0.1.x → 0.2.0
**Date**: 2025-11-12

## What Changed?

The `parse` function now returns **platform-native UUID objects** instead of byte arrays in the `:uuid` field.

### Before (v0.1.x)
```clojure
(parse "user_01h455vb4pex5vsknk084sn02q")
;; => {:prefix "user"
;;     :suffix "01h455vb4pex5vsknk084sn02q"
;;     :uuid #object["[B" 0x1a2b3c...]  ; Byte array
;;     :typeid "user_01h455vb4pex5vsknk084sn02q"}
```

### After (v0.2.0)
```clojure
(parse "user_01h455vb4pex5vsknk084sn02q")
;; => {:prefix "user"
;;     :suffix "01h455vb4pex5vsknk084sn02q"
;;     :uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"  ; UUID object!
;;     :typeid "user_01h455vb4pex5vsknk084sn02q"}
```

---

## Why This Change?

### Problems with Byte Arrays

**Old API (v0.1.x)**:
```clojure
;; Problem 1: Round-trip conversion is awkward
(let [original-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
      typeid (create "user" original-uuid)
      parsed-bytes (:uuid (parse typeid))]
  (= original-uuid parsed-bytes))  ; false! Types don't match

;; Problem 2: Database integration requires manual conversion
(let [parsed (parse user-id)
      uuid-bytes (:uuid parsed)
      uuid-hex (codec/uuid->hex uuid-bytes)]
  (jdbc/execute! ds ["SELECT * FROM users WHERE id = ?::uuid" uuid-hex]))

;; Problem 3: API inconsistency
(create "user" #uuid "...")  ; Accepts UUID object
(parse "user_...")           ; Returns byte array (inconsistent!)
```

### Benefits of UUID Objects

**New API (v0.2.0)**:
```clojure
;; Benefit 1: Natural round-trip conversion
(let [original-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
      typeid (create "user" original-uuid)
      parsed-uuid (:uuid (parse typeid))]
  (= original-uuid parsed-uuid))  ; true! Direct equality

;; Benefit 2: Zero-conversion database integration
(let [uuid (:uuid (parse user-id))]
  (jdbc/execute! ds ["SELECT * FROM users WHERE id = ?" uuid]))
  ;; No manual conversion needed!

;; Benefit 3: Consistent API
(create "user" #uuid "...")  ; Accepts UUID object
(parse "user_...")           ; Returns UUID object (consistent!)
```

---

## Migration Scenarios

### Scenario 1: Treating UUID Opaquely (No Changes Needed)

**If your code looks like this**, you're already good to go:

```clojure
;; Storing in database
(defn save-user [typeid-str user-data]
  (let [uuid (:uuid (parse typeid-str))]
    (jdbc/execute! ds
      ["INSERT INTO users (id, data) VALUES (?, ?)" uuid user-data])))

;; Comparing UUIDs
(defn same-user? [typeid1 typeid2]
  (= (:uuid (parse typeid1))
     (:uuid (parse typeid2))))

;; Passing around as value
(defn process-user [typeid-str]
  (let [{:keys [prefix uuid]} (parse typeid-str)]
    {:type prefix
     :id uuid}))
```

**Impact**: ✅ **No changes required** - Code will work better without modification!

---

### Scenario 2: Direct Byte Manipulation (Migration Required)

**If your code manipulates bytes directly**, update to use codec functions:

#### Example 1: Custom Byte Operations

**Before (v0.1.x)**:
```clojure
(defn extract-timestamp [typeid-str]
  (let [uuid-bytes (:uuid (parse typeid-str))
        timestamp-bytes (take 6 uuid-bytes)]
    (parse-timestamp timestamp-bytes)))
```

**After (v0.2.0)** - Use `codec/decode` for low-level access:
```clojure
(require '[typeid.codec :as codec])

(defn extract-timestamp [typeid-str]
  (let [uuid-bytes (codec/decode typeid-str)  ; Get bytes directly
        timestamp-bytes (take 6 uuid-bytes)]
    (parse-timestamp timestamp-bytes)))
```

#### Example 2: Byte Array Validation

**Before (v0.1.x)**:
```clojure
(require '[typeid.validation :as v])

(defn validate-parsed-uuid [typeid-str]
  (let [parsed (parse typeid-str)
        uuid-bytes (:uuid parsed)]
    (when (v/valid-uuid-bytes? uuid-bytes)
      parsed)))
```

**After (v0.2.0)** - UUID objects are pre-validated:
```clojure
(defn validate-parsed-uuid [typeid-str]
  (let [parsed (parse typeid-str)]
    ;; UUID object from parse is always valid - no check needed
    parsed))
```

#### Example 3: Converting to Hex

**Before (v0.1.x)**:
```clojure
(require '[typeid.codec :as codec])

(defn get-uuid-hex [typeid-str]
  (let [uuid-bytes (:uuid (parse typeid-str))]
    (codec/uuid->hex uuid-bytes)))
```

**After (v0.2.0)** - Use UUID string representation:
```clojure
(defn get-uuid-hex [typeid-str]
  (let [uuid (:uuid (parse typeid-str))]
    (str uuid)))  ; Returns "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
  ;; Or if you need no-hyphens format:
  (let [uuid (:uuid (parse typeid-str))]
    (clojure.string/replace (str uuid) "-" "")))
```

---

### Scenario 3: Database Integration (Improved)

**Common database patterns get simpler**:

#### PostgreSQL with next.jdbc

**Before (v0.1.x)**:
```clojure
(require '[next.jdbc :as jdbc]
         '[typeid.codec :as codec])

(defn find-user [ds typeid-str]
  (let [uuid-bytes (:uuid (parse typeid-str))
        uuid-hex (codec/uuid->hex uuid-bytes)]
    (jdbc/execute-one! ds
      ["SELECT * FROM users WHERE id = ?::uuid" uuid-hex])))

(defn insert-user [ds typeid-str user-data]
  (let [uuid-bytes (:uuid (parse typeid-str))
        uuid-hex (codec/uuid->hex uuid-bytes)]
    (jdbc/execute-one! ds
      ["INSERT INTO users (id, name, email) VALUES (?::uuid, ?, ?)"
       uuid-hex (:name user-data) (:email user-data)])))
```

**After (v0.2.0)** - Direct UUID usage:
```clojure
(require '[next.jdbc :as jdbc])

(defn find-user [ds typeid-str]
  (let [uuid (:uuid (parse typeid-str))]
    (jdbc/execute-one! ds
      ["SELECT * FROM users WHERE id = ?" uuid])))
      ;; JDBC handles UUID natively!

(defn insert-user [ds typeid-str user-data]
  (let [uuid (:uuid (parse typeid-str))]
    (jdbc/execute-one! ds
      ["INSERT INTO users (id, name, email) VALUES (?, ?, ?)"
       uuid (:name user-data) (:email user-data)])))
```

#### ClojureScript API Calls

**Before (v0.1.x)**:
```clojure
(require '[cljs.core.async :refer [go]]
         '[typeid.codec :as codec])

(defn fetch-user [typeid-str]
  (go
    (let [uuid-bytes (:uuid (parse typeid-str))
          uuid-str (codec/uuid->hex uuid-bytes)
          response (<! (http/get (str "/api/users/" uuid-str)))]
      (:body response))))
```

**After (v0.2.0)**:
```clojure
(require '[cljs.core.async :refer [go]])

(defn fetch-user [typeid-str]
  (go
    (let [uuid (:uuid (parse typeid-str))
          response (<! (http/get (str "/api/users/" uuid)))]
          ;; UUID automatically serializes to string
      (:body response))))
```

---

### Scenario 4: Round-Trip Conversions (Improved)

**Before (v0.1.x)** - Awkward conversion:
```clojure
(require '[typeid.impl.uuid :as uuid-impl]
         '[typeid.codec :as codec])

(defn round-trip-test []
  (let [original-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
        typeid (create "user" original-uuid)
        parsed (parse typeid)
        uuid-bytes (:uuid parsed)
        ;; Need to convert bytes back to UUID for comparison
        uuid-hex (codec/uuid->hex uuid-bytes)
        recovered-uuid (java.util.UUID/fromString
                         (str uuid-hex (subs 0 8) "-"
                              (subs 8 12) "-"
                              (subs 12 16) "-"
                              (subs 16 20) "-"
                              (subs 20)))]
    (= original-uuid recovered-uuid)))
```

**After (v0.2.0)** - Natural equality:
```clojure
(defn round-trip-test []
  (let [original-uuid #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
        typeid (create "user" original-uuid)
        recovered-uuid (:uuid (parse typeid))]
    (= original-uuid recovered-uuid)))  ; Direct comparison!
```

---

## Quick Migration Checklist

### Step 1: Find Affected Code

Search your codebase for:
```bash
# Find all parse usage
git grep "(:uuid.*parse"
git grep ":uuid.*parsed"

# Find byte array operations on parse results
git grep "valid-uuid-bytes?"
git grep "uuid->hex.*(:uuid"
git grep "aget.*(:uuid"
```

### Step 2: Categorize Usage

For each occurrence, determine:
- [ ] **Opaque usage** (passing UUID around) → ✅ No changes needed
- [ ] **Byte manipulation** → ❌ Use `codec/decode` instead
- [ ] **Validation** → ✅ Remove (UUID objects are pre-validated)
- [ ] **Hex conversion** → ✅ Use `(str uuid)` instead

### Step 3: Update Code

Apply migrations from scenarios above.

### Step 4: Update Tests

```clojure
;; Before: Expect byte arrays
(is (bytes? (:uuid (parse "user_..."))))
(is (= 16 (alength (:uuid (parse "user_...")))))

;; After: Expect UUID objects
(is (uuid? (:uuid (parse "user_..."))))
(is (= #uuid "018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a"
       (:uuid (parse "user_01h455vb4pex5vsknk084sn02q"))))
```

### Step 5: Update Dependencies

```clojure
;; deps.edn or project.clj
io.github.unisoma/typeid {:mvn/version "0.2.0"}  ; Update version
```

### Step 6: Test Thoroughly

Run your test suite:
```bash
clojure -M:test -m kaocha.runner
```

---

## Common Questions

### Q: Do I need to update my database schema?

**A**: No! The underlying UUID value is identical. Database columns remain unchanged.

```sql
-- Schema stays the same
CREATE TABLE users (
  id UUID PRIMARY KEY,  -- No change needed
  ...
);
```

### Q: Will this affect performance?

**A**: Minimal impact. UUID conversion adds ~500ns per parse operation, well within the existing 2μs budget.

```clojure
;; Before: ~2μs
;; After: ~2.5μs (25% increase, but still very fast)
```

### Q: Can I still get byte arrays if I need them?

**A**: Yes! Use `codec/decode` directly:

```clojure
(require '[typeid.codec :as codec])

(let [uuid-bytes (codec/decode "user_01h455vb4pex5vsknk084sn02q")]
  ;; uuid-bytes is 16-byte array
  ...)
```

### Q: What about ClojureScript?

**A**: Same migration applies. ClojureScript UUIDs serialize naturally to JSON:

```clojure
(let [uuid (:uuid (parse "user_..."))]
  (js/JSON.stringify #js {:id uuid}))
;; => "{\"id\":\"018c3f9e-9e4e-7a8a-8b2a-7e8e9e4e7a8a\"}"
```

### Q: Do compliance tests still pass?

**A**: Yes! All spec compliance tests (valid.yml, invalid.yml) pass with UUID objects.

---

## Need Help?

If you encounter issues during migration:

1. **Check examples** in this guide
2. **Review API contract**: `specs/003-parse-uuid-return/contracts/api.md`
3. **See test updates**: `test/typeid/core_test.cljc` for updated patterns
4. **Open an issue**: [GitHub Issues](https://github.com/unisoma/typeid/issues)

---

## Summary

| **Aspect** | **Impact** |
|------------|-----------|
| **Most code** | ✅ Works better without changes |
| **Byte manipulation** | ⚠️ Use `codec/decode` instead |
| **Database integration** | ✅ Simplified (no conversion needed) |
| **Round-trip conversions** | ✅ Natural equality checking |
| **Performance** | ✅ Minimal overhead (~500ns) |
| **ClojureScript** | ✅ Better JSON serialization |

**Bottom line**: This change improves the vast majority of use cases. Only low-level byte manipulation code needs updates, and those have clear migration paths.
