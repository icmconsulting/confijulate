(ns confijulate.core-expectations
	(:use expectations
				[confijulate core env test-namespace])
	(:require [me.raynes.fs :as fs]))


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
 (let [base-config (with-meta {:item 1} {:cfj-base true})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.test-namespace/base base-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :item)))))

;; ns with base and cfj-env map, but cfj-env map not selected should only return base values
(expect
 1
 (let [base-config (with-meta {:item 1} {:cfj-base true})
			 env-config (with-meta {:item 2} {:cfj-env "other"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-env (constantly nil)
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other env-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :item)))))

;; ns with base and cfj-env map, but with cfj-env map selected
(expect
 2
 (let [base-config (with-meta {:item 1} {:cfj-base true})
			 env-config (with-meta {:item 2} {:cfj-env "other"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-env (constantly "other")
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other env-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :item)))))

;; ns with base and multiple cfj-env map, but with second cfj-env map selected
(expect
 3
 (let [base-config (with-meta {:item 1} {:cfj-base true})
			 other-config (with-meta {:item 2} {:cfj-env "other"})
			 second-other-config (with-meta {:item 3} {:cfj-env "second"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-env (constantly "second")
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other other-config
															confijulate.test-namespace/second-other second-other-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :item)))))

;; Value not found in any map, returns nil
(expect
 nil?
 (let [base-config (with-meta {:item 1} {:cfj-base true})
			 env-config (with-meta {:item 2} {:cfj-env "other"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-env (constantly "other")
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other env-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :not-exist)))))

;; Specified cfj-env does not exist, throws exception
(expect
 RuntimeException
 (let [base-config (with-meta {:item 1} {:cfj-base true})
			 env-config (with-meta {:item 2} {:cfj-env "other"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-env (constantly "not-exist")
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other env-config]
									(init-ns 'confijulate.test-namespace)))))

;; Specifying a non existant file throws exception
(expect
 RuntimeException
 (let [base-config (with-meta {:item 1} {:cfj-base true})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-file (constantly "this-file-doesnt-exist.clj")
															confijulate.test-namespace/base base-config]
									(init-ns 'confijulate.test-namespace)))))

;; Specifying an empty config file throws exception
(expect
 RuntimeException
 (let [base-config (with-meta {:item 1} {:cfj-base true})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/empty_config_file.clj"))
															confijulate.test-namespace/base base-config]
									(init-ns 'confijulate.test-namespace)))))

;; If the file contents are not a map, should throw exception
(expect
 RuntimeException
 (let [base-config (with-meta {:item 1} {:cfj-base true})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/not_a_map_config_file.clj"))
															confijulate.test-namespace/base base-config]
									(init-ns 'confijulate.test-namespace)))))


(given [config-key config-value]
	(expect
	 config-value
	 (let [base-config (with-meta {:item 1} {:cfj-base true})]
		 (redef-state [confijulate.core]
									(with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/non_overriding_config_file.clj"))
																confijulate.test-namespace/base base-config]
										(init-ns 'confijulate.test-namespace)
										(get-cfg config-key)))))
	:other-value 2
	:item 1)


(expect
	 1000
	 (let [base-config (with-meta {:item 1} {:cfj-base true})]
		 (redef-state [confijulate.core]
									(with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/overriding_config_file.clj"))
																confijulate.test-namespace/base base-config]
										(init-ns 'confijulate.test-namespace)
										(get-cfg :item)))))


;; When a map value is requested...

;; Given there is only a base config
;; Should only return the map value in the base map
(expect
 {:item 1}
 (let [base-config (with-meta {:config-map {:item 1}} {:cfj-base true})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.test-namespace/base base-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :config-map)))))

;; Given there is a base map and other overridding map
;; Should return matching merged map
(expect
 {:item 2 :second-item 3 :other-item 2}
 (let [base-config (with-meta {:config-map {:item 1 :second-item 3}} {:cfj-base true})
			 env-config (with-meta {:config-map {:item 2 :other-item 2}} {:cfj-env "other"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-env (constantly "other")
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other env-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :config-map)))))

(expect
 {:item 3 :second-item 3 :other-item 3 :new-value "A"}
 (let [base-config (with-meta {:config-map {:item 1 :second-item 3}} {:cfj-base true})
			 env-config (with-meta {:config-map {:item 2 :other-item 2}} {:cfj-env "other"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/overriding_config_file.clj"))
															confijulate.env/cfj-env (constantly "other")
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other env-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :config-map)))))

(expect
 {:merged-map {:item 1 :sub-map {:item 2}}}
 (let [base-config (with-meta {:sub-map-merge {:merged-map {:item 1 :sub-map {:item 0}}}} {:cfj-base true})
			 env-config (with-meta {:sub-map-merge {:merged-map {:sub-map {:item 1}}}} {:cfj-env "other"})]
	 (redef-state [confijulate.core]
								(with-redefs [confijulate.env/cfj-file (constantly (fs/absolute-path "test/overriding_config_file.clj"))
															confijulate.env/cfj-env (constantly "other")
															confijulate.test-namespace/base base-config
															confijulate.test-namespace/other env-config]
									(init-ns 'confijulate.test-namespace)
									(get-cfg :sub-map-merge)))))
