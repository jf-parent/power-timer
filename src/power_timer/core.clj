(ns power-timer.core
  (:require [seesaw.core :as seesaw]
            [seesaw.dev :as sdev]
            [clojure.spec.alpha :as s]
            [me.raynes.fs :as fs]
            [power-timer.timer :as timer-lib]))


(def start-icon "开")
(def stop-icon "停")
(def reset-icon "重设")
(def timer-regex #"^([0-9]+[hHsSmM]+){0,3}$")

(defn list-sounds []
  (let [sounds-file (fs/list-dir (str fs/*cwd* "/resources/alarm_sounds/"))]
    (map #(first (clojure.string/split (last (clojure.string/split (str %) #"/")) #"\.")) sounds-file)))

(def sounds (set (list-sounds)))

(defn uniq-time-unit [str]
  (let [groups (group-by identity (clojure.string/lower-case str))]
    (and (<= (count (groups \h [])) 1)
         (<= (count (groups \m [])) 1)
         (<= (count (groups \s [])) 1))))

(defn validate-config [conf]
  (s/def ::name string?)
  (s/def ::init (s/and uniq-time-unit #(re-matches timer-regex %)))
  (s/def ::sound sounds)
  (s/def ::custom (s/or :nil nil? :boolean boolean?))
  (doseq [c (:timers conf)]
    (let [valid-name (s/valid? ::name (:name c))
          valid-init (s/valid? ::init (:init c))
          valid-sound (s/valid? ::sound (:sound c))
          valid-custom (s/valid? ::custom (:custom c))]
      (when-not valid-name
        (s/explain ::name (:name c)))
      (when-not valid-init
        (s/explain ::init (:init c)))
      (when-not valid-sound
        (s/explain ::sound (:sound c)))
      (when-not valid-custom
        (s/explain ::custom (:custom c)))
      (when-not (and valid-name valid-init valid-sound valid-custom)
        (throw (Exception. "Config Error!")))))
  conf)

(def config (validate-config (-> "./config.edn" slurp clojure.edn/read-string)))

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

(defn get-timer-duration [str]
  (let [{total-seconds :total-seconds} (parse-human-readable-timer str)]
    (* total-seconds 1000)))

(defn refresh-start-stop-timer! [e t container]
  (if (= (:state @t) :active)
    (seesaw/config! (seesaw/select container [:#start-stop-btn]) :text stop-icon :tip "stop")
    (seesaw/config! (seesaw/select container [:#start-stop-btn]) :text start-icon :tip "start")))

(defn start-stop-timer! [e t container]
  (if (= (:state @t) :active)
    (timer-lib/stop! t)
    (timer-lib/start! t))
  (refresh-start-stop-timer! e t container))

(defn timer-input-handler [e t container]
  (let [enter-key-code 10
        current-key-code (.getKeyCode e)]
    ;; (println "current-key-code" current-key-code)
    (when (= current-key-code enter-key-code)
      (let [timer-text (seesaw/select container [:#timer-text])
            raw-str (seesaw/config timer-text :text)
            total-seconds (get-timer-duration raw-str)
            formatted-time (parse-total-seconds-to-human-readable total-seconds)]
        (swap! t assoc :total-duration total-seconds)
        (seesaw/config! timer-text :text formatted-time)))))

(defn refresh-timer! [container state timer]
  (let [new-hr-time (parse-total-seconds-to-human-readable (- (:total-duration state) (:elapsed-time state)))]
    (seesaw/config! (seesaw/select container [:#timer-text]) :text new-hr-time)
    (refresh-start-stop-timer! nil timer container)))

(defn create-timer [timer-cfg]
  (let [t (timer-lib/create-timer (:name timer-cfg) (get-timer-duration (:init timer-cfg)) (:sound timer-cfg))
        start-stop-btn (seesaw/button :id :start-stop-btn :text start-icon :tip "start")
        reset-btn (seesaw/button :text reset-icon :tip "reset")
        input-text (seesaw/text :id :timer-text :font (seesaw.font/font :size 24) :text (parse-total-seconds-to-human-readable (get-timer-duration (:init timer-cfg))) :enabled? (:custom timer-cfg false))
        panel (seesaw/vertical-panel
               :items [(seesaw/horizontal-panel :items [start-stop-btn reset-btn])
                       input-text])]
    (seesaw/listen start-stop-btn :action (fn [e] (start-stop-timer! e t panel)))
    (seesaw/listen reset-btn :action (fn [e] (timer-lib/reset-timer! t)))
    (seesaw/listen input-text :key-pressed (fn [e] (timer-input-handler e t panel)))
    (add-watch t :refresh (fn [k r o n] (refresh-timer! panel n t)))
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
