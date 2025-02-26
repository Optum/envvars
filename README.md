<div id="top"></div>

# envvars

The **envvars** library is meant to provide the heavy-lifting for implementing your own environment
variable management system.

It is based on the philosophy of a separation of duties between "configuration producers" and "configuration consumers" with consideration and support for varying contexts and enforced constraints.

> Where your code runs is complicated. Your configuration shouldn't be.

## Motivating Use Case
Configuration typically varies.  It varies by environment, it varies by release, it varies by
platform type, it varies by lots of things.  Different people know how different parts vary.  What
would be nice is to have those different variation domains isolated and managed independently; then somehow blended together into a complete configuration.

The **envvars** library provides features to help you do that.

## Envvars Doesn't Actually Create Environment Variables
> The **envvars** library... *is a library*.

It doesn't do anything on its own right out of the box.  It is intended to be the environment variable kernel of a larger application configuration management system.

It is meant to be embedded in your CICD pipeline.  Are you creating property files? deployment configurations? config maps? distributed maps?  Well, these binding are all great, but **envvars** does not natively support any of them.

That's the work left for you to do.

## Library Module Design
The core **envvars** library module (envvars-lib) is only dependent on com.optum.templ:templ-lib.  This should make using envvars-lib easy to adopt into any project without dealing with \[too many\] transitive dependencies or security vulnerabilities.

But envvars-lib doesn't implement file processing/parsing.  For that you can choose JSON or YAML, and the transitive dependencies of Jackson or Snake respectively.  The most complete support is provided by the YAML library, as stand-alone **envvars** files are implemented in the YAML library.  But JSON is also supported, to a limited degree.

> JSON isn't perfect.  Neither is YAML.  But YAML has comments.

The envvars-cli includes sample code to kickstart your project.

## Notes About Types
The **envvars** library is untyped, or more specifically: all keys are considered Strings (not surprising) and also all values are considered Strings.

When using YAML with **envvars**, be careful and remember that YAML is typed.  YAML infers types in ways that may not be obvious.  When in doubt (or always to be safe) put your YAML values in quotes so YAML treats them as Strings!  On the plus side, **envvars** has no problem being passed multi-line Strings from YAML.  It may have dire and unexpected consequences when you export data *out* of **envvars**.  But the **envvars** engine couldn't care less.

## Key Features

### Producers and Consumers
A piece of configuration is typically produced by one group (a DBA says "The transaction records database in the performance environment is hosted on transrecdb.perf.ourcompany.com port 3306") and consumed by another group (an application developer says "I need the host and port of the transaction records database.")

Ideally we want a decoupling between the configuration producer and the consumer.

The philosophy and terminology in envvars for this decoupling is that "producers define" and "consumers inject."

>The "meeting in the middle" is effectively done with `environment variables`; not shell environment variables, but rather variables in the **envvars** engine that look and behave kind of like shell environment variables - so let's just call them environment variables and remember the "environment" they are in is the **envvars** engine, not the container runtime or operating system or shell.

So conceptually:
#### DBA Produces
```
define:
  HOST: "transrecdb.perf.ourcompany.com"
  PORT: "3306"
```
#### Developer Consumes
```
inject:
  - HOST
  - PORT
```

### Templates
A simple templating language is supported (com.optum.templ) that is tuned to typical environment variable composition use cases.

Suppose the developer wants to inject a URL instead of HOST and PORT?  Our configuration changes to this:

#### DBA Produces
```
define:
  HOST: "transrecdb.perf.ourcompany.com"
  PORT: "3306"
  URL: "jdbc:mysql://{{HOST}}:{{PORT}}/db"
```
#### Developer Consumes
```
inject:
  - URL
```

This becomes more valuable when you realize you can separate the "meta-definition" of URL (which is universal) from the "instance-definitions" of HOST and PORT (which varies by environment).

Consider this scenario where three environments are defined:

