package com.trading.hf.patterns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.hf.model.PatternDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class GenericPatternParser {
    private static final Logger log = LoggerFactory.getLogger(GenericPatternParser.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, PatternDefinition> loadPatterns(String directoryPath) {
        Map<String, PatternDefinition> patterns = new HashMap<>();

        try (
                InputStream in = getClass().getClassLoader().getResourceAsStream(directoryPath);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                if (resource.endsWith(".json")) {
                    try (InputStream jsonStream = getClass().getClassLoader()
                            .getResourceAsStream(directoryPath + "/" + resource)) {
                        if (jsonStream == null) {
                            log.error("Cannot find resource: {}", resource);
                            continue;
                        }
                        PatternDefinition definition = objectMapper.readValue(jsonStream, PatternDefinition.class);
                        patterns.put(definition.getPatternId(), definition);
                        log.info("Loaded pattern: {}", definition.getPatternId());
                    } catch (IOException e) {
                        log.error("Error parsing pattern file: {}", resource, e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not read strategies from directory: {}", directoryPath, e);
        } catch (NullPointerException e) {
            log.error("Could not find the strategy resource directory: {}", directoryPath, e);
        }

        return patterns;
    }
}
