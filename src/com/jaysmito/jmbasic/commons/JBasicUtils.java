package com.jaysmito.jmbasic.commons;

public class JBasicUtils {

    private JBasicUtils(){}

    public static int signum(double x){
        if (x > 0)
            return 1;
        if (x == 0)
            return 0;
        if (x < 0)
            return -1;
        return 0;
    }

    public static String fixEscapeSequences(String value) {
        return value.replace("\\N", "\n");
    }

    public static boolean containsRawString(String val) {
        val = val.strip().trim();
        if(val.charAt(0)!='\"' || val.charAt(val.length()-1)!= '\"')
            return false;
        return true;
    }

    public static String escape(String s){
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\'", "\\'")
                .replace("\"", "\\\"");
    }
}
