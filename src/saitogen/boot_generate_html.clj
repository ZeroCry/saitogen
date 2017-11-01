(ns saitogen.boot-generate-html
  {:boot/export-tasks true}
  (:require
    [boot.core       :as boot]
    [clojure.java.io :as io]
    [saitogen.core :as sg]))

; https://github.com/boot-clj/boot/wiki/Filesets

(boot/deftask generate-html
  "Docstring"
  [] ; task-args)
  (let [tmp (boot/tmp-dir!)]
    ; shortcut for tasks with no post-processing
    (boot/with-pre-wrap fileset
      (let [in-files (boot/input-files fileset)
            md-files (boot/by-ext [".md"] in-files)]
        (doseq [in md-files]
          (let [in-file  (boot/tmp-file in)
                in-path  (boot/tmp-path in)]
                ;out-path (lc->uc in-path)
                ;out-file (io/file tmp out-path)]
            (-> (sg/parse-file in-file)
                (println)))))
      (-> fileset
          (boot/add-resource tmp)
          boot/commit!))))
