package com.optum.envvars.mapdata;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.key.KeySourceOfTruth;
import com.optum.envvars.mapdata.source.EnvVarMapDataSource;
import com.optum.envvars.set.EnvVarsStaticSets;
import com.optum.templ.TemplDataSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Struct semantics and convenience methods for packaging the results of parsing an EnvVars object tree.
 */
public class EnvVarsMapData implements TemplDataSource {
    private final Map<String, Set<String>> envVarsContextForeignKeysFoundDirectory;
    private final Map<String, String> envVarsDefineSet;
    private final Map<String, String> envVarsInjectSet;
    private final Map<String, String> envVarsRemapSet;
    private final Map<String, String> envVarsDefineSecretSet;
    private final Map<String, String> envVarsDefineConfigMapSet;
    private final Map<String, String> skipInjectIfNotDefined;
    private final EnvVarsStaticSets envVarsStaticSets;

    public EnvVarsMapData() {
        this(null);
    }

    public EnvVarsMapData(EnvVarsStaticSets envVarsStaticSets) {
        this.envVarsContextForeignKeysFoundDirectory = new HashMap<>();
        this.envVarsDefineSet = new HashMap<>();
        this.envVarsInjectSet = new HashMap<>();
        this.envVarsRemapSet = new HashMap<>();
        this.envVarsDefineSecretSet = new HashMap<>();
        this.envVarsDefineConfigMapSet = new HashMap<>();
        this.skipInjectIfNotDefined = new HashMap<>();
        if (envVarsStaticSets!=null) {
            this.envVarsStaticSets = envVarsStaticSets;
        } else {
            this.envVarsStaticSets = new EnvVarsStaticSets();
        }
    }

    public void putAll(EnvVarMapDataSource envVarMapDataSource) throws EnvVarsException {
        putAll(envVarMapDataSource.getEnvVarMapData(envVarsStaticSets));
    }

    public void putAll(EnvVarsMapData envVarsMapData) {
        putAllEnvVarsContextForeignKeysFound(envVarsMapData.envVarsContextForeignKeysFoundDirectory);
        putAllEnvVarsDefine(envVarsMapData.envVarsDefineSet);
        putAllEnvVarsInject(envVarsMapData.envVarsInjectSet);
        putAllEnvVarsRemap(envVarsMapData.envVarsRemapSet);
        putAllEnvVarsDefineSecret(envVarsMapData.envVarsDefineSecretSet);
        putAllEnvVarsDefineConfigMap(envVarsMapData.envVarsDefineConfigMapSet);
        putAllSkipInjectIfNotDefined(envVarsMapData.skipInjectIfNotDefined);
    }

    public void putAllEnvVarsContextForeignKeysFound(Map<String, Set<String>> contextForeignKeysFoundDirectory) {
        for(Map.Entry<String, Set<String>> entry : contextForeignKeysFoundDirectory.entrySet()) {
            final String contextName = entry.getKey();
            final Set<String> foreignKeysFound = entry.getValue();
            putEnvVarContextForeignKeysFound(contextName, foreignKeysFound);
        }
    }

    public void putAllEnvVarsDeclare(List<String> envVarsDeclareList) throws EnvVarsException {
        putAllEnvVarsDefine(envVarsStaticSets.expandDefineSetReferences(envVarsDeclareList, "Declare Expansion"));
    }

    public void putAllEnvVarsDefine(Map<String, String> envVarsDefineSet) {
        this.envVarsDefineSet.putAll(envVarsDefineSet);
    }

    public void putAllEnvVarsInject(Map<String, String> envVarsInjectSet) {
        this.envVarsInjectSet.putAll(envVarsInjectSet);
    }

    public void putAllEnvVarsRemap(Map<String, String> envVarsRemapSet) {
        this.envVarsRemapSet.putAll(envVarsRemapSet);
    }

    public void putAllEnvVarsDefineSecret(Map<String, String> envVarsDefineSecretSet) {
        this.envVarsDefineSecretSet.putAll(envVarsDefineSecretSet);
    }

    public void putAllEnvVarsDefineConfigMap(Map<String, String> envVarsDefineConfigMapSet) {
        this.envVarsDefineConfigMapSet.putAll(envVarsDefineConfigMapSet);
    }

    public void putAllSkipInjectIfNotDefined(Map<String, String> skipInjectIfNotDefined) {
        this.skipInjectIfNotDefined.putAll(skipInjectIfNotDefined);
    }

    public void putEnvVarContextForeignKeysFound(String context, Set<String> foreignKeysFoundSet) {
        Set<String> globalSet = envVarsContextForeignKeysFoundDirectory.computeIfAbsent(context, k -> new HashSet<>());
        globalSet.addAll(foreignKeysFoundSet);
    }

    public void putEnvVarDeclare(String key) throws EnvVarsException {
        List<String> single = Collections.singletonList(key);
        putAllEnvVarsDefine(envVarsStaticSets.expandDefineSetReferences(single, "Declare Expansion"));
    }

    public void putEnvVarDeclareSecrets(String key) throws EnvVarsException {
        List<String> single = Collections.singletonList(key);
        putAllEnvVarsDefineSecret(envVarsStaticSets.expandDefineSetReferences(single, "DeclareSecrets Expansion"));
    }

    public void putEnvVarInject(String key, String from) {
        this.envVarsInjectSet.put(key, from);
    }

    public void putEnvVarRemap(String key, String from) {
        this.envVarsRemapSet.put(key, from);
    }

    public void putEnvVarDefine(String key, String value) {
        this.envVarsDefineSet.put(key, value);
    }

    public void putSkipInjectIfNotDefined(String key) {
        this.skipInjectIfNotDefined.put(key, key);
    }

    public void throwExceptionIfForeignKeysNotIn(KeySourceOfTruth keySourceOfTruth) throws EnvVarsException {
        for(Map.Entry<String, Set<String>> entry : envVarsContextForeignKeysFoundDirectory.entrySet()) {
            final String context = entry.getKey();
            final Set<String> foreignKeys = entry.getValue();
            keySourceOfTruth.throwExceptionIfForeignKeysNotFound(context, foreignKeys);
        }
    }

    public Set<String> getForeignKeysFor(String context, boolean required) throws EnvVarsException {
        Set<String> keys = envVarsContextForeignKeysFoundDirectory.get(context);

        if (keys==null) {
            if (required) {
                throw new EnvVarsException("Foreign Keys for context " + context + " is required, but was not found.");
            }
            return Collections.emptySet();
        }

        return keys;
    }

    public Map<String, String> getEnvVarsDefineSet() {
        return envVarsDefineSet;
    }

    public Map<String, String> getEnvVarsInjectSet() {
        return envVarsInjectSet;
    }

    public Map<String, String> getEnvVarsRemapSet() {
        return envVarsRemapSet;
    }

    public Map<String, String> getEnvVarsDefineSecretSet() {
        return envVarsDefineSecretSet;
    }

    public Map<String, String> getEnvVarsDefineConfigMapSet() {
        return envVarsDefineConfigMapSet;
    }

    public boolean isSkipInjectIfNotDefined(String key) {
        return skipInjectIfNotDefined.containsKey(key);
    }

    @Override
    public String lookupValueForKey(String key) {
        return envVarsDefineSet.get(key);
    }

}
