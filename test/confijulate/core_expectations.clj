(ns confijulate.core-expectations
	(:use expectations
				[confijulate core env])
	(:require [me.raynes.fs :as fs]))

(defn in-context
	"rebind a var, expecations are run in the defined context"
	{:expectations-options :in-context}
	[work]
	(create-ns 'confijulate.test-namespace)
	(redef-state [confijulate.core]
							 (work))
	(remove-ns 'confijulate.test-namespace))


;; namespace doesn't exist throws exception
(expect
 RuntimeException
 (init-ns 'non-existant.namespace))

;; no base configuration in namespace throws exception
(expect
 RuntimeException
 (init-ns 'confijulate.core-expectations))

;; ns with base-config only
(expect
 1
 (let [base-config {:item 1}]
	 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
	 (init-ns 'confijulate.test-namespace)
	 (get-cfg :item)))

;; ns with base and cfj-env map, but cfj-env map not selected should only return base values
(expect
 1
 (let [base-config {:item 1}
			 env-config {:item 2}]
	 (with-redefs [confijulate.env/cfj-env (constantly nil)]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)

		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :item))))

;; ns with base and cfj-env map, but with cfj-env map selected
(expect
 2
 (let [base-config {:item 1}
			 env-config {:item 2}]
	 (with-redefs [confijulate.env/cfj-env (constantly "test-env")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)

		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :item))))

;; ns with base and multiple cfj-env map, but with second cfj-env map selected
(expect
 3
 (let [base-config {:item 1}
			 env-config {:item 2}
			 other-env-config {:item 3}]

	 (with-redefs [confijulate.env/cfj-env (constantly "other-env")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)
		 (intern 'confijulate.test-namespace (with-meta 'other {:cfj-env "other-env"}) other-env-config)

		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :item))))

;; Value not found in any map, returns nil
(expect
 nil?
 (let [base-config {:item 1}
			 env-config {:item 2}]

	 (with-redefs [confijulate.env/cfj-env (constantly "test-env")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)

		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :not-exist))))


;; Specified cfj-env does not exist, throws exception
(expect
 RuntimeException
 (let [base-config {:item 1}
			 env-config {:item 2}]

	 (with-redefs [confijulate.env/cfj-env (constantly "not-exist")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)

		 (init-ns 'confijulate.test-namespace))))

;; Specifying a non existant file throws exception
(expect
 RuntimeException
 (let [base-config {:item 1}]

	 (with-redefs [confijulate.env/cfj-file (constantly "this-file-doesnt-exist.clj")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (init-ns 'confijulate.test-namespace))))

;; Specifying an empty config file throws exception
(expect
 RuntimeException
 (let [base-config {:item 1}]

	 (with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/empty_config_file.clj"))]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (init-ns 'confijulate.test-namespace))))

;; If the file contents are not a map, should throw exception
(expect
 RuntimeException
 (let [base-config {:item 1}]

	 (with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/not_a_map_config_file.clj"))]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (init-ns 'confijulate.test-namespace))))


(given [config-key config-value]
			 (expect
				config-value
				(let [base-config {:item 1}]

					(with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/non_overriding_config_file.clj"))]
						(intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
						(init-ns 'confijulate.test-namespace)
						(get-cfg config-key))))
			 :other-value 2
			 :item 1)


(expect
 1000
 (let [base-config {:item 1}]
	 (with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/overriding_config_file.clj"))]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :item))))


;; When a map value is requested...

;; Given there is only a base config
;; Should only return the map value in the base map
(expect
 {:item 1}
 (let [base-config {:config-map {:item 1}}]
	 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
	 (init-ns 'confijulate.test-namespace)
	 (get-cfg :config-map)))

;; Given there is a base map and other overridding map
;; Should return matching merged map
(expect
 {:item 2 :second-item 3 :other-item 2}
 (let [base-config {:config-map {:item 1 :second-item 3}}
			 env-config {:config-map {:item 2 :other-item 2}}]
	 (with-redefs [confijulate.env/cfj-env (constantly "test-env")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)
		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :config-map))))

(expect
 {:item 3 :second-item 3 :other-item 3 :new-value "A"}
 (let [base-config {:config-map {:item 1 :second-item 3}}
			 env-config {:config-map {:item 2 :other-item 2}}]

	 (with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/overriding_config_file.clj"))
								 confijulate.env/cfj-env (constantly "test-env")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)
		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :config-map))))

(expect
 {:merged-map {:item 1 :sub-map {:item 2}}}
 (let [base-config {:sub-map-merge {:merged-map {:item 1 :sub-map {:item 0}}}}
			 env-config {:sub-map-merge {:merged-map {:sub-map {:item 1}}}}]
	 (with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/overriding_config_file.clj"))
								 confijulate.env/cfj-env (constantly "test-env")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (intern 'confijulate.test-namespace (with-meta 'env {:cfj-env "test-env"}) env-config)
		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :sub-map-merge))))


;; When an extension value is specified via system property
;; Then when querying for that value, the overriding value should be returned
(expect
 "3"
 (let [base-config {:item 1}]

	 (with-redefs [confijulate.env/cfj-extension-values (constantly {"item" "3"})]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :item))))


;; If the configuration namespace isn't initialised, then should search for the namespace
(expect
 1
 (let [base-config {:item 1}]
	 	 (alter-meta! (find-ns 'confijulate.test-namespace) assoc :cfj-config true)
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (get-cfg :item)))

;; No config namespace specified
(expect
 RuntimeException
 (let [base-config {:item 1}]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (get-cfg :item)))

;; Multiple config namespaces specified
(expect
 RuntimeException
 (let [base-config {:item 1}]
 	 	 (alter-meta! (find-ns 'confijulate.expectations) assoc :cfj-config true)
 	 	 (alter-meta! (find-ns 'confijulate.test-namespace) assoc :cfj-config true)
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (get-cfg :item)))

;; convert string passed in on command line to int
(expect
 23
 (let [base-config {}]
   (with-redefs [confijulate.env/cfj-system-property-names (constantly '("cfj.item"))
                   confijulate.env/cfj-system-property-value (constantly "#s->i#23")]
		 (intern 'confijulate.test-namespace (with-meta 'base {:cfj-base true}) base-config)
		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :item))))
