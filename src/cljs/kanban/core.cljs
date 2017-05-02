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
  {:columns (map (fn [{id :id name :name}] {:id id :title name}) (filter #(-> % :closed not) (:lists data)))
   :cards (map (fn [{id :id name :name id-list :idList}] {:id id :title name :column-id id-list}) (:cards data))})

(defn load-data []
  (GET "https://trello.com/b/TFWoOH5n.json"
       {:response-format :json
        :keywords? true
        :handler #(re-frame/dispatch [:import-db (map-trello-data %)])}))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  ; (load-data)
  (mount-root))
