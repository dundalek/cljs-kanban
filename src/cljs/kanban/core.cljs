(ns kanban.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [kanban.events]
              [kanban.subs]
              [kanban.views :as views]
              [kanban.config :as config]
              [kanban.importer]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (kanban.importer/load-data)
  (mount-root))
