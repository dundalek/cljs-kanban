(ns kanban.events
    (:require [re-frame.core :as re-frame]
              [kanban.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))
