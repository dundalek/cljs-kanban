(ns kanban.views
    (:require [re-frame.core :as re-frame]))

(defn main-panel []
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
