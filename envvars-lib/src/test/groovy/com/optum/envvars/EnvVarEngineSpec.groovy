package com.optum.envvars

import com.optum.envvars.key.KeySourceOfTruth
import com.optum.envvars.key.MapKeySourceOfTruth
import com.optum.envvars.mapdata.EnvVarsMapData
import com.optum.envvars.mapdata.EnvVarsMapDataEngine
import com.optum.envvars.mapdata.SimpleNodeSectionsPolicy
import com.optum.envvars.mapdata.StandardNodeSectionsPolicy
import com.optum.envvars.schema.EnvVarsRuntimeSelectors
import com.optum.envvars.set.EnvVarsStaticSets
import groovy.json.JsonSlurper
import spock.lang.Specification

class EnvVarEngineSpec extends Specification {

    SimpleNodeSectionsPolicy ALLOWALL = new SimpleNodeSectionsPolicy(true, true, true, true, true, true, true)

    final Map badDataEnv = new JsonSlurper().parseText(
            """{
"environments": {
    "env1": {
    },
    "env2": {
        "case1": "val1",
        "case2": "val2"
    }
}
}"""
    ) as Map

    final Map badDataApp = new JsonSlurper().parseText(
            """{
"applications": {
    "app1": {
        "inject": [
            "missing"
        ]
    },
    "app2": {
        "inject": [
            "missingTwo from missingcase"
        ]
    }
}
}"""
    ) as Map

