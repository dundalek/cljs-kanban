(ns kanban.views
    (:require [clojure.string :as str]
              [re-frame.core :refer [subscribe dispatch]]
              [reagent.core :as r]
              [reagent-dnd.core :as dnd]))
              ; [dnd-examples.example-sortable :refer [main]]))

(def card-source
  #js {:beginDrag (fn [props] {:id (.-card.id props)})
       :endDrag (fn [props monitor]
                    (let [item (.getItem monitor)
                          drop-result (.getDropResult monitor)]
                         (when (not (nil? drop-result))
                          (js/console.log "dropped" (:id item) "into" (.-id drop-result))
                          (dispatch [:move-card (:id item) (.-id drop-result)]))))})

(defn card-collect [connect monitor]
  #js {:connectDragSource (.dragSource connect)
       :isDragging (.isDragging monitor)})

(defn render-card [{card :card is-dragging :isDragging connect-drag-source :connectDragSource}]
  (let [title (.-title card)
        card-id (.-id card)]
    (connect-drag-source
      (r/as-element
        [:div.kanban-card {:key card-id
                           :style {:opacity (if is-dragging 0 1)}}
                          title]))))

(defn create-card [column-id]
 (let [text (r/atom "")
       creating? (r/atom false)
       reset (fn [] (reset! creating? false) (reset! text ""))]
   (fn [column-id]
     (if @creating?
      [:div
       [:div.kanban-card [:textarea {:value @text :auto-focus true :on-change #(reset! text (-> % .-target .-value))}]]
       [:div [:button {:on-click (fn [] (dispatch [:add-card {:column-id column-id :title @text}]) (reset))} "Add"]
             [:button {:on-click reset} "X"]]]
      [:div {:on-click #(reset! creating? true)} "Add card"]))))

(def column-target
  #js {:drop (fn [props] #js{:id (.-column.id props)})})

(defn column-collect [connect monitor]
  #js {:connectDropTarget (.dropTarget connect)
       :isOver (.isOver monitor)
       :canDrop (.canDrop monitor)})

(defn render-column [{column :column
                      cards :cards
                      draggable-card :draggableCard
                      connect-drop-target :connectDropTarget}]
  (let [title (.-title column)
        column-id (.-id column)]
    (connect-drop-target
      (r/as-element
        [:div
          [:div.kanban-column
            {:key column-id}
            [:header title]
            [:section.wrapper.ui-sortable
              (for [card cards]
                [draggable-card {:card card}])]
            [:footer [create-card column-id]]]]))))

(defn board [draggable-card droppable-column]
  (let [columns (subscribe [:columns])
        cards (subscribe [:cards])]
    (fn []
      (let [column-cards-idx (group-by :column-id @cards)]
        [:div.kanban-board
          (for [{column-id :id :as column} @columns]
            (let [column-cards (column-cards-idx column-id)]
                 [droppable-column {:column column :cards column-cards :draggable-card draggable-card}]))]))))

(defn main-panel []
  (let [context-provider (r/adapt-react-class (.-DragDropContextProvider js/ReactDnD))
        drag-source (.-DragSource js/ReactDnD)
        drop-target (.-DropTarget js/ReactDnD)
        backend (.-default js/ReactDnDHTML5Backend)
        draggable-card (r/adapt-react-class ((drag-source "card" card-source card-collect) (r/reactify-component render-card)))
        droppable-column (r/adapt-react-class ((drop-target "card" column-target column-collect) (r/reactify-component render-column)))]
    [context-provider {:backend backend}
      [board draggable-card droppable-column]]))

; (defn main-panel []
;   [main])