#### DBA Produces
```
environments:
  default:
    define:
      URL: "jdbc:mysql://{{HOST}}:{{PORT}}/db"
  development:
    define:
      HOST: "transrecdb.dev.ourcompany.com"
      PORT: "3306"
  performance:
    define:
      HOST: "transrecdb.perf.ourcompany.com"
      PORT: "3306"
  production:
    define:
      HOST: "transrecdb.ourcompany.com"
      PORT: "3306"
  
```
#### Developer Consumes
```
inject:
  - URL
```

Upon further consideration, we realize it shouldn't be the DBA who needs to understand that our application is using JDBC and needs our URL formatted in that way.  Let's refactor the URL meta-definition into the domain of the developer:

#### DBA Produces
```
environments:
  development:
    define:
      HOST: "transrecdb.dev.ourcompany.com"
      PORT: "3306"
  performance:
    define:
      HOST: "transrecdb.perf.ourcompany.com"
      PORT: "3306"
  production:
    define:
      HOST: "transrecdb.ourcompany.com"
      PORT: "3306"
  
```
#### Developer Consumes
```
environments:
  default:
    define:
      URL: "jdbc:mysql://{{HOST}}:{{PORT}}/db"

inject:
  - URL
```

Now what happens when the DBA decides to migrate our two non-prod databases onto the same host but different ports?  The DBA updates the defines, and the developers don't care:

#### DBA Produces
```
environments:
  development:
    define:
      HOST: "transrecdb.nonprod.ourcompany.com"
      PORT: "33060"
  performance:
    define:
      HOST: "transrecdb.nonprod.ourcompany.com"
      PORT: "33070"
  production:
    define:
      HOST: "transrecdb.ourcompany.com"
      PORT: "3306"
  
```
#### Developer Consumes
```
environments:
  default:
    define:
      URL: "jdbc:mysql://{{HOST}}:{{PORT}}/db"

inject:
  - URL
```

### Template handling of "missing" data.

The com.optum.templ library has a convenient feature for handling construction delimiters in the case of "missing" data.

The JDBC MySql driver will use port 3306 as default if it is not specified.  Our DBA wants to be efficient and stop specifying the port when it is the default port.

*But this breaks our URL template!*

Library templ to the rescue!

Specifying `{{:PORT}}` instead of `:{{PORT}}` will cause the `:` to only be used if the value of `PORT` is not blank.

So with these definitions:
#### DBA Produces
```
environments:
  development:
    define:
      HOST: "transrecdb.nonprod.ourcompany.com"
      PORT: "33060"
  performance:
    define:
      HOST: "transrecdb.nonprod.ourcompany.com"
      PORT: "33070"
  production:
    define:
      HOST: "transrecdb.ourcompany.com"
  
```
#### Developer Consumes
```
environments:
  default:
    define:
      BADURL: "jdbc:mysql://{{HOST}}:{{PORT}}/db"
      GOODURL: "jdbc:mysql://{{HOST}}{{:PORT}}/db"
      
inject:
  - BADURL
  - GOODURL
```

The values of `BADURL` and `GOODURL` in development and performance will be the same.
But in production:
```
BADURL=jdbc:mysql://transrecdb.nonprod.ourcompany.com:
GOODURL=jdbc:mysql://transrecdb.nonprod.ourcompany.com
```

### Templates help make optional data simple
Developers might want connection properties

#### DBA Produces
```
environments:
  development:
    define:
      HOST: "transrecdb.nonprod.ourcompany.com"
      PORT: "33060"
  performance:
    define:
      HOST: "transrecdb.nonprod.ourcompany.com"
      PORT: "33070"
  production:
    define:
      HOST: "transrecdb.ourcompany.com"
  
```
#### Developer Consumes
```
environments:
  default:
    define:
      URL: "jdbc:mysql://{{HOST}}{{:PORT}}/db{{?PROPERTIES}}"
  production:
    define:
      PROPERTIES: "connectTimeout=3600&socketTimeout=36000"    

inject:
  - URL
```

Our results will be:

