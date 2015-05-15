(ns web-game.input
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [web-game.utilities :as u]
   [cljs.core.async
    :as a
    :refer [>! <! chan put! take! pub]]))

(defn get-topic [e]
  (.-keyIdentifier e))

(def input-chan (chan))

(def input-pub (pub input-chan get-topic))

(defn general-input [e]
  (go
   (if (= (.-type e) "keydown")
     (<! (u/timeout 40)))
   (>! input-chan e)))

(defn initialize-input []
  (let [listener (window.keypress.Listener.)]
    (.register_combo listener (clj->js {:keys "up"
                                        :on_keyup general-input
                                        :on_keydown general-input
                                        :prevent_repeat true}))
    (.register_combo listener (clj->js {:keys "down"
                                        :on_keyup general-input
                                        :on_keydown general-input
                                        :prevent_repeat true}))
    (.register_combo listener (clj->js {:keys "left"
                                        :on_keyup general-input
                                        :on_keydown general-input
                                        :prevent_repeat true}))
    (.register_combo listener (clj->js {:keys "right"
                                        :on_keyup general-input
                                        :on_keydown general-input
                                        :prevent_repeat true}))))
