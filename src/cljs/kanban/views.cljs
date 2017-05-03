(ns kanban.views
    (:require [clojure.string :as str]
              [re-frame.core :refer [subscribe dispatch]]
              [reagent.core :as r]
              [reagent-dnd.core :as dnd]))
              ; [dnd-examples.example-sortable :refer [main]]))

(def card-source
  #js {:beginDrag (fn [props]
                      #js {:id (.-card.id props)
                           :index (.-card.index props)})
       :endDrag (fn [props monitor]
                    (let [item (.getItem monitor)
                          drop-result (.getDropResult monitor)]
                         (when (not (nil? drop-result))
                          (dispatch [:move-card (.-id item) (.-id drop-result)]))))})

(def card-target
  #js {:hover
        (fn [props monitor component]
           (let [drag-index (-> monitor .getItem .-index)
                 hover-index (.-card.index props)
                 hover-column (aget props "card" "column-id")]
              (when (not= drag-index hover-index)
                 (let [hoverBoundingRect (-> component js/ReactDOM.findDOMNode .getBoundingClientRect)
                       hoverMiddleY (/ (- (.-bottom hoverBoundingRect) (.-top hoverBoundingRect)) 2)
                       clientOffset (.getClientOffset monitor)
                       hoverClientY (- (.-y clientOffset) (.-top hoverBoundingRect.top))]
                    (when (not (or
                                 (and (< drag-index hover-index) (< hoverClientY hoverMiddleY))
                                 (and (> drag-index hover-index) (> hoverClientY hoverMiddleY))))
                      (.moveCard props hover-column drag-index hover-index)
                      (set! (.-index (.getItem monitor)) hover-index))))))})

(defn card-source-collect [connect monitor]
  #js {:connectDragSource (.dragSource connect)
       :isDragging (.isDragging monitor)
       :dragItem (.getItem monitor)})

(defn card-target-collect [connect monitor]
  #js {:connectDropTarget (.dropTarget connect)})

(defn render-card [{card :card
                    drag-item :dragItem
                    is-dragging :isDragging
                    connect-drag-source :connectDragSource
                    connect-drop-target :connectDropTarget}]
    (connect-drag-source
      (connect-drop-target
        (r/as-element
          (let [title (.-title card)
                card-id (.-id card)
                is-dragging-x (and (not (nil? drag-item)) (= card-id (.-id drag-item)))]
            [:div.kanban-card {:style {:opacity (if (or is-dragging is-dragging-x) 0.2 1)}}
                              title])))))

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
                      connect-drop-target :connectDropTarget
                      move-card :moveCard}]
  (let [title (.-title column)
        column-id (.-id column)]
    (connect-drop-target
      (r/as-element
        [:div
          [:div.kanban-column
            [:header title]
            [:section.wrapper.ui-sortable
              (for [card cards]
                ^{:key (.-id card)} [draggable-card {:card card :move-card move-card}])]
            [:footer [create-card column-id]]]]))))

(defn board [draggable-card droppable-column]
  (let [columns (subscribe [:columns])
        cards (subscribe [:cards])
        move-card (fn [column-id drag-index hover-index]
                    (dispatch [:move-card-x column-id drag-index hover-index]))]
    (fn []
      (let [cards-indexed (vec (map-indexed (fn [idx item] (assoc item :index idx)) @cards))
            column-cards-idx (group-by :column-id cards-indexed)]
        [:div.kanban-board
          (for [{column-id :id :as column} @columns]
            (let [column-cards (column-cards-idx column-id)]
                 ^{:key column-id} [droppable-column {:column column :cards column-cards :draggable-card draggable-card :move-card move-card}]))]))))

(defn main-panel []
  (let [context-provider (r/adapt-react-class (.-DragDropContextProvider js/ReactDnD))
        drag-source (.-DragSource js/ReactDnD)
        drop-target (.-DropTarget js/ReactDnD)
        backend (.-default js/ReactDnDHTML5Backend)
        card-item
          (r/adapt-react-class
            ((drop-target "card" card-target card-target-collect) ((drag-source "card" card-source card-source-collect) (r/reactify-component render-card))))
        droppable-column (r/adapt-react-class ((drop-target "card" column-target column-collect) (r/reactify-component render-column)))]
    [context-provider {:backend backend}
      [board card-item droppable-column]]))

; (defn main-panel []
;   [main])