In development:
```
URL=jdbc:mysql://transrecdb.nonprod.ourcompany.com:33060
```

In performance:
```
URL=jdbc:mysql://transrecdb.nonprod.ourcompany.com:33070
```

In production:
```
URL=jdbc:mysql://transrecdb.ourcompany.com?connectTimeout=3600&socketTimeout=36000
```

Now we see how both DBAs and Developers are able to independently manage specific pieces of data that get merged together using consistent, universal template definitions.

## Nested Template Processing - Indirection
The concept of *which* captures the power of indirection.  The selection of *which* can be done by either the producer of consumer - with pros and cons likely in each case; so use indirection with the consideration "who should be deciding which?"

Indirection works by a feature in **templ** where template variables themselves are built from templates.

Setting up an example takes a few extra pieces (the layer of indirection); but this should still be simple enough to follow.

> I have frequently adopted a `T_` prefix for variables that are intended to be used exclusively for template processing and not expected to be used directly.  This is especially useful when paired with schema restrictions on `T_` variables.  You may or may not find value in similar patterns.

#### Network Admin Produces
```
environments:
  default:
    define:
      T_SERVICE_LDAP_EAST: "ldap.east.ourcompany.com"
      T_SERVICE_SMTP_EAST: "smtp.east.ourcompany.com"
      T_SERVICE_LDAP_WEST: "ldap.west.ourcompany.com"
      T_SERVICE_SMTP_WEST: "smtp.west.ourcompany.com"
      SERVICE_LDAP: "{{T_SERVICE_LDAP_{{^T_ZONE}}}}"
      SERVICE_SMTP: "{{T_SERVICE_SMTP_{{^T_ZONE}}}}"
  development:
    define:
      T_ZONE: "east"
  performance:
    define:
      T_ZONE: "east"
  production:
    define:
      T_ZONE: "west"
```
#### Developer Consumes
```
inject:
  - SERVICE_LDAP
  - SERVICE_SMTP
```

> Note the use of the `{{^VAR}}` **templ** syntax to upper-case the templated variable, changing T_ZONE "east" to "EAST".

One critically-important feature in this pattern is the atomicity of selecting a ZONE.  By an environment "Selecting Which" using a single variable (T_ZONE) we have the power to atomically configure multiple variables.  There is no opportunity for a half-configured environment - which seems to happen over and over with copy/paste/edit bugs each time a new environment is created: one of the variables doesn't get updated.  With nested template indirection you guarantee all of the variables based on T_ZONE are configured correctly.

## Inject Sets
What if you want to make sure an application gets all of the variables from some necessary set of variables?  Inject Sets provides support for this pattern.

Let's adjust the above example to show injection sets at work:

#### Network Admin Produces
```
environments:
  default:
    define:
      T_SERVICE_LDAP_EAST: "ldap.east.ourcompany.com"
      T_SERVICE_SMTP_EAST: "smtp.east.ourcompany.com"
      T_SERVICE_LDAP_WEST: "ldap.west.ourcompany.com"
      T_SERVICE_SMTP_WEST: "smtp.west.ourcompany.com"
      SERVICE_LDAP: "{{T_SERVICE_LDAP_{{^T_ZONE}}}}"
      SERVICE_SMTP: "{{T_SERVICE_SMTP_{{^T_ZONE}}}}"
  development:
    define:
      T_ZONE: "east"
  performance:
    define:
      T_ZONE: "east"
  production:
    define:
      T_ZONE: "west"

inject_sets:
  Services:
    - SERVICE_LDAP
    - SERVICE_SMTP
```
#### Developer Consumes
```
inject:
  - "*Services"
```

Now we see that the developer simply says "Give me all the variables defined in the Services set" and whatever they are, they get.

## Define Sets and Declare
We saw that indirection was fantastic to guarantee all of the configuration pieces of an environment correctly "map" together.  But what do you do if they don't all switch the same, but you want to make sure they are all defined correctly?

Define Sets (and Declare) makes this possible.

