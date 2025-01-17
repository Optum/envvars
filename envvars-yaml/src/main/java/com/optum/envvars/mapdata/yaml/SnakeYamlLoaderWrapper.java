package com.optum.envvars.mapdata.yaml;

import org.yaml.snakeyaml.Yaml;

public interface SnakeYamlLoaderWrapper {
    <T> T load(Yaml yaml);
}
