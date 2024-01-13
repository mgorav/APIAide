package com.gonnect.apiaide.python;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PythonExecutionService {

    private final PythonInterpreter interpreter;

    public PythonExecutionService(PythonInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public Object execute(String script, Map<String, Object> bindings) {

        PyObject scope = interpreter.getLocals();

        if (bindings != null) {
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {

                String key = entry.getKey();
                Object value = entry.getValue();

                PyObject pyKey = new PyString(key);
                PyObject pyValue = Py.java2py(value);

                scope.__set__(pyKey, pyValue);
            }
        }

        return interpreter.eval(script);
    }
}