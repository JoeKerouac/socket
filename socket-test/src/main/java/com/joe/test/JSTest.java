package com.joe.test;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @author joe
 */
public class JSTest {
    public static void main(String[] args) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();

        ScriptEngine engine = manager.getEngineByName("javascript");
        String script = "function(){var a = 0 ; var b = 10 ; var c = a + b;return c;}";
        ScriptObjectMirror mirror = (ScriptObjectMirror)engine.eval(script);
        System.out.println(mirror.call(null ));
        System.out.println(engine.eval(script).getClass());
    }
}
