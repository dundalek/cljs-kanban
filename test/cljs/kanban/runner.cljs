(ns kanban.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [kanban.core-test]))

(doo-tests 'kanban.core-test)
