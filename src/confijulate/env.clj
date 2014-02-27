(ns confijulate.env
  (:use confijulate.coerce))

(defn cfj-env
	"Returns the currently selected confijulate env, or nil if none selected"
	[]
	(System/getProperty "cfj-env"))

(defn cfj-file
	"Returns the path to the currently selected confijulate external config map file, or nil if none selected"
	[]
	(System/getProperty "cfj-file"))

(defn cfj-system-property-names
  "Returns system property names"
  []
  (-> (System/getProperties) .propertyNames enumeration-seq))

(defn cfj-system-property-value
  "Returns system property for a given string key"
  [system-property-name]
  (System/getProperty system-property-name))

(defn cfj-extension-values
	"Returns a map of all cfj extension values specified via the command line"
	[]
	(let [property-re #"^cfj\.(.+)"
        cfj-property-names (filter #(re-find property-re %) (cfj-system-property-names))]
		(zipmap
		 (map #(-> (re-find property-re %) last) cfj-property-names)
		 (map #(cfj-type-coerce (cfj-system-property-value %)) cfj-property-names))))
