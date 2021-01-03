(ns power-timer.core-test
  (:require [clojure.test :refer :all]
            [seesaw.core :as seesaw]
            [power-timer.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(testing "Timer Regex"
  (testing "Valid time w/ hour, minute and second"
    (let [valid-time "12h30m30s"]
      (is (= valid-time (first (re-matches timer-regex valid-time))))))
  (testing "Valid time w/ hour only"
    (let [valid-time "36h"]
      (is (= valid-time (first (re-matches timer-regex valid-time))))))
  (testing "Valid time w/ hour and second"
    (let [valid-time "1h36s"]
      (is (= valid-time (first (re-matches timer-regex valid-time))))))
  (testing "Invalid time with extra stuff"
    (let [invalid-time "12h3m4s4ms"]
      (is (= nil (re-matches timer-regex invalid-time)))))
  (testing "Invalid time without hour, minute and second"
    (let [invalid-time "12a3b4c"]
      (is (= nil (re-matches timer-regex invalid-time))))))

(testing "Unique Time Unit defn"
  (testing "Valid time w/ hour, minute and second"
    (let [valid-time "12h30m30s"]
      (is (true? (uniq-time-unit valid-time)))))
  (testing "Valid time w/ hour only"
    (let [valid-time "12h"]
      (is (true? (uniq-time-unit valid-time)))))
  (testing "Invalid time w/ hour present two time"
    (let [invalid-time "12h24H"]
      (is (false? (uniq-time-unit invalid-time))))))

(testing "List sounds return some sound"
  (is (>= (count (list-sounds)) 1)))

(testing "Sounds properly initialised"
  (is (>= (count sounds) 1)))

(testing "Validate config"
  (testing "Valid config"
    (let [valid-config {:timers [{:name "Test" :init "12h30m3s" :sound "railroad"}]}]
      (is (= valid-config (validate-config valid-config)))))
  (testing "Invalid config - wrong name"
    (let [invalid-config {:timers [{:name 1 :init "12m" :sound "railroad"}]}]
      (is (thrown-with-msg? Exception #"Config Error" (validate-config invalid-config)))))
  (testing "Invalid config - wrong init"
    (let [invalid-config {:timers [{:name "Test" :init "12v" :sound "railroad"}]}]
      (is (thrown-with-msg? Exception #"Config Error" (validate-config invalid-config)))))
  (testing "Invalid config - wrong sound"
    (let [invalid-config {:timers [{:name "Test" :init "12m" :sound "this-doesnt-exist"}]}]
      (is (thrown-with-msg? Exception #"Config Error" (validate-config invalid-config)))))
  (testing "Invalid config - invalid custom value"
    (let [invalid-config {:timers [{:name "Test" :init "12h" :sound "railroad" :custom "should be a boolean"}]}]
      (is (thrown-with-msg? Exception #"Config Error" (validate-config invalid-config))))))

(testing "Parse human readable timer"
  (testing "Hour/Minute/Second"
    (let [valid-input "12h12M12s"
          valid-output (parse-human-readable-timer valid-input)]
      (is (= (:hours valid-output) 12))
      (is (= (:minutes valid-output) 12))
      (is (= (:seconds valid-output) 12))
      (is (= (:total-seconds valid-output) 43932))))
  (testing "Minute only"
    (let [valid-input "12m"
          valid-output (parse-human-readable-timer valid-input)]
      (is (= (:hours valid-output) 0))
      (is (= (:minutes valid-output) 12))
      (is (= (:seconds valid-output) 0)))))

(testing "Parse total seconds to human readable"
  (testing "Hour, minute and seconds"
    (is (= "01:01:01" (parse-total-seconds-to-human-readable 3661000))))
  (testing "Minute and seconds"
    (is (= "01:01" (parse-total-seconds-to-human-readable 61000))))
  (testing "Seconds"
    (is (= "01" (parse-total-seconds-to-human-readable 1000)))))

(testing "Get Timer duration"
  (is (= 1000 (get-timer-duration "1s"))))

(testing "Refresh start stop timer"
  (testing "Start button -> Stop button"
    (let [container (create-timer {:name "Test" :init "12s"})
          timer (atom {:state :active})]
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :text) start-icon))
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :tip) "start"))
      (refresh-start-stop-timer! nil timer container)
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :text) stop-icon))
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :tip) "stop"))))
  (testing "Stop button -> Start button"
    (let [container (create-timer {:name "Test" :init "12s"})
          timer (atom {:state :inactive})]
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :text) stop-icon))
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :tip) "stop"))
      (refresh-start-stop-timer! nil timer container)
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :text) start-icon))
      (is (= (seesaw/config (seesaw/select container [:#start-stop-btn]) :tip) "start")))))

;; TODO start-stop-timer!
;; TODO timer-input-handler
;; TODO refresh-timer!
;; TODO create-timer
;; TODO create-sections
