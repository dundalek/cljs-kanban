(ns kanban.db)

(def default-db
  {:columns [{:title "Backlog"
              :cards [{:title "a"}
                      {:title "b"}]}
             {:title "To Do"
              :cards [{:title "a"}
                      {:title "b"}]}
             {:title "In Progress"
              :cards [{:title "a"}
                      {:title "b"}]}
             {:title "Done"
              :cards []}]})
