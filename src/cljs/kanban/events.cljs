(ns kanban.events
    (:require [re-frame.core :refer [reg-event-db]]
              [datascript.core :as d]
              [kanban.db :as db]))

(defn get-prev-card [db card]
  (->>
    (d/q '[:find ?card-prev ?card-prev-order
           :in $ ?card
           :where [?card :column ?column]
                  [?card :order ?order]
                  [?card-prev :column ?column]
                  [?card-prev :order ?card-prev-order]
                  [(< ?card-prev-order ?order)]]
         db
         card)
    (sort-by second)
    (reverse)
    (first)
    (first)))

(defn get-next-card [db card]
  (->>
    (d/q '[:find ?card-prev ?card-prev-order
           :in $ ?card
           :where [?card :column ?column]
                  [?card :order ?order]
                  [?card-prev :column ?column]
                  [?card-prev :order ?card-prev-order]
                  [(> ?card-prev-order ?order)]]
         db
         card)
    (sort-by second)
    (first)
    (first)))

(defn get-card-column [db card]
  (d/q '[:find ?column .
         :in $ ?card
         :where [?card :column ?column]]
     conn
     card))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   (let [conn (d/create-conn db/schema)]
     (d/transact! conn db/data)
     {:conn conn
      :db @conn})))

(reg-event-db
  :import-db
  (fn  [_ [_ data]]
    data))

(reg-event-db
  :move-card
  (fn [db [_ card-id column-id]]
    (let [conn (:conn db)
          column-old (get-card-column @conn card-id)]
      (d/transact! conn [[:db/retract card-id :column column-old]
                         [:db/add card-id :column column-id]])
      (assoc db :db @conn))))

(reg-event-db
  :add-card
  (fn [db [_ {card-title :title column-id :column-id}]]
    (let [conn (:conn db)
          card {:db/id -1 :card/title card-title :column column-id}]
      (d/transact! conn [card])
      (assoc db :db @conn))))

(reg-event-db
  :move-card-x
  (fn [{items :cards :as db} [_ column-id drag-index hover-index]]
     (let [drag-card (assoc (get items drag-index) :column-id column-id)
           items-removed
             (vec (concat (subvec items 0 drag-index) (subvec items (inc drag-index))))
           items-added
             (vec (concat (subvec items-removed 0 hover-index) [drag-card] (subvec items-removed hover-index)))]
       (assoc db :cards items-added))))
