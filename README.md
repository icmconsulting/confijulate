# confijulate

Simple clojure application configuration library.

NOTE: this is still very much a work in progress. This isn't available in clojars yet, but feel free to download the source and use until then.

## What?

Manage your environment specific constants using clojure constructs (maps). What every enterprise-y application needs!

A "confijulate" application configuration consists of:

- environment (region) specific configuration maps
- a Base configuration map
- optionally, an external configuration map - i.e. a file outside of the deployment artefact containing machine specific overrides, passed via command line arg
- optionally, individual value overrides, passed in via command line

When your application starts, a confijulate config hierachy is created.

```
VALUE OVERRIDES (e.g. -Dcfj:example-value="newValue")
	||
  \/
EXTERNAL CONFIG MAP (e.g. -Dcfj-file="/var/app/config/test-config.clj")
	||
  \/
ENVIRONMENT CONFIG
	||
  \/
BASE CONFIG
```

Values are retrieved via a clojure map "path" e.g.
```clojure
(confijulate.core/get-cfg :my-application :my-subsystem :value-name)
```

If the path queried for doesn't exist (falsey) in the top level of the confijulate heirachy, then get-cfg looks at the next level - and so forth until it gets to the base configuration.


## Show me

Create a configuration namespace for your application.
```clojure
(ns my-application.config)
```

Define a base configuration map in a namespace somewhere. The base configuration essentially defines your system's default setup.

```clojure
(def ^:cfj-base base
	{
		:jndi-ds "jdbc/MyLocalDatabase"
	 	:scheduling
			{
				:daily-email-job {:cron "00 00 10 * * ?"}
			}
	}
)
```

Define environment specific configuration maps, e.g. for QA region overrides.

```clojure
(def ^{cfj-env :test} test-config
	{
		:jndi-ds "jdbc/MyTestDatabase"
	 	:scheduling
			{
				:daily-email-job {:cron "00 15 11 * * ?"}
			}
	}
)

(def ^{cfj-env :prod} prod-config
	{
		:jndi-ds "jdbc/MyProdDatabase"
	}
)
```

In your application startup function, call the confijulate init-ns function, passing in a reference to your config namespace (where you define your environment and base maps).

```clojure
(confijulate.core/init-ns 'my-application.config)
```

For example, for a standard ring based web application, you would call the init function in the ring init function:

```clojure
:ring {:handler my-application.handler/app
         :init my-application.handler/init-hook}

;; ...meanwhile, in my-application.handler...

(defn init-hook
	[]
	(confijulate.core/init-ns 'my-application.config))
```

Then wherever your application needs environment specific values, call the confijulate get-cfg function.

```clojure
(def daily-email-schedule
	(confijulate.core/get-cfg :scheduling :daily :cron))

;; If no environment command line/system arg specified, then base is used => "00 00 10 * * ?"
;; If -Dcfj-region=test sent via command line, then "test" map is used => "00 15 11 * * ?"
;; If -Dcfj-region=prod sent via command line, then "prod" map is used. But prod does not
;; 	define a value for the above path, so the "base" default value is returned => "00 00 10 * * ?"
```


## Command line/System/Environment overrides
Values loaded from an external configuration map takes precedence above any values defined in your configuration
namespace.

To load an external configuration file, add the following command line argument to your jvm startup command:

```
-Dcfj-file=/path/to/file
```

It's expected that the file contains just a clojure map, for example:
```clojure
;; This is all that should be in the file
{
	:my-value 2
	:my-subsystem {
		:some-other-value 2
	}
}
```


## In unit/integration tests
TODO


## TODO

1. Expose as JMX bean - targetting this in version 2
2. Ring middleware, for binding environment maps to a request thread based on values in a http request


## License

Copyright © 2013 ICM Consulting http://www.icm-consulting.com.au

Distributed under the Eclipse Public License, the same as Clojure.
