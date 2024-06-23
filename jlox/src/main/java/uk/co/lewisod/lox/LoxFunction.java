package uk.co.lewisod.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    public LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var environment = new Environment(closure);
        for (var i = 0; i < declaration.params.size(); i++) {
            var param = declaration.params.get(i);
            var argument = arguments.get(i);
            environment.define(param.lexeme, argument);
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            // Force initializers to always return this
            if (isInitializer) {
                return closure.getAt(0, "this");
            }

            return returnValue.value;
        }

        if (isInitializer) {
            return closure.getAt(0, "this");
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    public LoxFunction bind(LoxInstance instance) {
        var environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
