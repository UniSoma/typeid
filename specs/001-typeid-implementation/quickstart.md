# TypeID Quickstart Guide

**Feature**: TypeID Clojure/ClojureScript Library
**Date**: 2025-11-10
**Purpose**: Step-by-step guide to get started with the TypeID library

## What is TypeID?

TypeIDs are type-safe extensions of UUIDv7 that combine:
- **Type prefix**: Human-readable identifier type (e.g., "user", "order", "session")
- **Timestamp**: Millisecond-precision creation time (from UUIDv7)
- **Uniqueness**: Cryptographically strong randomness

**Example TypeID**: `user_01h5fskfsk4fpeqwnsyz5hj55t`
- **Prefix**: `user` (denotes this is a user ID)
- **Separator**: `_`
- **Suffix**: `01h5fskfsk4fpeqwnsyz5hj55t` (base32-encoded UUIDv7)

**Benefits**:
- **Sortable**: TypeIDs sort chronologically (timestamp in first 48 bits)
- **Type-safe**: Prefix prevents mixing IDs from different entities
- **Compact**: Base32 encoding is shorter than hex UUIDs
- **URL-safe**: All lowercase, no special characters

---

## Installation

### Clojure CLI (deps.edn)

Add to your `deps.edn`:

```clojure
{:deps
 {com.example/typeid {:mvn/version "0.1.0"}}}
```

### Leiningen (project.clj)

Add to your `project.clj`:

```clojure
:dependencies [[com.example/typeid "0.1.0"]]
```

### ClojureScript

The library works identically in ClojureScript. Add the same dependency as above.

---

## Basic Usage

### 1. Generate a TypeID

The most common operation is generating a new TypeID for an entity:

```clojure
(require '[typeid.core :as typeid])

;; Generate a user ID
(typeid/generate "user")
;;=> {:ok "user_01h5fskfsk4fpeqwnsyz5hj55t"}

;; Generate an order ID
(typeid/generate "order")
;;=> {:ok "order_01h5fskp7y2t3z9x8w6v5u4s3r"}

;; Generate a session ID
(typeid/generate "session")
;;=> {:ok "session_01h5fskq1a2b3c4d5e6f7g8h9j"}
```

**Without prefix** (for cases where type is implicit):

```clojure
(typeid/generate "")
;;=> {:ok "01h5fskfsk4fpeqwnsyz5hj55t"}
```

**Extracting the result**:

```clojure
(let [{:keys [ok error]} (typeid/generate "user")]
  (if ok
    (println "Generated TypeID:" ok)
    (println "Error:" (:message error))))
```

---

### 2. Parse a TypeID

Parse a TypeID string to extract its components:

```clojure
(typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok {:prefix "user"
;;         :suffix "01h5fskfsk4fpeqwnsyz5hj55t"
;;         :uuid #bytes[0x01 0x88 0xe5 ...]
;;         :typeid "user_01h5fskfsk4fpeqwnsyz5hj55t"}}
```

**Extract just the prefix**:

```clojure
(let [{:keys [ok]} (typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")]
  (:prefix ok))
;;=> "user"
```

**Extract the UUID bytes**:

```clojure
(let [{:keys [ok]} (typeid/parse "user_01h5fskfsk4fpeqwnsyz5hj55t")]
  (:uuid ok))
;;=> #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d ...]
```

---

### 3. Validate a TypeID

Quickly check if a string is a valid TypeID:

```clojure
;; Valid TypeID
(typeid/validate "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok true}

;; Invalid: uppercase in prefix
(typeid/validate "User_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:error {:type :invalid-case
;;            :message "TypeID must be all lowercase"
;;            :data {:typeid "User_01h5fskfsk4fpeqwnsyz5hj55t"}}}

;; Invalid: suffix overflow (first char > 7)
(typeid/validate "user_8zzzzzzzzzzzzzzzzzzzzzzzzz")
;;=> {:error {:type :suffix-overflow
;;            :message "First character of suffix must be 0-7"
;;            :data {:suffix "8zzzzzzzzzzzzzzzzzzzzzzzzz"}}}
```

**Use in validation pipelines**:

```clojure
(defn create-user [user-id name email]
  (let [{:keys [ok error]} (typeid/validate user-id)]
    (if ok
      {:status :success :user {:id user-id :name name :email email}}
      {:status :error :message (str "Invalid user ID: " (:message error))})))

(create-user "user_01h5fskfsk4fpeqwnsyz5hj55t" "Alice" "alice@example.com")
;;=> {:status :success :user {...}}

(create-user "invalid-id" "Alice" "alice@example.com")
;;=> {:status :error :message "Invalid user ID: ..."}
```

---

### 4. Convert UUID to TypeID

Encode an existing UUID (e.g., from a database) as a TypeID:

```clojure
;; Convert hex UUID to TypeID
(let [uuid-hex "0188e5f5f34a7b3d9f2a1c5de67fa8c1"
      {:keys [ok]} (typeid/hex->uuid uuid-hex)
      {:keys [ok]} (typeid/encode ok "user")]
  ok)
;;=> "user_01h5fskfsk4fpeqwnsyz5hj55t"

;; Directly encode UUID bytes
(def uuid-bytes (byte-array [0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
                              0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]))
(typeid/encode uuid-bytes "order")
;;=> {:ok "order_01h5fskfsk4fpeqwnsyz5hj55t"}
```

