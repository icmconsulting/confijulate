(ns confijulate.core-expectations
	(:use expectations
				confijulate.core))


;; (defn in-context
;; 	"rebind a var, expecations are run in the defined context"
;; 	{:expectations-options :in-context}
;; 	[work]
;; 	(do
;; 		(create-ns 'confijulate.test-ns)
;; 		(work)
;; 		(remove-ns 'confijulate.test-ns)))

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
	 (with-redefs [confijulate.test-namespace/base base-config]
		 (init-ns 'confijulate.test-namespace)
		 (get-cfg :item))))