Again we will adjust the example above, this time constructing the definitions so that our network administator can independently adjust the location of our LDAP and SMTP servers, but in a way that they can't forget one or the other.

#### Network Admin Produces
```
define_sets:
  EnvironmentServices:
    SERVICE_LDAP: "ldap.{{$1}}.ourcompany.com"
    SERVICE_SMTP: "smtp.{{$2}}.ourcompany.com"

environments:
  development:
    declare:
      - "EnvironmentServices< east , east >"
  performance:
    declare:
      - "EnvironmentServices< east , west >"
  production:
    declare:
      - "EnvironmentServices< west , west >"

inject_sets:
  Services:
    - SERVICE_LDAP
    - SERVICE_SMTP
```
#### Developer Consumes
```
inject:
  - "*Services"
```

We now know that the performace environment is split zoned, with LDAP pointing to the east server, and SMTP pointing to the west server.  This pattern avoids intermediate variables, but requires that the arguments are syntactically correct - which may be too much to ask.

A hybrid system is possible using both template indirection and define sets.  It seems convoluted and obtuse for these simple examples; but trust me that it can be a lifesaver with complicated configurations.

#### Network Admin Produces
```
define_sets:
  EnvironmentServices:
    T_ZONE_A: "{{$1}}"
    T_ZONE_B: "{{$2}}"

environments:
  default:
    define:
      T_SERVICE_LDAP_EAST: "ldap.east.ourcompany.com"
      T_SERVICE_SMTP_EAST: "smtp.east.ourcompany.com"
      T_SERVICE_LDAP_WEST: "ldap.west.ourcompany.com"
      T_SERVICE_SMTP_WEST: "smtp.west.ourcompany.com"
      SERVICE_LDAP: "{{T_SERVICE_LDAP_{{^T_ZONE_A}}}}"
      SERVICE_SMTP: "{{T_SERVICE_SMTP_{{^T_ZONE_B}}}}"

  development:
    declare:
      - "EnvironmentServices< east , east >"
  performance:
    declare:
      - "EnvironmentServices< east , west >"
  production:
    declare:
      - "EnvironmentServices< west , west >"

inject_sets:
  Services:
    - SERVICE_LDAP
    - SERVICE_SMTP
```
#### Developer Consumes
```
inject:
  - "*Services"
```

## Define Secrets
The **envvars** library has special support for secrets.

> The **envvars** library cannot protect secrets!

Let me say that again a different way: 

> Do not put actual secrets in **envvars** data, or let the **envvars** library process your actual secrets!

Rather, secrets in **envvars** are meant to only be named references to secretIDs that somehow map to a secret management system at your runtime or deploytime.

So back to a simple example, it might look something like this:
#### DBA Produces
```
define:
  HOST: "transrecdb.perf.ourcompany.com"
  PORT: "3306"

defineSecrets:
  USERNAME: "vaultKey_transrecdb_perf_uname"
  PASSWORD: "vaultKey_transrecdb_perf_pass"
  
```
#### Developer Consumes
```
inject:
  - HOST
  - PORT
  - USERNAME
  - PASSWORD
```

The important thing to realize:
> A variables "secretness" is defined when it is produced (defined), not when it is consumed (injected).

Consumers should not be directly aware whether a variable is protected or not.  Some policies treat a username as a secret, other policies do not.  The application developer shouldn't need to care if the username is protected or not - it just needs the username.

> But your CICD code that uses the **envvars** library will need to specially map the secret data as part of its output generation.

## Define ConfigMaps
> Work in Progress

Secrets are private variables having values that are expected to actually be indirect references to protected data stored somewhere else.

ConfigMaps are public variables having values that are expected to actually be indirect references to public data stores somewhere else.

## Allowing Missing Declarations
Is a missing declaration a defect?  Opinions vary.  The **envvars* library lets you choose your own side on the "missing means error" vs "missing means default" argument.

The **envvars** library is opinionated to the extent that by default, **envvars** treats missing data as an error.

