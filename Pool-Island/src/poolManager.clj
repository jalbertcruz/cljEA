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

(import 'java.util.Date)
(import 'sheduling.ShedulingUtility)

(require '[clojure.set])

(extend-type TPoolManager
  poolManager/PoolManager
  ;agent que comparte un conjunto de individuos con
  ;agentes evaluadores y reproductores
  (init [self conf]
    (swap! (.pmConf self) #(identity %2)
      (dissoc conf :population )
      )

    (swap! (.evals self) #(identity %2)
      (set (for [_ (range (:evaluatorsCount conf))]
             (agent (evaluator/create *agent* (.profiler self)) ;               :error-mode :continue
               :error-handler pea/evaluator-error)
             ))
      )

    (swap! (.reps self) #(identity %2)
      (set (for [_ (range (:reproducersCount conf))]
             (agent (reproducer/create *agent* (.profiler self)) ;               :error-mode :continue
               :error-handler pea/reproducer-error)
             ))
      )

    (swap! (.active self) #(identity %2) true)

    (let [
           population (:population conf)
           ]

      (swap! (.poolSize self) #(identity %2) (count population))

      (dosync
        (alter (.table self) #(identity %2)
          (zipmap population (for [_ population] [-1 1]))
          )
        )
      )
    self
    )

  (updatePool [self newPool]
    ;    (println "updatePool")
    (dosync
      ;      (alter (.table self) #(identity %2)
      ; (adjustPool @(.table self) newPool @(.poolSize self)))
      (alter (.table self) #(identity %2)
        newPool)
      )

    self
    )


  (add2Pool-Ind-Fit-State [self individuos]
    (dosync
      ;        (alter (.table self) #(identity %2)
      ; (adjustPool @(.table self) ndata @(.poolSize self)))
      (alter (.table self) #(identity %2)
        (into @(.table self) individuos))
      )

    self
    )

  (migrantsDestination [self dests]
    (swap! (.migrantsDestination self) #(identity %2) dests)
    self
    )

  (migration [self ParIndividuoFitness]
    (poolManager/add2Pool-Ind-Fit-State self [[(nth ParIndividuoFitness 0) [(nth ParIndividuoFitness 1) 2]]])
    self
    )
  ;
  ;  (setPoolsManager [self newManager]
  ;    (swap! (.manager self) #(identity %2) newManager)
  ;    self
  ;    )

  (evaluatorFinalized [self pid]
    (let [
           nEvals (swap! (.evals self) #(disj %1 %2) pid)
           ]
      (when (and
              (empty? @(.reps self))
              (empty? nEvals)
              )
        (send pid finalize/finalize)
        )
      )
    self
    )

  (reproducerFinalized [self pid]
    (let [
           nReps (swap! (.reps self) #(disj %1 %2) pid)
           ]
      (when (and
              (empty? nReps)
              (empty? @(.evals self))
              )
        (send pid finalize/finalize)
        )
      )
    self
    )

  (evolveDone [self pid]
    (if @(.active self)
      (do
        (when (rand-nth [true false])
          (send pid reproducer/emigrateBest (rand-nth @(.migrantsDestination self)))
          )
        (send pid reproducer/evolve (:reproducersCapacity @(.pmConf self)))
        )
      (send pid finalize/finalize)
      )
    self
    )

  (evalDone [self pid]
    (if @(.active self)
      (do
        (send (.manager self) manager/evalDone *agent*)
        (send pid evaluator/evaluate (:evaluatorsCapacity @(.pmConf self)))
        )
      (send pid finalize/finalize)
      )
    self
    )

  (sReps [self]
    (doseq [e @(.reps self)]
      (send e reproducer/evolve (:reproducersCapacity @(.pmConf self)))
      )
    self
    )

  (sEvals [self]
    (doseq [e @(.evals self)]
      (send e evaluator/evaluate (:evaluatorsCapacity @(.pmConf self)))
      )
    self
    )

  (solutionReachedbyPool [self]
    (when @(.active self)
      ;      (doseq [e (into @(.reps self) @(.evals self))]
      ;        (send e finalize/finalize)
      ;        )
      (swap! (.active self) #(identity %2) false)
      )
    self
    )

  (solutionReachedbyEvaluator [self solution pid]
    ;        (println "solutionReachedbyEvaluator" solution)
    (when @(.active self)
      ;      (send (.manager self) manager/endEvol (.getTime (Date.)))
      (send (.manager self) manager/solutionReached *agent*)
      (swap! (.active self) #(identity %2) false)
      )
    self
    )

  (evalEmpthyPool [self pid]
    ;    (println "esperando 50 msegs en evalEmpthyPool")
    (let [
           f (fn []
               ;               (println "eval")
               (send pid evaluator/evaluate (:evaluatorsCapacity @(.pmConf self)))
               )
           ]

      (ShedulingUtility/send_after 100 f)
      )
    self
    )

  (repEmpthyPool [self pid]

    (let [
           f (fn []
               ;               (println "repr")
               (send pid reproducer/evolve (:reproducersCapacity @(.pmConf self)))
               )
           ]

      (ShedulingUtility/send_after 50 f)
      )
    self
    )

  finalize/Finalize
  (finalize [self]
    (send (.manager self) manager/poolManagerEnd *agent*)
    self
    )

  )