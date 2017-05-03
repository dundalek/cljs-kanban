(ns dnd-examples.example-sortable
    (:require [reagent.core :as r]))

(def container-style
  {:width 400})

(def card-style
   {:border "1px dashed gray"
    :padding "0.5rem 1rem"
    :margin-bottom ".5rem"
    :background-color "white"
    :cursor "move"})

(def card-source
  #js {:beginDrag (fn [props] #js {:id (.-name props) :index (.-index props)})})

(def card-target
  #js {:hover
        (fn [props monitor component]
           (let [drag-index (-> monitor .getItem .-index)
                 hover-index (.-index props)]
              (when (not= drag-index hover-index)
                 (let [hoverBoundingRect (-> component js/ReactDOM.findDOMNode .getBoundingClientRect)
                       hoverMiddleY (/ (- (.-bottom hoverBoundingRect) (.-top hoverBoundingRect)) 2)
                       clientOffset (.getClientOffset monitor)
                       hoverClientY (- (.-y clientOffset) (.-top hoverBoundingRect.top))]
                    (when (not (or
                                 (and (< drag-index hover-index) (< hoverClientY hoverMiddleY))
                                 (and (> drag-index hover-index) (> hoverClientY hoverMiddleY))))
                      (.moveCard props drag-index hover-index)
                      (set! (.-index (.getItem monitor)) hover-index))))))})

(defn source-collect [connect monitor]
  #js {:connectDragSource (.dragSource connect)
       :isDragging (.isDragging monitor)})

(defn target-collect [connect monitor]
  #js {:connectDropTarget (.dropTarget connect)})

(defn card [{connect-drag-source :connectDragSource
             connect-drop-target :connectDropTarget
             is-dragging :isDragging
             text :text}]
  (let [opacity (if is-dragging 0 1)
        style (assoc card-style :opacity opacity)]
    (-> (r/as-element [:div {:style style} text])
        connect-drop-target
        connect-drag-source)))

(defn move-card [items drag-index hover-index]
   (let [drag-card (get items drag-index)
         items-removed
           (vec (concat (subvec items 0 drag-index) (subvec items (inc drag-index))))
         items-added
           (vec (concat (subvec items-removed 0 hover-index) [drag-card] (subvec items-removed hover-index)))]
     items-added))

(defn main []
  (let [context-provider (r/adapt-react-class (.-DragDropContextProvider js/ReactDnD))
        drag-source (.-DragSource js/ReactDnD)
        drop-target (.-DropTarget js/ReactDnD)
        backend (.-default js/ReactDnDHTML5Backend)
        card-item
          (r/adapt-react-class
            ((drop-target "card" card-target target-collect) ((drag-source "card" card-source source-collect) (r/reactify-component card))))
        cards (r/atom
                [{:id 1
                  :text "Write a cool JS library"}
                 {:id 2
                  :text "Make it generic enough"}
                 {:id 3
                  :text "Write README"}
                 {:id 4
                  :text "Create some examples"}
                 {:id 5
                  :text "Spam in Twitter and IRC to promote it (note that this element is taller than the others)"}
                 {:id 6
                  :text "???"}
                 {:id 7
                  :text "PROFIT"}])
        move-card (fn [drag-index hover-index]
                    (swap! cards move-card drag-index hover-index))]
    (fn []
      [context-provider {:backend backend}
        [:div
          [:div {:style container-style}
            (map-indexed (fn [idx {id :id text :text}]
                           [card-item
                             {:key id
                              :index idx
                              :id id
                              :text text
                              :move-card move-card}])
                        @cards)]]])))
