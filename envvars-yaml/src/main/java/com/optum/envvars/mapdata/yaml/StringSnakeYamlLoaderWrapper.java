package com.optum.envvars.mapdata.yaml;

import org.yaml.snakeyaml.Yaml;

public class StringSnakeYamlLoaderWrapper implements SnakeYamlLoaderWrapper {
    private final String yaml;

    public StringSnakeYamlLoaderWrapper(String yaml) {
        this.yaml = yaml;
    }

    @Override
    public <T> T load(Yaml yaml) {
        return yaml.load(this.yaml);
    }
}
