(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.github.jonasrodrigues/typeid)
(def version (or (System/getenv "RELEASE_VERSION") "0.1.0-SNAPSHOT"))
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
                             [:name "Jonas Rodrigues"]]]]}))

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
