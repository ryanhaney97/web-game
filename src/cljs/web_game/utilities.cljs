(ns web-game.utilities
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [cljs.core.async
    :as a
    :refer [>! <! chan close!]]))

(def entities-vector (r/atom []))

(defn add-entity [entity]
  (swap! entities-vector conj entity))

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
  (let [axis-length (if (= :x axis) (.-innerWidth js/window) (.-innerHeight js/window))]
    (str (int (* axis-length (/ percentage 100))) "px")))

(defn change-velocity [e vx vy]
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

(go
 (while true
   (<! (timeout 16.67))
   (dorun (map #(swap! %1 move-element) @entities-vector))))
