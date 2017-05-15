(ns kanban.exporter
  (:require [clojure.string :as s]))

(defn export-json [{:keys [columns cards]}]
  (js/JSON.stringify
    (clj->js {:cards (map (fn [{:keys [id title column-id]}] {:id id :name title :idList column-id}) cards)
              :lists (map (fn [{:keys [id title]}] {:id id :name title}) columns)})))

(defn export-markdown [{:keys [columns cards]}]
  (let [column-cards (group-by :column-id cards)]
    (->> columns
      (mapcat (fn [{column-id :id column-title :title}]
                (concat [(str "\n# " column-title "\n")]
                    (map #(str "## " (:title %)) (column-cards column-id)))))
      (s/join "\n"))))

(defn download
  ([filename text]
   (download filename text "text/plain"))
  ([filename text mime]
   (let [element (.createElement js/document "a")]
     (.setAttribute element "href" (str "data:" mime ";charset=utf-8," (js/encodeURIComponent text)))
     (.setAttribute element "download" filename)
     (set! (.. element -style -display) "none")
     (.appendChild (.-body js/document) element)
     (.click element)
     (.removeChild (.-body js/document) element))))
