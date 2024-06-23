package uk.co.lewisod.lox;

import java.util.Map;
import java.util.List;
import java.util.Optional;

public class LoxClass implements LoxCallable {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;

    public LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var instance = new LoxInstance(this);
        findMethod("init")
                .ifPresent(init -> init.bind(instance).call(interpreter, arguments));
        return instance;
    }

    @Override
    public int arity() {
        return findMethod("init")
                .map(LoxFunction::arity)
                .orElse(0);
    }

    public Optional<LoxFunction> findMethod(String name) {
        if (methods.containsKey(name)) {
            return Optional.of(methods.get(name));
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return Optional.empty();
    }
}
