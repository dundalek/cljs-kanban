(ns kanban.views
    (:require [clojure.string :as str]
              [re-frame.core :refer [subscribe dispatch]]
              [re-frame.db :refer [app-db]]
              [re-com.core :refer [button throbber alert-box]]
              [reagent.core :as r]
              [kanban.importer :as importer]
              [kanban.exporter :as exporter]))

(def card-source
  #js {:beginDrag (fn [props]
                      (.add js/document.body.classList "is-dragging")
                      #js {:id (.-card.id props)})
       :endDrag (fn [props monitor]
                    (.remove js/document.body.classList "is-dragging")
                    (let [item (.getItem monitor)
                          drop-result (.getDropResult monitor)]
                         (when (not (nil? drop-result))
                          (dispatch [:move-card (.-id item) (.-id drop-result)]))))})

(def card-target
  #js {:hover
        (fn [props monitor component]
           (let [drag-id (-> monitor .getItem .-id)
                 hover-id (.-card.id props)
                 hover-column (aget props "card" "column-id" 0 "id")]
              (when (not= drag-id hover-id)
                 (let [hoverBoundingRect (-> component js/ReactDOM.findDOMNode .getBoundingClientRect)
                       hoverMiddleY (/ (- (.-bottom hoverBoundingRect) (.-top hoverBoundingRect)) 2)
                       clientOffset (.getClientOffset monitor)
                       hoverClientY (- (.-y clientOffset) (.-top hoverBoundingRect))
                       direction (if (< hoverClientY hoverMiddleY) :above :below)]
                    (.moveCard props hover-column drag-id hover-id direction)))))})

(defn card-source-collect [connect monitor]
  #js {:connectDragSource (.dragSource connect)
       :isDragging (.isDragging monitor)
       :dragItem (.getItem monitor)})

