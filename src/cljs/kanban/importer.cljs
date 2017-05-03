(ns kanban.importer
    (:require [ajax.core :refer [GET POST]]))

(defn map-trello-data [data]
  (let [columns (map (fn [{id :id name :name}]
                       {:id id :title name})
                     (filter #(-> % :closed not) (:lists data)))
        cards (map (fn [{id :id name :name id-list :idList}]
                      {:id id :title name :column-id id-list})
                   (:cards data))]
    {:columns (vec columns)
     :cards (vec cards)}))

(defn load-trello-data [url handler]
  (GET url
       {:response-format :json
        :keywords? true
        :handler #(handler (map-trello-data %))}))

(defn map-github-issues-data [data]
  (let [cards (map (fn [{id :id title :title labels :labels}]
                     {:id id :title title :column-id (-> labels first :name)})
                   data)
        columns (map-indexed (fn [idx item] {:id (inc idx) :title item}) (distinct (map :column-id cards)))
        column-index (group-by :title columns)
        cards-mapped (map #(assoc % :column-id (-> (% :column-id) column-index first :id)) cards)]
       {:columns (vec columns)
        :cards (vec cards-mapped)}))

(defn load-github-issues-data [url handler]
  (GET url
    {:response-format :json
     :keywords? true
     :handler #(handler (map-github-issues-data %))}))
