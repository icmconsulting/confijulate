# confijulate

Simple clojure application configuration library.

# Install
In leiningen:

```
[confijulate "0.3.1"]
```

In Maven:

```
<dependency>
  <groupId>confijulate</groupId>
  <artifactId>confijulate</artifactId>
  <version>0.3.1</version>
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
EXTERNAL CONFIG MAP (e.g. -Dcfj-file=/var/app/config/test-config.clj)
	||
  \/
ENVIRONMENT CONFIG (e.g. -Dcfj-env=test)
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
	my-application.config)
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
;; If -Dcfj-env=test sent via command line, then "test" map is used => "00 15 11 * * ?"
;; If -Dcfj-env=prod sent via command line, then "prod" map is used. But prod does not
;; 	define a value for the above path, so the "base" default value is returned => "00 00 10 * * ?"
```

If the value being returned is a map, then the map (and any map values in the map) will be merged down the heirarchy.


The first time get-cfg is called, it will initialise a heirachy of configuration maps. However, your config namespace needs to be required at some stage in your ns graph, otherwise confijulate won't be able to find it.
The easiest way to get around this issue, is to "alias" the confijulate.core/get-cfg function from within your configuration namespace.

```clojure
(ns
	^:cfj-config
	my-application.config
	(:require [confijurate.core :as cfj]))

(def get-cfg cfj/get-cfg)
```

The advantage of this approach is that you contain the confijulate dependencies in your app to just your configuration namespace.
Wherever you need a config value, just call the aliased function in your config namespace.

#### Note on namespace metadata
There is a long oustanding bug in [Clojure](http://dev.clojure.org/jira/browse/CLJ-130) where the AOT compiler strips out ns metadata - this is evident when running lein commands such as "ring uberwar".

The workaround for this issue is to do what clojure.core does, and explicitly alter the ns metadata while constructing the ns.

```clojure
(ns
	^:cfj-config
	my-application.config)

;; SNIP

(alter-meta! (find-ns 'my-application.config) assoc :cfj-config true)
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

You can also override single values via system properties. For example, in the above map file, if you decided you needed
:some-other-value to be "A" instead, you would add this system property:

```
-Dcfj.my-subsystem.some-other-value=A
```

There are a few caveats when using this option.
1. The type of the values returned from the get-cfg function will default to be a String.
2. If you need it to be an Integer or a Boolean then prefix the values as follows:
```
	-Dcfj.my-boolean=#s->b#true -Dcfj.my-integer=#s->i#2
```
...this calls the function between the #'s (i.e. s->b and s->i) in the namespace confijulate.coerce to convert the Strings. Alternatively you can call your own function with the fully qualified namespace. E.g.
```
	-Dcfj.my-value=#my-namespace/my-function#xyz987
```

## Force initialisation

There might be a few reasons why you need to force the configuration to initialise itself. These include:

- There are more than 1 namespace in your application with cfj-config metadata
- The parameters that set up the configuration have changed (e.g. via System/setProperty)
- For some reason, you don't want to use the cfj-config metadata

To force initialisation, in your application bootstrap function, call:

```clojure
(confijulate.core/init-ns 'my.config-namespace)
```

Or from within the configuration namespace itself:

```clojure
(confijulate.core/init-ns *ns*)
```

The namespace referred to above doesn't need to have the cfj-config metadata attached.


## TODO

1. Expose as JMX bean - targetting this in version 2
2. Ring middleware, for binding environment maps to a request thread based on values in a http request


## License

Copyright © 2013 ICM Consulting Pty Ltd. http://www.icm-consulting.com.au

Distributed under the Eclipse Public License, the same as Clojure.
