(ns kanban.views
    (:require [re-frame.core :as re-frame]))

(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.kanban-board
        [:div.kanban-column
         [:header "Backlog"]
         [:section.wrapper.ui-sortable
          [:div.kanban-card {:draggable "true"} "a"]
          [:div.kanban-card {:draggable "true"} "b"]]
         [:footer "Add Card"]]
        [:div.kanban-column
         [:header "To Do"]
         [:section.wrapper.ui-sortable]
         [:footer "Add Card"]]
        [:div.kanban-column
         [:header "In Progress"]
         [:section.wrapper.ui-sortable]
         [:footer "Add Card"]]])))
