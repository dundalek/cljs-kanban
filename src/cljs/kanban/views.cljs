(ns kanban.views
    (:require [clojure.string :as str]
              [re-frame.core :refer [subscribe dispatch]]
              [reagent.core :as r]
              [reagent-dnd.core :as dnd]))

(defn render-card [{title :title card-id :id} drag-state]
  (let [dragging (= card-id (-> @drag-state :item :id))]
    [:div.kanban-card {:key card-id
                       :style {:opacity (if dragging 0 1)}}
                      title]))

(defn render-draggable-card [card drag-state]
  [dnd/drag-source
   :type       :card
   :end-drag   (fn [state]
                 (dispatch [:move-card (-> state :item :id) (-> state :drop-result :id)])
                 (js/console.error "dropped on: " (clj->js state)))
   :begin-drag (fn [] {:id (:id card)})
   :state      drag-state
   :child [render-card card drag-state]])

(defn board []
  (let [columns (subscribe [:columns])
        cards (subscribe [:cards])
        drag-state (r/atom {})
        drop-state (r/atom {})]
    (fn []
      (let [column-cards (group-by :column-id @cards)]
        [:div.kanban-board
          (for [{title :title column-id :id} @columns]
              [dnd/drop-target
               :types [:card]
               :drop (fn [state]
                       (js/console.error "dropped item: " (clj->js (:item state)))
                       {:id column-id})
               :state drop-state
               :child
                 [:div.kanban-column
                   {:key title}
                   [:header title]
                   [:section.wrapper.ui-sortable
                     (for [card (column-cards column-id)]
                       [render-draggable-card card drag-state])]
                   [:footer "Add Card"]]])]))))

(defn main-panel []
  [(dnd/with-drag-drop-context
    @dnd/html5-backend
    board)])
