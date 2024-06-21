package uk.co.lewisod.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        this.enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing == null) {
            throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
        }

        enclosing.assign(name, value);
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public Object getAt(int distance, String lexeme) {
        var targetEnvironment = ancestor(distance);
        return targetEnvironment.values.get(lexeme);
    }

    public void assignAt(int distance, Token name, Object value) {
        var targetEnvironment = ancestor(distance);
        targetEnvironment.values.put(name.lexeme, value);
    }

    private Environment ancestor(int distance) {
        var environment = this;
        for (var i = 0; i < distance; i++) {
            if (environment == null) {
                throw new IllegalStateException("Attempt to access parent of global environment");
            }
            environment = environment.enclosing;
        }
        return environment;
    }
}
