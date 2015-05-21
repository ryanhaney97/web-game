(ns web-game.utilities
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [cljs.core.async
    :as a
    :refer [>! <! chan close!]]))

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

(defn on-frame []
  (swap! entities-vector (partial map on-loop))
  (swap! entities-vector (partial map move-element))
  (swap! entities-vector (partial sort-by #(get-property %1 :priority))))

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

(defn get-id [entity]
  (get-external-property entity :id))

(defn change-velocity [e vx vy]
  (change-entity (get-id e) change-entity-velocity vx vy))

(defn get-center [entity]
  {:x (+ (:x (get-property entity :middle)) (get-property entity :left)) :y (+ (:y (get-property entity :middle)) (get-property entity :bottom))})

(go
 (while true
   (<! (timeout 16.67))
   (swap! frame inc)
   (on-frame)))

(defn spawn-bullet [bullet-key & args]
  (let [bullet (get @bullet-types bullet-key)]
    (if bullet
      (apply add-entity bullet args))))
