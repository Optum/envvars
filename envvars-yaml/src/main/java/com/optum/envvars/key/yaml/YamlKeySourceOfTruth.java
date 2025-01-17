package com.optum.envvars.key.yaml;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.key.MapKeySourceOfTruth;
import com.optum.envvars.mapdata.yaml.YamlLoadWrapper;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class YamlKeySourceOfTruth {
    static public MapKeySourceOfTruth fromResource(ClassLoader classLoader, String resourceName) throws EnvVarsException {
        Yaml yaml = new Yaml();
        YamlLoadWrapper<Map<Object, Object>> yamlLoadWrapper = YamlLoadWrapper.fromResource(yaml, classLoader, resourceName);
        Map<Object, Object> globalScope = yamlLoadWrapper.load();
        if (globalScope==null) {
            throw new EnvVarsException("YamlKeySourceOfTruth unable to load resource: " + resourceName);
        }
        return new MapKeySourceOfTruth(resourceName, globalScope);
    }
}