(defn card-target-collect [connect monitor]
  #js {:connectDropTarget (.dropTarget connect)})

(defn render-card [{card :card
                    drag-item :dragItem
                    is-dragging :isDragging
                    connect-drag-source :connectDragSource
                    connect-drop-target :connectDropTarget}]
    (connect-drag-source
      (connect-drop-target
        (r/as-element
          (let [title (.-title card)
                card-id (.-id card)
                is-dragging-x (and (not (nil? drag-item)) (= card-id (.-id drag-item)))]
            [:div.kanban-card {:style {:opacity (if (or is-dragging is-dragging-x) 0.2 1)}}
                              title])))))

(defn create-card [column-id]
 (let [text (r/atom "")
       creating? (r/atom false)
       reset (fn [] (reset! creating? false) (reset! text ""))]
   (fn [column-id]
     (if @creating?
      [:div.create-card
       [:textarea {:value @text :auto-focus true :on-change #(reset! text (-> % .-target .-value))}]
       [:div [button :label "Add" :class "btn-primary" :on-click (fn [] (dispatch [:add-card {:column-id column-id :title @text}]) (reset))]
             [button :label "X" :class "btn-link" :on-click reset]]]
      [:div.create-card-link {:on-click #(reset! creating? true)} "Add a card"]))))

(def column-target
  #js {:drop (fn [props] #js{:id (.-column.id props)})
       :hover
         (fn [props monitor component]
           (when (and (nil? (.-cards props)))
            (dispatch [:move-card (-> monitor .getItem .-id) (.-column.id props)])))})

(defn column-collect [connect monitor]
  #js {:connectDropTarget (.dropTarget connect)
       :isOver (.isOver monitor)
       :canDrop (.canDrop monitor)})

(defn render-column [{column :column
                      cards :cards
                      draggable-card :draggableCard
                      connect-drop-target :connectDropTarget
                      move-card :moveCard}]
  (let [title (.-title column)
        column-id (.-id column)]
    (connect-drop-target
      (r/as-element
          [:div.kanban-column
            [:header title]
            [:section.wrapper.ui-sortable
              (for [card cards]
                ^{:key (.-id card)} [draggable-card {:card card :move-card move-card}])]
            [:footer [create-card column-id]]]))))

(defn board [draggable-card droppable-column]
  (let [columns (subscribe [:columns])
        move-card (fn [column-id drag-index hover-index direction]
                    (dispatch [:move-card-x column-id drag-index hover-index direction]))]
    (fn []
      [:div.kanban-board
        (for [{column-id :db/id title :column/title column-cards :cards} @columns]
          (let [column {:id column-id :title title}
                cards (map (fn [{card-id :db/id card-title :card/title card-column-id :column}]
                              {:id card-id :title card-title :column-id card-column-id})
                        column-cards)]
            ^{:key column-id} [droppable-column {:column column :cards cards :draggable-card draggable-card :move-card move-card}]))])))

(def import-services
 {:trello
   {:label "Import Trello board"
    :placeholder "Trello import URL"
    :default-value "https://trello.com/b/TFWoOH5n.json"
    :info [:div
            [:br]
            [:h4 "Import from Trello"]
            [:ul
              [:li "Go to your Trello board."]
              [:li "Open the menu on the right."]
              [:li "Select Other -> Export as JSON."]
              [:li "Copy and paste the link below."]]]
    :import-fn importer/load-trello-data}
  :github
   {:label "Import Github issues"
    :placeholder "username/repo"
    :default-value "https://api.github.com/repos/tinytacoteam/zazu/issues?filter=is:issue%20is:open"
    :info [:div
            [:br]
            [:h4 "Import from Github"]]
    :import-fn importer/load-github-issues-data}
  :markdown
   {:label "Import Markdown"
    :placeholder "URL"
    :default-value "/data/example-todo.md"
    :info [:div
            [:br]
            [:h4 "Import Markdown"]]
    :import-fn importer/load-markdown-data}})

(defn import-form [{:keys [info import-fn placeholder default-value]} on-cancel]
  (let [text (r/atom default-value)
        loading (r/atom false)
        error (r/atom nil)]
    (fn []
      [:div
        info
        [:form {:on-submit
                  (fn [ev]
                    (.preventDefault ev)
                    (reset! error nil)
                    (reset! loading true)
                    (import-fn @text
                      (fn [err data]
                        (if err
                          (do
                            (reset! error err)
                            (reset! loading false))
                          (do
                            (on-cancel)
                            (dispatch [:import-db data]))))))}
          [:div.form-group
            {:class (when @error "has-error")}
            [:input {:type "text"
                     :class-name "form-control"
                     :placeholder placeholder
                     :auto-focus true
                     :value @text
                     :on-change #(reset! text (.-target.value %))
                     :disabled @loading}]]
          (when @error
            [alert-box :alert-type :danger :body @error])
          [:div.btn-group
            [button :label "Cancel" :on-click (fn [ev] (.preventDefault ev) (on-cancel))]
            [button :label [:span
                              "Import"
                              (when @loading [throbber :size :small :color "#fff" :style {:margin "0 0 0 10px"}])]
                    :class "btn-primary"
                    :attr {:type "submit"
                           :disabled @loading}]]]])))

(defn sidebar []
  (let [active (r/atom nil)
        cancel #(reset! active nil)]
    (fn []
      [:div.sidebar
        [:h2 "Kanban Demo"]
        (if @active
          [import-form (@active import-services) cancel]
          [:div
            [:p "A sample project featuring Drag and Drop using " [:a {:href "https://react-dnd.github.io/react-dnd/"} "react-dnd"] " written in ClojureScript."]
            [:p "Check out the " [:a {:href "https://github.com/dundalek/cljs-kanban"} "source code"] " on Github."]
            [:br]
            [:h4 "Import data"]
            (for [[key {label :label}] (seq import-services)]
              ^{:key key} [:div
                            [button :label label
                                    :on-click #(reset! active key)]])
            [:br]
            [:h4 "Export data"]
            [:div
              [button :label "Export as JSON"
                      :on-click #(exporter/download "board.json" (exporter/export-json @app-db))]]
            [:div
              [button :label "Export as Markdown"
                      :on-click #(exporter/download "board.md" (exporter/export-markdown @app-db))]]])])))

(defn main-panel []
  (let [context-provider (r/adapt-react-class (.-DragDropContextProvider js/ReactDnD))
        drag-source (.-DragSource js/ReactDnD)
        drop-target (.-DropTarget js/ReactDnD)
        backend (aget js/ReactDnDHTML5Backend "default")
        card-item
          (r/adapt-react-class
            ((drop-target "card" card-target card-target-collect) ((drag-source "card" card-source card-source-collect) (r/reactify-component render-card))))
        droppable-column (r/adapt-react-class ((drop-target "card" column-target column-collect) (r/reactify-component render-column)))]
    [context-provider {:backend backend}
      [:div.app-container
        [board card-item droppable-column]
        [sidebar]]]))
