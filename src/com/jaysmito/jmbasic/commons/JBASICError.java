package com.jaysmito.jmbasic.commons;

import java.io.IOException;

public class JBASICError extends IOException {

    public JBASICError(){
        super("JBASICError has  occurred");
    }

    public JBASICError(String msg){
        super(msg);
    }
}
