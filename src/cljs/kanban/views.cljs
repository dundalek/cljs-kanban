(ns kanban.views
    (:require [clojure.string :as str]
              [re-frame.core :refer [subscribe dispatch]]
              [reagent.core :as r]
              [reagent-dnd.core :as dnd]))

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
               :types [:knight]
               :drop (fn [state]
                       (js/console.error "dropped item: " (clj->js (:item state)))
                       {:id column-id})
               :state drop-state
               :child
                 [:div.kanban-column
                   {:key title}
                   [:header title]
                   [:section.wrapper.ui-sortable
                     (for [{title :title card-id :id} (column-cards column-id)]
                         [dnd/drag-source
                          :type       :knight
                          :end-drag   (fn [state]
                                        (dispatch [:move-card (-> state :item :id) (-> state :drop-result :id)])
                                        (js/console.error "dropped on: " (clj->js state)))
                          :begin-drag (fn [] {:id card-id})
                          :state      drag-state
                          :child
                            [:div.kanban-card {:key title :draggable "true"} title]])]
                   [:footer "Add Card"]]])]))))

(defn main-panel []
  [(dnd/with-drag-drop-context
    @dnd/html5-backend
    board)])
