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

(defn add-entity [entity]
  (if (coll? entity)
    (swap! entities-vector conj (r/atom entity))
    (swap! entities-vector conj entity)))

(defn add-bullet-type [bullet-key bullet-ns]
  (swap! bullet-types assoc bullet-key bullet-ns))

(defn change-attribute [e attribute value]
  (let [css-map (second e)]
    (if (map? css-map)
      (let [new-css (assoc-in css-map [:style attribute] value)]
        (into [] (concat [(first e) new-css] (rest (rest e))))))))

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

(defn get-property [e property]
  (let [css-map (second e)]
    (if (map? css-map)
      (get-in css-map [:style property])
      nil)))

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

(defn on-frame []
  (dorun (map #(swap! %1 move-element) @entities-vector)))

(go
 (while true
   (<! (timeout 16.67))
   (swap! frame inc)
   (on-frame)))

(defn spawn-bullet [bullet-key & args]
  (let [bullet (get @bullet-types bullet-key)]
    (if bullet
      (add-entity (apply (get-property bullet :init) args)))))
