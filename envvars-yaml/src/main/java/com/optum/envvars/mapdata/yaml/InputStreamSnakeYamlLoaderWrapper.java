package com.optum.envvars.mapdata.yaml;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class InputStreamSnakeYamlLoaderWrapper implements SnakeYamlLoaderWrapper {
    private final InputStream is;

    public InputStreamSnakeYamlLoaderWrapper(InputStream is) {
        this.is = is;
    }

    @Override
    public <T> T load(Yaml yaml) {
        return yaml.load(is);
    }
}
