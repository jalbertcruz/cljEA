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

(load-file "scripts/loaderFile.clj")

(ns sequential.experiment-run)

(use '[clojure.java.io :only (writer file)])

(import 'java.util.Date)

(def evaluations (atom 0))
(def solutionFound (atom false))

(defn bestSolution [pool]
  (let [
         all (for [[ind [fitness state]] pool
                   :when (= state 2)]
               [ind fitness]
               )
         ]
    (nth (reduce #(if (< (%1 1) (%2 1)) %2 %1) all) 1)
    )
  )

(defn runSeqEA [& {:keys [
                           initPool
                           ]}
                ]
  (swap! solutionFound #(identity %2) false)
  (loop [
          pool (atom initPool)
          evalDone 0
          bSolution [nil -1]
          ]
    (let [
           newEvalDone (atom 0)
           terminationCondition (fn []
                                  (or
                                    (>= (+ evalDone @newEvalDone) problem/evaluations)
                                    @solutionFound
                                    )
                                  )
           ;           evaluatorsCapacity (min (swap! evaluations #(- %1 %2) evalDone) problem/evaluatorsCapacity)
           evaluatorsCapacity (min (- problem/evaluations evalDone) problem/evaluatorsCapacity)
           current-best-sol (if (> evaluatorsCapacity 0)
                              (let [
                                     [resEval solFound nSels bs] (pea/evaluate
                                                                   :sels (take evaluatorsCapacity
                                                                           (for [[ind [_ state]] @pool
                                                                                 :when (= state 1)]
                                                                             ind
                                                                             )
                                                                           )
                                                                   )
                                     ]
                                (when resEval
                                  (let [pnSels (for [[i f] nSels] [i [f 2]])]
                                    (swap! pool #(into %1 %2) pnSels)
                                    (swap! newEvalDone #(identity %2) (count pnSels))
                                    (swap! solutionFound #(identity %2) solFound)
                                    )
                                  )
                                (if-not @solutionFound
                                  (if (> (bs 1) (bSolution 1)) bs bSolution)
                                  bs
                                  )
                                )
                              bSolution
                              )

           subpop (pea/extractSubpopulation
                    (for [[ind [fitness state]] @pool
                          :when (= state 2)]
                      [ind fitness]
                      )
                    problem/reproducersCapacity
                    )

           [res [noParents nInds bestParents]] (pea/evolve
                                                 :subpop subpop
                                                 :parentsCount (quot (count subpop) 2)
                                                 )

           ]

      (when res
        (swap! pool #(identity %2) (pea/mergeFunction
                                     @pool
                                     subpop noParents
                                     nInds bestParents (count @pool)
                                     )
          )
        )


      ;      (println (+ evalDone @newEvalDone))
      (if (terminationCondition)
        [(current-best-sol 1) (+ evalDone @newEvalDone)]
        (recur pool (+ evalDone @newEvalDone) current-best-sol)
        )

      )
    )
  )

;(try
;  (runSeqEA
;    :initPool (into {} (for [ind (problem/genInitPop problem/popSize problem/chromosomeSize)] [ind [-1 1]]))
;    )
;
;  (catch Exception a
;    (clojure.repl/pst a)
;    )
;  )

(defn testsRunSeqEA []
  (println (format "Doing experiment (time -> %2d)" (.getTime (Date.))))
  (let [
         initEvol (.getTime (Date.))
         res (runSeqEA
               :initPool (into {} (for [ind (problem/genInitPop problem/popSize problem/chromosomeSize)] [ind [-1 1]]))
               )
         ]
    (flatten [(- (.getTime (Date.)) initEvol) res])
    )
  )

(defn run []
  (let [nRes (for [_ (range problem/repetitions)] (testsRunSeqEA))]
    ;(doseq [n nRes]
    ;  (println n)
    ;  )

    (with-open [w (writer (file problem/seqOutputFilename))]
      (.write w "EvolutionDelay,Evaluations,BestSol\n")
      (doseq [[evolutionDelay bestSol evals] nRes]
        (.write w (format "%1d,%1d,%1d\n" evolutionDelay evals bestSol))
        )
      )

    (println "Ends!")
    )
  )

;(run)