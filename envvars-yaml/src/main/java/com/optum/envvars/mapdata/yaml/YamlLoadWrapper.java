package com.optum.envvars.mapdata.yaml;

import com.optum.envvars.EnvVarsException;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public class YamlLoadWrapper<T> {
    private final Yaml yaml;
    private final SnakeYamlLoaderWrapper snakeYamlLoaderWrapper;

    /**
     * SnakeYaml load method 1 of 3 (java.lang.String)
     */
    private YamlLoadWrapper(Yaml yaml, String string) {
        this.yaml = yaml;
        this.snakeYamlLoaderWrapper = new StringSnakeYamlLoaderWrapper(string);
    }

    /**
     * SnakeYaml load method 2 of 3 (java.io.InputStream)
     */
    private YamlLoadWrapper(Yaml yaml, InputStream inputStream) {
        this.yaml = yaml;
        this.snakeYamlLoaderWrapper = new InputStreamSnakeYamlLoaderWrapper(inputStream);
    }

    /**
     * SnakeYaml load method 3 of 3 (java.io.Reader)
     */
    private YamlLoadWrapper(Yaml yaml, Reader reader) {
        this.yaml = yaml;
        this.snakeYamlLoaderWrapper = new ReaderSnakeYamlLoaderWrapper(reader);
    }

    public static <E> YamlLoadWrapper<E> fromString(Yaml yaml, String string) {
        return new YamlLoadWrapper<>(yaml, string);
    }

    public static <E> YamlLoadWrapper<E> fromURL(Yaml yaml, URL url) throws EnvVarsException {
        try {
            return new YamlLoadWrapper<>(yaml, url.openStream());
        } catch (IOException e) {
            throw new EnvVarsException("Unable to load YAML URL " + url, e);
        }
    }

    public static <E> YamlLoadWrapper<E> fromFile(Yaml yaml, String filename) throws EnvVarsException {
        try {
            return new YamlLoadWrapper<>(yaml, new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            throw new EnvVarsException("Unable to load YAML file " + filename, e);
        }
    }

    public static <E> YamlLoadWrapper<E> fromResource(Yaml yaml, ClassLoader classLoader, String resourcename) throws EnvVarsException {
        InputStream is = classLoader.getResourceAsStream(resourcename);
        if (is==null) {
            throw new EnvVarsException("Unable to open YAML resource " + resourcename + ".  Probably due to resource not found.");
        }
        return new YamlLoadWrapper<>(yaml, is);
    }

    public T load() {
        return snakeYamlLoaderWrapper.load(yaml);
    }
}
