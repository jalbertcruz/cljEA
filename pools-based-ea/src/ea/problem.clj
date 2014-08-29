(ns
  ea.problem
  (:gen-class))

(require '(ea [evaluator :as evaluator]
              [reproducer :as reproducer]
              )
         )

(defprotocol Problem

  (fitnessFunction [self])
  (qualityFitnessFunction [self v])

  (genIndividual [self])
  (getPop [self])

  (runSeqCEvals [self])

  )

(def any-problem
  { :genIndividual (fn [self]
                     (for [_ (range (:ChromosomeSize (.config self)))] (rand-int 2))
                     )

    :getPop (fn [self]
              (for [_ (range (:PopSize (.config self)))] (genIndividual self))
              )

    :runSeqCEvals (fn [self]
                    (let [config (merge (.config self) {:ff (fitnessFunction self) :qf (fn [_] false) :df (fn [_])})]
                      (loop [p2Eval (getPop self)]
                        (let [indEvals (evaluator/evaluate :config config :p2Eval p2Eval)
                              ordIndEvals (sort-by #(nth % 1) > indEvals)]
                          (if (< (swap! (.Evaluations self) #(+ % (count indEvals))) (:Evaluations (.config self)))
                            (recur (reproducer/reproduce :config config :iEvals ordIndEvals))
                            (first ordIndEvals)
                            )
                          )
                        )
                      )
                    )

    })


(defrecord MaxOne[config Emigrations Evaluations])

(defn create-maxOneProblem [conf]
  (MaxOne. conf (atom 0) (atom 0))
  )

(extend MaxOne
  Problem  (assoc any-problem
             :fitnessFunction (fn[self]
                                #(count (for [a % :when (= a 1)] a))
                                )

             :qualityFitnessFunction (fn[self]
                                       #(> % (- (:ChromosomeSize (.config self)) 2))
                                       )

             )
  )