    def "CONTEXT: Missing required context"() {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("platforms", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(badDataEnv)
        badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Expected context platforms was not found.'
    }

    def "CONTEXT: Missing optional context"() {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("platforms", false, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(badDataEnv)
        badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        noExceptionThrown()
    }

    def "SELECTOR: Missing required selector"() {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "missing", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(badDataEnv)
        badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Unable to find [environments:missing] which is required.'
    }

    def "SELECTOR: Missing optional selector"() {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "missing", false, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(badDataEnv)
        badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        noExceptionThrown()
    }

    def "Missing injectEnvvars reference in app1"() {
        when:
        EnvVarsEngine envVars = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(badDataEnv)
        envVars.add(badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "app1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataAppProcessor = new EnvVarsMapDataEngine(badDataApp)
        envVars.add(badDataAppProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVars.process()

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Unable to find missing'
    }

    def "Missing injectQualifiedEnvvars reference in app2"() {
        when:
        EnvVarsEngine envVars = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(badDataEnv)
        envVars.add(badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "app2", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataAppProcessor = new EnvVarsMapDataEngine(badDataApp)
        envVars.add(badDataAppProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVars.process()

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Unable to find missingTwo from missingcase'
    }

    final Map data = new JsonSlurper().parseText(
            """
{
    "environments" : {
        "optinunused": {
            "dinner": "pizza"
        },
        "optinused": {
            "inject": [
                "dinner"
            ],
            "define": {
                "dinner": "pizza"
            }
        },
        "hassecrets": {
            "inject": [
                "credentials"
            ],
            "define": {
                "T_ENV_PREFIX": "alpha"
            },
            "defineSecrets": {
                "credentials": "{{T_ENV_PREFIX}}--secret handshake"
            }
        },
        "qualified": {
            "define": {
                "secure": "https://",
                "unsecure": "http://"
            },
            "inject": [
                "URL from secure"
            ]
        },
        "unqualified": {
            "define": {
                "hobby": "knitting"
            },
            "inject": [
                "hobby"
            ]
        },
        "indirectqualified": {
            "define": {
                "secure": "https://",
                "unsecure": "http://",
                "T_TYPE": "un"
            },
            "inject": [
                "URL from {{T_TYPE}}secure"
            ]
        },
        "blankindirectqualified": {
            "define": {
                "secure": "https://",
                "unsecure": "http://",
                "T_TYPE": ""
            },
            "inject": [
                "URL from {{T_TYPE}}secure"
            ]
        },
        "unqualifiedsecret": {
            "define": {
                "T_ENV_PREFIX": "alpha"
            },
            "defineSecrets": {
                "hideout": "{{T_ENV_PREFIX}}--batcave"
            },
            "inject": [
                "hideout"
            ]
        },
        "qualifiedsecret": {
            "define": {
                "T_ENV_PREFIX": "alpha"
            },
            "defineSecrets": {
                "batman": "{{T_ENV_PREFIX}}--bruce",
                "superman": "{{T_ENV_PREFIX}}--clark"
            },
            "inject": [
                "identity from batman"
            ]
        },
        "indirectqualifiedsecret": {
            "define": {
                "T_ENV_PREFIX": "alpha",
                "T_ANIMAL": "bat"
            },
            "defineSecrets": {
                "batman": "{{T_ENV_PREFIX}}--bruce",
                "superman": "{{T_ENV_PREFIX}}--clark"
            },
            "inject": [
                "identity from {{T_ANIMAL}}man"
            ]
        },
        "remapdefines": {
          "breakfast": "waffles",
          "lunch": "sandwich",
          "food": "undefined"
        }
    }
}
"""
    ) as Map

    final Map client = new JsonSlurper().parseText(
            """
{
    "applications" : {
        "optinunused": {
            "hacks": {
                "pet": "snake"
            }
        },
        "optinused": {
            "hacks": {
                "pet": "snake"
            },
            "inject": [
                "dinner"
            ]            
        },
        "optinmissing": {
            "hacks": {
                "pet": "snake"
            },
            "inject": [
                "supper"
            ]
        },
        "optinoverride": {
            "hacks": {
                "pet": "snake",
                "dinner": "pasta"
            }
        },
        "remappeduse": {
            "inject": [
                "food"
            ],
            "remap": [
                "food from breakfast"
            ]
        },
        "remappedunused": {
            "remap": [
                "food from lunch"
            ]
        },
        "remappednonexistant": {
            "remap": [
                "meal from breakfast"
            ]
        }
    }
}
"""
    ) as Map

    final Map defaultdata = new JsonSlurper().parseText(
            """
{
    "dataset" : {
        "default": {
            "define": {
                "T_ENV_PREFIX": "alpha"
            },
            "defineSecrets": {
                "batman": "{{T_ENV_PREFIX}}--bruce",
                "superman": "{{T_ENV_PREFIX}}--clark"
            },
            "inject": [
                "identity from superman"
            ]
        },
        "simple": {
        }
    }
}
"""
    ) as Map

    final Map defaultdata2 = new JsonSlurper().parseText(
            """
{
    "dataset" : {
        "default": {
            "inject": [
                "sport"
            ],
            "define": {
                "T_ENV_PREFIX": "alpha"
            },
            "defineSecrets": {
                "sport": "{{T_ENV_PREFIX}}--baseball"
            }
        },
        "simple": {
        }
    }
}
"""
    ) as Map

    final Map doubleQuotes = new JsonSlurper().parseText(
            """
{
    "dataset" : {
        "sample": {
            "define": {
                "routes": "[\\"GET /\\", \\"GET /routes\\"]"
            },
            "inject": [
                "routes"
            ]
        }
    }
}
"""
    ) as Map

    // Making this shared emphasized the point that it is static to the data and must be able to process
    // the get() method with different runtime selectors.
    final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)

    def "Read primary keys from EnvVarsEngine"() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optinunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == ""
    }

    def "Unreferenced define are not injected."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optinunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == ""
    }

    def "Referenced define are injected."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optinunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "optinused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsClientMap = new EnvVarsMapDataEngine(client)
        envVarsEngine.add(envVarsClientMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "dinner", "value" : "pizza" }"""
    }

    def "Remap gives right value."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "remapdefines", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "remappeduse", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsClientMap = new EnvVarsMapDataEngine(client)
        envVarsEngine.add(envVarsClientMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "food", "value" : "waffles" }"""
    }

    def "Remap does not inject"() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()

        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "remapdefines", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "remappedunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsClientMap = new EnvVarsMapDataEngine(client)
        envVarsEngine.add(envVarsClientMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVarsEngine.process()

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The following variables were listed in remap blocks but never injected: [food]'
    }

    def "Remap gives right value for values manually (externally) injected."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsMapData results = new EnvVarsMapData();
        final EnvVarsMapDataEngine envVarsClientMap = new EnvVarsMapDataEngine(client)
        results.putAll(envVarsClientMap.processInjectEnvvars("From injectEnvironmentVariables", Arrays.asList("food")));
        envVarsEngine.add(results)

        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "remapdefines", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "remappedunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsClientMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "food", "value" : "sandwich" }"""
    }

    // There is no define for meal - this is normally an error.  But we only use meal FROM something else.  So it is fine.
    // This means we can use our own names for variables and they never have to be defined if they are always remapped.
    def "Remap gives right value for undefined values injected.  This lets you 'from' without a default."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsMapData results = new EnvVarsMapData();
        final EnvVarsMapDataEngine envVarsClientMap = new EnvVarsMapDataEngine(client)
        results.putAll(envVarsClientMap.processInjectEnvvars("From injectEnvironmentVariables", Arrays.asList("meal")));
        envVarsEngine.add(results)

        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "remapdefines", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "remappednonexistant", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsClientMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "meal", "value" : "waffles" }"""
    }

    def "Duplicate unreferenced define is allowed."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optinunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        EnvVarsRuntimeSelectors.Node node2 = new EnvVarsRuntimeSelectors.Node("applications", true, "optinoverride", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsClientMap = new EnvVarsMapDataEngine(client)
        envVarsEngine.add(envVarsClientMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node2))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == ""
    }


    def "Secrets are injected as secrets."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "hassecrets", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "credentials", "valueFrom" : { "secretKeyRef" : { "name" : "secretdata", "key"  : "alpha--secret handshake"}}}"""
    }

    def "Qualified resolves."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "qualified", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "URL", "value" : "https://" }"""
    }

    def "Unqualified resolves."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "unqualified", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "hobby", "value" : "knitting" }"""
    }

    def "Indirect qualified resolves."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "indirectqualified", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "URL", "value" : "http://" }"""
    }

    def "Blank indirect qualified resolves."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "blankindirectqualified", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "URL", "value" : "https://" }"""
    }

    def "Unqualified secret resolves."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "unqualifiedsecret", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "hideout", "valueFrom" : { "secretKeyRef" : { "name" : "secretdata", "key"  : "alpha--batcave"}}}"""
    }

    def "Qualified secret resolves."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "qualifiedsecret", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "identity", "valueFrom" : { "secretKeyRef" : { "name" : "secretdata", "key"  : "alpha--bruce"}}}"""
    }

    def "Indirect qualified secret resolves."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "indirectqualifiedsecret", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "identity", "valueFrom" : { "secretKeyRef" : { "name" : "secretdata", "key"  : "alpha--bruce"}}}"""
    }

    def "Default processing works."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("dataset", true, "simple", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        final EnvVarsMapDataEngine envVarsDefaultMap = new EnvVarsMapDataEngine(defaultdata)
        envVarsEngine.add(envVarsDefaultMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "identity", "valueFrom" : { "secretKeyRef" : { "name" : "secretdata", "key"  : "alpha--clark"}}}"""
    }

    def "Default processing works 2."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("dataset", true, "simple", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        final EnvVarsMapDataEngine envVarsDefault2Map = new EnvVarsMapDataEngine(defaultdata2)
        envVarsEngine.add(envVarsDefault2Map.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "sport", "valueFrom" : { "secretKeyRef" : { "name" : "secretdata", "key"  : "alpha--baseball"}}}"""
    }

    def "Double quotes are correctly escaped."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("dataset", true, "sample", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsDoubleQuotesMap = new EnvVarsMapDataEngine(doubleQuotes)
        envVarsEngine.add(envVarsDoubleQuotesMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        envVarsEngine.process()

        then:
        envVarsEngine.toJSON() == """{ "name" : "routes", "value" : "[\\"GET /\\", \\"GET /routes\\"]" }"""
    }

    final Map envKeys = new JsonSlurper().parseText(
            """
{
    "environments" : [
        "optinunused",
        "optinused",
        "hassecrets",
        "qualified",
        "unqualified",
        "indirectqualified",
        "blankindirectqualified",
        "unqualifiedsecret",
        "qualifiedsecret",
        "indirectqualifiedsecret",
        "remapdefines",
        "extraunusedkey"
    ]
}
"""
    ) as Map

    def "Validate Primary Keys in EnvVarsEngine"() {
        when:
        KeySourceOfTruth keySourceOfTruth = new MapKeySourceOfTruth("testcase", envKeys);
        keySourceOfTruth.registerKeys("environments")

        EnvVarsEngine envVarsEngine = new EnvVarsEngine(keySourceOfTruth)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optinunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
            true;
    }

    final Map missingKeys = new JsonSlurper().parseText(
            """
{
    "environments" : [
        "optinunused",
        "optinused",
        "hassecrets",
        "qualified",
        "unqualified",
        "indirectqualified",
        "blankindirectqualified",
        "unqualifiedsecret",
        "qualifiedsecret",
        "indirectqualifiedsecret"
    ]
}
"""
    ) as Map

    def "Validate Primary Keys in EnvVarsEngine throws exception with missing key"() {
        when:
        KeySourceOfTruth keySourceOfTruth = new MapKeySourceOfTruth("testcase", missingKeys);
        keySourceOfTruth.registerKeys("environments")

        EnvVarsEngine envVarsEngine = new EnvVarsEngine(keySourceOfTruth)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optinunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        envVarsEngine.add(envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Context:environments is not valid for key:remapdefines.  Is key mistyped or retired?'
    }

    final Map templates = new JsonSlurper().parseText(
            """{
"environments": {
    "dev": {
        "T_ENV_PREFIX": "dev"
    },
    "prd": {
        "T_ENV_PREFIX": "prd",
        "T_SECOND": "ctc",
        "T_THIRD": "",
        "T_WHICH": "SECOND"
    }
}
}"""
    ) as Map

    def "Template Substitution."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "prd", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsTemplatesMap = new EnvVarsMapDataEngine(templates)
        envVarsEngine.add(envVarsTemplatesMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
        envVarsEngine.templEngine.processTemplate("one.{{T_ENV_PREFIX}}.three") == "one.prd.three"

        envVarsEngine.templEngine.processTemplate("one.{{T_ENV_PREFIX}}.{{T_SECOND}}.three") == "one.prd.ctc.three"

        envVarsEngine.templEngine.processTemplate("one.{{T_ENV_PREFIX}}.{{T_SECOND}}.{{T_THIRD}}.three") == "one.prd.ctc..three"

        envVarsEngine.templEngine.processTemplate("one.{{T_{{T_WHICH}}}}.three") == "one.ctc.three"

    }

    final Map nested = new JsonSlurper().parseText(
            """{
"environments": {
    "default": {
        "DATABASE": "https://normal",
        "DATABASE_BLUE": "https://blue",
        "DATABASE_GREEN": "https://green",
        "MYDATABASE" : "{{DATABASE{{_T_COLOR}}}}",
        "T_COLOR": ""
    },
    "dev": {
    },
    "zulublue": {
        "T_COLOR": "BLUE",
    },
    "zulugreen": {
        "T_COLOR": "GREEN",
    }
}
}"""
    ) as Map

    def "Template Nested Substitution - Missing Value."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(nested)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("MYDATABASE") == "https://normal"
    }

    def "Template Nested Substitution - First Value."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "zulublue", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(nested)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("MYDATABASE") == "https://blue"
    }

    def "Template Nested Substitution - Second Value."() {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "zulugreen", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(nested)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("MYDATABASE") == "https://green"
    }

    final Map declare = new JsonSlurper().parseText(
            """{
"environments": {
    "default": {
        "declare": [
            "Database"
        ]
    }, 
    "dev": {
    }
}
}"""
    ) as Map

    final Map defineSets = ["Database": ["MYDATABASE": "https://database"]]

    def "Basic Declares"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), defineSets)
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, declare)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("MYDATABASE") == "https://database"
    }

    final Map declareParam = new JsonSlurper().parseText(
            """{
"environments": {
    "default": {
        "declare": [
            "Database<  Main  >"
        ]
    }, 
    "dev": {
    }
}
}"""
    ) as Map

    final Map defineSetsParam = ["Database": ['MYDATABASE_{{$1}}': 'https://{{$1}}:3306']]

    def "Parameterized Declares"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), defineSetsParam)
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, declareParam)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("MYDATABASE_Main") == "https://Main:3306"
    }

    final Map uppercaseDefineSetsParam = ["Database": ['MYDATABASE_{{^$1}}': 'https://{{^$1}}:3306']]

    def "Parameterized Uppercase Declares"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), uppercaseDefineSetsParam)
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, declareParam)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("MYDATABASE_MAIN") == "https://MAIN:3306"
    }

    final Map declareCascade = new JsonSlurper().parseText(
            """{
"environments": {
    "default": {
        "declare": [
            "Database<Main>"
        ]
    }, 
    "dev": {
        "declare": [
            "Rabbit<MQ>"
        ]        
    }
}
}"""
    ) as Map

    final Map defineSetsCascade1 = ["Database": ['MYDATABASE_{{$1}}': 'https://{{$1}}:3306']]
    final Map defineSetsCascade2 = ["Rabbit": ['MYRABBIT_{{$1}}': 'https://{{$1}}:5672']]

    def "Cascading Declares"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), defineSetsCascade1)
        envVarsStaticSets.add(new EnvVarsStaticSets(Collections.emptyMap(), defineSetsCascade2))
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, declareCascade)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("MYDATABASE_Main") == "https://Main:3306"
        results.get("MYRABBIT_MQ") == "https://MQ:5672"
    }

    final Map defineComplexArguments = new JsonSlurper().parseText(
            """{
"environments": {
    "dev": {
        "define": {
            "NORMAL" : "pizza"
        },
        "declare": [
            "Set< x , y, a-b >"
        ]
    }
}
}"""
    ) as Map

    final Map defineSetsComplexArguments = ["Set":
       ['KEY{{$1}}': 'VALUE{{$2}}',
       'KEY{{^$1}}': 'VALUE{{$2}}',
       'KEY{{$1}}{{_^$2}}': 'VALUE{{^$2}}',
       'KEY{{_^$1}}': 'VALUE{{NORMAL}}',
       'KEY{{_.,#$1:?}}': 'VALUE{{(@,$2;:)}}',
       'KEY{{^$3}}' : 'VALUE{{$3}}',
       'KEY{{$3}}' : 'VALUE{{^$3}}']
    ]

    def "Resolve Complex Arguments"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), defineSetsComplexArguments)
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, defineComplexArguments)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("KEYx") == "VALUEy"
        results.get("KEYX") == "VALUEy"
        results.get("KEYx_Y") == "VALUEY"
        results.get("KEY_X") == "VALUEpizza"
        results.get("KEY_.,#x:?") == "VALUE(@,y;:)"
        results.get("KEYA_B") == "VALUEa-b"
        results.get("KEYa-b") == "VALUEA_B"
    }

    final Map defineEmptyComplexArguments = new JsonSlurper().parseText(
            """{
"environments": {
    "dev": {
        "define": {
            "NORMAL" : "pizza"
        },
        "declare": [
            "Set< x ,  >"
        ]
    }
}
}"""
    ) as Map

    final Map defineSetsEmptyComplexArguments = ["Set":
       ['KEY{{$1}}': 'VALUE{{$2}}',
       'KEY{{^$1}}': 'VALUE{{$2}}',
       'KEY{{$1}}{{_^$2}}': 'VALUE{{^$2}}',
       'KEY{{_^$1}}{{_^$2}}': 'VALUE{{^$2}}{{NORMAL}}',
       'KEY{{_.,#$1:?}}': 'VALUE{{(@,$2;:)}}']
    ]

    def "Resolve Empty Complex Arguments"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), defineSetsEmptyComplexArguments)
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, defineEmptyComplexArguments)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        Map<String, String> results = envVarsEngine.generateBridgeData()
        then:
        results.get("KEYx") == "VALUE"
        results.get("KEYX") == "VALUE"
        results.get("KEYx") == "VALUE"
        results.get("KEY_X") == "VALUEpizza"
        results.get("KEY_.,#x:?") == "VALUE"
    }

        final Map defineEmptyComplexExtraArguments = new JsonSlurper().parseText(
            """{
"environments": {
    "dev": {
        "define": {
            "NORMAL" : "pizza"
        },
        "declare": [
            "Set< x ,  , >"
        ]
    }
}
}"""
    ) as Map

        final Map defineSetsEmptyComplexExtraArguments = ["Set":
       ['KEY{{$1}}': 'VALUE{{$2}}']
    ]

    def "Resolve Empty Complex Extra Arguments"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), defineSetsEmptyComplexExtraArguments)
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, defineEmptyComplexExtraArguments)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        envVarsEngine.generateBridgeData()
        then:
        EnvVarsException ex = thrown()
        ex.message == 'Parameterized Inject Set "Set" was invoked with argument list "[ x ,   ,  ]" but the definition does not contain a use of "$3".  Set values are: {KEY{{$1}}=VALUE{{$2}}}'
    }


    final Map inlineSkipInject = new JsonSlurper().parseText(
            """{
"environments": {
    "default": {
        "inject": [
            "*Database"
        ]
    }, 
    "dev": {
        "define": {
            "OTHER" : "value",
            "MYDATABASE" : "http://host.com:3306"
        }
    },
    "tst": {
        "define": {
            "OTHER" : "value",
        }
    }
}
}"""
    ) as Map

    final Map inlineSkipInjectSet = ["Database": ['?MYDATABASE', 'OTHER']]

    def "Inline Skip Injects"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(inlineSkipInjectSet, Collections.emptyMap())
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "tst", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, inlineSkipInject)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        envVarsEngine.process();
        then:
        envVarsEngine.toJSON() == """{ "name" : "OTHER", "value" : "value" }"""
    }

    final Map recursiveInject = new JsonSlurper().parseText(
            """{
"environments": {
    "default": {
        "define": {
            "URL_A" : "http://a",
            "URL_B" : "http://b",
            "URL_C" : "http://c",
            "USER_A" : "asdf",
            "USER_B" : "sdfg",
            "USER_C" : "dfgh",
        }
    }, 
    "good": {
        "inject": [
            "*Database"
        ]
    },
    "bad": {
        "inject": [
            "*BadDatabase<A>"
        ]
    }
}
}"""
    ) as Map

    final Map recursiveInjectSet = [
            "Database": ['*DatabaseInstance<A>',
                         '*DatabaseInstance<B>',
                         '*DatabaseInstance<C>'
            ],
            "DatabaseInstance": ['CONNECTION_URL_{{\$1}} from URL_{{\$1}}',
                                 'USERNAME_{{\$1}} from USER_{{\$1}}'
            ],
            "BadDatabase": ['*BadDatabase<{{\$1}}>',
                            '*BadDatabase<{{\$1}}>',
                            '*BadDatabase<{{\$1}}>'
            ]
    ]

    def "Recursive Injects"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(recursiveInjectSet, Collections.emptyMap())
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "good", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, recursiveInject)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        envVarsEngine.process();
        then:
        envVarsEngine.toJSON() == """{ "name" : "CONNECTION_URL_A", "value" : "http://a" },
{ "name" : "CONNECTION_URL_B", "value" : "http://b" },
{ "name" : "CONNECTION_URL_C", "value" : "http://c" },
{ "name" : "USERNAME_A", "value" : "asdf" },
{ "name" : "USERNAME_B", "value" : "sdfg" },
{ "name" : "USERNAME_C", "value" : "dfgh" }"""
    }

    def "Infinitly Recursive Injects"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(recursiveInjectSet, Collections.emptyMap())
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "bad", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, recursiveInject)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        envVarsEngine.process();
        then:
        EnvVarsException ex = thrown()
        ex.message == 'Detected a recursive inject_set reference: "*BadDatabase<A>" beyond the recursive depth limit.  Recursion is only allow to depth 10.'
    }


    final Map missingSet = new JsonSlurper().parseText(
            """{
"environments": {
    "default": {
        "declare": [
            "*NotThere<One>"
        ]
    }, 
    "dev": {
    }
}
}"""
    ) as Map

    final Map inlineMisingSet = ["set": ['k', 'v']]

    def "Missing Inject Set"() {
        when:
        EnvVarsStaticSets envVarsStaticSets = new EnvVarsStaticSets(Collections.emptyMap(), inlineMisingSet)
        EnvVarsEngine envVarsEngine = new EnvVarsEngine(envVarsStaticSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "dev", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine nestedMap = new EnvVarsMapDataEngine(null, envVarsStaticSets, missingSet)
        envVarsEngine.add(nestedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        envVarsEngine.process();
        then:
        EnvVarsException ex = thrown()
        ex.message == 'Unable to find a define_set for key *NotThere'
    }

    final Map unresolvedTemplate = new JsonSlurper().parseText(
            """{
    "environments": {
        "default": {
            "KEY": "VALUE{{PIECE}}"
        },
        "env1": {
            "SAFE": "env1"
        },
        "env2": {
            "SAFE": "env2",
            "PIECE": "defined"
        }
    }
    }"""
    ) as Map

    def "Unresolved reference blows up generateBridgeData for env1, since PIECE is used but undefined in env1"() {
        when:
        EnvVarsEngine envVars = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(unresolvedTemplate)
        envVars.add(badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        envVars.generateBridgeData()

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Value is missing for template: {{PIECE}}.  If you want it to be optional, an empty value is needed instead of a missing value.'
    }

    def "Unresolved reference in env1 does NOT blow up generateSparseBridgeData or when accessing SAFE from env1"() {
        when:
        EnvVarsEngine envVars = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(unresolvedTemplate)
        envVars.add(badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        SparseBridgeData sparseBridgeData = envVars.generateSparseBridgeData()

        then:
        sparseBridgeData.get("SAFE") == "env1"
    }

    def "Unresolved reference in env1 does NOT blow up generateSparseBridgeData but DOES blow up when accessing KEY from env1"() {
        when:
        EnvVarsEngine envVars = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badDataEnvProcessor = new EnvVarsMapDataEngine(unresolvedTemplate)
        envVars.add(badDataEnvProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))
        SparseBridgeData sparseBridgeData = envVars.generateSparseBridgeData()
        sparseBridgeData.get("KEY")

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Value is missing for template: {{PIECE}}.  If you want it to be optional, an empty value is needed instead of a missing value.'
    }


}
