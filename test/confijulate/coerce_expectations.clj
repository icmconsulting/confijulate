(ns confijulate.coerce-expectations
	(:use expectations
				[confijulate coerce]))

;; convert String to Integer
(expect
 231
 (s->i "231"))

;; convert String to Boolean
(expect
 true
 (s->b "true"))

(expect
 false
 (s->b "abc"))

(expect
 false
 (s->b "false"))

(expect
 false
 (s->b nil))

;; call function from String
(expect
 "132"
 (cfj-type-coerce "#clojure.string/reverse#231"))

;; call function from String, default to confijulate.coerce namespace if no namespace supplied
(expect
 231
 (cfj-type-coerce "#s->i#231"))

(expect
 true
 (cfj-type-coerce "#s->b#true"))

(expect
 "Blah"
 (cfj-type-coerce "Blah"))
