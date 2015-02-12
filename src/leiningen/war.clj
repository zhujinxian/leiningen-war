(ns leiningen.war
  (:require [leiningen.compile :as compile]
            [clojure.java.io :as io]
            [clojure.string :as string])
        
  (:use leiningen.web-xml)
  (:import [java.util.jar Manifest
                          JarEntry
                          JarOutputStream]
           [java.io BufferedOutputStream
                    FileOutputStream
                    ByteArrayInputStream]))

(defn war-file-path [project war-name]
  (let [target-dir (or (:target-dir project) (:target-path project))]
    (.mkdirs (io/file target-dir))
    (str target-dir "/" war-name)))

(defn default-war-name
  "Returns the name of the war file to create"
  [project]
  (or (-> project :war :name)
      (str (:name project) "-" (:version project) ".war")))

(defn- to-byte-stream [^String s]
  (ByteArrayInputStream. (.getBytes s)))

(defn skip-file? [project war-path file]
  (or (re-find #"^\.?#" (.getName file))
      (re-find #"~$" (.getName file))
      (some #(re-find % war-path)
            (get-in project [:ring :war-exclusions] [#"(^|/)\."]))))

(defn make-manifest [project]
  (Manifest.
   (ByteArrayInputStream.
    (. 
     (str 
      "Manifest-Version: 1.0" \newline
      "Created-By: Leiningen War Plugin" \newline
      "Built-By: " (System/getProperty "user.name") \newline
      "Build-Jdk: " (System/getProperty "java.version") \newline
      \newline) getBytes))))

(defn create-war [project file-path]
  (-> (FileOutputStream. file-path)
      (BufferedOutputStream.)
      (JarOutputStream. (make-manifest project))))

(defn write-entry [war war-path entry]
  (.putNextEntry war (JarEntry. war-path))
  (io/copy entry war))

(defn str-entry [war war-path content]
  (write-entry war war-path (to-byte-stream content)))

(defn in-war-path [war-path root file]
  (str war-path
       (-> (.toURI (io/file root))
           (.relativize (.toURI file))
           (.getPath))))

(defn file-entry [war project war-path file]
  (when (and (.exists file)
             (.isFile file)
             (not (skip-file? project war-path file)))
    (write-entry war war-path file)))

(defn dir-entry [war project war-root dir-path]
  (doseq [file (file-seq (io/file dir-path))]
    (let [war-path (in-war-path war-root dir-path file)]
      (file-entry war project war-path file))))

(defn war-resources-paths [project]
  (filter identity
          (distinct (concat [(:war-resources-path project "war-resources")]
                            (:war-resource-paths project)))))

(defn source-and-resource-paths
  "Return a distinct sequence of the project's source and resource paths,
  unless :omit-source is true, in which case return only resource paths."
  [project]
  (let [resource-paths (concat [(:resources-path project)] (:resource-paths project))
        source-paths (if (:omit-source project)
                       '()
                       (concat [(:source-path project)] (:source-paths project)))]
    (distinct (concat source-paths resource-paths))))

(defn write-war [project war-path]
  (with-open [war-stream (create-war project war-path)]
    (doto war-stream
      (file-entry project "WEB-INF/web.xml" (io/file (webxml-path project)))
      (dir-entry project "WEB-INF/classes/" (:compile-path project)))
    (doseq [path (source-and-resource-paths project)
            :when path]
      (dir-entry war-stream project "WEB-INF/classes/" path))
    (doseq [path (war-resources-paths project)]
      (dir-entry war-stream project "" path))
    war-stream))

(defn war
  "This command does not include dependencies in the war file and is intended for cases
   where the servlet container classpath is setup manually.

   Create a $PROJECT-$VERSION.war (unless :war-name is specified in project)
   file containing the following directory structure:

   destination                 default source         project.clj 
   ---------------------------------------------------------------------        
   WEB-INF/web.xml             src/web.xml            :war {:webxml}
   WEB-INF/classes             classes                :compile-path 
   /                           src/html               :war {:web-content}
   WEB-INF/classes             resources              :resources-path
   WEB-INF/classes             src                    :source-path"
  [project & args]
  (compile/compile project)
  (let [war-path (war-file-path project (default-war-name project))] 
    (write-war project war-path)))

