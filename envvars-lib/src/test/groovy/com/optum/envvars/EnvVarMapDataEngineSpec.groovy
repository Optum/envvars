package com.optum.envvars

import com.optum.envvars.key.KeySourceOfTruth
import com.optum.envvars.key.MapKeySourceOfTruth
import com.optum.envvars.mapdata.EnvVarsMapData
import com.optum.envvars.mapdata.EnvVarsMapDataEngine
import com.optum.envvars.mapdata.NodeSectionsPolicy
import com.optum.envvars.mapdata.SimpleNodeSectionsPolicy
import com.optum.envvars.mapdata.StandardNodeSectionsPolicy
import com.optum.envvars.schema.EnvVarsRuntimeSelectors
import com.optum.envvars.set.EnvVarsStaticSets
import groovy.json.JsonSlurper
import spock.lang.Specification

class EnvVarMapDataEngineSpec extends Specification {

    /**
     * These test CONTEXT and SELECTOR cases.
     */
    final Map contextSelector = new JsonSlurper().parseText(
            """{
"environments": {
    "env1": {
    },
    "env2": {
        "define": {
            "varidata": {
                "case1": "val1",
                "case2": "val2"
            }
        }
    }
}
}"""
    ) as Map

    final NodeSectionsPolicy ALLOWALL = new SimpleNodeSectionsPolicy(true, true, true, true, true, true, true)

    def "CONTEXT: Missing required context" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("platforms", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine contextSelectorProcessor = new EnvVarsMapDataEngine(contextSelector)
        contextSelectorProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Expected context platforms was not found.'
    }

    def "CONTEXT: Missing optional context" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("platforms", false,"env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine contextSelectorProcessor = new EnvVarsMapDataEngine(contextSelector)
        contextSelectorProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        noExceptionThrown()
    }

