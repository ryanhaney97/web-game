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
  ([x y vx vy]
   (init "images/simple-bullet.png" x y vx vy))
  ([image x y vx vy]
   (-> simple-bullet
       (u/change-attribute :src image)
       (u/change-attribute :left x)
       (u/change-attribute :bottom y)
       (u/change-velocity vx vy))))

(def simple-bullet
  [:img {:src "images/simple-bullet.png"
         :style {:position "absolute"
                 :bottom (u/percent-to-pixels :y 100)
                 :left (u/percent-to-pixels :x 47.5)
                 :vx 0
                 :vy 0
                 :init init
                 :bullet :simple}}])
