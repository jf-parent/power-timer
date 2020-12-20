(ns power-timer.timer
  (:require [clojure.core.async :as a]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time-core]
            [power-timer.alarm :as alarm]))

(defrecord Timer [id name state total-duration started-at elapsed-time last-check chan terminated on-termination])

(defn get-uuid []
  (java.util.UUID/randomUUID))

(defn create-timer [name total-duration]
  (atom (->Timer (get-uuid) name :inactive total-duration
            0 0 0 (a/chan) false nil)))

(defn reset-timer! [timer]
  (swap! timer assoc
         :state :inactive
         :started-at 0
         :elapsed-time 0
         :last-check 0
         :terminated false))

;; (alarm/create name)
(def timer (create-timer "test" 10000))

(defn tick! [timer]
  (when (= (:state @timer) :active)
    (let [{last-check :last-check started-at :started-at total-duration :total-duration} @timer
          now (time-coerce/to-long (time-core/now))
          elapsed-time  (+ last-check (- now started-at))]
      (println (:name @timer) elapsed-time)
      (if (< elapsed-time total-duration)
        (swap! timer assoc :elapsed-time elapsed-time)
        (do
          ;; start alarm
          (swap! timer assoc :state :inactive :terminated true))))))

(defn start! [timer]
  (when (= (:state @timer) :inactive)
    (swap! timer assoc
           :state :active
           :last-check (:elapsed-time @timer)
           :started-at (time-coerce/to-long (time-core/now)))
    (a/go-loop []
      (let [[msg _] (a/alts! [(:chan @timer) (a/timeout 100)])]
        (when-not msg
          (tick! timer)
          (recur))))))

(defn stop! [timer]
  (when (= (:state @timer) :active)
    (a/>!! (:chan @timer) :stop)
    (swap! timer assoc :state :inactive)))
