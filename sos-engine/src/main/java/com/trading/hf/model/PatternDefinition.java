package com.trading.hf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class PatternDefinition {

    @JsonProperty("pattern_id")
    private String patternId;

    @JsonProperty("regime_config")
    private Map<String, RegimeConfig> regimeConfig;

    @JsonProperty("phases")
    private List<Phase> phases;

    @JsonProperty("execution")
    private Execution execution;

    // Getters and setters

    public String getPatternId() {
        return patternId;
    }

    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }

    public Map<String, RegimeConfig> getRegimeConfig() {
        return regimeConfig;
    }

    public void setRegimeConfig(Map<String, RegimeConfig> regimeConfig) {
        this.regimeConfig = regimeConfig;
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public void setPhases(List<Phase> phases) {
        this.phases = phases;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    public static class RegimeConfig {
        @JsonProperty("allow_entry")
        private boolean allowEntry = true;
        @JsonProperty("tp_mult")
        private double tpMult = 1.0;
        @JsonProperty("quantity_mod")
        private double quantityMod = 1.0;

        // Getters and setters
        public boolean isAllowEntry() {
            return allowEntry;
        }

        public void setAllowEntry(boolean allowEntry) {
            this.allowEntry = allowEntry;
        }

        public double getTpMult() {
            return tpMult;
        }

        public void setTpMult(double tpMult) {
            this.tpMult = tpMult;
        }

        public double getQuantityMod() {
            return quantityMod;
        }

        public void setQuantityMod(double quantityMod) {
            this.quantityMod = quantityMod;
        }
    }

    public static class Phase {
        private String id;
        private List<String> conditions;
        private Map<String, String> capture;
        private int timeout;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getConditions() {
            return conditions;
        }

        public void setConditions(List<String> conditions) {
            this.conditions = conditions;
        }

        public Map<String, String> getCapture() {
            return capture;
        }

        public void setCapture(Map<String, String> capture) {
            this.capture = capture;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    public static class Execution {
        private String entry;
        private String sl;
        private String tp;

        // Getters and setters
        public String getEntry() {
            return entry;
        }

        public void setEntry(String entry) {
            this.entry = entry;
        }

        public String getSl() {
            return sl;
        }

        public void setSl(String sl) {
            this.sl = sl;
        }

        public String getTp() {
            return tp;
        }

        public void setTp(String tp) {
            this.tp = tp;
        }
    }
}
