# confijulate

Simple clojure application configuration library.

# Install
In leiningen:

```
[confijulate "0.2.0-SNAPSHOT"]
```

In Maven:

```
<dependency>
  <groupId>confijulate</groupId>
  <artifactId>confijulate</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## What?

Manage your environment specific constants using clojure constructs (maps). What every enterprise-y application needs!

A "confijulate" application configuration consists of:

- environment (region) specific configuration maps
- a Base configuration map
- optionally, an external configuration map - i.e. a file outside of the deployment artefact containing machine specific overrides, passed via command line arg
- optionally, individual value overrides, passed in via command line

When your application starts, a confijulate config hierachy is created.

```
EXTENSION VALUE OVERRIDES (e.g. -Dcfj.example-value=newValue)
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
(ns
	^:cfj-config
	my-application.config
	(:use [confijurate.core :only [init-ns]]))
```

Define a base configuration map in the config namespace. The base configuration essentially defines your system's default setup.

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
(def ^{cfj-env "test"} test-config
	{
		:jndi-ds "jdbc/MyTestDatabase"
	 	:scheduling
			{
				:daily-email-job {:cron "00 15 11 * * ?"}
			}
	}
)

(def ^{cfj-env "prod"} prod-config
	{
		:jndi-ds "jdbc/MyProdDatabase"
	}
)
```

Then, wherever your application needs environment specific values, call the confijulate get-cfg function.

```clojure
(def daily-email-schedule
	(confijulate.core/get-cfg :scheduling :daily :cron))

;; If no environment command line/system arg specified, then base is used => "00 00 10 * * ?"
;; If -Dcfj-region=test sent via command line, then "test" map is used => "00 15 11 * * ?"
;; If -Dcfj-region=prod sent via command line, then "prod" map is used. But prod does not
;; 	define a value for the above path, so the "base" default value is returned => "00 00 10 * * ?"
```

If the value being returned is a map, then the map (and any map values in the map) will be merged down the heirarchy.


The first time get-cfg is called, it will initialise a heirachy of configuration maps.


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

You can also override single values via system properties. For example, in the above map file, if you decided you needed
:some-other-value to be "A" instead, you would add this system property:

```
-Dcfj.my-subsystem.some-other-value=A
```

There are a few caveats when using this option.
1. The type of the values returned from the get-cfg function will ALWAYS be a string.
2. If you doing something silly like...
```
	-Dcfj.item=1 -Dcfj.item.sub-value=2
```
...then, you're on your own - I cannot help you.

## Force initialisation

There might be a few reasons why you need to force the configuration to initialise itself. These include:

- There are more than 1 namespace in your application with cfj-config metadata
- The parameters that set up the configuration have changed (e.g. via System/setProperty)
- For some reason, you don't want to use the cfj-config metadata

To force initialisation, in your application bootstrap function, call:

```clojure
(confijulate.core/init-ns 'my.config-namespace)
```

The namespace referred to above doesn't need to have the cfj-config metadata attached.


## In unit/integration tests
TODO


## TODO

1. Expose as JMX bean - targetting this in version 2
2. Ring middleware, for binding environment maps to a request thread based on values in a http request


## License

Copyright © 2013 ICM Consulting Pty Ltd. http://www.icm-consulting.com.au

Distributed under the Eclipse Public License, the same as Clojure.
