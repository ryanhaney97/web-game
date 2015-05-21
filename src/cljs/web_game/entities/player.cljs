(ns web-game.entities.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [web-game.input :refer [input-pub]]
   [web-game.utilities :as u]
   [cljs.core.async
    :as a
    :refer [>! <! chan sub]]))

(defn init [entity & args]
  (let [id "player"]
    (-> entity
        (u/change-external-attribute :id id)
        (u/change-external-attribute :on-load (partial u/change-entity id u/load-dimensions)))))

(def player
  [:img {:src "images/player.png"
         :style {:position "absolute"
                 :bottom (u/percent-to-pixels :y 8.5)
                 :left (u/percent-to-pixels :x 47.5)
                 :width "64px"
                 :height "59px"
                 :priority 1
                 :init init
                 :vx 0
                 :vy 0}}])

(defn make-move-block [bound-key velocity axis]
  (go
   (let [direction-chan (sub input-pub bound-key (chan))]
     (while true
       (let [input (<! direction-chan)
             player (u/get-entity-by-id "player")
             velocities (if (= axis :vy)
                          [(partial u/get-velocity-x player) (partial identity velocity) (partial u/get-velocity-x player) (partial identity 0)]
                          [(partial identity velocity) (partial u/get-velocity-y player) (partial identity 0) (partial u/get-velocity-y player)])]
         (if (= (.-type input) "keydown")
           (u/change-velocity player ((first velocities)) ((second velocities)))
           (u/change-velocity player ((nth velocities 2)) ((nth velocities 3)))))))))

(make-move-block "Up" 3 :vy)
(make-move-block "Down" -3 :vy)
(make-move-block "Right" 3 :vx)
(make-move-block "Left" -3 :vx)
