(ns power-timer.core
  (:require [seesaw.core :as seesaw]
            [seesaw.dev :as sdev]
            [power-timer.timer :as timer-lib]))

(def config (-> "./config.edn" slurp clojure.edn/read-string))

(defn parse-human-readable-timer [str]
  (let [hours (Integer/parseInt (or (second (re-find #"(\d?\d)[hH]" str)) "0"))
        minutes (Integer/parseInt (or (second (re-find #"(\d?\d)[mM]" str)) "0"))
        seconds (Integer/parseInt (or (second (re-find #"(\d?\d)[sS]" str)) "0"))
        total-seconds (reduce + [(* hours 3600) (* minutes 60) seconds])]
    {:hours hours :minutes minutes :seconds seconds :total-seconds total-seconds}))

(defn parse-total-seconds-to-human-readable [total-seconds]
  (let [hour   (* 1000 60 60)
        minute (* 1000 60)
        second 1000
        h      (int (quot total-seconds hour))
        mh     (mod  total-seconds hour)
        m      (int (quot mh minute))
        mm     (mod  mh minute)
        s      (int (quot mm second))]
    (cond
      (> h 0) (format "%02d:%02d:%02d" h m s)
      (> m 0) (format "%02d:%02d" m s)
      :else (format "%02d" s))))

(defn format-timer [str]
  (let [{hours :hours minutes :minutes seconds :seconds} (parse-human-readable-timer str)]
    (cond
      (> hours 0) (format "%02d:%02d:%02d" hours minutes seconds)
      (> minutes 0) (format "%02d:%02d" minutes seconds)
      :else (format "%02d" seconds))))

(defn get-timer-duration [str]
  (let [{total-seconds :total-seconds} (parse-human-readable-timer str)]
    (* total-seconds 1000)))

(defn refresh-start-stop-timer! [e t container]
  (if (= (:state @t) :active)
    (seesaw/config! (seesaw/select container [:#start-stop-btn]) :text "\u229E" :tip "stop")
    (seesaw/config! (seesaw/select container [:#start-stop-btn]) :text "\u25B6" :tip "start")))

(defn start-stop-timer! [e t container]
  (if (= (:state @t) :active)
    (timer-lib/stop! t)
    (timer-lib/start! t))
  (refresh-start-stop-timer! e t container))

(defn timer-input-handler [e t container]
  (let [enter-key-code 10
        current-key-code (.getKeyCode e)]
    (println "current-key-code" current-key-code)
    (when (= current-key-code enter-key-code)
      (let [timer-text (seesaw/select container [:#timer-text])
            raw-str (seesaw/config timer-text :text)
            formatted-time (format-timer raw-str)
            total-seconds (get-timer-duration raw-str)]
        (swap! t assoc :total-duration total-seconds)
        (seesaw/config! timer-text :text formatted-time)))))

(defn refresh-timer! [container state]
  (let [new-hr-time (parse-total-seconds-to-human-readable (- (:total-duration state) (:elapsed-time state)))]
    (seesaw/config! (seesaw/select container [:#timer-text]) :text new-hr-time)))

(defn create-timer [timer-cfg]
  (let [t (timer-lib/create-timer (:name timer-cfg) (get-timer-duration (:init timer-cfg)))
        start-stop-btn (seesaw/button :id :start-stop-btn :text "\u25B6" :tip "start")
        reset-btn (seesaw/button :text "\u27F3" :tip "reset")
        input-text (seesaw/text :id :timer-text :font (seesaw.font/font :size 24) :text (format-timer (:init timer-cfg)) :enabled? (:custom timer-cfg false))
        panel (seesaw/vertical-panel
               :items [(seesaw/horizontal-panel :items [start-stop-btn reset-btn])
                       input-text])]
    (seesaw/listen start-stop-btn :action (fn [e] (start-stop-timer! e t panel)))
    (seesaw/listen input-text :key-pressed (fn [e] (timer-input-handler e t panel)))
    (seesaw/listen reset-btn :action (fn [e]
                                       (do
                                         (timer-lib/reset-timer! t)
                                         (refresh-start-stop-timer! e t panel))))
    (add-watch t :refresh (fn [k r o n] (refresh-timer! panel n)))
    panel))

(defn create-sections []
  (map
   #(seesaw/grid-panel :border (:name %) :items [(create-timer %)])
   (:timers config)))

(defn start-gui []
  (seesaw/invoke-later
   (-> (seesaw/frame
        :title "Power Timer"
        :content (seesaw/grid-panel :items (create-sections))
        :size [(-> config :gui :width) :by (-> config :gui :height)]
        :on-close :exit)
       seesaw/show!)))

(defn -main [& args]
  (start-gui))
