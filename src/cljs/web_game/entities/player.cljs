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

(defn flicker
  [amount id]
  (let [c (chan)]
    (go
     (loop [current-amount 0]
       (<! (u/wait 1))
       (let [entity (u/get-entity-by-id id)
             visible? (= (u/get-property entity :visibility) "visible")]
         (if visible?
           (u/change-entity id #(u/change-attribute %2 :visibility "hidden"))
           (u/change-entity id #(u/change-attribute %2 :visibility "visible")))
         (if (<= amount current-amount)
           (do
             (u/change-entity id #(u/change-attribute %2 :visibility "visible"))
             (>! c ""))
           (recur (inc current-amount))))))
    c))

(defn pichun []
  (go
   (u/change-entity "player" #(u/change-attribute %2 :invulnerable? true))
   (u/change-entity "player" #(u/change-attribute %2 :visibility "hidden"))
   (<! (u/wait 60))
   (u/change-entity "player" #(u/change-attribute %2 :bottom (u/percent-to-pixels :y 8.5)))
   (u/change-entity "player" #(u/change-attribute %2 :left (u/percent-to-pixels :x 47.5)))
   (u/change-entity "player" #(u/change-attribute %2 :visibility "visible"))
   (<! (flicker 120 "player"))
   (u/change-entity "player" #(u/change-attribute %2 :invulnerable? false))))

(defn on-collision [player collisions]
  (let [bullets (filter #(u/get-property %1 :bullet) collisions)]
    (if (not (or (empty? bullets) (u/get-property player :invulnerable?)))
      (pichun))))

(def player
  [:img {:src "images/player.png"
         :style {:position "absolute"
                 :bottom (u/percent-to-pixels :y 8.5)
                 :left (u/percent-to-pixels :x 47.5)
                 :priority 1
                 :init init
                 :hitbox 7
                 :visiblility "visible"
                 :invulnerable? false
                 :on-collision on-collision
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
