(ns saitogen.core
  (:require [hiccup.core :as hiccup]
            [saitogen.translate :as t])
  (:import [com.vladsch.flexmark.ext.yaml.front.matter YamlFrontMatterExtension]
           [com.vladsch.flexmark.parser Parser]
           [com.vladsch.flexmark.util.options MutableDataSet]
           [org.apache.commons.io FileUtils]))

(defn parse
  ""
  [md]
  (let [options (-> (new MutableDataSet)
                    (.set Parser/EXTENSIONS
                          [(YamlFrontMatterExtension/create)]))
        parser (-> (Parser/builder options) .build)
        root-node (.parse parser md)
        meta (t/extract-metadata root-node)]
    (println meta)
    (t/translate root-node (:references meta))))

(defn parse-file
  ""
  [java-file java-path]
  (let [path-components (->> (map #(.toString %) java-path) (into []))
        base (butlast path-components)
        name (last path-components)
        content (FileUtils/readFileToString java-file "utf8")]
    (println path-components)
    (parse content)))

(defn barebones-html [h]
  (-> [:html [:head [:title "FIXME"]] (into [:body] h)]
      hiccup/html))




(import '[java.io File])
(defn proof-of-concept [filename]
  (let [f (new File filename)]
    (->> (parse-file f (.toPath f)) barebones-html (spit "out.html"))))