---

### 5. Convert TypeID to UUID

Extract the UUID from a TypeID (for database storage):

```clojure
;; Decode to UUID bytes
(typeid/decode "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:ok #bytes[0x01 0x88 0xe5 0xf5 0xf3 0x4a 0x7b 0x3d
;;               0x9f 0x2a 0x1c 0x5d 0xe6 0x7f 0xa8 0xc1]}

;; Decode to hex string
(let [{:keys [ok]} (typeid/decode "user_01h5fskfsk4fpeqwnsyz5hj55t")
      {:keys [ok]} (typeid/uuid->hex ok)]
  ok)
;;=> "0188e5f5f34a7b3d9f2a1c5de67fa8c1"
```

---

## Common Patterns

### Pattern 1: Generate ID for New Entity

```clojure
(defn create-user [name email]
  (let [{:keys [ok error]} (typeid/generate "user")]
    (if ok
      {:id ok :name name :email email :created-at (System/currentTimeMillis)}
      (throw (ex-info "Failed to generate user ID" {:error error})))))

(create-user "Alice" "alice@example.com")
;;=> {:id "user_01h5fskfsk4fpeqwnsyz5hj55t"
;;    :name "Alice"
;;    :email "alice@example.com"
;;    :created-at 1699564800000}
```

### Pattern 2: Validate TypeID from User Input

```clojure
(defn get-user-by-id [typeid-str]
  (let [{:keys [ok error]} (typeid/validate typeid-str)]
    (if ok
      (fetch-user-from-db typeid-str)  ; Your DB query
      {:error (str "Invalid TypeID: " (:message error))})))

(get-user-by-id "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:id "user_..." :name "Alice" ...}

(get-user-by-id "invalid")
;;=> {:error "Invalid TypeID: ..."}
```

### Pattern 3: Store UUID in Database, Return TypeID to Clients

```clojure
(defn save-and-return-order [order-data]
  (let [{:keys [ok]} (typeid/generate "order")
        {:keys [ok uuid]} (typeid/parse ok)
        uuid-hex (-> uuid typeid/uuid->hex :ok)]
    ;; Save uuid-hex to database (efficient storage)
    (save-to-db! {:uuid uuid-hex :data order-data})
    ;; Return TypeID to client (human-readable)
    {:id ok :data order-data}))

(save-and-return-order {:items [...] :total 99.99})
;;=> {:id "order_01h5fskp7y2t3z9x8w6v5u4s3r" :data {...}}
```

### Pattern 4: Fetch by TypeID, Query by UUID

```clojure
(defn fetch-order [typeid-str]
  (let [{:keys [ok error]} (typeid/decode typeid-str)]
    (if ok
      (let [uuid-hex (-> ok typeid/uuid->hex :ok)]
        (query-db {:uuid uuid-hex}))  ; Query by UUID
      {:error (str "Invalid TypeID: " (:message error))})))

(fetch-order "order_01h5fskp7y2t3z9x8w6v5u4s3r")
;;=> {:uuid "..." :items [...] :total 99.99}
```

### Pattern 5: Prefix-Based Type Checking

```clojure
(defn delete-user [typeid-str]
  (let [{:keys [ok error]} (typeid/parse typeid-str)]
    (if ok
      (if (= "user" (:prefix ok))
        (do
          (delete-from-db! (:uuid ok))
          {:status :deleted :id typeid-str})
        {:error "Expected user TypeID, got different type"})
      {:error (str "Invalid TypeID: " (:message error))})))

(delete-user "user_01h5fskfsk4fpeqwnsyz5hj55t")
;;=> {:status :deleted :id "user_01h5fskfsk4fpeqwnsyz5hj55t"}

(delete-user "order_01h5fskp7y2t3z9x8w6v5u4s3r")
;;=> {:error "Expected user TypeID, got different type"}
```

---

## Integration Examples

### With Datomic

```clojure
(require '[datomic.api :as d])

;; Store UUID bytes, generate TypeID on read
(defn create-entity [conn entity-type data]
  (let [{:keys [ok]} (typeid/generate entity-type)
        {:keys [prefix uuid]} (:ok (typeid/parse ok))]
    @(d/transact conn
       [{:db/id (d/tempid :db.part/user)
         :entity/type prefix
         :entity/uuid uuid
         :entity/data data}])
    {:typeid ok :data data}))

(defn read-entity [db typeid-str]
  (let [{:keys [ok]} (typeid/parse typeid-str)
        {:keys [prefix uuid]} ok
        entity (d/q '[:find ?e .
                      :in $ ?uuid
                      :where [?e :entity/uuid ?uuid]]
                    db uuid)]
    (when entity
      (assoc (d/entity db entity) :typeid typeid-str))))
```

### With PostgreSQL

