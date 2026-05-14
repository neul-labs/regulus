package com.neullabs.regulus.dsl.yaml;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class YamlPipelineParser {

    public Map<String, Object> parse(InputStream inputStream) {
        Yaml yaml = new Yaml();
        return yaml.load(inputStream);
    }
}
