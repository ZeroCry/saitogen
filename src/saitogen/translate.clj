(ns saitogen.translate
  (:require [camel-snake-kebab.core :as case-type]
            [hickory.core :as hickory])
  (:import [com.vladsch.flexmark.ext.yaml.front.matter YamlFrontMatterNode]
           [com.vladsch.flexmark.ast
            BulletList
            BulletListItem
            Code
            Document
            Emphasis
            Heading
            HtmlBlock
            IndentedCodeBlock
            Link
            LinkRef
            Paragraph
            Reference
            SoftLineBreak
            StrongEmphasis
            Text
            ThematicBreak]))

(defn node-children [n]
  (loop [c (.getFirstChild n)
         acc []]
    (if (some? c)
      (recur (.getNext c) (conj acc c))
      acc)))

(defn escape-html [s]
  (clojure.string/escape s {\< "&lt;" \> "&gt;" \& "&amp;"}))

(defprotocol ExtractsMetadata
  "Implemented by flexmark AST-nodes that contain metadata."
  (collect-metadata [n db c]
    "Assoc metadata into db based on node n and children-seq c."))

(defn extract-metadata
  ([n]
   (extract-metadata n {}))
  ([n db]
   (if (satisfies? ExtractsMetadata n)
     (collect-metadata n db (node-children n))
     db)))

(extend-protocol ExtractsMetadata
  Document
  (collect-metadata [_ db c]
    (loop [db db
           [child & r] c]
      (if (some? child)
       (recur (extract-metadata child db) r)
       db)))
  Reference
  (collect-metadata [n db _]
    (assoc-in db [:references (.toString (.getReference n))]
              (escape-html (.toString (.getUrl n)))))
  YamlFrontMatterNode
  (collect-metadata [n db c]
    (println "TODO extract YAML front matter.")
    db))

(defprotocol TranslatesToHiccup
  "Implemented by flexmark AST-nodes that can be represented as hiccup."
  (as-hiccup [n c refs]
    "Args: node and seq of children and ref-map."))

(defn translate
  [n refs]
  (if (satisfies? TranslatesToHiccup n)
    (as-hiccup n (node-children n) refs)
    (println "Skipping hiccup translation for " (.getClass n))))

(defn translate-into
  "Translate children c and append as body to hiccup h."
  [h c refs]
  (into h (map #(translate % refs) c)))

(extend-protocol TranslatesToHiccup
  BulletList
  (as-hiccup [n c refs]
    (translate-into [:ul] c refs))
  BulletListItem
  (as-hiccup [n [child-paragraph] refs]
    (->> (translate child-paragraph refs)
         rest
         (into [:li])))
  Code
  (as-hiccup [n c refs]
    (translate-into [:code] c refs))
  Document
  (as-hiccup [n c refs]
    (->> (keep #(translate % refs) c)
         (into [])))
  Emphasis
  (as-hiccup [n _ _]
    [:em (-> n .getText .toString escape-html)])
  Heading
  (as-hiccup [n _ _]
    (let [text (-> n .getText .toString escape-html)
          level (.getLevel n)
          id (case-type/->kebab-case text)
          h-tag (keyword (str "h" level))]
      [h-tag {:id id} text]))
  HtmlBlock
  (as-hiccup [n _ _]
    (->> n
         .getContentChars
         .toString
         hickory/parse-fragment
         (map hickory/as-hiccup)
         (into [:div])))
  IndentedCodeBlock
  (as-hiccup [n _ _]
    (->> n
         .getContentLines
         (map #(.toString %))
         (map escape-html)
         (into [:code])))
  Link
  (as-hiccup [n c refs]
    (translate-into [:a {:href (-> n .getUrl .toString escape-html)}] c refs))
  LinkRef
  (as-hiccup [n c refs]
    (let [ref (.toString (.getReference n))
          url (get refs ref)]
      (translate-into [:a {:href url}] c refs)))
  Paragraph
  (as-hiccup [n c refs]
    (translate-into [:p] c refs))
  Reference
  (as-hiccup [_ _ _]
    nil)
  StrongEmphasis
  (as-hiccup [n _ _]
    [:b (-> n .getText .toString escape-html)])
  SoftLineBreak
  (as-hiccup [_ _ _]
    " ")
  Text
  (as-hiccup [n _ _]
    (-> n .getChars .toString escape-html)))
