package com.optum.envvars;

import com.optum.envvars.impl.ReferenceEnvVar;
import com.optum.envvars.impl.SecretEnvVar;
import com.optum.envvars.impl.SimpleEnvVar;
import com.optum.envvars.key.KeySourceOfTruth;
import com.optum.envvars.mapdata.EnvVarsMapData;
import com.optum.envvars.mapdata.source.EnvVarMapDataSource;
import com.optum.envvars.set.EnvVarsStaticSets;
import com.optum.templ.TemplEngine;
import com.optum.templ.exceptions.MissingKeyTemplException;
import com.optum.templ.exceptions.TemplException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class EnvVarsEngine {
    private final KeySourceOfTruth keySourceOfTruth;
    private final EnvVarsMapData envVarMapData;
    private TreeMap<String, EnvVar> results;
    private final TemplEngine templEngine;

    public EnvVarsEngine() {
        this(new EnvVarsStaticSets());
    }

    public EnvVarsEngine(EnvVarsStaticSets envVarsStaticSets) {
        this(null, envVarsStaticSets);
    }

    public EnvVarsEngine(KeySourceOfTruth keySourceOfTruth) {
        this(keySourceOfTruth, new EnvVarsStaticSets());
    }

    public EnvVarsEngine(KeySourceOfTruth keySourceOfTruth, EnvVarsStaticSets envVarsStaticSets) {
        this.keySourceOfTruth = keySourceOfTruth;
        this.envVarMapData = new EnvVarsMapData(envVarsStaticSets);
        this.results = null;
        this.templEngine = new TemplEngine(envVarMapData);
    }

    public void add(EnvVarMapDataSource envVarMapDataSource) throws EnvVarsException {
        envVarMapData.putAll(envVarMapDataSource);
        throwExceptionIfForeignKeysNotDefined();
    }

    public void add(EnvVarsMapData envVarMapData) throws EnvVarsException {
        this.envVarMapData.putAll(envVarMapData);
        throwExceptionIfForeignKeysNotDefined();
    }

    public Set<String> getForeignKeysFor(String context, boolean required) throws EnvVarsException {
        return envVarMapData.getForeignKeysFor(context, required);
    }

    private void throwExceptionIfForeignKeysNotDefined() throws EnvVarsException {
        if (keySourceOfTruth!=null) {
            envVarMapData.throwExceptionIfForeignKeysNotIn(keySourceOfTruth);
        }
    }

    /**
     * This method IGNORES "inject" logic.  It returns all "declares" "define" and "defineSecret" key/values after running template processing.
     * It does not change the internal state of the EnvVarEngine and does not rely on the process() method.
     * @return
     */
    public Map<String, String> generateBridgeData() throws EnvVarsException {
        Map<String, String> results = new HashMap<>();
        for (Map.Entry<String, String> entry : envVarMapData.getEnvVarsDefineSet().entrySet()) {
            EnvVar envVar = resolveFromQualifier(entry.getKey(), null, null);
            if (envVar==null)
                continue;
            results.put(envVar.getKey(), envVar.getValue());
        }
        for (Map.Entry<String, String> entry : envVarMapData.getEnvVarsDefineSecretSet().entrySet()) {
            EnvVar envVar = resolveFromQualifier(entry.getKey(), null, null);
            if (envVar==null)
                continue;
            results.put(envVar.getKey(), envVar.getValue());
        }
        return results;
    }

    public SparseBridgeData generateSparseBridgeData() {
        return new SparseBridgeData(this);
    }

    /**
     * This method is a lazy-load version of generateBridgeData() that is tolerant of sparely-valid data.
     * If you have a dataset that may contain invalid (template-unresolved) data, but that's okay as long as you
     * don't ask for it, use this method.  The SparseBridgeData class does this for you, and caches the results.
     */
    public String generateBridgeValue(String key) throws EnvVarsException {
        final String defineValue = envVarMapData.getEnvVarsDefineSet().get(key);
        if (defineValue!=null) {
            EnvVar envVar = resolveFromQualifier(key, null, null);
            if (envVar!=null) {
                return envVar.getValue();
            }
        }

        final String defineSecretValue = envVarMapData.getEnvVarsDefineSecretSet().get(key);
        if (defineSecretValue!=null) {
            EnvVar envVar = resolveFromQualifier(key, null, null);
            if (envVar!=null) {
                return envVar.getValue();
            }
        }
        return null;
    }

    public void process() throws EnvVarsException {
        results = new TreeMap<>();

        // Local copy of remaps.
        Map<String, String> remainingRemaps = new HashMap<>();
        remainingRemaps.putAll(envVarMapData.getEnvVarsRemapSet());

        // Process inject data
        for (Map.Entry<String, String> entry : envVarMapData.getEnvVarsInjectSet().entrySet()) {
            final String key = entry.getKey();
            final String qualifier = entry.getValue();
            final String remapQualifier = remainingRemaps.remove(key);
            EnvVar envVar = resolveFromQualifier(key, qualifier, remapQualifier);
            if (envVar!=null) {
                results.put(envVar.getKey(), envVar);
            }
        }

        if (!remainingRemaps.isEmpty()) {
            throw new EnvVarsException("The following variables were listed in remap blocks but never injected: " + remainingRemaps.keySet());
        }
    }

    /**
     * This logic performs:
     *   inject aliases (" from " syntax)
     *   define templates ("{{}}" syntax)
     *
     *   Results come from definitions first, then only if missing do they come from secrets.
     */
    private EnvVar resolveFromQualifier(final String reference, final String qualifier, final String remapQualifier) throws EnvVarsException {
        String effectiveReference = reference;

        try {
            // If the qualifier has been remapped, use the remapQualifier.
            final String qualifierToUse;
            if (remapQualifier != null) {
                qualifierToUse = templEngine.processTemplate(remapQualifier);
            } else {
                qualifierToUse = templEngine.processTemplate(qualifier);
            }

            String value;
            if (qualifierToUse != null) {
                effectiveReference = reference + " from " + qualifierToUse;
                value = envVarMapData.getEnvVarsDefineSet().get(qualifierToUse);
            } else {
                value = envVarMapData.getEnvVarsDefineSet().get(reference);
            }
            if (value != null) {
                final String val = templEngine.processTemplate(value);
                return new SimpleEnvVar(reference, val);
            }

            String secretValue;
            if (qualifierToUse != null) {
                effectiveReference = reference + " from " + qualifierToUse;
                secretValue = envVarMapData.getEnvVarsDefineSecretSet().get(qualifierToUse);
            } else {
                secretValue = envVarMapData.getEnvVarsDefineSecretSet().get(reference);
            }
            if (secretValue != null) {
                final String val = templEngine.processTemplate(secretValue);
                return new SecretEnvVar(reference, val);
            }

            String referenceValue;
            if (qualifierToUse != null) {
                effectiveReference = reference + " from " + qualifierToUse;
                referenceValue = envVarMapData.getEnvVarsDefineReferenceSet().get(qualifierToUse);
            } else {
                referenceValue = envVarMapData.getEnvVarsDefineReferenceSet().get(reference);
            }
            if (referenceValue != null) {
                final String val = templEngine.processTemplate(referenceValue);
                return new ReferenceEnvVar(reference, val);
            }
        } catch (MissingKeyTemplException e) {
            throw new EnvVarsException("Value is missing for template: {{" + e.getKey() + "}}.  If you want it to be optional, an empty value is needed instead of a missing value.");
        } catch (TemplException e) {
            throw new EnvVarsException("Unspecified Template Exception: " + e.getMessage(), e);
        }

        if (envVarMapData.isSkipInjectIfNotDefined(reference)) {
            return null;
        }
        throw new EnvVarsException("Unable to find " + effectiveReference);
    }

    public void add(String key, String value) throws EnvVarsException {
        add(new SimpleEnvVar(key, value));
    }

    public TreeMap<String, EnvVar> getResults() throws EnvVarsException {
        validateState();
        return results;
    }

    private void add(EnvVar envVar) throws EnvVarsException {
        results.put(envVar.getKey(), envVar);
    }

    private void validateState() throws EnvVarsException {
        if (results == null) {
            throw new EnvVarsException("Method not supported before process method is invoked.");
        }
    }

}
