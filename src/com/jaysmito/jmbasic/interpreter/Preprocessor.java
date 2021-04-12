package com.jaysmito.jmbasic.interpreter;

import com.jaysmito.jmbasic.commons.StaticData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Preprocessor {
    private Preprocessor(){}

    public static InputStream preProcessRawCode(InputStream code) throws IOException {
        String codeText = new String(code.readAllBytes(), Charset.defaultCharset());
        Scanner codeReader = new Scanner(code);
        while (codeReader.hasNextLine()){
            String statement = codeReader.nextLine();
            if(statement.startsWith("LOAD ") || statement.startsWith("IMPORT ")){
                String parts[] =statement.split(" ");
                if(parts.length!=2)
                    return null;
                String fileName = parts[1];
                if (StaticData.workingDirectory.length() == 0)
                    return null;
                String filePath = StaticData.workingDirectory + fileName + ".jmbasiclib";
                if(Files.exists(Path.of(filePath))){
                    for(String line : Files.readAllLines(Path.of(filePath)) ){
                        codeText += line + "\n";
                    }
                }else
                    return null;
            }else
                codeText += statement + "\n";
        }
        codeText = codeText.trim().strip();
        return new ByteArrayInputStream(codeText.getBytes(Charset.defaultCharset()));
    }
}
