(ns kanban.importer
    (:require [ajax.core :refer [GET POST]]))

(defn map-trello-data [data]
  (let [columns (map (fn [{:keys [id name]}]
                       {:id id :title name})
                     (filter #(-> % :closed not) (:lists data)))
        cards (map (fn [{id :id name :name id-list :idList}]
                      {:id id :title name :column-id id-list})
                   (:cards data))]
    {:columns (vec columns)
     :cards (vec cards)}))

(defn load-trello-data [url handler]
  (GET url
       {:response-format :json
        :keywords? true
        :handler #(handler (map-trello-data %))}))

(defn map-github-issues-data [data]
  (let [cards (map (fn [{:keys [id title labels]}]
                     {:id id :title title :column-id (-> labels first :name)})
                   data)
        columns (map-indexed (fn [idx item] {:id (inc idx) :title item}) (distinct (map :column-id cards)))
        column-index (group-by :title columns)
        cards-mapped (map #(assoc % :column-id (-> (% :column-id) column-index first :id)) cards)]
       {:columns (vec columns)
        :cards (vec cards-mapped)}))

(defn load-github-issues-data [url handler]
  (GET url
    {:response-format :json
     :keywords? true
     :handler #(handler (map-github-issues-data %))}))

(defn parse-markdown [src]
  (let [md (js/Remarkable.)
        tokens (.parse md src #js{})]
    (js->clj tokens :keywordize-keys true)))

(defn extract-headings [tokens]
  (->> tokens
    (drop-while #(not= (:type %) "heading_open"))
    (partition-by #(= (:type %) "heading_open"))
    (map first)
    (partition 2)
    (map (fn [[{level :hLevel}
               {name :content}]]
           {:level level :name name}))))

(defn add-child [node child]
  (update-in node [:children] #(conj (vec %1) %2) child))

(defn make-tree
  ([coll] (let [root (first (make-tree {:level 0 :name "root" :children []} coll))
                children (:children root)]
             (if (= (count children) 1)
               (first children)
               root)))
  ([node coll] (let [[x & xs] coll
                     diff (- (:level x) (:level node) 1)]
                 (if (< diff 0)
                     [node coll]
                     (let [[child more] (if (= diff 0)
                                          (make-tree x xs)
                                          (make-tree {:level (inc (:level node)) :name ""} coll))]
                       (make-tree (add-child node child) more))))))

(defn map-markdown-tree [data]
  {:columns (into [] (map-indexed
                      (fn [idx {name :name}]
                        {:id (inc idx) :title name})
                      (:children data)))
   :cards (into [] (apply concat
                     (map-indexed
                        (fn [column-idx {children :children}]
                            (let [column-id (inc column-idx)]
                              (map-indexed (fn [idx {name :name}]
                                              {:id (inc idx) :title name :column-id column-id})
                                           children)))
                        (:children data))))})

(defn load-markdown-data [url handler]
  (GET url
     :handler (comp
                 handler
                 map-markdown-tree
                 make-tree
                 extract-headings
                 parse-markdown)))
