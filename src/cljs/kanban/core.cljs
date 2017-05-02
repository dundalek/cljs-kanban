(ns kanban.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [kanban.events]
              [kanban.subs]
              [kanban.views :as views]
              [kanban.config :as config]
              [ajax.core :refer [GET POST]]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn map-trello-data [data]
  {:columns (map (fn [{id :id name :name}]
                     {:id id :title name})
                 (filter #(-> % :closed not) (:lists data)))
   :cards (map (fn [{id :id name :name id-list :idList}]
                   {:id id :title name :column-id id-list})
               (:cards data))})

(defn load-trello-data []
  (GET "https://trello.com/b/TFWoOH5n.json"
       {:response-format :json
        :keywords? true
        :handler #(re-frame/dispatch [:import-db (map-trello-data %)])}))

(defn map-github-issues-data [data]
  (let [cards (map (fn [{id :id title :title labels :labels}]
                       {:id id :title title :column-id (-> labels first :name)})
                   data)
        columns (map-indexed (fn [idx item] {:id (inc idx) :title item}) (distinct (map :column-id cards)))
        column-index (group-by :title columns)
        cards-mapped (map #(assoc % :column-id (-> (% :column-id) column-index first :id)) cards)]
       {:columns columns
        :cards cards-mapped}))

(defn load-github-issues-data []
  (GET "https://api.github.com/repos/tinytacoteam/zazu/issues?filter=is:issue%20is:open"
    {:response-format :json
     :keywords? true
     :handler #(re-frame/dispatch [:import-db (map-github-issues-data %)])}))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  ; (load-github-issues-data)
  (mount-root))
