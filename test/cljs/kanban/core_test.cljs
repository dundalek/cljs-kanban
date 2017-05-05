(ns kanban.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [kanban.core :as core]
            [kanban.importer :refer [make-tree]]))


(def headings1
  [{:level 1 :name "a"}
   {:level 2 :name "b"}])

(def tree1
  {:level 1 :name "a" :children
    [{:level 2 :name "b"}]})

(def headings2
  [{:level 1, :name "a"}
   {:level 2, :name "bb"}
   {:level 3, :name "ccc"}
   {:level 3, :name "ddd"}
   {:level 2, :name "ee"}
   {:level 3, :name "fff"}])

(def tree2
   {:level 1, :name "a" :children
      [{:level 2, :name "bb" :children
         [{:level 3, :name "ccc"}
          {:level 3, :name "ddd"}]}
       {:level 2, :name "ee" :children
         [{:level 3, :name "fff"}]}]})

(def headings3
  [{:level 1 :name "a"}
   {:level 4 :name "b"}])

(def tree3
  {:level 1 :name "a" :children
    [{:level 2 :name "" :children
      [{:level 3 :name "" :children
        [{:level 4 :name "b"}]}]}]})


(deftest fake-test
  (testing "fake description"
    (is (= (make-tree headings1) tree1))
    (is (= (make-tree headings2) tree2))
    (is (= (make-tree headings3) tree3))))
