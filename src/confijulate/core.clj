(ns confijulate.core)

(defn- base-map-from-ns
	[config-ns]
	(->>
	 (ns-interns config-ns)
	 (map #(-> (val %) var-get))
	 (filter #(-> (meta %) :cfj-base))
	 first))

(def ^:private config-heirachy (atom []))

(defn init-ns
	"Initialise an application configuration, using interns defined within the given namespace.
	Looks for maps within the ns with the following metadata:
		- :cfj-base true => the base (default) configuration map. Generally used for local dev environments.
		- :cfj-env \"someValue\" => configuration for the given environment. Will be activated when command line argument cfj-region=someValue
	Returns the original configuration namespace.
	"
	[config-ns]
	(if-let [config-ns (find-ns config-ns)]
		(if-let [base-map (base-map-from-ns config-ns)]
			(swap! config-heirachy conj base-map)
			(ex-info "Cannot find base config map in namespace" {:ns config-ns}))
		(ex-info "Cannot find namespace" {:ns config-ns})))


(defn get-cfg
	""
	[& kws]
	(some #(get-in % kws) @config-heirachy))
