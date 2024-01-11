package com.gonnect.apiaide.python;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.util.Map;

@Service
public class PythonExecutionService {

    private final ScriptEngine engine;

    public PythonExecutionService(ScriptEngine engine) {
        this.engine = engine;
    }

    @SneakyThrows
    public Object execute(String script, Map<String, Object> bindings) {

        if (bindings != null) {
            SimpleBindings b = new SimpleBindings(bindings);
            engine.setBindings(b, ScriptContext.ENGINE_SCOPE);
        }

        return engine.eval(script);
    }
}