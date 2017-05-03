(ns kanban.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :columns
 :columns)

(re-frame/reg-sub
 :cards
 (fn [db]
   (:cards db)))
