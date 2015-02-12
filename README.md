Leiningen war plugin
====================

package war with Leiningen 
 

Readme
==========

This plugin creates standard war files for use with java web application servers.

This plugin is available at [http://clojars.org/](http://clojars.org/uk.org.alienscience/leiningen-war)

    :plugins [[zhu.road/leiningen-war "0.0.1"]]

This plugin adds three commands to leiningen:

lein web-xml
------------

Create a web.xml file if one does not already exist. For most non-trivial
applications this file will need to be edited manually.

By default the file is created in src/web.xml but this can be overidden
by setting `:webxml` in your `:war` configuration in project.clj.

The servlet class is assumed to be the first entry in the
`:aot` setting given in project.clj.

lein uberwar
------------

Create a war file containing the following directory structure:

    destination                   default source              project.clj 
    ----------------------------------------------------------------------------        
    WEB-INF/web.xml               src/web.xml                 :war {:webxml}
    WEB-INF/classes               classes                     :compile-path 
    WEB-INF/lib                   lib                         :library-path
    /                             src/html                    :war {:web-content}
    WEB-INF/classes               resources                   :resources-path
    WEB-INF/classes               src                         :source-path

lein war
--------

This command does not include dependencies in the war file and is intended for cases
where the servlet container classpath is setup manually.

Create a war file containing the following directory structure:

    destination                 default source         project.clj 
    ---------------------------------------------------------------------        
    WEB-INF/web.xml             src/web.xml            :war {:webxml}
    WEB-INF/classes             classes                :compile-path 
    /                           src/html               :war {:web-content}
    WEB-INF/classes             resources              :resources-path
    WEB-INF/classes             src                    :source-path

Simple Example
==============

    (defproject example "0.0.1"
      :dependencies [[org.clojure/clojure "1.6.0"]
                     [org.clojure/clojure-contrib "1.2.0"]]
      :plugins [[uk.org.alienscience/leiningen-war "0.0.15"]])

`lein war` will create a war file with the following structure:

    WEB-INF/
    WEB-INF/web.xml   <--- taken from src/web.xml
    WEB-INF/classes   <---- taken from classes
    index.html        <---- taken from src/html/index.html

`lein uberwar` will create a similar directory structure with the addition:

    WEB-INF/lib       <----  taken from lib

War name
--------

The default filename used for the .war file is $PROJECT-$VERSION.war.  You can change this
by specifying the `:name` key in your `:war` configuration in project.clj.  Don't forget to
include ".war" on the end of the name.
