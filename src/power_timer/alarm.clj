(ns power-timer.alarm
  (:require [clj-audio.core :as audio]
            [me.raynes.fs :as fs]))

(defrecord Alarm [name sound])

(defn play [alarm]
  (future (audio/play (audio/->stream (:sound @alarm)))))

(defn init [name sound]
  (let [path (str fs/*cwd* "/resources/alarm_sounds/")
        sound (str path sound ".wav")]
    (atom (->Alarm name sound))))
