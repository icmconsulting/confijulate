(defproject confijulate "0.4.1"
	:description "Simple clojure application configuration library"
	:url "https://github.com/bbbates/confijulate"
	:license {:name "Eclipse Public License - v 1.0"
						:url "http://www.eclipse.org/legal/epl-v10.html"
						:distribution :repo
						:comments "same as Clojure"}
	:dependencies [[org.clojure/clojure "1.5.1"]
								 [me.raynes/fs "1.4.4"]
								 [org.clojure/tools.reader "0.8.1"]
 								 [org.clojure/tools.logging "0.2.6"]
								 [expectations "1.4.56" :scope "test"]]
	:plugins [[lein-expectations "0.0.7"]]
	)
