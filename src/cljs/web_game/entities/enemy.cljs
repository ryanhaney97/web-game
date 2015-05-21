(ns web-game.entities.enemy
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [web-game.utilities :as u]
   [cljs.core.async
    :as a
    :refer [>! <! chan sub]]))

(defn init [entity & args]
  (let [id (gensym "enemy")
        enemy (-> entity
                  (u/change-external-attribute :id id)
                  (u/change-external-attribute :on-load (partial u/change-entity id u/load-dimensions)))]
    (go
     (u/change-velocity enemy 0 -1)
     (while (not= (u/get-property (u/get-entity-by-id id) :bottom) (u/percent-to-pixels :y 80))
       (<! (u/wait 1)))
     (u/change-velocity enemy 0 0)
     (while true
       (u/spawn-bullet :simple (u/get-property (u/get-entity-by-id id) :left) (u/get-property (u/get-entity-by-id id) :bottom) 0 -1)
       (<! (u/wait 60))))
    enemy))

(def enemy
  [:img {:src "images/enemy.png"
         :style {:position "absolute"
                 :bottom (u/percent-to-pixels :y 100)
                 :left (u/percent-to-pixels :x 47.5)
                 :width "42px"
                 :height "44px"
                 :priority 2
                 :init init
                 :vx 0
                 :vy 0}}])
