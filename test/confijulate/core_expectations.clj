(ns confijulate.core-expectations
	(:use expectations
				[confijulate core env test-namespace]))


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
