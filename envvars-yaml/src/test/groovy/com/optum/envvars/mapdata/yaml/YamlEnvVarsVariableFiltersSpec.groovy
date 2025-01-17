package com.optum.envvars.mapdata.yaml

import com.optum.envvars.EnvVarsEngine
import com.optum.envvars.EnvVarsException
import com.optum.envvars.mapdata.EnvVarsMapDataEngine
import com.optum.envvars.mapdata.StandardNodeSectionsPolicy
import com.optum.envvars.schema.EnvVarsDefinitions
import com.optum.envvars.schema.EnvVarsRuntimeSelectors
import com.optum.envvars.schema.EnvVarsStaticSchema
import com.optum.envvars.schema.EnvVarsVariableFilters
import com.optum.envvars.schema.VariableNameMustMatchException
import com.optum.envvars.schema.VariableNameMustNotMatchException
import com.optum.envvars.schema.VariableNameNotOneOfException
import com.optum.envvars.schema.yaml.YamlEnvVarsDefinitionsReader
import com.optum.envvars.schema.yaml.YamlEnvVarsSchemaReader
import groovy.json.JsonSlurper
import spock.lang.Specification

class YamlEnvVarsVariableFiltersSpec extends Specification {

    static {
        URL.setURLStreamHandlerFactory(new TestResourceURLStreamHandler());
    }

    final String definitions =
'''# Define variable name patterns to enforce suitability.
node_types:
  ENVIRONMENT_NODE_TYPE:
    name: Environment
  COMPONENT_NODE_TYPE:
    name: Component
  CLOUDPROVIDER_NODE_TYPE:
    name: Cloud Provider

variable_filters:
  UNIX_ENVIRONMENT_VARIABLES:
    regex: "[a-zA-Z_]+[a-zA-Z0-9_]*"
    name: Unix Shell Variables
    description: Upper case, lower case, digits and underscore; must not begin with a digit.
    usage: An application environment variable compatible with standard Unix shells.
  UNIX_RESERVED_VARIABLES:
    regex: "HISTORY|HOME|PATH|USER"
    name: Unix Reserved Variables
    description: HISTORY, HOME, PATH and other reserved Unix variables.
    usage: These variables are commonly used by Unix shells and systems and must not be set by an application.
  ROOT_VARIABLES:
    regex: "^R_.+$"
    name: Root Variables
    description: Begins with R_
    usage: Reserved for system-defined root variables.
  GENERATED_VARIABLES:
    regex: "^G_.+$"
    name: Generated Variables
    description: Begins with G_
    usage: Reserved for system-generated variables.
  DATABASE_VARIABLES:
    regex: "^.+(?:_BASEURL|_HOST|_PORT)$"
    name: Database Variables
    description: Ends in _BASEURL, _HOST or _PORT
    usage: Variables used for defining databases.
'''

    def "Raw Definitions Parsing from a String to test the Yaml parsing" () {
        when:
        EnvVarsDefinitions envVarsDefinitions = new YamlEnvVarsDefinitionsReader().getFromString(definitions);

        then:
        envVarsDefinitions.getNodeTypesCount() == 3
        envVarsDefinitions.getVariableFiltersCount() == 5

        'Environment' == envVarsDefinitions.getNodeType('ENVIRONMENT_NODE_TYPE').getName()
        'Database Variables' == envVarsDefinitions.getVariableFilter('DATABASE_VARIABLES').getName()
        'Ends in _BASEURL, _HOST or _PORT' == envVarsDefinitions.getVariableFilter("DATABASE_VARIABLES").getDescription()
        'Variables used for defining databases.' == envVarsDefinitions.getVariableFilter("DATABASE_VARIABLES").getUsage()
    }

    final String schema =
            '''
node_type: CLOUDPROVIDER_NODE_TYPE

variable_filters:
  all_of:
    - UNIX_ENVIRONMENT_VARIABLES
  one_of:
    - ROOT_VARIABLES
  none_of:
    - DATABASE_VARIABLES
'''

    def "Success: Variable Name Matches OneOf" () {
        when:
        EnvVarsStaticSchema envVarsStaticSchema = new YamlEnvVarsSchemaReader().buildFromString(schema, definitions);
        EnvVarsVariableFilters envVarsVariableFilters = envVarsStaticSchema.envVarsVariableFilters

        then:
        envVarsVariableFilters.confirmSuitability('R_GOOD')
    }

    def "Fail: Variable Name Doesn't Match OneOf" () {
        when:
        EnvVarsStaticSchema envVarsStaticSchema = new YamlEnvVarsSchemaReader().buildFromString(schema, definitions);
        EnvVarsVariableFilters envVarsVariableFilters = envVarsStaticSchema.envVarsVariableFilters
        envVarsVariableFilters.confirmSuitability('MISC_OTHER')

        then:
        VariableNameNotOneOfException ex = thrown()
    }

    def "Fail: Variable Name Matches NoneOf" () {
        when:
        EnvVarsStaticSchema envVarsStaticSchema = new YamlEnvVarsSchemaReader().buildFromString(schema, definitions);
        EnvVarsVariableFilters envVarsVariableFilters = envVarsStaticSchema.envVarsVariableFilters
        envVarsVariableFilters.confirmSuitability('R_GLOBAL_BASEURL')

        then:
        VariableNameMustNotMatchException ex = thrown()
    }

