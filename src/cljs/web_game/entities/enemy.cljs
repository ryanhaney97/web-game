(ns web-game.entities.enemy
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [web-game.utilities :as u]
   [cljs.core.async
    :as a
    :refer [>! <! chan sub]]))

(def enemy
  (r/atom
   [:img {:src "images/enemy.png"
          :style {:position "absolute"
                  :bottom (u/percent-to-pixels :y 100)
                  :left (u/percent-to-pixels :x 47.5)
                  :vx 0
                  :vy 0}}]))
