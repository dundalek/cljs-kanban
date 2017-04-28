(ns reagent-dnd.react-dnd
  (:require [reagent.core :as r]))

(def drag-drop-context
  (delay
    (.-DragDropContext js/ReactDnD)))

(def drag-layer
  (delay
    (.-DragLayer js/ReactDnD)))

(def drag-source
  (delay
    (.-DragSource js/ReactDnD)))

(def drop-target
  (delay
    (.-DropTarget js/ReactDnD)))

(def html5-backend
  (delay
    (.-HTML5 js/ReactDnD)))

(def get-empty-image
  (delay
    (.getEmptyImage @html5-backend)))

(defn with-drag-drop-context [backend]
  (fn [component]
    (r/adapt-react-class
     ((@drag-drop-context backend)
      (r/reactify-component component)))))
