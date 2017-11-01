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

(defprotocol TranslatesToHiccup
  "Implemented by flexmark AST-nodes that can be represented as hiccup."
  (as-hiccup [n c]
    "Args: node and seq of children."))

(defn translate
  [n]
  (if (satisfies? TranslatesToHiccup n)
    (as-hiccup n (node-children n))
    (println "Skipping hiccup translation for " (.getClass n))))

(defn translate-into
  "Translate children c and append as body to hiccup h."
  [h c]
  (into h (map translate c)))

(extend-protocol TranslatesToHiccup
  BulletList
  (as-hiccup [n c]
    (translate-into [:ul] c))
  BulletListItem
  (as-hiccup [n [child-paragraph]]
    (->> (translate child-paragraph)
         rest
         (into [:li])))
  Code
  (as-hiccup [n c]
    (translate-into [:code] c))
  Document
  (as-hiccup [n c]
    (->> (keep translate c)
         (into [])))
  Emphasis
  (as-hiccup [n c]
    [:em (-> n .getText .toString escape-html)])
  Heading
  (as-hiccup [n _]
    (let [text (-> n .getText .toString escape-html)
          level (.getLevel n)
          id (case-type/->kebab-case text)
          h-tag (keyword (str "h" level))]
      [h-tag {:id id} text]))
  HtmlBlock
  (as-hiccup [n c]
    (->> n
         .getContentChars
         .toString
         hickory/parse-fragment
         (map hickory/as-hiccup)
         (into [:div])))
  IndentedCodeBlock
  (as-hiccup [n c]
    (->> n
         .getContentLines
         (map #(.toString %))
         (map escape-html)
         (into [:code])))
  Link
  (as-hiccup [n c]
    (translate-into [:a {:href (-> n .getUrl .toString escape-html)}] c))
  Paragraph
  (as-hiccup [n c]
    (translate-into [:p] c))
  StrongEmphasis
  (as-hiccup [n c]
    [:b (-> n .getText .toString escape-html)])
  SoftLineBreak
  (as-hiccup [_ _]
    " ")
  Text
  (as-hiccup [n _]
    (-> n .getChars .toString escape-html)))