If you wish for missing data to be allowed (for **envvars** to not complain if you try to inject a variable that is undefined) you must explicitly tell the **envvars** engine to skip injecting that variable if it is not defined.

#### DBA Produces
```
environment:
  development:
    define:
      HOST: "transrecdb.dev.ourcompany.com"
      PORT: "3306"
  performance:
    define:
      HOST: "transrecdb.perf.ourcompany.com"
  production:
    define:
      HOST: "transrecdb.ourcompany.com"
      PORT: "3306"
```
#### Developer Consumes
```
inject:
  - HOST
  - PORT
```

The example above will error in *performance* because `PORT` is not defined.

Some variables "activate" by their existence - regardless of value, and it's better (or required) to not inject them in *false* cases.
#### Operations Produces
```
environment:
  development:
    define:
      DOCKER_TLS_VERIFY: "0"
  performance:
    define:
      DOCKER_TLS_VERIFY: "0"
  production:
    define:
      DOCKER_TLS_VERIFY: "1"
```
#### Developer Consumes
```
inject:
  - DOCKER_TLS_VERIFY
```

The code above works, but some folks prefer to not inject DOCKER_TLS_VERIFY if it shouldn't be activated.  How do you conditionally inject based on the defined status?

Never define the "0" cases, and then have your code tell the engine SkipInjectIfNotDefined("DOCKER_TLS_VERIFY").

#### Operations Produces
```
environment:
  development:
    define:
  performance:
    define:
  production:
    define:
      DOCKER_TLS_VERIFY: "1"
```
#### Developer Consumes
```
inject:
  - DOCKER_TLS_VERIFY
```

## Built-in SkipInjectIfNotDefined Support
There *is* one built-in way that **envvars** supports the "skip inject if not defined" feature.  There is a special syntax when using inject_sets with a `?` prefix to indicate that the variable to inject can be skipped if not defined.

> Why isn't `?` supported by normal inject? It is a design choice by **envvars** to reserve this \[potentially dangerous\] feature to the \[presumably fewer and more experienced\] persons that manage inject_sets.

So if we wanted the above to work without needing to write special code to interface directly with the EnvVarsEngine, we could do this:

#### Operations Produces
```
environment:
  development:
    define:
  performance:
    define:
  production:
    define:
      DOCKER_TLS_VERIFY: "1"

inject_sets:
  DockerVariables:
   - "?DOCKER_TLS_VERIFY"
```
#### Developer Consumes
```
inject:
  - "*DockerVariables"
```

## Optional Define Shortcut
The "define:" block of a Node is inferred if there are no other blocks or no other nestd blocks.

Meaning this:

```
environment:
  default:
    define:
      URL: "jdbc:mysql://{{HOST}}:{{PORT}}/db"
  development:
    define:
      HOST: "transrecdb.dev.ourcompany.com"
      PORT: "3306"
  performance:
    define:
      HOST: "transrecdb.perf.ourcompany.com"
      PORT: "3306"
  production:
    define:
      HOST: "transrecdb.ourcompany.com"
      PORT: "3306"
```

can be reduced to this:
```
environment:
  default:
    URL: "jdbc:mysql://{{HOST}}:{{PORT}}/db"
  development:
    HOST: "transrecdb.dev.ourcompany.com"
    PORT: "3306"
  performance:
    HOST: "transrecdb.perf.ourcompany.com"
    PORT: "3306"
  production:
    HOST: "transrecdb.ourcompany.com"
    PORT: "3306"
```


## Composable Configuration
Configuration (defines and injects) are composable, allowing different data domains to exist in different files or even different formats (YAML/JSON).

## Nested Configuration
Configuration (defines and injects) are nestable, allowing for a hierarchical configuration definition.

## Configuration Features Can Be Constrained
Constraints (in the form of a NodeSectionsPolicy) determines which features are allowed for a given Node context.

