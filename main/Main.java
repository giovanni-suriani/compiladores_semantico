package main;

import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.ParserException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        /*
         * if (args.length != 1) {
         * System.err.println("Uso: java main.Main <arquivo-fonte>");
         * System.exit(1);
         * }
         */
        String filename;
        if (args.length > 0)
            filename = args[0];
        else
            // For testing purposes, replace with your file name
            // filename = "teste2.txt";
            // For testing purposes, replace with your file name
            filename = "teste3.txt"; // For testing purposes, replace with your file name
        try {
            Lexer lex = new Lexer(filename);
            Token t;
            while ((t = lex.scan()) != null) {
                System.out.println("TOKEN: " + t);
            }
            // Parser parser = new Parser(lex);
            // parser.parse();
        } catch (ParserException | IOException e) {
            System.err.println(e.getMessage());
        }
    }
}

// find -name "*.java" -exec cat {} + > codigo_todo.java
// for dir in raw_testes primeira_modificada; do     for i in {1..5}; do         /usr/bin/env /usr/lib/jvm/java-17-openjdk-amd64/bin/java             -XX:+ShowCodeDetailsInExceptionMessages             -cp /home/gi/.config/Code/User/workspaceStorage/2e3b82d01d3e7ac12c7df6fd79e9be5f/redhat.java/jdt_ws/Trabalho_Pratico_3935c5a7/bin             main.Main $dir/teste$i.txt 2> $dir/resultados/$dir-erro$i;     done; done