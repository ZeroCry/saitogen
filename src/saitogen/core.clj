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
        root-node (.parse parser md)]
    (t/translate root-node)))

(defn parse-file
  ""
  [f]
  (-> (FileUtils/readFileToString f) parse))

(defn barebones-html [h]
  (-> [:html [:head [:title "FIXME"]] (into [:body] h)]
      hiccup/html))
