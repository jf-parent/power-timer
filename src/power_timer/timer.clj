(ns power-timer.timer
  (:require [clojure.core.async :as a]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time-core]
            [power-timer.alarm :as alarm]))

(defrecord Timer [id name state total-duration started-at elapsed-time last-check chan alarm])

(defn get-uuid []
  (java.util.UUID/randomUUID))

(defn create-timer [name total-duration sound]
  (let [alarm (when sound (alarm/init name sound))]
    (atom (->Timer (get-uuid) name :inactive total-duration
                  0 0 0 (a/chan) alarm))))

(defn reset-timer! [timer]
  (swap! timer assoc
         :state :inactive
         :started-at 0
         :elapsed-time 0
         :last-check 0))

(defn tick! [timer]
  (when (= (:state @timer) :active)
    (let [{last-check :last-check started-at :started-at total-duration :total-duration} @timer
          now (time-coerce/to-long (time-core/now))
          elapsed-time  (+ last-check (- now started-at))]
      ;; (println (:name @timer) elapsed-time)
      (if (< elapsed-time total-duration)
        (do
          (swap! timer assoc :elapsed-time elapsed-time)
          true)
        (do
          (reset-timer! timer)
          (alarm/play (:alarm @timer))
          false)))))

(defn start! [timer]
  (when (= (:state @timer) :inactive)
    (swap! timer assoc
           :state :active
           :last-check (:elapsed-time @timer)
           :started-at (time-coerce/to-long (time-core/now)))
    (a/go-loop []
      (let [[msg _] (a/alts! [(:chan @timer) (a/timeout 100)])]
        (when-not msg
          (let [continue (tick! timer)]
            (when continue
              (recur))))))))

(defn stop! [timer]
  (when (= (:state @timer) :active)
    (a/>!! (:chan @timer) :stop)
    (swap! timer assoc :state :inactive)))
