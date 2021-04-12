package com.jaysmito.jmbasic.commons;

public class Value {
    public String value;
    public boolean isValue;
    public Variable var;
    public String type;

    public Value(String value, boolean isValue) {
        this.value = value;
        this.isValue = isValue;
    }

    public Value(Variable var, boolean isValue) {
        this.var = var;
        this.isValue = isValue;
    }

    public Value(String value, boolean isValue, Variable var) {
        this.value = value;
        this.isValue = isValue;
        this.var = var;
    }

    public String getValue() {
        if(this.isValue)
            return value;
        return var.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
