(ns confijulate.env)

(defn cfj-env
	"Returns the currently selected confijulate env, or nil if none selected"
	[]
	(System/getProperty "cfj-env"))

(defn cfj-file
	"Returns the path to the currently selected confijulate external config map file, or nil if none selected"
	[]
	(System/getProperty "cfj-file"))

(defn cfj-extension-values
	"Returns a map of all cfj extension values specified via the command line"
	[]
	(let [all-property-names (-> (System/getProperties) .propertyNames enumeration-seq)
				property-re #"^cfj\.(.+)"
				cfj-property-names (filter #(re-find property-re %) all-property-names)]
		(zipmap
		 (map #(-> (re-find property-re %) last) cfj-property-names)
		 (map #(System/getProperty %) cfj-property-names))))
