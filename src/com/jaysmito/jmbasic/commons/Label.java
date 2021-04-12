package com.jaysmito.jmbasic.commons;

public class Label {
    public int lineNumber;
    public String name;

    public Label(int lineNumber, String name) {
        this.lineNumber = lineNumber;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return name.equals(((Label)obj).name);
    }

    @Override
    public String toString() {
        return "Label{" +
                "lineNumber=" + lineNumber +
                ", name='" + name + '\'' +
                '}';
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
