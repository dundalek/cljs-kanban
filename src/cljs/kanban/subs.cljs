(ns kanban.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]
              [datascript.core :as d]))

(re-frame/reg-sub
 :columns
 (fn [{db :db}]
   (->>
     (d/q '[:find (pull ?c [:db/id :column/title :order {:_column [*]}])
            :where [?c :column/title _]]
          db)
     (map first)
     (sort-by :order)
     (map (fn [c]
            (-> c
              (dissoc :_column)
              (assoc :cards (sort-by :order (:_column c)))))))))
