(ns confijulate.core
	(:use confijulate.env))

(def config-heirachy (atom []))

(defn- base-map-from-ns
	[config-ns]
	(->>
	 (ns-interns config-ns)
	 (map #(-> (val %) var-get))
	 (filter #(-> (meta %) :cfj-base))
	 first))

(defn- env-map-from-ns
	[config-ns env]
	(->>
	 (ns-interns config-ns)
	 (map #(-> (val %) var-get))
	 (filter #(-> (meta %) :cfj-env (= env)))
	 first))


(defn init-ns
	"Initialise an application configuration, using interns defined within the given namespace.
	Looks for maps within the ns with the following metadata:
		- :cfj-base true => the base (default) configuration map. Generally used for local dev environments.
		- :cfj-env \"someValue\" => configuration for the given environment. Will be activated when command line argument cfj-region=someValue
	Returns the original configuration namespace.
	"
	[config-ns]
	(if-let [config-ns (find-ns config-ns)]
		(let [base-map (base-map-from-ns config-ns)
					selected-env (cfj-env)
					env-map (when selected-env (env-map-from-ns config-ns selected-env))]
			(cond
			 (nil? base-map) (ex-info "Cannot find base config map in namespace" {:ns config-ns})
			 (and selected-env (not env-map)) (ex-info (format "Cannot find env config %s in namespace" selected-env) {:ns config-ns})
			 :else (swap! config-heirachy conj (if env-map env-map {}) base-map)))

		(ex-info "Cannot find namespace" {:ns config-ns})))


(defn get-cfg
	"Get the first value that matches the given map path defined in kws.
	Searches the config heirachy in the following order:
	1. Specified env config, if any
	2. Base config
	If no value can be found in any config map via the given path, returns nil.
	"
	[& kws]
	(some #(get-in % kws) @config-heirachy))
