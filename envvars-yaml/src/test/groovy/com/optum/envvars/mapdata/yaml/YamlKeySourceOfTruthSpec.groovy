package com.optum.envvars.mapdata.yaml

import com.optum.envvars.EnvVarsException
import com.optum.envvars.key.EnvVarsKeys
import com.optum.envvars.key.MapKeySourceOfTruth
import com.optum.envvars.key.yaml.YamlKeySourceOfTruth
import spock.lang.Specification

class YamlKeySourceOfTruthSpec  extends Specification {
    def "Load a key set from a file" () {
        when:
        MapKeySourceOfTruth keySourceOfTruth = YamlKeySourceOfTruth.fromResource(YamlKeySourceOfTruth.class.getClassLoader(), "keys/environments.yaml")
        EnvVarsKeys envVarsKeys = keySourceOfTruth.registerKeys("environments")

        then:
        envVarsKeys.getKeys().size() == 3
        envVarsKeys.getKeys().containsAll(Set.of(
                "alpha",
                "bravo",
                "production"))

    }

    def "Load two key sets from a single file" () {
        when:
        MapKeySourceOfTruth keySourceOfTruth = YamlKeySourceOfTruth.fromResource(YamlKeySourceOfTruth.class.getClassLoader(), "keys/deployments.yaml")
        EnvVarsKeys cloudProviderKeys = keySourceOfTruth.registerKeys("cloudProviders")
        EnvVarsKeys environmentKeys = keySourceOfTruth.registerKeys("environments")

        then:
        cloudProviderKeys.getKeys().size() == 2
        cloudProviderKeys.getKeys().containsAll(Set.of(
                "aws",
                "gcp"))

        environmentKeys.getKeys().size() == 3
        environmentKeys.getKeys().containsAll(Set.of(
                "alpha",
                "dev",
                "production"))

    }

    def "Load a key set from a file with a null value throws exception" () {
        when:
        MapKeySourceOfTruth keySourceOfTruth = YamlKeySourceOfTruth.fromResource(YamlKeySourceOfTruth.class.getClassLoader(), "keys/environments-nullentry.yaml")
        keySourceOfTruth.registerKeys("environments")

        then:
        EnvVarsException ex = thrown()
        ex.message == 'KeySourceOfTruth [keys/environments-nullentry.yaml] Context:environments key is null.'
    }

    def "Load a key set from a file with a numeric key throws exception" () {
        when:
        MapKeySourceOfTruth keySourceOfTruth = YamlKeySourceOfTruth.fromResource(YamlKeySourceOfTruth.class.getClassLoader(), "keys/environments-numerickey.yaml")
        keySourceOfTruth.registerKeys("environments")

        then:
        EnvVarsException ex = thrown()
        ex.message == 'KeySourceOfTruth [keys/environments-numerickey.yaml] Context:environments key expected to be a String but was a java.lang.Integer'
    }

    def "Load a key set from a file with a complex key throws exception" () {
        when:
        MapKeySourceOfTruth keySourceOfTruth = YamlKeySourceOfTruth.fromResource(YamlKeySourceOfTruth.class.getClassLoader(), "keys/environments-complexkey.yaml")
        keySourceOfTruth.registerKeys("environments")

        then:
        EnvVarsException ex = thrown()
        ex.message == 'KeySourceOfTruth [keys/environments-complexkey.yaml] Context:environments key expected to be a String but was a java.util.LinkedHashMap'
    }

    def "Load a key set from a file without the expected context throws exception" () {
        when:
        MapKeySourceOfTruth keySourceOfTruth = YamlKeySourceOfTruth.fromResource(YamlKeySourceOfTruth.class.getClassLoader(), "keys/environments-nullentry.yaml")
        keySourceOfTruth.registerKeys("dataCenters")

        then:
        EnvVarsException ex = thrown()
        ex.message == 'KeySourceOfTruth [keys/environments-nullentry.yaml] Context:dataCenters missing.'
    }


}
