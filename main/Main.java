package main;

import lexer.Lexer;
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
            filename = "teste5.txt"; // For testing purposes, replace with your file name
        try {
            Lexer lex = new Lexer(filename);
            Parser parser = new Parser(lex);
            parser.parse();
        } catch (ParserException | IOException e) {
            System.err.println(e.getMessage());
        }
    }
}

// find -name "*.java" -exec cat {} + > codigo_todo.java