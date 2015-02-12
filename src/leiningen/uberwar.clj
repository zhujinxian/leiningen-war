(ns leiningen.uberwar
  (:require [leiningen.war :as war]
            [leiningen.compile :as compile]
            [clojure.java.io :as io])
  (:use leiningen.web-xml)
  (:import [java.util.jar JarFile JarEntry]))

(defn default-uberwar-name
  "Returns the name of the war file to create"
  [project]
  (or (-> project :war :name)
      (str (:name project) "-" (:version project) "-standalone.war")))

(defn get-classpath [project]
  (if-let [get-cp (resolve 'leiningen.core.classpath/get-classpath)]
    (get-cp project)
    (->> (:library-path project) io/file .listFiles (map str))))

(defn contains-entry? [^java.io.File file ^String entry]
  (with-open [jar-file (JarFile. file)]
    (some (partial = entry)
          (map #(.getName ^JarEntry %)
               (enumeration-seq (.entries jar-file))))))

(defn jar-dependencies [project]
  (for [pathname (get-classpath project)
        :let [file (io/file pathname)
              fname (.getName file)]
        :when (and (.endsWith fname ".jar")
                   ;; Servlet container will have it's own servlet-api impl
                   (not (contains-entry? file "javax/servlet/Servlet.class")))]
    file))

(defn jar-entries [war project]
  (doseq [jar-file (jar-dependencies project)]
    (let [dir-path (.getParent jar-file)
          war-path (war/in-war-path "WEB-INF/lib/" dir-path jar-file)]
      (war/file-entry war project war-path jar-file))))

(defn write-uberwar [project war-path]
  (with-open [war-stream (war/create-war project war-path)]
    (doto war-stream
      (war/file-entry project "WEB-INF/web.xml" (io/file (webxml-path project)))
      (war/dir-entry project "WEB-INF/classes/" (:compile-path project)))
    (doseq [path (war/source-and-resource-paths project)
            :when path]
      (war/dir-entry war-stream project "WEB-INF/classes/" path))
    (doseq [path (war/war-resources-paths project)]
      (war/dir-entry war-stream project "" path))
    (jar-entries war-stream project)))

(defn uberwar
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
  (let [war-path (war/war-file-path project (default-uberwar-name project))] 
    (write-uberwar project war-path)))

