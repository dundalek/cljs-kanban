(ns kanban.views
    (:require [clojure.string :as str]
              [re-frame.core :as re-frame]
              [reagent.core :as r]
              [reagent-dnd.core :as dnd]))

(defn knight [state]
  [:span {:style {:opacity          (if (:dragging? @state) 0.1 1)
                  :cursor           :move
                  :background-color (if (:dragging? @state)
                                      :white
                                      :grey)
                  :font-size        "64px"}}
   "♘"])

(defn draggable
  [state]
  (fn []
    [dnd/drag-source
     :type       :knight
     :end-drag   (fn [state]
                   (js/console.log "dropped on: " (clj->js (:drop-result state))))
     :child      [knight state]
     :begin-drag (fn [] {:id 1})
     :state      state]))

(defn square [state]
  [:div {:style {:height           "64px"
                 :width            "64px"
                 :background-color (cond
                                     (:is-over? @state) "#555"
                                     (:can-drop? @state) "#777"
                                     :default "#999")}}])

(defn droppable
  [state]
  (fn []
    [dnd/drop-target
     :types [:knight]
     :drop (fn [state]
             (js/console.log "dropped item: " (clj->js (:item state)))
             {:id 5})
     :child [square state]
     :state state]))

(defn about []
 (let [drag-state (r/atom {})
       drop-state (r/atom {})]
   (fn []
     [:div
      [draggable drag-state]
      [droppable drop-state]])))

(defn board []
  (let [columns (re-frame/subscribe [:columns])]
    (fn []
      [:div.kanban-board
        (for [{title :title cards :cards} @columns]
             [:div.kanban-column
               {:key title}
               [:header title]
               [:section.wrapper.ui-sortable
                 (for [{title :title} cards]
                      [:div.kanban-card {:key title :draggable "true"} title])]
               [:footer "Add Card"]])])))

(defn main-panel []
  [(dnd/with-drag-drop-context
    @dnd/html5-backend
    about)])
    ;single-target-example)])
