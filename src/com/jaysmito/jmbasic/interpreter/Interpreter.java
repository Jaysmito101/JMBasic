package com.jaysmito.jmbasic.interpreter;

import com.jaysmito.jmbasic.commons.*;


import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Interpreter {
    private ArrayList<Variable> variables;
    private ArrayList<Label> labels;
    private int compareValue;

    private ArrayList<String> codeLines;
    private PrintStream errorStreamWriter, outputStreamWriter;

    private InputStream stdin;
    private int lineNumber;
    private OutputStream outputStream, errorStream;
    private int[] jm;
    public String execPath;

    private static Interpreter interpreter;

    public static Interpreter getInterpreter() throws Exception {
        if (interpreter == null)
            throw new Exception("Interpreter has not been created");
        return interpreter;
    }

    public static Interpreter createInterpreter(OutputStream outputStream, OutputStream errorStream, InputStream stdin) {
        if (interpreter == null)
            interpreter = new Interpreter(outputStream, errorStream, stdin);
        return interpreter;
    }

    private Interpreter(OutputStream outputStream, OutputStream errorStream, InputStream stdin) {
        this.compareValue = 0;
        this.stdin = stdin;
        this.lineNumber = 0;
        this.codeLines = new ArrayList<>();
        this.outputStream = outputStream;
        this.errorStream = errorStream;
        this.errorStreamWriter = new ErrorStreamPrinter(errorStream);
        this.outputStreamWriter = new PrintStream(outputStream);
        this.variables = new ArrayList<>();
        this.labels = new ArrayList<>();
        this.jm = new int[1024];
        for (int i = 0; i < 1024; i++)
            jm[i] = 0;
    }

    public boolean exec(InputStream code) throws Exception {
        code = Preprocessor.preProcessRawCode(code);
        if(code==null){
            errorStreamWriter.println("FailedToLoadModuleError at LOAD Statements");
        }
        Scanner codeReader = new Scanner(code);
        while (codeReader.hasNextLine())
            codeLines.add(codeReader.nextLine());
        if(!loadLabels()){
            return false;
        }
        // TODO: Make Functions Loader
        try {
            while (lineNumber < codeLines.size()) {
                if(!execStatement(codeLines.get(lineNumber))){
                    lineNumber++;
                    break;
                }else
                    lineNumber++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean execStatement(String statement) throws Exception{
        statement = statement.trim().strip().toUpperCase();

        //REM STATEMENT
        if(statement.startsWith("REM ")){
            return true;
        }
        //DECLARE STATEMENT
        else if (statement.startsWith("DECLARE ") || statement.startsWith("DECL ") ) {
            String[] parts = statement.split(" ");
            if (parts.length != 3) {
                errorStreamWriter.println("InvalidDeclarationError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String type = parts[1];
                String name = parts[2];
                if (! (new ArrayList<String>(Arrays.asList(Constants.DATA_TYPES)).contains(type))){
                    errorStreamWriter.println("UnrecognizedDataTypeError at line " + lineNumber);
                    return false;
                }
                if (!Variable.isValidName(name)) {
                    errorStreamWriter.println("InvalidDeclarationError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    if (variables.contains(new Variable(name, type))) {
                        errorStreamWriter.println("VariableAlreadyDeclaredError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    variables.add(new Variable(name, type));
                }
            }
        }
        //UNDECLARE STATEMENT
        else if (statement.startsWith("UNDECLARE ") || statement.startsWith("UNDEC ") ) {
            String[] parts = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidUndeclarationError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String name = parts[1];
                if (!variables.contains(new Variable(name, ""))) {
                    errorStreamWriter.println("UndeclaredVariableUndeclarationError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    removeVariable(name);
                }
            }
        }
        // UNDECLARE ALL
        else if (statement.equals("UNDECALL")){
            variables.clear();
        }
        // SET STATEMENT OR MOV STATEMENT
        else if (statement.startsWith("SET ") || statement.startsWith("MOV ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 3) {
                errorStreamWriter.println("InvalidInitializationError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String name = parts[1];
                String val = parts[2];
                if (!variables.contains(new Variable(name, ""))) {
                    errorStreamWriter.println("UndeclaredVariableInitializationError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    switch (setVariableValue(name, val)) {
                        case 0: {
                            break;
                        }
                        case -1: {
                            errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                            return false;
                        }
                        case 1: {
                            errorStreamWriter.println("INTExpectedError at line " + lineNumber + " code: " + statement);
                            return false;
                        }
                        case 2: {
                            errorStreamWriter.println("DOUBLEExpectedError at line " + lineNumber + " code: " + statement);
                            return false;
                        }
                        case 3: {
                            errorStreamWriter.println("STRINGExpectedError at line " + lineNumber + " code: " + statement);
                            return false;
                        }
                        case 4: {
                            errorStreamWriter.println("BOOLEANExpectedError at line " + lineNumber + " code: " + statement);
                            return false;
                        }
                    }
                }
            }
        }
        // JUMP STATEMENT
        else if (statement.startsWith("JUMP ") || statement.startsWith("GOTO ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidJumpError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String name = parts[1];
                if (!labels.contains(new Label(-1, name))) {
                    errorStreamWriter.println("LabelNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    for (Label label : labels) {
                        if (label.name.equals(name)) {
                            lineNumber = label.lineNumber;
                            break;
                        }
                    }
                }
            }
        }
        // EXIT STATEMENT
        else if (statement.startsWith("EXIT ")) {
            String parts[] = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidExitError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                try {
                    int statusCode = Integer.parseInt(parts[1]);
                    System.exit(statusCode);
                } catch (NumberFormatException fmne) {
                    errorStreamWriter.println("InvalidExitStatusCodeError at line " + lineNumber + " code: " + statement);
                    return false;
                }
            }
        }
        // ADD STATEMENT
        else if (statement.startsWith("ADD ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 4) {
                errorStreamWriter.println("InvalidAdditionError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                String val1 = parts[2];
                String val2 = parts[3];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    String type = result.type;
                    if(type.equals("BOOLEAN")){
                        errorStreamWriter.println("BooleanAdditionNotAllowedError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    Value value1 = resolveValue(val1, type);
                    Value value2 = resolveValue(val2, type);
                    if(value1==null || value2 == null){
                        errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    switch (type){
                        case "INT":{
                            int sum = 0;
                            if(value1.isValue)
                                sum += Integer.parseInt(value1.value);
                            else
                                sum += Integer.parseInt(value1.var.value);
                            if(value2.isValue)
                                sum += Integer.parseInt(value2.value);
                            else
                                sum += Integer.parseInt(value2.var.value);
                            result.value = "" + sum;
                            break;
                        }
                        case "DOUBLE":{
                            double sum = 0.0;
                            if(value1.isValue)
                                sum += Double.parseDouble(value1.value);
                            else
                                sum += Double.parseDouble(value1.var.value);
                            if(value2.isValue)
                                sum += Double.parseDouble(value2.value);
                            else
                                sum += Double.parseDouble(value2.var.value);
                            result.value = "" + sum;
                            break;
                        }
                        case "STRING":{
                            String sum = "";
                            if(value1.isValue)
                                sum += (value1.value);
                            else
                                sum += (value1.var.value);
                            if(value2.isValue)
                                sum += (value2.value);
                            else
                                sum += (value2.var.value);
                            result.value = "" + sum;
                            break;
                        }

                    }
                }
            }
        }
        // SUB STATEMENT
        else if (statement.startsWith("SUB ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 4) {
                errorStreamWriter.println("InvalidSubtractionError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                String val1 = parts[2];
                String val2 = parts[3];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    String type = result.type;
                    if(type.equals("BOOLEAN")){
                        errorStreamWriter.println("BooleanSubtractionNotAllowedError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    Value value1 = resolveValue(val1, type);
                    Value value2 = resolveValue(val2, type);
                    if(value1==null || value2 == null){
                        errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    switch (type){
                        case "INT":{
                            int sum = 0;
                            if(value1.isValue)
                                sum += Integer.parseInt(value1.value);
                            else
                                sum += Integer.parseInt(value1.var.value);
                            if(value2.isValue)
                                sum -= Integer.parseInt(value2.value);
                            else
                                sum -= Integer.parseInt(value2.var.value);
                            result.value = "" + sum;
                            break;
                        }
                        case "DOUBLE":{
                            double sum = 0.0;
                            if(value1.isValue)
                                sum += Double.parseDouble(value1.value);
                            else
                                sum += Double.parseDouble(value1.var.value);
                            if(value2.isValue)
                                sum -= Double.parseDouble(value2.value);
                            else
                                sum -= Double.parseDouble(value2.var.value);
                            result.value = "" + sum;
                            break;
                        }
                        case "STRING":{
                            String v1 = "";
                            String v2 = "";
                            if(value1.isValue)
                                v1= (value1.value);
                            else
                                v1= (value1.var.value);
                            if(value2.isValue)
                                v2= (value2.value);
                            else
                                v2= (value2.var.value);
                            for(int i=0;i<v2.length();i++){
                                v1 = v1.replace(String.valueOf(v1.charAt(i)), "");
                            }
                            result.value = v1;
                            break;
                        }

                    }
                }
            }
        }
        // MUL STATEMENT
        else if (statement.startsWith("MULTIPLY ") || statement.startsWith("MUL ") || statement.startsWith("IMUL ") || statement.startsWith("MULT ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 4) {
                errorStreamWriter.println("InvalidMultiplicationError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                String val1 = parts[2];
                String val2 = parts[3];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    String type = result.type;
                    if(type.equals("BOOLEAN")){
                        errorStreamWriter.println("BooleanMultiplicationNotAllowedError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    switch (type){
                        case "INT":{
                            Value value1 = resolveValue(val1, "INT");
                            Value value2 = resolveValue(val2, "INT");
                            if(value1==null || value2 == null){
                                errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            int sum = 1;
                            if(value1.isValue)
                                sum *= Integer.parseInt(value1.value);
                            else
                                sum *= Integer.parseInt(value1.var.value);
                            if(value2.isValue)
                                sum *= Integer.parseInt(value2.value);
                            else
                                sum *= Integer.parseInt(value2.var.value);
                            result.value = "" + sum;
                            break;
                        }
                        case "DOUBLE":{
                            Value value1 = resolveValue(val1, "DOUBLE");
                            Value value2 = resolveValue(val2, "DOUBLE");
                            if(value1==null || value2 == null){
                                errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            double sum = 1.0;
                            if(value1.isValue)
                                sum *= Double.parseDouble(value1.value);
                            else
                                sum *= Double.parseDouble(value1.var.value);
                            if(value2.isValue)
                                sum *= Double.parseDouble(value2.value);
                            else
                                sum *= Double.parseDouble(value2.var.value);
                            result.value = "" + sum;
                            break;
                        }
                        case "STRING":{
                            Value value1 = resolveValue(val1, "STRING");
                            Value value2 = resolveValue(val2, "INT");
                            if(value1==null || value2 == null){
                                errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            String string = "";
                            int n = 0;
                            if(value1.isValue)
                                string= (value1.value);
                            else
                                string= (value1.var.value);
                            if(value2.isValue)
                                n= Integer.parseInt(value2.value);
                            else
                                n= Integer.parseInt(value2.var.value);
                            String fin = "";
                            for(int i=0;i<n;i++)
                                fin += string;
                            result.value = fin;
                            break;
                        }

                    }
                }
            }
        }
        // DIV STATEMENT
        else if (statement.startsWith("DIVIDE ") || statement.startsWith("DIV ") || statement.startsWith("IDIV ") || statement.startsWith("DIVI ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 4) {
                errorStreamWriter.println("InvalidDivisionError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                String val1 = parts[2];
                String val2 = parts[3];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    String type = result.type;
                    if(type.equals("BOOLEAN")){
                        errorStreamWriter.println("BooleanDivisionNotAllowedError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    if(type.equals("STRING")){
                        errorStreamWriter.println("StringDivisionNotAllowedError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    switch (type){
                        case "INT":{
                            Value value1 = resolveValue(val1, "INT");
                            Value value2 = resolveValue(val2, "INT");
                            if(value1==null || value2 == null){
                                errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            int sum = 1;
                            if(value1.isValue)
                                sum *= Integer.parseInt(value1.value);
                            else
                                sum *= Integer.parseInt(value1.var.value);
                            try {
                                if (value2.isValue)
                                    sum /= Integer.parseInt(value2.value);
                                else
                                    sum /= Integer.parseInt(value2.var.value);
                            }catch (ArithmeticException ae){
                                errorStreamWriter.println("DivisionByZeroError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            result.value = "" + sum;
                            break;
                        }
                        case "DOUBLE":{
                            Value value1 = resolveValue(val1, "DOUBLE");
                            Value value2 = resolveValue(val2, "DOUBLE");
                            if(value1==null || value2 == null){
                                errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            double sum = 1.0;
                            if(value1.isValue)
                                sum *= Double.parseDouble(value1.value);
                            else
                                sum *= Double.parseDouble(value1.var.value);
                            try {
                                if (value2.isValue)
                                    sum /= Double.parseDouble(value2.value);
                                else
                                    sum /= Double.parseDouble(value2.var.value);
                            }catch (ArithmeticException ae){
                                errorStreamWriter.println("DivisionByZeroError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            result.value = "" + sum;
                            break;
                        }
                    }
                }
            }
        }
        // MOD STATEMENT
        else if (statement.startsWith("MODULUS ") || statement.startsWith("MOD ") || statement.startsWith("IMOD ") || statement.startsWith("MODULO ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 4) {
                errorStreamWriter.println("InvalidModulusError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                String val1 = parts[2];
                String val2 = parts[3];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    String type = result.type;
                    if(type.equals("BOOLEAN")){
                        errorStreamWriter.println("BooleanModulusNotAllowedError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    if(type.equals("STRING")){
                        errorStreamWriter.println("StringModulusNotAllowedError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                    switch (type){
                        case "INT":{
                            Value value1 = resolveValue(val1, "INT");
                            Value value2 = resolveValue(val2, "INT");
                            if(value1==null || value2 == null){
                                errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            int sum = 1;
                            if(value1.isValue)
                                sum *= Integer.parseInt(value1.value);
                            else
                                sum *= Integer.parseInt(value1.var.value);
                            try {
                                if (value2.isValue)
                                    sum %= Integer.parseInt(value2.value);
                                else
                                    sum %= Integer.parseInt(value2.var.value);
                            }catch (ArithmeticException ae){
                                errorStreamWriter.println("DivisionByZeroError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            result.value = "" + sum;
                            break;
                        }
                        case "DOUBLE":{
                            Value value1 = resolveValue(val1, "DOUBLE");
                            Value value2 = resolveValue(val2, "DOUBLE");
                            if(value1==null || value2 == null){
                                errorStreamWriter.println("RuntimeError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            double sum = 1.0;
                            if(value1.isValue)
                                sum *= Double.parseDouble(value1.value);
                            else
                                sum *= Double.parseDouble(value1.var.value);
                            try {
                                if (value2.isValue)
                                    sum %= Double.parseDouble(value2.value);
                                else
                                    sum %= Double.parseDouble(value2.var.value);
                            }catch (ArithmeticException ae){
                                errorStreamWriter.println("DivisionByZeroError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                            result.value = "" + sum;
                            break;
                        }
                    }
                }
            }
        }
        // CONVERTTYPE STATEMENT
        else if (statement.startsWith("CONVERTTYPE ") ||statement.startsWith("CVT ") || statement.startsWith("CONV ")){
            String[] parts = statement.split(" ");
            if (parts.length != 3) {
                errorStreamWriter.println("InvalidTypeConversionError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String name = parts[1];
                String toType = parts[2];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(name)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    if(toType.equals("STRING")){
                        result.type = "STRING";
                    }
                    else if(!result.type.equals(toType)){
                        if(result.type.equals("INT")){
                            if(toType.equals("DOUBLE")){
                                result.type = "DOUBLE";
                                result.value = String.valueOf(Double.parseDouble(result.value));
                            }
                            else{
                                errorStreamWriter.println("TypeConversionNotAllowedError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                        }
                        else if (result.type.equals("DOUBLE")){
                            if(toType.equals("INT")){
                                result.type = "INT";
                                result.value = String.valueOf((int)(Double.parseDouble(result.value)));
                            }
                            else{
                                errorStreamWriter.println("TypeConversionNotAllowedError at line " + lineNumber + " code: " + statement);
                                return false;
                            }
                        }else{
                            errorStreamWriter.println("TypeConversionNotAllowedError at line " + lineNumber + " code: " + statement);
                            return false;
                        }
                    }
                }
            }
        }
        // WHATTYPE
        else if (statement.startsWith("WHATTYPE ") ||statement.startsWith("WHT ") || statement.startsWith("GETTYPE ")){
            String[] parts = statement.split(" ");
            if (parts.length != 3) {
                errorStreamWriter.println("InvalidTypeFetchError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String dstVarName = parts[1];
                String srcVarName = parts[2];
                Variable srcVar = null;
                Variable dstVar = null;
                for (Variable var : variables) {
                    if (var.name.equals(srcVarName)) {
                        srcVar = var;
                    }
                    if (var.name.equals(dstVarName)) {
                        dstVar = var;
                    }
                }
                if (srcVar == null || dstVar == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    if(!dstVar.type.equals("STRING")){
                        errorStreamWriter.println("CannotFetchTypeToNonStringVariableError at line " + lineNumber + " code: " + statement);
                        return false;
                    }else{
                        dstVar.value = srcVar.type;
                    }
                }
            }
        }
        // PRINT
        else if (statement.startsWith("PRINT ") ||statement.startsWith("DISP ") || statement.startsWith("DISPLAY ")){
            String[] parts = statement.split(" ");
            if (parts.length != 2 && statement.indexOf("\"")==-1) {
                errorStreamWriter.println("InvalidPrintError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String val = parts[1];
                if(statement.indexOf("\"")>0 && parts.length > 2){
                    for(int i=2;i<parts.length;i++)
                        val += " " + parts[i];
                }
                Value value = null;
                for(int i=0;i< Constants.DATA_TYPES.length;i++)
                    if ((value = resolveValue(val, Constants.DATA_TYPES[i])) != null)
                        break;
                if (value == null) {
                    errorStreamWriter.println("UnknownPrintValueError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    outputStreamWriter.print(JBasicUtils.fixEscapeSequences(value.getValue()));
                }
            }
        }
        // PRINTLN
        else if (statement.startsWith("PRINTLN ") ||statement.startsWith("DISPLN ") || statement.startsWith("DISPLAYLN ")){
            String[] parts = statement.split(" ");
            if (parts.length != 2 && statement.indexOf("\"")==-1) {
                errorStreamWriter.println("InvalidPrintError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String val = parts[1];
                if(statement.indexOf("\"")>0 && parts.length > 2){
                    for(int i=2;i<parts.length;i++)
                        val += " " + parts[i];
                }
                Value value = null;
                for(int i=0;i< Constants.DATA_TYPES.length;i++)
                    if ((value = resolveValue(val, Constants.DATA_TYPES[i])) != null)
                        break;
                if (value == null) {
                    errorStreamWriter.println("UnknownPrintValueError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    outputStreamWriter.println(JBasicUtils.fixEscapeSequences(value.getValue()));
                }
            }
        }
        // COMPARE STATEMENT
        else if (statement.startsWith("COMPARE ") ||statement.startsWith("CMP ") || statement.startsWith("COMPARETO ")){
            String[] parts = statement.split(" ");
            if (parts.length != 3) {
                errorStreamWriter.println("InvalidComparisionError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String val1 = parts[1];
                String val2 = parts[2];
                Value value1 = null;
                Value value2 = null;
                for(int i=0;i< Constants.DATA_TYPES.length;i++)
                    if ((value1 = resolveValue(val1, Constants.DATA_TYPES[i])) != null) {
                        value1.type = Constants.DATA_TYPES[i];
                        break;
                    }
                for(int i=0;i< Constants.DATA_TYPES.length;i++)
                    if ((value2 = resolveValue(val2, Constants.DATA_TYPES[i])) != null) {
                        value2.type = Constants.DATA_TYPES[i];
                        break;
                    }
                if (value1 == null || value2 == null) {
                    errorStreamWriter.println("UnknownComparisonValueError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    if(value1.type.equals(value2.type)){
                        switch (value1.type){
                            case "INT":{
                                this.compareValue = JBasicUtils.signum(Integer.parseInt(value1.getValue()) - Integer.parseInt(value2.getValue()));
                                break;
                            }
                            case "DOUBLE":{
                                this.compareValue = JBasicUtils.signum(Double.parseDouble(value1.getValue()) - Double.parseDouble(value2.getValue()));
                                break;
                            }
                            case "STRING":{
                                this.compareValue = JBasicUtils.signum(value1.getValue().compareTo(value2.getValue()));
                            }
                        }
                    }else {
                        errorStreamWriter.println("InvalidComparisonValueError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                }
            }
        }
        // JUMPZ
        else if (statement.startsWith("JUMPZ ") || statement.startsWith("JMZ ")) {
            if(this.compareValue!=0)
                return true;
            String[] parts = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidJumpError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String name = parts[1];
                if (!labels.contains(new Label(-1, name))) {
                    errorStreamWriter.println("LabelNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    for (Label label : labels) {
                        if (label.name.equals(name)) {
                            this.lineNumber = label.lineNumber - 1;
                            break;
                        }
                    }
                }
            }
        }
        // JUMPN
        else if (statement.startsWith("JUMPN ") || statement.startsWith("JMN ")) {
            if(!(this.compareValue<0))
                return true;
            String[] parts = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidJumpError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String name = parts[1];
                if (!labels.contains(new Label(-1, name))) {
                    errorStreamWriter.println("LabelNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    for (Label label : labels) {
                        if (label.name.equals(name)) {
                            lineNumber = label.lineNumber;
                            break;
                        }
                    }
                }
            }
        }
        // JUMPP
        else if (statement.startsWith("JUMPP ") || statement.startsWith("JMP ")) {
            if(!(this.compareValue>0))
                return true;
            String[] parts = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidJumpError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String name = parts[1];
                if (!labels.contains(new Label(-1, name))) {
                    errorStreamWriter.println("LabelNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    for (Label label : labels) {
                        if (label.name.equals(name)) {
                            lineNumber = label.lineNumber;
                            break;
                        }
                    }
                }
            }
        }
        // INCREMENT STATEMENT
        else if (statement.startsWith("INCREMENT ") || statement.startsWith("INC ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidIncrementationError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    if(result.type.equals("INT")){
                        result.value = String.valueOf( Integer.parseInt(result.value) + 1);
                    }
                    else if(result.type.equals("DOUBLE")){
                        result.value = String.valueOf( Double.parseDouble(result.value) + 1);
                    }
                    else{
                        errorStreamWriter.println("IncrementNotAllowedOnGivenTypeError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                }
            }
        }
        // DECREMENT STATEMENT
        else if (statement.startsWith("DECREMENT ") || statement.startsWith("DEC ")) {
            String[] parts = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidDecrementationError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    if(result.type.equals("INT")){
                        result.value = String.valueOf( Integer.parseInt(result.value) - 1);
                    }
                    else if(result.type.equals("DOUBLE")){
                        result.value = String.valueOf( Double.parseDouble(result.value) - 1);
                    }
                    else{
                        errorStreamWriter.println("DecrementNotAllowedOnGivenTypeError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                }
            }
        }
        // INPUT STATEMENTS
        else if (statement.startsWith("INPUT ")){
            String parts[] = statement.split(" ");
            if (parts.length != 2) {
                errorStreamWriter.println("InvalidInputError at line " + lineNumber + " code: " + statement);
                return false;
            } else {
                String varname = parts[1];
                Variable result = null;
                for (Variable var : variables) {
                    if (var.name.equals(varname)) {
                        result = var;
                    }
                }
                if (result == null) {
                    errorStreamWriter.println("VariableNotFoundError at line " + lineNumber + " code: " + statement);
                    return false;
                } else {
                    String type = result.type;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
                    String input = reader.readLine().toUpperCase();
                    try {
                        switch (type){
                            case "STRING":{
                                result.value = input;
                                break;
                            }
                            case  "BOOLEAN":{
                                if(input.equals("TRUE") || input.equals("FALSE")){
                                    result.value = input;
                                }else{
                                    errorStreamWriter.println("BooleanInputExpectedError at line " + lineNumber + " code: " + statement);
                                    return false;
                                }
                                break;
                            }
                            case "INT":{
                                try {
                                    result.value = String.valueOf(Integer.parseInt(input));
                                }catch (NumberFormatException nmfe){
                                    errorStreamWriter.println("IntInputExpectedError at line " + lineNumber + " code: " + statement);
                                    return false;
                                }
                                break;
                            }
                            case "DOUBLE":{
                                try {
                                    result.value = String.valueOf(Double.parseDouble(input));
                                }catch (NumberFormatException nmfe){
                                    errorStreamWriter.println("DoubleInputExpectedError at line " + lineNumber + " code: " + statement);
                                    return false;
                                }
                                break;
                            }
                        }
                    }catch (Exception ex){
                        errorStreamWriter.println("UnknownError at line " + lineNumber + " code: " + statement);
                        return false;
                    }
                }
            }
        }
        // PRINT COMPARE VALUE
        else if (statement.equals("CMPVAL")){
            outputStreamWriter.println("CMPVAL = " + compareValue);
        }
        // CLOSE STDIN
        else if (statement.equals("CLOSESTDIN")){
            stdin.close();
        }
        // VERSION STATEMENT
        else if (statement.equals("VERSION")){
            outputStreamWriter.println(Constants.VERSION);
        }
        // AUTHOR STATEMENT
        else if (statement.equals("AUTHOR") || statement.equals("DEVELOPER")){
            outputStreamWriter.println("This Programming Languages has been\n" +
                    "Developed by Jaysmito Mukherjee\n" +
                    "For More information visit : \n" +
                    "https://www.youtube.com/channel/UCvVUCzb12l-3FM740TdD-Vw");
        }
        // SHELL STATEMENT
        else if (statement.startsWith("SHELL ") || statement.startsWith("CMD ") || statement.startsWith("EXEC ")){
            try {
                statement = codeLines.get(lineNumber);
                String command = JBasicUtils.escape(statement.substring(6)).replace("  ", " ");
                Process process = Runtime.getRuntime().exec(command.split(" "));
                Scanner outputScanner = new Scanner(process.getInputStream());
                Scanner errorScanner = new Scanner(process.getErrorStream());
                while (process.isAlive());
                while (outputScanner.hasNextLine()) {
                    outputStreamWriter.println(outputScanner.nextLine());
                }
                while (errorScanner.hasNextLine()) {
                    errorStreamWriter.println(errorScanner.nextLine());
                }
            }catch (Exception ex){
                errorStreamWriter.println("ShellExecutionError at line " + lineNumber + " code: " + statement);
            }
            statement = statement.toUpperCase();
        }
        // SHELLEX STATEMENT
        else if (statement.startsWith("SHELLEX ") || statement.startsWith("CMDEX ") || statement.startsWith("EXECEX ")){
            try {
                statement = codeLines.get(lineNumber);
                String command = JBasicUtils.escape(statement.substring(6)).replace("  ", " ");
                statement = statement.toUpperCase();
                Process process = Runtime.getRuntime().exec(command.split(" "));
                Scanner outputScanner = new Scanner(process.getInputStream());
                Scanner errorScanner = new Scanner(process.getErrorStream());
                while (process.isAlive());
                int exitVal = process.exitValue();
                while (outputScanner.hasNextLine()) {
                    outputStreamWriter.println(outputScanner.nextLine());
                }
                while (errorScanner.hasNextLine()) {
                    errorStreamWriter.println(errorScanner.nextLine());
                }
                outputStreamWriter.println("Exit Value : " + exitVal);
            }catch (Exception ex){
                errorStreamWriter.println("ShellExecutionError at line " + lineNumber + " code: " + statement);
            }
            statement = statement.toUpperCase();
        }
        // EXIT STATEMENT
        else if (statement.equals("EXIT")){
            System.exit(0);
        }
        // PRINT VARIABLE VALUE IF EXISTS
        else{
            boolean tmpFlag = false;
            for (Variable variable:variables){
                if(variable.name.equals(statement.strip().trim())){
                    outputStreamWriter.println(statement + " = " + variable.getValueFormatted());
                    tmpFlag = true;
                }
            }
            if(!tmpFlag){
                errorStreamWriter.println("UnrecognizedStatementError at line " + lineNumber);
            }
        }
        return true;
    }

    private boolean loadLabels() {
        this.labels.clear();
        int lNumber = 0;
        while (lNumber < codeLines.size()){
            String statement = codeLines.get(lNumber).toUpperCase().strip().trim();
            lNumber++;
            // LABEL Statement
            if (statement.startsWith("LABEL ")) {
                String[] parts = statement.split(" ");
                if (parts.length != 2) {
                    errorStreamWriter.println("InvalidLabelDeclarationError at line " + lNumber + " code: " + statement);
                    return false;
                } else {
                    String name = parts[1];
                    if (!Variable.isValidName(name)) {
                        errorStreamWriter.println("InvalidLabelDeclarationError at line " + lNumber + " code: " + statement);
                        return false;
                    } else {
                        if (labels.contains(new Label(lNumber, name))) {
                            errorStreamWriter.println("LabelAlreadyDeclaredError at line " + lNumber + " code: " + statement);
                            return false;
                        }
                        labels.add(new Label(lNumber, name));
                    }
                }
            }
        }
        return true;
    }

    public Value resolveValue(String val, String type) {
        Variable var = null;
        for (Variable variable : variables) {
            if (variable.name.equals(val)) {
                var = variable;
                break;
            }
        }
        if (var == null) {
            switch (type) {
                case "INT": {
                    try {
                        return new Value("" +Integer.parseInt(val), true);
                    } catch (NumberFormatException nmfe) {
                        return null;
                    }
                }
                case "DOUBLE": {
                    try {
                        return new Value("" +Double.parseDouble(val), true);
                    } catch (NumberFormatException nmfe) {
                        return null;
                    }
                }
                case "BOOLEAN": {
                    if (val.equals("TRUE") || val.equals("FALSE")) {
                        return new Value(val, true);
                    } else {
                        return null;
                    }
                }
                case "STRING": {
                    if (val.indexOf("\"") >= 0) {
                        return new Value(val.replace("\"", ""), true);
                    }else {
                        return null;
                    }
                }
                default: {
                    return null;
                }
            }
        } else if (!var.type.equals(type)){
            return null;
        }
        else if (var != null) {
            return new Value(var, false);
        }
        return null;
    }

    public int setVariableValue(String name, String val) {
        Variable var = null;
        for (int i = 0; i < variables.size(); i++) {
            if (variables.get(i).name.equals(name)) {
                var = variables.get(i);
                break;
            }
        }
        val = val.strip().trim();

        switch (var.type) {
            case "INT": {
                try {
                    var.value = "" + Integer.parseInt(val);
                } catch (NumberFormatException nmfe) {
                    if (val.indexOf("\"") >= 0) {
                        return 1;
                    } else {
                        for (Variable variable : variables) {
                            if (variable.name.equals(val)) {
                                if (variable.type.equals(var.type)) {
                                    var.value = new String(variable.value);
                                } else {
                                    return 1;
                                }
                            }
                        }
                    }
                }
                break;
            }
            case "STRING": {
                if (JBasicUtils.containsRawString(val)) {
                    var.value = val.replace("\"", "");
                } else {
                    for (Variable variable : variables) {
                        if (variable.name.equals(val)) {
                            if (variable.type.equals(var.type)) {
                                var.value = new String(variable.value);
                            } else {
                                return 3;
                            }
                        }
                    }
                }
                break;
            }
            case "DOUBLE": {
                try {
                    var.value = "" + Double.parseDouble(val);
                } catch (NumberFormatException nmfe) {
                    if (val.indexOf("\"") >= 0) {
                        return 2;
                    } else {
                        for (Variable variable : variables) {
                            if (variable.name.equals(val)) {
                                if (variable.type.equals(var.type)) {
                                    var.value = new String(variable.value);
                                } else {
                                    return 2;
                                }
                            }
                        }
                    }
                }
                break;
            }
            case "BOOLEAN": {
                if (val.equals("TRUE") || val.equals("FALSE")) {
                    var.value = val;
                } else {
                    for (Variable variable : variables) {
                        if (variable.name.equals(val)) {
                            if (variable.type.equals(var.type)) {
                                var.value = new String(variable.value);
                            } else {
                                return 4;
                            }
                        }
                    }
                }
                break;
            }
        }
        return 0;
    }

    public void removeVariable(String name) {
        for (int i = 0; i < variables.size(); i++) {
            if (variables.get(i).name.equals(name)) {
                variables.remove(i);
            }
        }
    }
}

