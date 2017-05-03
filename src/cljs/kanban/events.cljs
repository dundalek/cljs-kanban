(ns kanban.events
    (:require [re-frame.core :refer [reg-event-db]]
              [kanban.db :as db]))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(reg-event-db
  :import-db
  (fn  [_ [_ data]]
    data))

(reg-event-db
  :move-card
  (fn [db [_ card-id column-id]]
    (let [cards (:cards db)
          next-cards (map #(if (= card-id (:id %))
                               (assoc % :column-id column-id)
                               %)
                          cards)]
      (assoc db :cards next-cards))))

(reg-event-db
  :add-card
  (fn [db [_ card]]
    (let [max-id (apply max (map :id (:cards db)))
          new-id (if (nil? max-id) 1 (inc max-id))]
      (update-in db [:cards] conj (assoc card :id new-id)))))
