import com.jaysmito.jmbasic.interpreter.Interpreter;

import java.io.*;
import java.nio.charset.Charset;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Interpreter interpreter = Interpreter.createInterpreter(System.out, System.err, System.in);
            Scanner sc = new Scanner(System.in);
            boolean flag = true;
            while (true) {
                if (flag) {
                    System.out.print(">> ");
                    flag = false;
                } else
                    System.out.print("\n>> ");
                String code = sc.nextLine();
                interpreter.exec(new ByteArrayInputStream(code.getBytes(Charset.defaultCharset())));
            }
        }catch (Exception ex){
            System.err.println("Closing Interpreter due to Internal Error");
        }
    }
}
