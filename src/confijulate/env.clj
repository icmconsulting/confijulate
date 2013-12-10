(ns confijulate.env)

(defn cfj-env
	"Returns the currently selected confijulate env, or nil if none selected"
	[]
	(System/getProperty "cfj-env"))

(defn cfj-file
	"Returns the path to the currently selected confijulate external config map file, or nil if none selected"
	[]
	(System/getProperty "cfj-file"))


