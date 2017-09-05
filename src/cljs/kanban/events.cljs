(ns kanban.events
    (:require [re-frame.core :refer [reg-event-db]]
              [datascript.core :as d]
              [kanban.db :as db]))

(defn get-prev-card-order [db card active-card]
  (->>
    (d/q '[:find ?card-prev-order
           :in $ ?card ?active-card
           :where [?card :column ?column]
                  [?card :order ?order]
                  [?card-prev :column ?column]
                  [?card-prev :order ?card-prev-order]
                  [(not= ?card-prev ?active-card)]
                  [(< ?card-prev-order ?order)]]
         db
         card
         active-card)
    (map first)
    (sort)
    (reverse)
    (first)))

(defn get-next-card-order [db card active-card]
  (->>
    (d/q '[:find ?card-prev-order
           :in $ ?card ?active-card
           :where [?card :column ?column]
                  [?card :order ?order]
                  [?card-prev :column ?column]
                  [?card-prev :order ?card-prev-order]
                  [(not= ?card-prev ?active-card)]
                  [(> ?card-prev-order ?order)]]
         db
         card
         active-card)
    (map first)
    (sort)
    (first)))

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
          column-old (-> (d/entity @conn card-id) :column first :db/id)]
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
  (fn [{items :cards :as db} [_ column-id drag-id hover-id direction]]
    (let [conn (:conn db)
          column-old (-> (d/entity @conn drag-id) :column first :db/id)
          hover-order (:order (d/entity @conn hover-id))
          other-order (if (= :above direction) (get-prev-card-order @conn hover-id drag-id) (get-next-card-order @conn hover-id drag-id))
          order (if (nil? other-order)
                  (+ hover-order (if (= :above direction) -100 100))
                  (/ (+ hover-order other-order) 2))]
      (d/transact! conn [[:db/retract drag-id :column column-old]
                         {:db/id drag-id :column column-id :order order}])
      (assoc db :db @conn))))
