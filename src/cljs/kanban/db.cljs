(ns kanban.db)

(def default-db
  {:columns [{:id 1 :title "Backlog"}
             {:id 2 :title "To Do"}
             {:id 3 :title "In Progress"}
             {:id 4 :title "Done"}]
   :cards
    (vec
      (concat
        [{:id 1 :title "a" :column-id 1}
         {:id 2 :title "b" :column-id 1}
         {:id 6 :title "f" :column-id 1}
         {:id 7 :title "g" :column-id 1}
         {:id 3 :title "c" :column-id 2}
         {:id 4 :title "d" :column-id 2}
         {:id 5 :title "e" :column-id 3}]))})
        ;(map (fn [id] {:id id :title (str "card-" id) :column-id 1}) (range 10 30))))})
