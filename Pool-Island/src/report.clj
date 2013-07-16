;;
;; Author José Albert Cruz Almaguer <jalbertcruz@gmail.com>
;; Copyright 2013 by José Albert Cruz Almaguer.
;;
;; This program is licensed to you under the terms of version 3 of the
;; GNU Affero General Public License. This program is distributed WITHOUT
;; ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
;; MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
;; AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
;;

(ns pea)

(use '[clojure.java.io :only (writer file)])

(extend-type TReport
  report/Report

  (experimentEnd [self EvolutionDelay NEmig Conf NIslands NumberOfEvals]
    (println "experimentEnd")

    (let [
           nRes (swap! (.results self) #(conj %1 %2) [EvolutionDelay NEmig Conf NIslands NumberOfEvals])
           ]

      (if (= @(.numberOfExperiments self) 1)
        (do
          (println "all ends")
          ;        (with-open [w (writer (file "results.csv"))]
          ;          (.write w "EvolutionDelay,NumberOfEvals,Emigrations,EvaluatorsCount,ReproducersCount,IslandsCount\n")
          ;          (doseq [[EvolutionDelay1 NEmig1 Conf1 NIslands1 NumberOfEvals1] nRes]
          ;            (def ^:private Ec (:evaluatorsCount Conf1))
          ;            (def ^:private Rc (:reproducersCount Conf1))
          ;            (.write w (format "%1.6f,%2d,%3d,%4d,%5d,%6d \n" EvolutionDelay1 NumberOfEvals1 NEmig1 Ec Rc NIslands1))
          ;            )
          ;          )
          (ShedulingUtility/shutdown)
          )
        )

      )

    (swap! (.numberOfExperiments self) dec)
    self
    )

  (mkExperiment [self]
    (println "mkExperiment")
    (def ^:private insts @(.instances self))

    (if (not (empty? insts))
      (do
        (println (format "Doing experiment: %1s" ""))
        ((peek insts))
        (swap! (.instances self) #(identity %2) (pop insts))
;        (swap! (.numberOfExperiments self) dec)
        )
      )
    self
    )

  (session [self Funs]
    (swap! (.instances self) #(identity %2) Funs)
    (swap! (.numberOfExperiments self) #(identity %2) (count Funs))
    (send @(.pid self) report/mkExperiment)
    self
    )

  HasPid
  (setPid [self pid]
    (swap! (.pid self) #(identity %2) pid)
    self
    )
  )