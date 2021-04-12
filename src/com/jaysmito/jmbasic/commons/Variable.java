package com.jaysmito.jmbasic.commons;

public class Variable {
    public String name;
    public String type;
    public String value;

    public Variable(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        return name.equals(((Variable)obj).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Variable(String name, String type) {
        this.name = name;
        this.type = type;
        switch (type){
            case "INT":{
                value = "0";
                break;
            }
            case "STRING":{
                value = "";
                break;
            }
            case "DOUBLE":{
                value = "0.0";
                break;
            }
            case "BOOLEAN":{
                value = "false";
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "Variable{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static boolean isValidName(String name){
        if(!Character.isLetter(name.charAt(0)))
            return false;
        for(int i=1;i<name.length();i++)
            if(!Character.isLetter(name.charAt(i)) && !Character.isDigit(name.charAt(i)))
                return false;
        return true;
    }

    public String getValueFormatted() {
        switch (type){
            case "STRING":{
                return "\"" + value + "\"";
            }
            default:{
                return value;
            }
        }
    }
}
