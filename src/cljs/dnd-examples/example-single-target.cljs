(ns dnd-examples.example-single-target
    (:require [reagent.core :as r]))

(def box-style {:border "1px dashed gray"
                 :background-color "white"
                 :padding "0.5rem 1rem"
                 :margin-right "1.5rem"
                 :margin-bottom "1.5rem"
                 :cursor "move"
                 :float "left"})

(def dustbin-style {:height "12rem"
                    :width "12rem"
                    :margin-right "1.5rem"
                    :margin-bottom "1.5rem"
                    :color "white"
                    :padding "1rem"
                    :text-align "center"
                    :font-size "1rem"
                    :line-height "normal"
                    :float "left"})

(def dustbin-target
  #js {:drop (fn [] #js{:name "Dustbin"})})

(defn dustbin-collect [connect monitor]
  #js {:connectDropTarget (.dropTarget connect)
       :isOver (.isOver monitor)
       :canDrop (.canDrop monitor)})

(defn dustbin [{can-drop :canDrop is-over :isOver, connect-drop-target :connectDropTarget}]
  (let [is-active (and can-drop is-over)
        bg-color (cond
                    is-active "darkgreen"
                    can-drop "darkkhaki"
                    :else "#222")
        style (assoc dustbin-style :background-color bg-color)]
    (connect-drop-target
      (r/as-element [:div {:style style}
                          (if is-active "Release to drop" "Drag a box here")]))))

(def box-source
  #js {:beginDrag (fn [props] {:id (.-name props)})
       :endDrag (fn [props monitor]
                    (let [item (.getItem monitor)
                          drop-result (.getDropResult monitor)]
                         (when (not (nil? drop-result))
                          (js/console.log "dropped" (:id item) "into" (.-name drop-result)))))})

(defn box-collect [connect monitor]
  #js {:connectDragSource (.dragSource connect)
       :isDragging (.isDragging monitor)})

(defn box [{connect-drag-source :connectDragSource is-dragging :isDragging name :name}]
  (let [opacity (if is-dragging 0.4 1)
        style (assoc box-style :opacity opacity)]
    (connect-drag-source
     (r/as-element [:div {:style style} name]))))

(defn main []
  (let [context-provider (r/adapt-react-class (.-DragDropContextProvider js/ReactDnD))
        drag-source (.-DragSource js/ReactDnD)
        drop-target (.-DropTarget js/ReactDnD)
        backend (.-default js/ReactDnDHTML5Backend)
        draggable-box (r/adapt-react-class ((drag-source "box" box-source box-collect) (r/reactify-component box)))
        droppable-dustbin (r/adapt-react-class ((drop-target "box" dustbin-target dustbin-collect) (r/reactify-component dustbin)))]
    [context-provider {:backend backend}
      [:div
        [:div {:style {:overflow "hidden" :clear "both"}}
          [droppable-dustbin]]
        [:div {:style {:overflow "hidden" :clear "both"}}
          [draggable-box {:name "Glass"}]
          [draggable-box {:name "Banana"}]
          [draggable-box {:name "Paper"}]]]]))
