(ns web-game.entities.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [web-game.input :refer [input-pub]]
   [web-game.utilities :as u]
   [cljs.core.async
    :as a
    :refer [>! <! chan sub]]))

(def player
  (r/atom
   [:img {:src "images/player.png"
          :style {:position "absolute"
                  :bottom (u/percent-to-pixels :y 8.5)
                  :left (u/percent-to-pixels :x 47.5)
                  :vx 0
                  :vy 0}}]))

(defn make-move-block [bound-key velocity axis]
  (let [velocities (if (= axis :vy)
                     [(comp u/get-velocity-x (partial deref player)) (partial identity velocity) (comp u/get-velocity-x (partial deref player)) (partial identity 0)]
                     [(partial identity velocity) (comp u/get-velocity-y (partial deref player)) (partial identity 0) (comp u/get-velocity-y (partial deref player))])]
    (go
     (let [direction-chan (sub input-pub bound-key (chan))]
       (while true
         (let [input (<! direction-chan)]
           (if (= (.-type input) "keydown")
             (swap! player u/change-velocity ((first velocities)) ((second velocities)))
             (swap! player u/change-velocity ((nth velocities 2)) ((nth velocities 3))))))))))

(make-move-block "Up" 3 :vy)
(make-move-block "Down" -3 :vy)
(make-move-block "Right" 3 :vx)
(make-move-block "Left" -3 :vx)
