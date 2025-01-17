package com.optum.envvars.mapdata.source.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapData;
import com.optum.envvars.mapdata.EnvVarsMapDataEngine;
import com.optum.envvars.mapdata.EnvVarsMapDataReader;
import com.optum.envvars.mapdata.NodeSectionsPolicy;
import com.optum.envvars.mapdata.StandardNodeSectionsPolicy;
import com.optum.envvars.mapdata.source.EnvVarMapDataSource;
import com.optum.envvars.schema.EnvVarsRuntimeSelectors;
import com.optum.envvars.set.EnvVarsStaticSets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptableEnvVarMapDataSource implements EnvVarMapDataSource {
    private final EnvVarsRuntimeSelectors envVarsRuntimeSelectors;
    private final EnvVarsMapDataReader envVarsMapDataReader;

    public ScriptableEnvVarMapDataSource(String data, String syntax, EnvVarsMapDataReader envVarsMapDataReader) throws JsonProcessingException, EnvVarsException {
        List<Object> rules = new ObjectMapper().readValue(syntax, List.class);
        Map<String, Object> values = new ObjectMapper().readValue(data, HashMap.class);
        envVarsRuntimeSelectors = new EnvVarsRuntimeSelectors(getNodes(rules, values));
        this.envVarsMapDataReader = envVarsMapDataReader;
    }

    private List<EnvVarsRuntimeSelectors.Node> getNodes(List<Object> syntaxList, Map<String, Object> values) throws EnvVarsException {
        List<EnvVarsRuntimeSelectors.Node> rootNodeList = new ArrayList();
        for(Object syntaxObject : syntaxList) {
            Map<String, Object> syntax = (Map)syntaxObject;
            final String context = pullString(syntax, "context", "c", null);
            final boolean contextRequired = Boolean.parseBoolean(pullString(syntax, "contextRequired", "cr", "true"));
            final boolean valueRequired = Boolean.parseBoolean(pullString(syntax, "valueRequired", "vr", "true"));
            final String RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR = pullString(syntax, "defaultSelectorValue", "dvs", "default");
            final EnvVarsMapDataEngine.DefaultProcessingPolicy defaultProcessing = EnvVarsMapDataEngine.DefaultProcessingPolicy.valueOf(pullString(syntax, "defaultProcessing", "dp", "SUPPORTED"));
            final NodeSectionsPolicy nodeSections = StandardNodeSectionsPolicy.valueOf(pullString(syntax, "nodeSections", "ns", "NOSECRETS"));
            final Object subNodesMap = syntax.get("subs");
            List<EnvVarsRuntimeSelectors.Node> subNodes = Collections.EMPTY_LIST;
            if (subNodesMap != null && (subNodesMap instanceof List)) {
                subNodes = getNodes((List) subNodesMap, values);
            }
            final Object value = values.get(context);
            if (value==null) {
                throw new EnvVarsException("Undefined value for context:" + context);
            }
            rootNodeList.add(
                    new EnvVarsRuntimeSelectors.Node(context, contextRequired, value.toString(), valueRequired,
                            defaultProcessing, nodeSections, RESERVED_SELECTOR_VALUE_FOR_DEFAULT_BEHAVIOR, subNodes));
        }
        return rootNodeList;
    }

    private String pullString(Map<String, Object> syntax, String longName, String shortName, String defaultValue) throws EnvVarsException {
        final Object longValue = syntax.get(longName);
        if (longValue!=null) {
            return checkString(longValue);
        }

        final Object shortValue = syntax.get(shortName);
        if (shortValue!=null) {
            return checkString(shortValue);
        }

        if (defaultValue!=null) {
            return defaultValue;
        }

        throw new EnvVarsException("Missing syntax value for key \"" + longName + "\" (short name \"" + shortName + "\"");
    }

    private String checkString(Object value) throws EnvVarsException {
        if (value instanceof String) {
            return (String) value;
        } else {
            throw new EnvVarsException("Syntax keyword " + value + " must be a String, but was a " + value.getClass().getName());
        }
    }

    @Override
    public EnvVarsMapData getEnvVarMapData(EnvVarsStaticSets envVarsStaticSets) throws EnvVarsException {
        return envVarsMapDataReader.getEnvVarMapData(envVarsStaticSets, envVarsRuntimeSelectors);
    }

}
