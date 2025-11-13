# typeid Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-11-10

## Active Technologies
- Clojure 1.11+, ClojureScript (targeting JVM and JS platforms) + Zero runtime dependencies (library only); Kaocha + test.check for testing (002-api-simplification)
- N/A (library does not handle storage) (002-api-simplification)
- Clojure 1.11+ (JVM), ClojureScript 1.12+ (JS) + Zero runtime dependencies (only org.clojure/clojure and org.clojure/clojurescript) (003-parse-uuid-return)

- Clojure 1.11+ (JVM) and ClojureScript (JS target); CI matrix testing Clojure 1.11/1.12 on JDK 17/21 (001-typeid-implementation)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Clojure 1.11+ (JVM) and ClojureScript (JS target); CI matrix testing Clojure 1.11/1.12 on JDK 17/21

## Code Style

Clojure 1.11+ (JVM) and ClojureScript (JS target); CI matrix testing Clojure 1.11/1.12 on JDK 17/21: Follow standard conventions

## Recent Changes
- 003-parse-uuid-return: Added Clojure 1.11+ (JVM), ClojureScript 1.12+ (JS) + Zero runtime dependencies (only org.clojure/clojure and org.clojure/clojurescript)
- 002-api-simplification: Added Clojure 1.11+, ClojureScript (targeting JVM and JS platforms) + Zero runtime dependencies (library only); Kaocha + test.check for testing

- 001-typeid-implementation: Added Clojure 1.11+ (JVM) and ClojureScript (JS target); CI matrix testing Clojure 1.11/1.12 on JDK 17/21

<!-- MANUAL ADDITIONS START -->

## REPL and nREPL Server

### Starting the nREPL Server

To start an nREPL server in headless mode (for programmatic access):

```bash
clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.4.0"}}}' -M -m nrepl.cmdline --host 127.0.0.1
```

This will:
- Start the nREPL server on an auto-assigned port
- Bind to localhost (127.0.0.1)
- Run in headless mode (no interactive REPL)
- Output the port number to stdout

### Testing the Connection

After starting the server, test it with:

```bash
clj-nrepl-eval -p <PORT> "(+ 1 2 3)"
```

### Checking Active Connections

To see all active nREPL connections:

```bash
clj-nrepl-eval --connected-ports
```

<!-- MANUAL ADDITIONS END -->
