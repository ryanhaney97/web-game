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
                  :width "42px"
                  :height "44px"
                  :vx 0
                  :vy 0}}]))

(go
 (swap! enemy u/change-velocity 0 -1)
 (while (not= (u/get-property @enemy :bottom) (u/percent-to-pixels :y 80))
   (<! (u/timeout 16.67)))
 (swap! enemy u/change-velocity 0 0)
 (while true
   (u/spawn-bullet :simple (u/get-property @enemy :left) (u/get-property @enemy :bottom) 0 -1)
   (<! (u/wait 60))))
