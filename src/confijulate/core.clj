(ns confijulate.core
	(:use confijulate.env
				[clojure.tools.logging :only [error warn info debug]])
	(:require [clojure.tools.reader.edn :as edn]
						[me.raynes.fs :as fs]))

(def config-heirachy (atom []))

(defn- base-map-var-from-ns
	[config-ns]
	(->>
	 (ns-interns config-ns)
	 (map val)
	 (filter #(-> (meta %) :cfj-base))
	 first))

(defn- env-map-var-from-ns
	[config-ns env]
	(->>
	 (ns-interns config-ns)
	 (map val)
	 (filter #(-> (meta %) :cfj-env (= env)))
	 first))

(defn- parse-selected-config-file
	[selected-file]
	(if-not (fs/exists? selected-file)
		(throw (ex-info (str "Cannot find selected configuration file at path " selected-file) {:file selected-file}))
		(let [config-map (edn/read-string (slurp selected-file))]
			(cond
			 (nil? config-map) (throw (ex-info (str "No configuration map can be found in file at path " selected-file) {:file selected-file}))
			 (not (map? config-map)) (throw (ex-info (str "File at path " selected-file " does not contain a map") {:file selected-file}))
			 :else config-map))))

(defn- safely-assoc?
	([m ks] (safely-assoc? m (butlast ks) 1 []))
	([m ks key-num maps?]
	 (if (> key-num (count ks))
		 (or (not (seq maps?)) (every? true? maps?))
		 (let [test-val (get-in m (take key-num ks))]
			 (safely-assoc? m ks (inc key-num) (conj maps? (or (nil? test-val) (map? test-val))))))))


(defn- ext-value-map
	[ext-values]
	(let [ext-keys (map key ext-values)]
		(reduce
		 (fn [m k]
			 (let [key-coords (map keyword (clojure.string/split (key k) #"\."))]
				 (if (safely-assoc? m key-coords)
					 (assoc-in m key-coords (val k))
					 (throw
						(ex-info (format "Extension value with key %s cannot be used because it overrides another value at a higher level key" (key k))
										 {:key-coords key-coords})))))
		 {} ext-values)))

(defn- merge-config-maps
	[config-maps]
	(let [merged-map (apply merge config-maps)
				maps-to-merge (map key (filter #(map? (val %)) merged-map))
				merged-maps (zipmap maps-to-merge (map #(merge-config-maps (map % config-maps)) maps-to-merge))]
		(merge merged-map merged-maps)))

(defn- log-config-init
	[selected-env ext-values-map external-file]
	(do
		(info (clojure.string/join (repeat 50 "*")))
		(info "Confijulate configuration successfully initialised")
		(info (if selected-env (format "Configuration environment: %s" selected-env) "NO cfj-env selected; using BASE"))
		(when external-file (info (format "External file: %s" external-file)))
		(when (seq ext-values-map) (info (format "Extension values: %s" (print-str ext-values-map))))
		(info (clojure.string/join (repeat 50 "*")))))

(defn- log-resolved-config-map
	[config-maps]
	(let [effective-config (merge-config-maps (reverse config-maps))]
		(do
			(debug "Confijulate - effective configuration values...")
			(debug (with-out-str (clojure.pprint/pprint effective-config))))))

(defn- init-ns*
	[config-ns]
	(do
		(swap! config-heirachy (constantly []))
		(let [base-map (base-map-var-from-ns config-ns)
					selected-file (cfj-file)
					file-map (if selected-file (parse-selected-config-file selected-file) {})
					ext-values-map (ext-value-map (cfj-extension-values))
					selected-env (cfj-env)
					env-map (when selected-env (env-map-var-from-ns config-ns selected-env))]
			(cond
			 (nil? base-map) (throw (ex-info "Cannot find base config map in namespace" {:ns config-ns}))
			 (and selected-env (not env-map)) (throw (ex-info (format "Cannot find env config %s in namespace" selected-env) {:ns config-ns}))
			 :else (do
							 (log-config-init selected-env ext-values-map selected-file)
							 (swap! config-heirachy conj ext-values-map file-map (if env-map (var-get env-map) {}) (var-get base-map))
							 (log-resolved-config-map @config-heirachy))))))

(defmulti init-ns
	"Initialise an application configuration, using interns defined within the given namespace.
	Looks for maps within the ns with the following metadata:
	- :cfj-base true => the base (default) configuration map. Generally used for local dev environments.
	- :cfj-env \"someValue\" => configuration for the given environment. Will be activated when command line argument cfj-region=someValue

	Additionally, looks for the following system properties:
	- cfj-file => loads map from the file at the given path. The values in this map will take precedence before values in the ns maps

	Returns the seq of config maps in precedence order.

	You shouldn't need to call this function directly, if you mark your configuration namespace with the cfj-config metadata flag.
	Some reasons that you MIGHT need to call this include:
	- There are more than 1 namespace in your application with a cfj-config namespace
	- The parameters that set up the configuration have changed (e.g. via System/setProperty)
	- For some reason, you don't want to use the cfj-config metadata"
	type)

(defmethod init-ns clojure.lang.Namespace [ns]
	(init-ns* ns))

(defmethod init-ns java.lang.String [ns-name]
	(if-let [found-ns (find-ns ns-name)]
		(init-ns* found-ns)
		(ex-info "Cannot find namespace" {:ns ns-name})))

(defn- find-and-init-config-ns
	[]
	(debug "Confijulate: configuration not initialised. Searching for configuration namespace.")

	(let [config-namespaces (filter #(:cfj-config (meta %)) (all-ns))]
		(cond
		 (< 1 (count config-namespaces)) (throw (ex-info "More than 1 possible configuration namespaces found. You may need to use init-ns" {:namespaces config-namespaces}))
		 (not (seq config-namespaces)) (throw (ex-info "No configuration namespaces found. Make sure you add the :cfj-config metadata to your namespace" {:namespaces config-namespaces}))
		 :else (init-ns* (first config-namespaces)))))

(defn get-cfg
	"Get the first value that matches the given map path defined in kws.
	Searches the config heirachy in the following order:
	1. Ext system property values
	2. External file, specified via cfj-file system property
	3. Specified env config, if any
	4. Base config
	If no value can be found in any config map via the given path, returns nil.
	Note: if the configuration has not yet been initialised, will search for a configuration
	namespace that shares a common base namespace with the current namespace...
	"
	[& kws]

	(when-not (seq @config-heirachy) (find-and-init-config-ns))

	(if-let [cfg-val (some #(get-in % kws) @config-heirachy)]
		(if (map? cfg-val)
			(merge-config-maps (reverse (map #(get-in % kws) @config-heirachy)))
			cfg-val)))