```clojure
(require '[next.jdbc :as jdbc])

;; Store UUID as BYTEA or UUID column
(defn insert-user [ds name email]
  (let [{:keys [ok]} (typeid/generate "user")
        {:keys [uuid]} (:ok (typeid/parse ok))
        uuid-hex (-> uuid typeid/uuid->hex :ok)]
    (jdbc/execute-one! ds
      ["INSERT INTO users (id, name, email) VALUES (?::uuid, ?, ?)"
       uuid-hex name email])
    {:typeid ok :name name :email email}))

(defn find-user [ds typeid-str]
  (let [{:keys [ok]} (typeid/decode typeid-str)
        uuid-hex (-> ok typeid/uuid->hex :ok)]
    (jdbc/execute-one! ds
      ["SELECT * FROM users WHERE id = ?::uuid" uuid-hex])))
```

### With Ring/Compojure (Web API)

```clojure
(require '[compojure.core :refer [defroutes GET POST]]
         '[ring.util.response :refer [response status]])

(defroutes app-routes
  (POST "/users" {body :body}
    (let [{:keys [ok]} (typeid/generate "user")]
      (response {:id ok
                 :name (:name body)
                 :email (:email body)})))

  (GET "/users/:id" [id]
    (let [{:keys [ok error]} (typeid/validate id)]
      (if ok
        (response (fetch-user-from-db id))
        (-> (response {:error (:message error)})
            (status 400))))))
```

---

## Error Handling

All functions return `{:ok result}` or `{:error error-map}`. Always destructure and check:

```clojure
;; ✅ Good: Check for errors
(let [{:keys [ok error]} (typeid/generate "user")]
  (if ok
    (save-to-db! ok)
    (log/error "Failed to generate TypeID:" error)))

;; ❌ Bad: Assumes success
(let [{:keys [ok]} (typeid/generate "user")]
  (save-to-db! ok))  ; ok might be nil if error occurred!
```

**Common errors**:

| Error Type | Cause | Fix |
|------------|-------|-----|
| `:invalid-prefix-format` | Prefix has uppercase, digits, or invalid chars | Use only lowercase `[a-z_]` |
| `:prefix-too-long` | Prefix exceeds 63 characters | Shorten prefix |
| `:suffix-overflow` | First suffix char > `7` | TypeID is invalid, reject it |
| `:invalid-case` | TypeID contains uppercase | TypeIDs must be all lowercase |

---

## Performance Tips

1. **Validation vs. Parsing**: Use `validate` for quick checks (doesn't decode); use `parse` when you need components
2. **Reuse UUID bytes**: If generating many TypeIDs at once, consider pre-generating UUIDs and encoding them
3. **Database storage**: Store UUIDs as binary (16 bytes) or UUID type, not as TypeID strings (more efficient)
4. **Disable validation in production**: If input is trusted, skip validation (measure performance impact first)

---

## ClojureScript Notes

The library works identically in ClojureScript with these differences (internal only):

- **Time source**: Uses `js/Date.now()` instead of `System.currentTimeMillis()`
- **Random source**: Uses `crypto.getRandomValues()` instead of `SecureRandom`
- **Same API**: All public functions work the same way

**Example (ClojureScript)**:

```clojure
(ns my-app.core
  (:require [typeid.core :as typeid]))

(defn create-session []
  (let [{:keys [ok]} (typeid/generate "session")]
    (.log js/console "Generated session ID:" ok)
    ok))

(create-session)
;; Console: "Generated session ID: session_01h5fskq1a2b3c4d5e6f7g8h9j"
```

---

## Next Steps

- **API Reference**: See [contracts/api.md](contracts/api.md) for full function specifications
- **Data Model**: See [data-model.md](data-model.md) for detailed entity descriptions
- **TypeID Spec**: See [typeid.md](../../../typeid.md) for the official specification

---

## FAQ

**Q: Can I use TypeIDs as database primary keys?**
A: Yes! Store the UUID portion (16 bytes or hex string) as the primary key, and generate the TypeID string when sending data to clients.

**Q: Are TypeIDs sortable?**
A: Yes, TypeIDs sort chronologically because the timestamp is in the first 48 bits of the UUID.

**Q: Can I have TypeIDs without a prefix?**
A: Yes, use `(generate "")` to create prefix-less TypeIDs (just the 26-character base32 suffix).

**Q: Are single-character prefixes allowed?**
A: Technically yes, but the spec recommends at least 3 characters for clarity (e.g., "usr" instead of "u").

**Q: Can I encode UUIDv4 (non-v7) as a TypeID?**
A: Yes! The `encode` function accepts any 16-byte UUID. Only `generate` produces UUIDv7.

**Q: How do I migrate from UUIDs to TypeIDs?**
A: Encode existing UUIDs with a prefix: `(encode uuid-bytes "user")`. The underlying UUID remains the same.

**Q: What's the performance impact of validation?**
A: Validation is ~500ns per call. If input is trusted (e.g., internal services), you can skip it for performance.

---

## Getting Help

- **Issues**: [github.com/example/typeid/issues](https://github.com/example/typeid/issues)
- **Documentation**: [https://example.com/typeid/docs](https://example.com/typeid/docs)
- **Clojurians Slack**: #typeid channel
