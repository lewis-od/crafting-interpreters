package uk.co.lewisod.lox;

import java.util.List;

public class LoxClass implements LoxCallable {
    final String name;

    public LoxClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new LoxInstance(this);
    }

    @Override
    public int arity() {
        return 0;
    }
}
