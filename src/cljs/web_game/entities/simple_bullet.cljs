(ns web-game.entities.simple-bullet
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [web-game.utilities :as u]
   [cljs.core.async
    :as a
    :refer [>! <! chan sub]]))

(declare simple-bullet)

(defn init
  ([entity x y vx vy]
   (init entity "images/simple-bullet.png" x y vx vy))
  ([entity image x y vx vy]
   (let [id (str (gensym "simple-bullet"))
         result (-> entity
                    (u/change-external-attribute :src image)
                    (u/change-attribute :left x)
                    (u/change-attribute :bottom y)
                    (#(u/change-entity-velocity id %1 vx vy))
                    (u/change-external-attribute :id id)
                    (u/change-external-attribute :on-load (partial u/change-entity id u/load-dimensions)))]
     result)))

(defn on-loop [bullet]
  (let [x (js/parseFloat (u/get-property bullet :left))
        y (js/parseFloat (u/get-property bullet :bottom))]
    (if (or (< x 0) (< y 0) (> x (* (.-innerWidth js/window) 0.8)) (> y (.-innerHeight js/window)))
      (u/remove-entity bullet))
    bullet))

(def simple-bullet
  [:img {:src "images/simple-bullet.png"
         :style {:position "absolute"
                 :bottom (u/percent-to-pixels :y 100)
                 :left (u/percent-to-pixels :x 47.5)
                 :vx 0
                 :vy 0
                 :hitbox 19
                 :priority 3
                 :init init
                 :bullet :simple
                 :on-loop on-loop}}])