    def "Fail: Variable Name Does Not Match AllOf" () {
        when:
        EnvVarsStaticSchema envVarsStaticSchema = new YamlEnvVarsSchemaReader().buildFromString(schema, definitions);
        EnvVarsVariableFilters envVarsVariableFilters = envVarsStaticSchema.envVarsVariableFilters
        envVarsVariableFilters.confirmSuitability('29')

        then:
        VariableNameMustMatchException ex = thrown()
    }

    final String badschema =
            '''
node_type: CLOUDPROVIDER_NODE_TYPE

variable_filters:
  all_of:
    - UNIX_ENVIRONMNET_VARIBLES
  one_of:
    - ROOT_VARIABLES
  none_of:
    - DATABASE_VARIABLES
'''

    def "Bad Schema" () {
        when:
        new YamlEnvVarsSchemaReader().buildFromString(badschema, definitions)

        then:
        EnvVarsException ex = thrown()
        ex.message == 'Definition not found for variable filter: UNIX_ENVIRONMNET_VARIBLES'

    }

    final Map withremoteschema = new JsonSlurper().parseText(
            """{
"schema": "https:///schemas/root.yaml",
"environments": {
    "dev": {
        "R_ENV_PREFIX": "dev"
    },
    "prd": {
        "R_ENV_PREFIX": "prd"
    }
}
}"""
    ) as Map

    def "Remote Schema Good" () {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"prd", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsTemplatesMap = new EnvVarsMapDataEngine(new YamlEnvVarsSchemaReader(), null, withremoteschema)
        envVarsEngine.add(envVarsTemplatesMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
        envVarsEngine.templEngine.processTemplate("{{R_ENV_PREFIX}}") == "prd"
    }

    final Map badwithremoteschema = new JsonSlurper().parseText(
            """{
"schema": "https:///schemas/root.yaml",
"environments": {
    "prd": {
        "G_ENV_PREFIX": "one",
        "no spaces": "two",
        "PATH": "three",
        "My_PORT": "four"
    }
}
}"""
    ) as Map

    def "Remote Schema Bad" () {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"prd", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsTemplatesMap = new EnvVarsMapDataEngine(new YamlEnvVarsSchemaReader(), null, badwithremoteschema)
        envVarsEngine.add(envVarsTemplatesMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
        EnvVarsException ex = thrown()
        ex.message == 'The following variable names are not suitable:\n' +
                'G_ENV_PREFIX must match one of these, but matches none: Root Variables. (Begins with R_) - Reserved for system-defined root variables.\n' +
                'My_PORT must not match Database Variables. (Ends in _BASEURL, _HOST or _PORT) - Variables used for defining databases.\n' +
                'PATH must not match Unix Reserved Variables. (HISTORY, HOME, PATH and other reserved Unix variables.) - These variables are commonly used by Unix shells and systems and must not be set by an application.\n' +
                'no spaces must match Unix Shell Variables. (Upper case, lower case, digits and underscore; must not begin with a digit.) - An application environment variable compatible with standard Unix shells.'

    }

    final Map schemanoallof = new JsonSlurper().parseText(
            """{
"schema": "https:///schemas/no-allof.yaml",
"environments": {
    "prd": {
        "R_ENV_PREFIX": "prd"
    }
}
}"""
    ) as Map

    def "Remote Schema No Allof" () {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"prd", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsTemplatesMap = new EnvVarsMapDataEngine(new YamlEnvVarsSchemaReader(), null, schemanoallof)
        envVarsEngine.add(envVarsTemplatesMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
        envVarsEngine.templEngine.processTemplate("{{R_ENV_PREFIX}}") == "prd"
    }

    final Map schemanononeof = new JsonSlurper().parseText(
            """{
"schema": "https:///schemas/no-noneof.yaml",
"environments": {
    "prd": {
        "R_ENV_PREFIX": "prd"
    }
}
}"""
    ) as Map

    def "Remote Schema No Noneof" () {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"prd", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsTemplatesMap = new EnvVarsMapDataEngine(new YamlEnvVarsSchemaReader(), null, schemanononeof)
        envVarsEngine.add(envVarsTemplatesMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
        envVarsEngine.templEngine.processTemplate("{{R_ENV_PREFIX}}") == "prd"
    }

    final Map schemanooneof = new JsonSlurper().parseText(
            """{
"schema": "https:///schemas/no-oneof.yaml",
"environments": {
    "prd": {
        "R_ENV_PREFIX": "prd"
    }
}
}"""
    ) as Map

    def "Remote Schema No Oneof" () {
        when:
        EnvVarsEngine envVarsEngine = new EnvVarsEngine()
        EnvVarsRuntimeSelectors.Node node = new EnvVarsRuntimeSelectors.Node("environments", true,"prd", true, EnvVarsMapDataEngine.DefaultProcessingPolicy.SUPPORTED, StandardNodeSectionsPolicy.NOSECRETS)
        final EnvVarsMapDataEngine envVarsTemplatesMap = new EnvVarsMapDataEngine(new YamlEnvVarsSchemaReader(), null, schemanooneof)
        envVarsEngine.add(envVarsTemplatesMap.get(new EnvVarsRuntimeSelectors(Collections.singletonList(node))))

        then:
        envVarsEngine.templEngine.processTemplate("{{R_ENV_PREFIX}}") == "prd"
    }

}
