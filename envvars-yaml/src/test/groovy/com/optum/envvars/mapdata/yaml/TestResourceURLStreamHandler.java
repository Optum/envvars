package com.optum.envvars.mapdata.yaml;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class TestResourceURLStreamHandler implements URLStreamHandlerFactory {
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                String path = u.getPath().substring(1);
                return TestResourceURLStreamHandler.class.getClassLoader().getResource(path).openConnection();
            }
        };
    }
}
