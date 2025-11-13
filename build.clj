(ns build
  (:require
    [clojure.string :as str]
    [clojure.tools.build.api :as b]))

(def lib 'io.github.unisoma/typeid)
(def version (or (System/getenv "RELEASE_VERSION") "0.3.0-SNAPSHOT"))
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean
  "Remove target directory."
  [_]
  (b/delete {:path "target"}))

(defn pom
  "Generate pom.xml for Maven/Clojars deployment."
  [_]
  ;; Generate standard POM first
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src"]
                :scm {:url "https://github.com/UniSoma/typeid"
                      :connection "scm:git:git://github.com/UniSoma/typeid.git"
                      :developerConnection "scm:git:ssh://git@github.com/UniSoma/typeid.git"
                      :tag (str "v" version)}
                :pom-data [[:description "Type-safe, K-sortable unique identifiers for Clojure/ClojureScript"]
                           [:url "https://github.com/UniSoma/typeid"]
                           [:licenses
                            [:license
                             [:name "MIT License"]
                             [:url "https://opensource.org/licenses/MIT"]]]
                           [:developers
                            [:developer
                             [:name "Jonas Rodrigues"]]]]})
  ;; Post-process POM to add "provided" scope to Clojure dependency
  (let [pom-file (b/pom-path {:lib lib :class-dir class-dir})
        pom-content (slurp pom-file)
        ;; Add <scope>provided</scope> after the Clojure dependency version
        updated-pom (str/replace pom-content
                      #"(<groupId>org\.clojure</groupId>\s*<artifactId>clojure</artifactId>\s*<version>[^<]+</version>)"
                      "$1\n      <scope>provided</scope>")]
    (spit pom-file updated-pom)))

(defn jar
  "Build library jar file."
  [_]
  (clean nil)
  (pom nil)
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install
  "Install jar to local Maven repository (~/.m2)."
  [_]
  (jar nil)
  (b/install {:basis @basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy
  "Deploy jar to Clojars.
   Requires CLOJARS_USERNAME and CLOJARS_PASSWORD environment variables."
  [{:keys [sign] :or {sign true}}]
  (jar nil)
  (let [pom-file (b/pom-path {:lib lib :class-dir class-dir})]
    ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
      {:installer :remote
       :artifact jar-file
       :pom-file pom-file
       :sign-releases? sign})))
