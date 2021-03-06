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
      (assoc db :cards (vec next-cards)))))

(reg-event-db
  :add-card
  (fn [db [_ card]]
    (let [max-id (apply max (map :id (:cards db)))
          new-id (if (nil? max-id) 1 (inc max-id))]
      (update-in db [:cards] conj (assoc card :id new-id)))))

(reg-event-db
  :move-card-x
  (fn [{items :cards :as db} [_ column-id drag-index hover-index]]
     (let [drag-card (assoc (get items drag-index) :column-id column-id)
           items-removed
             (vec (concat (subvec items 0 drag-index) (subvec items (inc drag-index))))
           items-added
             (vec (concat (subvec items-removed 0 hover-index) [drag-card] (subvec items-removed hover-index)))]
       (assoc db :cards items-added))))
