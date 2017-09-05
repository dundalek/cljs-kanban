(ns kanban.db)

(def schema {:column {:db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}})

(def data
  (->> [{:db/id -1 :column/title "Backlog X"}
        {:db/id -2 :column/title "To Do"}
        {:db/id -3 :column/title "In Progress"}
        {:db/id -4 :column/title "Done"}
        {:db/id -11 :card/title "a" :column -1}
        {:db/id -12 :card/title "b" :column -1}
        {:db/id -16 :card/title "f" :column -1}
        {:db/id -17 :card/title "g" :column -1}
        {:db/id -13 :card/title "c" :column -2}
        {:db/id -14 :card/title "d" :column -2}
        {:db/id -15 :card/title "e" :column -3}]
    (map-indexed (fn [idx item]
                   (assoc item :order (* idx 100))))))
