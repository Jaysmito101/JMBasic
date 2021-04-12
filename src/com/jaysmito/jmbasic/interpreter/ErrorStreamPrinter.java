package com.jaysmito.jmbasic.interpreter;

import com.jaysmito.jmbasic.commons.Constants;

import java.io.OutputStream;
import java.io.PrintStream;

public class ErrorStreamPrinter extends PrintStream {

    public ErrorStreamPrinter(OutputStream out) {
        super(out, true);
    }

    @Override
    public void print(String s) {
        super.print(Constants.ANSI_RED);
        super.print(s);
        super.print(Constants.ANSI_RESET);
    }

    @Override
    public void println(String s) {
        super.print(Constants.ANSI_RED);
        super.print(s);
        super.println(Constants.ANSI_RESET);
    }
}
