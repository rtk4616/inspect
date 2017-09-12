(ns inspect.main.core
  (:require [inspect.main.log]
            [taoensso.timbre :as timbre :refer-macros [info]]
            [inspect.main.menu :as menu]
            [inspect.main.update :as upd]
            [inspect.main.kafka :as kafka]
            [inspect.main.store :as st]
            [inspect.main.startup :as startup]
            [inspect.main.window-manager :as wm]
            [inspect.main.update-window :as um]
            [electron :refer [app]]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]))

(defonce switchboard (sb/component :electron/switchboard))

(defn start []
  (info "Starting CORE:" (.-resourcesPath process))
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(wm/cmp-map :electron/window-manager
                                   #{:exec/js
                                     :import/listen
                                     :kafka/status
                                     :observer/cmps-msgs})
                       (kafka/cmp-map :electron/kafka-cmp)
                       (st/cmp-map :electron/store-cmp)
                       (startup/cmp-map :electron/startup-cmp)
                       (upd/cmp-map :electron/update-cmp)
                       (sched/cmp-map :electron/scheduler-cmp)
                       (um/cmp-map :electron/update-win-cmp)
                       (menu/cmp-map :electron/menu-cmp)}]

     [:cmd/route {:from :electron/menu-cmp
                  :to   #{:electron/window-manager
                          :electron/update-win-cmp
                          :electron/kafka-cmp
                          :electron/startup-cmp
                          :electron/update-cmp}}]

     [:cmd/route {:from #{:electron/update-win-cmp
                          :electron/scheduler-cmp}
                  :to   #{:electron/update-cmp
                          :electron/kafka-cmp}}]

     [:cmd/route {:from :electron/update-cmp
                  :to   #{:electron/update-win-cmp
                          :electron/scheduler-cmp}}]

     [:cmd/route {:from #{:electron/update-cmp
                          :electron/scheduler-cmp}
                  :to   :electron/kafka-cmp}]

     [:cmd/route {:from #{:electron/kafka-cmp
                          :electron/scheduler-cmp}
                  :to   #{:electron/store-cmp
                          :electron/window-manager}}]

     [:cmd/route {:from :electron/store-cmp
                  :to   #{:electron/window-manager}}]

     [:cmd/send {:to  :electron/kafka-cmp
                 :msg [:cmd/start]}]

     [:cmd/send {:to  :electron/window-manager
                 :msg [:window/new]}]

     [:cmd/send {:to  :electron/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 1000
                                          :message [:state/publish]
                                          :repeat  true}]}]

     [:cmd/send {:to  :electron/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout (* 24 60 60 1000)
                                          :message [:update/auto-check]
                                          :repeat  true
                                          :initial true}]}]]))

(.on app "ready" start)
