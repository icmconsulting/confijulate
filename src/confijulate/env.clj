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
	"Returns a seq of all extension values specified via the command line"
	[]
	(let [all-property-names (-> (System/getProperties) .propertyNames enumeration-seq)]
		(->>
		 all-property-names
		 (filter #())

		 )
		;;TODO

		)

	)
