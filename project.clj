(defproject confijulate "0.1.0-SNAPSHOT"
  :description "Simple clojure application configuration library"
  :url "https://github.com/bbbates/confijulate"
  :dependencies [[org.clojure/clojure "1.5.1"]
								 [me.raynes/fs "1.4.4"]
								 [org.clojure/tools.reader "0.8.1"]
                 [expectations "1.4.56" :scope "test"]]
  :plugins [[lein-expectations "0.0.7"]]
)
