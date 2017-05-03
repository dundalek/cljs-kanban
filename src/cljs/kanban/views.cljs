(ns kanban.views
    (:require [clojure.string :as str]
              [re-frame.core :refer [subscribe dispatch]]
              [reagent.core :as r]
              [reagent-dnd.core :as dnd]))
              ; [dnd-examples.example-sortable :refer [main]]))

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

(defn create-card [column-id]
 (let [text (r/atom "")
       creating? (r/atom false)]
   (fn [column-id]
     (if @creating?
      [:div
       [:div.kanban-card [:textarea {:value @text :auto-focus true :on-change #(reset! text (-> % .-target .-value))}]]
       [:div [:button {:on-click #(dispatch [:add-card {:column-id column-id :title @text}])} "Add"]
             [:button {:on-click (fn [] (reset! creating? false) (reset! text ""))} "X"]]]
      [:div {:on-click #(reset! creating? true)} "Add card"]))))

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
                   [:footer [create-card column-id]]]])]))))

; (defn main-panel []
;   [main])

(defn main-panel []
  [(dnd/with-drag-drop-context
    @dnd/html5-backend
    board)])
