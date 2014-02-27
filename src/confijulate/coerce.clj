(ns confijulate.coerce)

(defn s->i
  "Returns an Integer for the given String"
  [string-to-convert]
  (Integer/valueOf string-to-convert))

(defn s->b
  "Returns a boolean for the given String"
  [string-to-convert]
  (Boolean/valueOf string-to-convert))

(defn cfj-type-coerce
  "Parses the given String for the function name supplied in the prefix. String format is:
      #<optional namespace><function>#<string value>
  This will execute <function> with <string value> as a parameter.
  E.g.
      #s->i#123 will resolve to (confijulate.coerce/s->i \"123\")
      #another-namespace/my-function#abc will resolve to (another-namespace/my-function \"abc\")"
  [x]
  (let [x-parsed (clojure.string/split x #"#")
        x-symbol (symbol (second x-parsed))
        x-func (resolve x-symbol)
        x-coerce-func (ns-resolve 'confijulate.coerce x-symbol)]
    (if x-func
      (x-func (last x-parsed))
      (if x-coerce-func
        (x-coerce-func (last x-parsed))
        x))))
