(ns web-game.utilities
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [cljs.core.async
    :as a
    :refer [>! <! chan close! take!]]))

(def entities-vector (r/atom []))

(def bullet-types (r/atom {}))

(def frame (atom 0))

(defn get-external-property [e property]
  (let [css-map (second e)]
    (if (map? css-map)
      (get css-map property)
      nil)))

(defn get-property [e property]
  (let [css-map (second e)]
    (if (map? css-map)
      (get-in css-map [:style property])
      nil)))

(defn add-entity [entity & args]
  (swap! entities-vector conj (apply (get-property entity :init) entity args)))

(defn remove-entity [entity]
  (swap! entities-vector (partial map #(if (= entity %1)
                                         nil
                                         %1)))
  (swap! entities-vector (partial filter identity)))

(defn add-bullet-type [bullet-key bullet-ns]
  (swap! bullet-types assoc bullet-key bullet-ns))

(defn change-attribute [e attribute value]
  (let [css-map (second e)]
    (if (map? css-map)
      (let [new-css (assoc-in css-map [:style attribute] value)]
        (into [] (concat [(first e) new-css] (rest (rest e)))))
      e)))

(defn change-external-attribute [e attribute value]
  (let [css-map (second e)]
    (if (map? css-map)
      (let [new-css (assoc css-map attribute value)]
        (into [] (concat [(first e) new-css] (rest (rest e)))))
      e)))

(defn move-element [e]
  (let [css-map (second e)]
    (if (map? css-map)
      (let [current-x (js/parseFloat  (get-in css-map [:style :left]))
            current-y (js/parseFloat  (get-in css-map [:style :bottom]))
            current-vx (get-in css-map [:style :vx])
            current-vy (get-in css-map [:style :vy])
            new-css (-> css-map
                        (assoc-in [:style :left] (str (+ current-vx current-x) "px"))
                        (assoc-in [:style :bottom] (str (+ current-vy current-y) "px")))]
        (into [] (concat [(first e) new-css] (rest (rest e)))))
      e)))

(defn percent-to-pixels [axis percentage]
  (let [axis-length (if (= :x axis) (* (.-innerWidth js/window) 0.8) (.-innerHeight js/window))]
    (str (int (* axis-length (/ percentage 100))) "px")))

(defn change-entity-velocity [id e vx vy & args]
  (let [css-map (second e)]
    (if (map? css-map)
      (let [new-css (-> css-map
                        (assoc-in [:style :vx] vx)
                        (assoc-in [:style :vy] vy))]
        (into [] (concat [(first e) new-css] (rest (rest e)))))
      e)))

(defn get-velocity [direction]
  (fn [e]
    (get-in (second e) [:style direction])))

(def get-velocity-x (get-velocity :vx))
(def get-velocity-y (get-velocity :vy))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn wait [frames]
  (let [c (chan)
        initial-frame @frame
        closed? (atom false)]
    (go
     (while (not @closed?)
       (<! (timeout 16.67))
       (if (= @frame (+ initial-frame frames))
         (do
           (close! c)
           (swap! closed? not)))))
    c))

(defn on-loop [entity]
  (let [loop-fn (get-property entity :on-loop)]
    (if loop-fn
      (loop-fn entity)
      entity)))

(defn get-id [entity]
  (get-external-property entity :id))

(defn get-center [entity]
  {:x (+ (:x (js/parseFloat (get-property entity :middle))) (js/parseFloat (get-property entity :left))) :y (+ (:y (js/parseFloat (get-property entity :middle))) (js/parseFloat (get-property entity :bottom)))})

(def get-distance
  (memoize
   (fn [point1 point2]
     (let [x1 (:x point1)
           y1 (:y point1)
           x2 (:x point2)
           y2 (:y point2)
           delta-x (.abs js/Math (- x1 x2))
           delta-y (.abs js/Math (- y1 y2))
           x-squared (.pow js/Math delta-x 2)
           y-squared (.pow js/Math delta-y 2)
           distance (.sqrt js/Math (+ x-squared y-squared))]
       distance))))

(defn colliding? [entity1 entity2]
  (let [point1 (get-center entity1)
        point2 (get-center entity2)
        distance (get-distance point1 point2)
        colliding-distance (+ (get-property entity1 :hitbox) (get-property entity2 :hitbox))]
    (<= distance colliding-distance)))

(defn get-collisions [entity]
  (let [result (chan)]
    (go
     (>! result (filter #(not= (get-id entity) (get-id %1)) (filter (partial colliding? entity) @entities-vector))))
    result))

(defn on-frame []
  (go
   (doall (swap! entities-vector (partial map on-loop)))
   (doall (swap! entities-vector (partial map move-element)))
   (doall (swap! entities-vector (partial sort-by #(get-property %1 :priority))))))

(defn- sub-change-entity [id func entity & args]
  (if (= (:id (second entity)) id)
    (let [result (apply func id entity args)]
      result)
    entity))

(defn change-entity [id func & args]
  (swap! entities-vector (partial mapv #(apply sub-change-entity id func %1 args))))

(defn get-entity-by-id [id]
  (first (filter #(= (get-external-property %1 :id) id) @entities-vector)))

(defn load-dimensions [id entity & args]
  (let [e (.getElementById js/document id)
        width (.-width e)
        height (.-height e)
        middle {:x (/ width 2) :y (/ height 2)}
        result (-> entity
                   (change-attribute :width width)
                   (change-attribute :height height)
                   (change-attribute :middle middle))]
    result))

(defn change-velocity [e vx vy]
  (change-entity (get-id e) change-entity-velocity vx vy))

(go
 (while true
   (<! (timeout 16.67))
   (swap! frame inc)
   (on-frame)))

(defn handle-collision [entity1 entity2]
  (go
   (let [collision-func (get-property entity2 :on-collision)]
     (if (fn? collision-func)
       (collision-func entity2 entity1))))
  entity2)

(defn handle-collisions [entity collisions]
  (let [collision-func (get-property entity :on-collision)]
    (if (fn? collision-func)
      (collision-func entity collisions)))
  (doall (map (partial handle-collision entity) collisions)))

(go
 (while true
   (<! (wait 3))
   (let [player (get-entity-by-id "player")
         collisions (<! (get-collisions player))]
     (if (not (empty? collisions))
       (handle-collisions player collisions)))))

(defn spawn-bullet [bullet-key & args]
  (let [bullet (get @bullet-types bullet-key)]
    (if bullet
      (apply add-entity bullet args))))
