(ns web-game.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [web-game.entities.player :refer [player]]
            [web-game.entities.enemy :refer [enemy]]
            [web-game.input :refer [initialize-input]]
            [web-game.utilities :as u]
            [cljs.core.async
             :as a
             :refer [>! <! chan]]))

(initialize-input)

(defn entities [entities-vector]
  (into [] (cons :div (map deref @entities-vector))))

(def root (.getElementById js/document "root"))

;; -------------------------
;; Initialize app
(defn mount-root []
  (r/render [entities u/entities-vector] root))

(defn ^:export init! []
  (reset! u/entities-vector [])
  (u/add-entity player)
  (u/add-entity enemy)
  (mount-root))