The constrained features for each node are:
* Declare
* Define
* Inject
* Remap
* SkipInjectIfNotDefined
* DefineSecrets
* DefineConfigMaps


## Schema Variable Constraints
Native **envvars** YAML files support a `schema` for variable name standards validation and confirmation.

Schemas use regular expressions to define definitions, and those definitions can be required or forbidden on a file-by-file basis.

## Variable Injection from a Different Definition
When trying to standardize naming conventions by domain in order to secure definition access by schema (aka: "Only DBA should have access to define database values"), it often creates cases where data is defined using a variable that is not the variables needed for injection (aka: "The DBAs define MYSQL_HOST but I need HOST").

The **envvars** library supports *inject from*, which is effectively an inline-rename operation.

First consider this scenario without rename:

#### Network Admin Produces
```
define:
  SERVICE_LDAP: "ldap.ourcompany.com"
```
#### Developer Consumes
```
define:
  LDAP_HOST: "{{SERVICE_LDAP}}"
inject:
  - LDAP_HOST
```

This works *fine* except if, for some reason, `SERVICE_LDAP` was not defined.  This happens if, let's say, you accidentally type `{{SRV_LDAP}}`, or if the network admin hasn't yet defined the host in a new environment.  Since variables are expected to be blank, there is no way *in template proccessing* to error on a missing (blank) variable.

*Plus there's the whole philosophical issue of not wanting (or even not *allowing*) developers to define variables.

From to the rescue.  Here is how you can inject `LDAP_HOST` without defining `LDAP_HOST`:

#### Network Admin Produces
```
define:
  SERVICE_LDAP: "ldap.ourcompany.com"
```
#### Developer Consumes
```
inject:
  - "LDAP_HOST from SERVICE_LDAP"
```

Elegant.

And now you will get an error if MYSQL_HOST isn't defined.

This also works great when you want/need to refactor global definitions, but individual apps are slow to adopt the new variable names.  They can individually map the new names to the old names.  Problem solved!


## Helpful Support for Key Files
When defining variables by environment, it becomes quickly important to know "What are the valid environments?"  Code could infer the list of environments as it detects new ones in the configuration, but that makes it impossible to detect errors when someone mistakenly types `prf` instead of `perf`.

You can certainly define an `enum` in your code to list your valid environments, but wouldn't it be nicer to keep data in a data file and out of your code?

So, **envvars** supports Key Files.

```
environments:
  - dev
  - perf
  - production
```

Now you can build key validation so that variables defined mistakenly for `prf` throws an error.


## Node Types
> Work in progress.

Node types is meant for hierarchical configuration nodes to be definable in metadata files rather that in code.

## Peeking At Envvars Engine Internals - Bridge Data
The **envvars** library is intended to be embedded in some larger configuration management CICD system.  There can be situations when that larger system needs \[or thinks it needs\] to *peek* into the running state of variables in the **envvars** engine.  Support for such behavior is provided by **bridge data** methods of the **EnvVarsEngine**.  Bridge Data can be completely generated by generateBridgeData() or generated-on-demand-and-cached by generateSparseBridgeData().

In either case, this is *not* how **envvars** processing is *supposed* to work, as it only reports on the "Producer Side" of the world, ignoring Inject processing.  Why does this matter?  Recall, Inject using the "from" syntax renames variables.  The bridge methods do not consider the from renaming of injects.  So: yes, you get a truthful state of the engine, but it might not be an accurate picture of what eventually is consumed by an application.

> Use Bridge Data with caution, as it may be an incomplete (or even inaccurate) picture of the variables your final consumer will see after calling EnvVarsEngine.process().

## Administrivia - Pronunciation
The **envvars** libary is spelled with two *v* letters; but phonetically, the first *v* is slurred so much into the second *v* as to be effectively silent.  Think "En Vars" not "Env Vars."  You, of course, can pronounce it as you like.  I pronounce it "En Vars."

<!-- LICENSE -->
## License

Distributed under the Apache 2.0 License. See `LICENSE.txt` for more information.
