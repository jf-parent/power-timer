(defproject power-timer "0.1.0-SNAPSHOT"
  :description "Power Timer"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.3.610"]
                 [clj-time "0.15.2"]
                 [seesaw "1.5.0"]]
  :repl-options {:init-ns power-timer.core})
