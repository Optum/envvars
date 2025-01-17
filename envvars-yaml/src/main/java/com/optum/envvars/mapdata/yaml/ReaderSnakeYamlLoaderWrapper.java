package com.optum.envvars.mapdata.yaml;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.Map;

public class ReaderSnakeYamlLoaderWrapper implements SnakeYamlLoaderWrapper {
    private final Reader reader;

    public ReaderSnakeYamlLoaderWrapper(Reader reader) {
        this.reader = reader;
    }

    @Override
    public Map<Object, Object> load(Yaml yaml) {
        return yaml.load(reader);
    }
}
