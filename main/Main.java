package main;

import lexer.Lexer;
import parser.Parser;
import parser.ParserException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java main.Main <arquivo-fonte>");
            System.exit(1);
        }

        try {
            Lexer  lex    = new Lexer(args[0]);
            Parser parser = new Parser(lex);
            parser.parse();
        } catch (ParserException | IOException e) {
            System.err.println(e.getMessage());
        }
    }
}

// find -name "*.java" -exec cat {} + > codigo_todo.java