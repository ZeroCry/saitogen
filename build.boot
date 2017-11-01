(set-env!
  :resource-paths #{"resources"}
  :source-paths #{"src"}
  :dependencies '[[camel-snake-kebab "0.4.0"]
                  [com.vladsch.flexmark/flexmark-all "0.27.0"]
                  [commons-io/commons-io "2.6"]
                  [hiccup "1.0.5"]
                  [hickory "0.7.1"]
                  [org.clojure/clojure "1.8.0"]])

(require '[saitogen.boot-generate-html :refer [generate-html]])

(deftask write []
  (comp (watch) (generate-html)))