    def "SELECTOR: Missing required selector" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"missing", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine contextSelectorProcessor = new EnvVarsMapDataEngine(contextSelector)
                contextSelectorProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Missing selector "missing".  Unable to find an element named "missing" inside the node "environments", and rules for this node require that it exists.'
    }

    def "SELECTOR: Missing optional selector" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"missing", false, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine contextSelectorProcessor = new EnvVarsMapDataEngine(contextSelector)
        contextSelectorProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        noExceptionThrown()
    }

    /**
     * These test bad map/list block cases.
     */
    final Map blockSelector = new JsonSlurper().parseText(
            """{
    "environments": {
        "env1": {
            "define": []
        },
        "env2": {
            "defineSecrets": []
        },
        "env3": {
            "inject": {}
        },
        "badenv": []
    },
    "badcontext": "wrongtypehere"
}"""
    ) as Map


    def "BLOCK: Bad envvars" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine blockProcessor = new EnvVarsMapDataEngine(blockSelector)
        blockProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid context "define".  The element "define" inside the selector "env1" inside the node "environments" must be a map, but it is not.'
    }

    def "BLOCK: Bad secretEnvvars" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"env2", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        final EnvVarsMapDataEngine blockProcessor = new EnvVarsMapDataEngine(blockSelector)
        blockProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid context "defineSecrets".  The element "defineSecrets" inside the selector "env2" inside the node "environments" must be a map, but it is not.'
    }

    def "BLOCK: Bad injectEnvvars" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"env3", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine blockProcessor = new EnvVarsMapDataEngine(blockSelector)
        blockProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The element [environments:env3:inject] must be a list, but it is not.'
    }

    def "BLOCK: Bad context type" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("badcontext", true,"anything", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine blockProcessor = new EnvVarsMapDataEngine(blockSelector)
        blockProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Context badcontext must be as map, but it is not.'
    }

    def "BLOCK: Bad selector type" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"badenv", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine blockProcessor = new EnvVarsMapDataEngine(blockSelector)
        blockProcessor.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid selector "badenv".  The element "badenv" inside the node "environments" must be a map, but it is not.'
    }
    /**
     * Three different flows, so each test in each of these blocks: envvars, injectedEnvvars, injectedQualifiedEnvvars.
     */

    final Map valueData = new JsonSlurper().parseText(
            """
{
    "environments" : {
        "blankkey": {
            "define": {
                "":"value"
            }
        },
        "blankvalue": {
            "define": {
                "key":""
            }
        },
        "quotekey": {
            "define": {
                "this is \\" not allowed":"value"
            }
        },
        "quotevalue": {
            "define": {
                "key":"this is \\" allowed"
            }
        },
        "booleanvalue": {
            "define": {
                "key": false
            }
        },
        "numbervalue": {
            "define": {
                "key": 42
            }
        },
        "blankkeyIE": {
            "inject": [
                ""
            ]
        },
        "quotekeyIE": {
            "inject": [
                "this is \\" not allowed"
            ]
        },
        "blankkeyIQE": {
            "inject": [
                ":value"
            ]
        },
        "blankvalueIQE": {
            "inject": [
                "key:"
            ]
        },
        "quotekeyIQE": {
            "inject": [
                "this is \\" not allowed:value"
            ]
        }
    }
}
"""
    ) as Map

    def "VALUE: EnvVars Blank key not allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "blankkey", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid blank key in [environments:blankkey:define].  It must not be blank.'
    }

    def "VALUE: EnvVars Blank value is allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "blankvalue", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        Map envVarDefs = valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVarDefs.size() == 1
        envVarDefs.get("key").toString() == ""
    }

    def "VALUE: EnvVars Double quote in key not allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "quotekey", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Key this is " not allowed in [environments:quotekey:define] contains a double quote.  Double quotes are not allowed.'
    }

    def "VALUE: EnvVars Double quote in value allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "quotevalue", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        Map envVarDefs = valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVarDefs.size() == 1
        envVarDefs.get("key").toString() == "this is \" allowed"
    }

    def "VALUE: EnvVars Boolean in value not allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "booleanvalue", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid value for key in [environments:booleanvalue:define].  The value is false.  It must be a String but is not.'
    }

    def "VALUE: EnvVars Number in value not allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "numbervalue", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid value for key in [environments:numbervalue:define].  The value is 42.  It must be a String but is not.'
    }


    def "VALUE: InjectedEnvvars Blank key not allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "blankkeyIE", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid blank key in [environments:blankkeyIE:inject].  It must not be blank.'
    }

    def "VALUE: InjectedEnvvars Double quote in key not allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "quotekeyIE", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine valueDataMap = new EnvVarsMapDataEngine(valueData)
        valueDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Key this is " not allowed in [environments:quotekeyIE:inject] contains a double quote.  Double quotes are not allowed.'
    }

    final Map escapedData = new JsonSlurper().parseText(
            """
{
    "environments" : {
        "env2": {
            "define": {
                "key": "\$\${ESCAPE}"
            }
        },
        "env3": {
            "defineSecrets": {
                "key": "\$\${ESCAPE}"
            }
        },
        "env4": {
            "inject": [
                "key:"
            ]
        }
    }
}""") as Map

    def "VALUE: Define does not do escape processing." () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env2", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine escapedDataMap = new EnvVarsMapDataEngine(escapedData)
        Map envVarDefs = escapedDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVarDefs.size() == 1
        envVarDefs.get("key").toString() == '$${ESCAPE}'
    }

    def "VALUE: DefineSecrets does not do escape processing." () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env3", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, ALLOWALL)
        final EnvVarsMapDataEngine escapedDataMap = new EnvVarsMapDataEngine(escapedData)
        Map secrets = escapedDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSecretSet()

        then:
        secrets.size() == 1
        secrets.get("key").toString() == '$${ESCAPE}'
    }

    /**
     * Json can't construct this bad map, so we have to do it by hand.
     * @return
     */
    def "VALUE: non String key" () {
        when:
        final Map contrivedContext = new HashMap()
        final Map contrivedSelector = new HashMap()
        final Map contrivedEnvVars = new HashMap()
        final Map contrivedValues = new HashMap()
        contrivedValues.put(new Integer(42), 'Anything Here')
        contrivedEnvVars.put("define", contrivedValues)
        contrivedSelector.put("env", contrivedEnvVars)
        contrivedContext.put("environments", contrivedSelector)

        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine contrivedMap = new EnvVarsMapDataEngine(contrivedContext)
        contrivedMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid key 42 in [environments:env:define].  It must be a String but is not.'
    }

    final Map injectedData = new JsonSlurper().parseText("""{
    "environments": {
        "env1": {
            "define": {
                "food": "pizza",
                "pet": "dog"
            },
            "inject": [
                "food",
                "pet"
            ]
        }
    }
}"""
    ) as Map

    def "INJECT: Injecting iterates more than one." () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine injectedDataMap = new EnvVarsMapDataEngine(injectedData)
        Map envVarDefs = injectedDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVarDefs.size() == 2
        envVarDefs.get("food").toString() == "pizza"
        envVarDefs.get("pet").toString() == "dog"
    }

    final Map badRemapData = new JsonSlurper().parseText("""{
    "environments": {
        "env1": {
            "remap": [
                "food"
            ]
        }
    }
}"""
    ) as Map

    def "REMAP: Remap missing from." () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine badRemapDataMap = new EnvVarsMapDataEngine(badRemapData)
        badRemapDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The element [environments:env1:remap] contains an invalid entry:"food" - no " from " found.'
    }


    final Map defaultData = new JsonSlurper().parseText("""{
    "environments": {
        "default": {
        },
        "env1": {
        },
        "badenv": {
        }
    },
    "applications": {
        "app1": {
        }
    }
}"""
    ) as Map

    def "DEFAULT: Forbidden default is present." () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.FORBIDDEN, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine defaultDataMap = new EnvVarsMapDataEngine(defaultData)
        defaultDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Node environments contains a default element, but default elements are forbidden.'
    }

    def "DEFAULT: Required default is missing." () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("applications", true, "app1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.REQUIRED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine defaultDataMap = new EnvVarsMapDataEngine(defaultData)
        defaultDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Missing selector "default".  Unable to find an element named "default" inside the node "applications", and rules for this node require that it exists.'
    }

    final Map envKeys = new JsonSlurper().parseText("""{
    "environments": [
        "env1",
        "env2"
        ]
}"""
    ) as Map

    def "FOREIGN KEYS: Foreign key is missing." () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "env1", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine defaultDataMap = new EnvVarsMapDataEngine(defaultData)
        EnvVarsMapData envVarsMapData = defaultDataMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))
        KeySourceOfTruth keySourceOfTruth = new MapKeySourceOfTruth("testcase", envKeys);
        keySourceOfTruth.registerKeys("environments")
        envVarsMapData.throwExceptionIfForeignKeysNotIn(keySourceOfTruth)

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Context:environments is not valid for key:badenv.  Is key mistyped or retired?'
    }


    final Map data = new JsonSlurper().parseText(
            """
{
    "environments" : {
        "default": {
            "inject": [
                "pet"
            ],
            "define": {
                "pet": "fish"
            }
        },
        "test": {
            "define": {
                "pet": "dog"
            }
        },
        "missingenvvars": {
        },
        "emptyenvvars": {
            "qualifiedRequirements": {
            }
        },
        "stage": {
            "define": {
                "pet": "cat"
            },
            "cloudProviders": {
                "ose-elr-dmz": {
                    "pet": "tiger"
                },
                "ose-ctc-dmz": {
                    "pet": "lion"
                }
            }
        },
        "production": {
            "define": {
                "pet": "cat"
            },
            "dataCenters": {
                "elr": {
                    "pet": "rabbit"
                },
                "ctc": {
                    "pet": "turtle"
                }
            },
            "cloudProviders": {
                "ose-elr-dmz": {
                    "pet": "tiger"
                },
                "ose-ctc-dmz": {
                    "pet": "lion"
                }
            }
        },
        "optinunused": {
            "define": {
                "dinner": "pizza",
                "pet": "bird"
            }
        },
        "optindefault": {
            "inject": [
                "dinner"
            ],
            "define": {
                "dinner": "pizza",
                "pet": "bird"
            }
        },
        "NoLongerSupportedQualifiedSyntax": {
            "define": {
                "URL": {
                    "secure": "https://",
                    "unsecure": "http://"
                }
            },
            "inject": [
                "URL:secure"
            ]
        }
    }
}
"""
    ) as Map

    def "Resolve Foreign Keys from Environment" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "production", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS, [
                new EnvVarsRuntimeSelectors.Node("dataCenters", false, "ctc", false, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS),
                new EnvVarsRuntimeSelectors.Node("cloudProviders", false, "ose-ctc-dmz", false, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        ])
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        EnvVarsMapData envVarsMapData = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))
        Set<String> environmentList = envVarsMapData.getForeignKeysFor("environments", true)
        Set<String> dataCenterList = envVarsMapData.getForeignKeysFor("dataCenters", true)
        Set<String> cloudProviderList = envVarsMapData.getForeignKeysFor("cloudProviders", true)

        then:
        environmentList.size() == 8
        environmentList.containsAll(Set.of(
                "emptyenvvars",
                "missingenvvars",
                "NoLongerSupportedQualifiedSyntax",
                "optindefault",
                "optinunused",
                "production",
                "stage",
                "test"
        ))
        dataCenterList.size() == 2
        dataCenterList.containsAll(Set.of(
                "ctc",
                "elr"
        ))
        cloudProviderList.size() == 2
        cloudProviderList.containsAll(Set.of(
                "ose-elr-dmz",
                "ose-ctc-dmz"
        ))
    }

    def "Missing non-required context (the actual cloudProviders block) doesn't blow up" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "test", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS, [
                new EnvVarsRuntimeSelectors.Node("cloudProviders", false, "aws", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        ])
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "dog"
    }

    def "Simple found environment and no subNodes defined" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "test", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "dog"
    }

    def "Simple found environment and no subNodes defined not confused by unused subNodes" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "stage", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "cat"
    }

    def "Simple found environment and found cloudProvider" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "stage", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS, [
                new EnvVarsRuntimeSelectors.Node("cloudProviders", true, "ose-ctc-dmz", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        ])
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "lion"
    }

    def "Simple two matching subNodes takes second - A" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "production", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS, [
                new EnvVarsRuntimeSelectors.Node("dataCenters", false, "ctc", false, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS),
                new EnvVarsRuntimeSelectors.Node("cloudProviders", false, "ose-ctc-dmz", false, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        ])
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "lion"
    }

    def "Simple two matching subNodes takes second - B" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "production", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS, [
                new EnvVarsRuntimeSelectors.Node("cloudProviders", true,"ose-ctc-dmz", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS),
                new EnvVarsRuntimeSelectors.Node("dataCenters", true, "ctc", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        ])
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "turtle"
    }

    def "Nothing in envvars doesn't choke" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "emptyenvvars", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "fish"
    }

    def "Nothing in envvars with DefaultProcessing.IGNORED doesn't choke" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "emptyenvvars", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.IGNORED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 0
    }

    def "Missing envvars is allowed" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "missingenvvars", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        Map envVars = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))).getEnvVarsDefineSet()

        then:
        envVars.size() == 1
        envVars.get("pet") == "fish"
    }

    def "Node Guard: Null context" () {
        when:
        new EnvVarsRuntimeSelectors.Node(null, true, "something", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)

        then:
        thrown(EnvVarsException)
    }

    def "Node Guard: Empty context" () {
        when:
        new EnvVarsRuntimeSelectors.Node("", true, "something", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)

        then:
        thrown(EnvVarsException)
    }

    def "Node Guard: Null selector" () {
        when:
        new EnvVarsRuntimeSelectors.Node("something", true, null, true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)

        then:
        thrown(EnvVarsException)
    }

    def "Node Guard: Empty selector" () {
        when:
        new EnvVarsRuntimeSelectors.Node("something", true, "", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)

        then:
        thrown(EnvVarsException)
    }

    def "Node Guard: Reserved selector 'default'" () {
        when:
        new EnvVarsRuntimeSelectors.Node("something", true, "default", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)

        then:
        thrown(EnvVarsException)
    }

    def "Node Guard: Null node list" () {
        when:
        new EnvVarsRuntimeSelectors.Node("something", true,"", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS, null)

        then:
        thrown(EnvVarsException)
    }

    def "Def is loaded, but not injected" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optinunused", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        EnvVarsMapData preEnvVarsData = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))
        Map envVarDefs = preEnvVarsData.getEnvVarsDefineSet()
        Map envVarInjects = preEnvVarsData.getEnvVarsInjectSet()

        then:
        envVarDefs.size() == 2
        envVarDefs.get("pet") == "bird"
        envVarDefs.get("dinner") == "pizza"
        envVarInjects.size() == 1
        envVarInjects.containsKey("pet")
    }

    def "Ref is loaded with default def value" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "optindefault", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        EnvVarsMapData preEnvVarsData = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))
        Map envVarDefs = preEnvVarsData.getEnvVarsDefineSet()
        Map envVarRefs = preEnvVarsData.getEnvVarsInjectSet()

        then:
        envVarDefs.size() == 2
        envVarDefs.get("pet") == "bird"
        envVarDefs.get("dinner") == "pizza"
        envVarRefs.size() == 2
        envVarRefs.containsKey("pet")
        envVarRefs.containsKey("dinner")
    }

    def "Qualified resolves" () {
        when:
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "NoLongerSupportedQualifiedSyntax", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(data)
        EnvVarsMapData preEnvVarsData = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))
        preEnvVarsData.getEnvVarsDefineSet()
        preEnvVarsData.getEnvVarsInjectSet()

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Invalid value for URL in [environments:NoLongerSupportedQualifiedSyntax:define].  The value is {secure=https://, unsecure=http://}.  It must be a String but is not.'
    }

    final Map setData = new JsonSlurper().parseText(
            """{
    "environments": {
        "default": {
            "declare": [
                "LocalDatabase",
                "ProxyDatabase"              
            ],
            "inject": [
                "*Profile",
                "*Database",
                "*OptionalData"
            ]
        },
        "hasdefaultset": {
            "define": {
                "PROFILE": "some value"
            }
        },
        "remapset": {
            "remap": [
                "*Proxy"
            ],
            "define": {
                "PROFILE": "some other value"
            }
        },
        "nofromremap": {
            "remap": [
                "*Profile"
            ]
        },
        "nooptionalremap": {
            "remap": [
                "*OptionalData"
            ]
        }
    }
}"""
    ) as Map

    final Map<String, List<String>> injectSets = [
            "Database": ["URL from DATABASE_URL"],
            "Proxy": ["URL from PROXY_URL"],
            "Profile": ["PROFILE"],
            "OptionalData": ["?BONUS"]
    ]

    final Map<String, Map<String, String>> defineSets = [
            "LocalDatabase": ["DATABASE_URL": "https://localhost:3306"],
            "ProxyDatabase": ["PROXY_URL": "https://localhost:1234"]
    ]

    def "Inject Set resolves" () {
        when:
        EnvVarsStaticSets staticSets = new EnvVarsStaticSets(injectSets, defineSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "hasdefaultset", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(null, staticSets, setData)
        EnvVarsMapData preEnvVarsData = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))
        Map envVarDefs = preEnvVarsData.getEnvVarsDefineSet()
        Map envVarRefs = preEnvVarsData.getEnvVarsInjectSet()
        Map envVarsRemaps = preEnvVarsData.getEnvVarsRemapSet()

        then:
        envVarDefs.size() == 3
        envVarDefs.get("DATABASE_URL") == "https://localhost:3306"
        envVarDefs.get("PROXY_URL") == "https://localhost:1234"
        envVarDefs.get("PROFILE") == "some value"
        envVarRefs.size() == 3
        envVarRefs.containsKey("URL")
        envVarRefs.get("URL") == "DATABASE_URL" // This is the "from"
        envVarRefs.containsKey("PROFILE")
        envVarRefs.get("PROFILE") == null // No "from"
        envVarRefs.containsKey("BONUS")
        envVarRefs.get("BONUS") == null // No "from"
        envVarsRemaps.size() == 0

    }

    def "Remapped Set resolves" () {
        when:
        EnvVarsStaticSets staticSets = new EnvVarsStaticSets(injectSets, defineSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "remapset", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(null, staticSets, setData)
        EnvVarsMapData preEnvVarsData = envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))
        Map envVarDefs = preEnvVarsData.getEnvVarsDefineSet()
        Map envVarRefs = preEnvVarsData.getEnvVarsInjectSet()
        Map envVarsRemaps = preEnvVarsData.getEnvVarsRemapSet()

        then:
        envVarDefs.size() == 3
        envVarDefs.get("DATABASE_URL") == "https://localhost:3306"
        envVarDefs.get("PROXY_URL") == "https://localhost:1234"
        envVarDefs.get("PROFILE") == "some other value"
        envVarRefs.size() == 3
        envVarRefs.containsKey("URL")
        envVarRefs.get("URL") == "DATABASE_URL" // This is the "from"
        envVarRefs.containsKey("PROFILE")
        envVarRefs.get("PROFILE") == null // No "from"
        envVarRefs.containsKey("BONUS")
        envVarRefs.get("BONUS") == null // No "from"
        envVarsRemaps.size() == 1
        envVarsRemaps.containsKey("URL");
        envVarsRemaps.get("URL") == "PROXY_URL" // This is the "from"
    }

    def "Cannot Remapped Set with a non-from inject" () {
        when:
        EnvVarsStaticSets staticSets = new EnvVarsStaticSets(injectSets, defineSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "nofromremap", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(null, staticSets, setData)
        envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The element [environments:nofromremap:remap] contains an invalid entry:"PROFILE" - no " from " found.'
    }

    def "Cannot Remapped Set with optional inject" () {
        when:
        EnvVarsStaticSets staticSets = new EnvVarsStaticSets(injectSets, defineSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "nooptionalremap", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(null, staticSets, setData)
        envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The element [environments:nooptionalremap:remap] contains an optional inject:"?" - optional injects are not allowed in a remap block.'
    }

    def "Define allowed but Declare is not allowed" () {
        when:
        final NodeSectionsPolicy DEFINEANDINJECTBUTNOTDECLARE = new SimpleNodeSectionsPolicy(false, true, true, false, false, true, false)

        EnvVarsStaticSets staticSets = new EnvVarsStaticSets(injectSets, defineSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "nooptionalremap", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, DEFINEANDINJECTBUTNOTDECLARE)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(null, staticSets, setData)
        envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The element [environments:default:declare] is not allowed to contain a declare section.'
    }

    def "Declare allowed but Define is not allowed" () {
        when:
        final NodeSectionsPolicy DECLAREANDINJECTBUTNOTDEFINE = new SimpleNodeSectionsPolicy(true, false, true, false, false, true, false)

        EnvVarsStaticSets staticSets = new EnvVarsStaticSets(injectSets, defineSets)
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true, "hasdefaultset", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, DECLAREANDINJECTBUTNOTDEFINE)
        final EnvVarsMapDataEngine envVarsMap = new EnvVarsMapDataEngine(null, staticSets, setData)
        envVarsMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node)))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The element [environments:hasdefaultset:define] is not allowed to contain a define section.'
    }


}
