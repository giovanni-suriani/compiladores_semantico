package lexer;

public class CharConst extends Token {
    
    public final char valor;

    public CharConst(char v) {
        super(Tag.CHAR_CONST);
        valor = v;
    }

    public String toString() {
        return "" + valor;
    }
}
package lexer;

import java.io.*;
import java.util.*;

// A classe Lexer mapeia cadeias em palavras
public class Lexer {
    public static int line = 1; // contador de linhas
    private char ch = ' '; // caractere lido do arquivo
    private FileReader file;

    private Hashtable<String, Word> words = new Hashtable<>();

    /* Metodo para inserir palavras reservadas na HashTable */
    private void reserve(Word w) {
        words.put(w.getLexeme(), w); // lexema é a chave para entrada na HashTable
    }

    /* Metodo construtor */
    public Lexer(String fileName) throws FileNotFoundException {
        try {
            file = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado");
            throw e;
        }

        // Palavras reservadas
        reserve(new Word("if", Tag.IF));
        reserve(new Word("program", Tag.PROGRAM));
        reserve(new Word("begin", Tag.BEGIN));
        reserve(new Word("end", Tag.END));
        reserve(new Word("type", Tag.TYPE));
        reserve(new Word("int", Tag.INT));
        reserve(new Word("float", Tag.FLOAT));
        reserve(new Word("char", Tag.CHAR));
        reserve(new Word("bool", Tag.BOOL));
        reserve(new Word("then", Tag.THEN));
        reserve(new Word("else", Tag.ELSE));
        reserve(new Word("while", Tag.WHILE));
        reserve(new Word("do", Tag.DO));
        reserve(new Word("repeat", Tag.REPEAT));
        reserve(new Word("until", Tag.UNTIL));
        reserve(new Word("in", Tag.IN));
        reserve(new Word("out", Tag.OUT));
    }

    /* Le o proximo caractere do arquivo */
    private void readch() throws IOException {
        int r = file.read();
        ch = (r == -1) ? (char) -1 : (char) r;
    }

    private Token single(int tag) throws IOException {
        Token t = new Token(tag); // cria o token
        readch(); // avança para o próximo caractere
        return t;
    }

    /* Le o proximo caractere do arquivo e verifica se eh igual a c */
    private boolean readch(char c) throws IOException {
        readch();
        if (ch != c)
            return false;
        ch = ' ';
        return true;
    }

    /*
     * ------------------------------------------------------------------
     * lexer/Lexer.java – trecho refatorado
     * ------------------------------------------------------------------
     */

    /** Devolve o próximo token da entrada ou null no EOF. */
    public Token scan() throws IOException {

        /* ---------- ignora espaços, tabs e quebras de linha ---------- */
        for (;; readch()) {
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\b')
                continue;
            else if (ch == '\n')
                line++;
            else
                break;
        }

        /* ---------- comentários ---------- */
        if (ch == '{') { // bloco { ... }
            do {
                readch();
                if (ch == (char) -1)
                    throw new IOException("Erro léxico: comentário de bloco não fechado");
            } while (ch != '}');
            readch(); // consome ‘}’
            return scan(); // reinicia o processo
        }

        if (ch == '%') { // linha %
            do {
                readch();
            } while (ch != '\n' && ch != (char) -1);
            line++;
            readch(); // consome '\n'
            return scan();
        }

        /* ---------- operadores e delimitadores ---------- */
        switch (ch) {

            /* compostos → usa readch(c) que já avança dentro dele */
            case '&':
                if (readch('&'))
                    return Word.and;
                return single('&'); // devolve ‘&’
            case '|':
                if (readch('|'))
                    return Word.or;
                return single('|'); // devolve ‘|’
            case '=':
                if (readch('='))
                    return Word.eq;
                return single(Tag.ASSIGN); // ‘=’
            case '!':
                if (readch('='))
                    return Word.ne;
                return single('!'); // ‘!’
            case '<':
                if (readch('='))
                    return Word.le;
                return single(Tag.LT); // ‘<’
            case '>':
                if (readch('='))
                    return Word.ge;
                return single(Tag.GT); // ‘>’

            /* 1 caractere (usa helper single) */
            case '+':
                return single(Tag.PLUS);
            case '-':
                return single(Tag.MINUS);
            case '*':
                return single(Tag.TIMES);
            case '/':
                return single(Tag.DIV);
            case ';':
                return single(Tag.SEMICOLON);
            case ':':
                return single(Tag.COLON);
            case ',':
                return single(Tag.COMMA);
            case '(':
                return single(Tag.LPAREN);
            case ')':
                return single(Tag.RPAREN);
        }

        /* ---------- constantes de caractere ---------- */
        if (ch == '\'') {
            readch(); // entra no conteúdo
            char valor = ch;
            readch();
            if (ch == '\'') {
                readch(); // consome a aspa final
                return new CharConst(valor);
            }
            throw new IOException("Erro léxico: caractere mal formado");
        }

        /* ---------- literais de string ---------- */
        if (ch == '"') {
            StringBuilder sb = new StringBuilder();
            readch(); // entra no conteúdo
            while (ch != '"' && ch != '\n' && ch != (char) -1) {
                sb.append(ch);
                readch();
            }
            if (ch == '"') {
                readch(); // consome a aspa final
                return new Literal(sb.toString());
            }
            throw new IOException("Erro léxico: string mal formada");
        }

        /* ---------- números ---------- */
        if (Character.isDigit(ch)) {
            int value = 0;
            do {
                value = 10 * value + Character.digit(ch, 10);
                readch();
            } while (Character.isDigit(ch));

            if (ch != '.')
                return new Num(value); // inteiro

            /* ponto flutuante */
            float x = value, d = 10;
            readch(); // consome '.'
            if (!Character.isDigit(ch))
                throw new IOException("Erro léxico: ponto sem dígitos em constante float");

            while (Character.isDigit(ch)) {
                x += Character.digit(ch, 10) / d;
                d *= 10;
                readch();
            }
            return new Real(x);
        }

        /* ---------- identificadores / palavras-chave ---------- */
        if (Character.isLetter(ch) || ch == '_') {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(ch);
                readch();
            } while (Character.isLetterOrDigit(ch) || ch == '_');

            String s = sb.toString().toLowerCase();
            Word w = words.get(s);
            if (w != null)
                return w; // palavra reservada

            w = new Word(s, Tag.ID); // novo identificador
            words.put(s, w);
            return w;
        }

        /* ---------- fim de arquivo ---------- */
        if (ch == (char) -1)
            return null;

        /* ---------- caractere isolado desconhecido ---------- */
        Token t = new Token(ch);
        readch(); // avança para evitar loop
        return t;
    }

}
package lexer;

public class Literal extends Token {
    
    public final String valor;

    public Literal(String v) {
        super(Tag.LITERAL);
        valor = v;
    }

    public String toString() {
        return "" + valor;
    }
}
package lexer;

public class Num extends Token {

    public final int valor;

    public Num(int v) {
        super(Tag.NUM);
        valor = v;
    }

    public String toString() {
        return "" + valor;
    }
}
package lexer;

// A classe Real eh para numeros de ponto flutuante
public class Real extends Token {

    public final float valor;

    public Real(float v) {
        super(Tag.REAL);
        valor = v;
    }

    public String toString() {
        return "" + valor;
    }
}
package lexer;

// A classe Tag define constantes para os tokens da linguagem
public class Tag {

    // Palavras reservadas
    public static final int 
        PROGRAM = 256,
        BEGIN = 257,
        END = 258,
        TYPE = 259,
        INT = 260,
        FLOAT = 261,
        CHAR = 262,
        BOOL = 263,
        IF = 264,
        THEN = 265,
        ELSE = 266,
        WHILE = 267,
        DO = 268,
        REPEAT = 269,
        UNTIL = 270,
        IN = 271,
        OUT = 272,

        // Operadores e pontuação
        EQ = 273,       // ==
        GT = 274,       // >
        GE = 275,       // >=
        LT = 276,       // <
        LE = 277,       // <=
        NE = 278,       // !=
        ASSIGN = 279,   // =
        PLUS = 280,     // +
        MINUS = 281,    // -
        OR = 282,       // ||
        TIMES = 283,    // *
        DIV = 284,      // /
        AND = 285,      // &&

        // Tokens diversos
        NUM = 286,          // numero inteiro
        REAL = 287,         // numero float
        CHAR_CONST = 288,   // caractere entre aspas simples
        LITERAL = 289,      // string entre aspas duplas
        ID = 290,           // identificadores

        // Delimitadores
        SEMICOLON = 291,    // ;
        COLON = 292,        // :
        COMMA = 293,        // ,
        LPAREN = 294,       // (
        RPAREN = 295;       // )
}
package lexer;

public class Token {

    public final int tag; // constante que representa o token

    public Token(int t) {
        tag = t;
    }

    public String toString() {
        switch (tag) {
            // --- palavras reservadas / identificadores ---
            case Tag.ID:
                return "ID";
            case Tag.NUM:
                return "NUM";
            case Tag.REAL:
                return "REAL";
            case Tag.CHAR_CONST:
                return "CHAR_CONST";
            case Tag.LITERAL:
                return "LITERAL";

            // --- pontuação e operadores ---
            case Tag.SEMICOLON:
                return ";";
            case Tag.COLON:
                return ":";
            case Tag.COMMA:
                return ",";
            case Tag.LPAREN:
                return "(";
            case Tag.RPAREN:
                return ")";
            case Tag.PLUS:
                return "+";
            case Tag.MINUS:
                return "-";
            case Tag.TIMES:
                return "*";
            case Tag.DIV:
                return "/";
            case Tag.ASSIGN:
                return "=";
            case Tag.EQ:
                return "==";
            case Tag.NE:
                return "!=";
            case Tag.LT:
                return "<";
            case Tag.GT:
                return ">";
            case Tag.LE:
                return "<=";
            case Tag.GE:
                return ">=";
            case Tag.AND:
                return "&&";
            case Tag.OR:
                return "||";

            // --- palavras-chave mais comuns ---
            case Tag.PROGRAM:
                return "program";
            case Tag.BEGIN:
                return "begin";
            case Tag.END:
                return "end";
            case Tag.INT:
                return "int";
            case Tag.FLOAT:
                return "float";
            case Tag.CHAR:
                return "char";

            // adicione outras se achar útil
        }

        /* Se a tag < 128, é um caractere ASCII literal (ex. '+', '*') */
        if (tag < 128)
            return Character.toString((char) tag);

        /* fallback genérico: Tag(290) */
        return "TAG(" + tag + ")";
    }

}
package lexer;

// A classe Word gerencia lexemas para palavras reservadas, identificadores e tokens compostos
public class Word extends Token {

    public String lexema = "";

    // Palavras-chave compostas e operadores logicos
    public static final Word
        and = new Word("&&", Tag.AND),
        or  = new Word("||", Tag.OR),
        eq  = new Word("==", Tag.EQ),
        ne  = new Word("!=", Tag.NE),
        le  = new Word("<=", Tag.LE),
        ge  = new Word(">=", Tag.GE);

    public Word(String s, int tag) {
        super(tag);
        lexema = s;
    }

    public String getLexeme() {
        return lexema;
    }
    

    public String toString() {
        return "" + lexema;
    }
}
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
            filename = "raw_testes/teste1.txt"; // For testing purposes, replace with your file name
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
// for dir in raw_testes primeira_modificada; do     for i in {1..5}; do         /usr/bin/env /usr/lib/jvm/java-17-openjdk-amd64/bin/java             -XX:+ShowCodeDetailsInExceptionMessages             -cp /home/gi/.config/Code/User/workspaceStorage/2e3b82d01d3e7ac12c7df6fd79e9be5f/redhat.java/jdt_ws/Trabalho_Pratico_3935c5a7/bin             main.Main $dir/teste$i.txt 2> $dir/resultados/$dir-erro$i;     done; donepackage parser;

/** Exceção lançada quando ocorre erro sintático. */
public class ParserException extends RuntimeException {
    public ParserException(String msg) { super(msg); }
}
/*  ────────────────────────────────────────────────────────────────
 *  Parser.java  –  análise sintática + semântica “on‑the‑fly”
 *                 Usa ParserException para erros de sintaxe
 *                 e SemanticException para violações semânticas.
 *  Pacote: parser
 *  ----------------------------------------------------------------
 */
package parser;

import lexer.Lexer;
import lexer.Tag;
import lexer.Token;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Parser {

    /*
     * ============================================================
     * Infra‑estrutura básica
     * ============================================================
     */
    private final Lexer lex;
    private Token look; // token corrente

    /* tabela de símbolos: pilha de mapas (um por escopo) */
    private final Deque<Map<String, Type>> scopes = new ArrayDeque<>();

    public Parser(Lexer lex) throws IOException {
        this.lex = lex;
        move(); // carrega primeiro token
        enterScope(); // escopo global
    }

    /*
     * ------------------------------------------------------------
     * Escopos e símbolos
     * ------------------------------------------------------------
     */
    private void enterScope() {
        scopes.push(new HashMap<>());
    }

    private void leaveScope() {
        scopes.pop();
    }

    private void declare(String id, Type t) {
        Map<String, Type> top = scopes.peek();
        if (top.containsKey(id))
            errorSemantic("identificador '" + id + "' já declarado neste bloco");
        top.put(id, t);
    }

    private Type lookup(String id) {
        for (Map<String, Type> s : scopes)
            if (s.containsKey(id))
                return s.get(id);
        errorSemantic("identificador '" + id + "' não declarado");
        return Type.ERROR; // nunca chega
    }

    /*
     * ------------------------------------------------------------
     * Leitura de tokens e utilidades de erro
     * ------------------------------------------------------------
     */
    private void move() throws IOException {
        look = lex.scan();
    } // null = EOF

    private void errorSyntax(String msg) {
        throw new ParserException(
                "Erro sintático na linha " + Lexer.line +
                        ": " + msg +
                        " (encontrado: " + (look == null ? "EOF" : look) + ")");
    }

    private void errorSemantic(String msg) {
        throw new SemanticException(msg, Lexer.line);
    }

    private void match(int tag) throws IOException {
        if (look != null && look.tag == tag) {
            move();
        } else {
            errorSyntax("esperado '" + tagToString(tag) + "'");
        }
    }

    /*
     * ------------------------------------------------------------
     * Entrada principal
     * ------------------------------------------------------------
     */
    public void parse() throws IOException {
        program();
        if (look != null)
            errorSyntax("tokens adicionais após 'end'");
        leaveScope(); // fecha escopo global
        System.out.println("Compilação concluída sem erros!");
    }

    /*
     * ============================================================
     * GRAMÁTICA + SEMÂNTICA
     * ============================================================
     */

    /* program ::= program [decl-list] begin stmt-list end */
    private void program() throws IOException {
        match(Tag.PROGRAM);
        if (isTypeStarter())
            declList();
        match(Tag.BEGIN);
        stmtList();
        match(Tag.END);
    }

    /* decl-list ::= decl {decl} */
    private void declList() throws IOException {
        do {
            decl();
        } while (isTypeStarter());
    }

    /* decl ::= type ":" ident-list ";" */
    private void decl() throws IOException {
        Type t = type();
        match(Tag.COLON);
        identList(t);
        match(Tag.SEMICOLON);
    }

    /* ident-list ::= identifier {"," identifier} */
    private void identList(Type t) throws IOException {
        String id = look.toString();
        match(Tag.ID);
        declare(id, t);
        while (look != null && look.tag == Tag.COMMA) {
            match(Tag.COMMA);
            id = look.toString();
            match(Tag.ID);
            declare(id, t);
        }
    }

    /* type ::= int | float | char */
    private Type type() throws IOException {
        switch (look.tag) {
            case Tag.INT:
                match(Tag.INT);
                return Type.INT;
            case Tag.FLOAT:
                match(Tag.FLOAT);
                return Type.FLOAT;
            case Tag.CHAR:
                match(Tag.CHAR);
                return Type.CHAR;
            default:
                errorSyntax("tipo esperado");
                return Type.ERROR;
        }
    }

    /* stmt-list ::= stmt {";" stmt} */
    private void stmtList() throws IOException {
        stmt();
        while (look != null && look.tag == Tag.SEMICOLON) {
            match(Tag.SEMICOLON);
            stmt();
        }
    }

    /*
     * stmt ::= assign‑stmt | if‑stmt | while‑stmt | repeat‑stmt | read‑stmt |
     * write‑stmt
     */
    private void stmt() throws IOException {
        if (look == null)
            errorSyntax("instrução inesperada (EOF)");
        switch (look.tag) {
            case Tag.ID:
                assignStmt();
                break;
            case Tag.IF:
                ifStmt();
                break;
            case Tag.WHILE:
                whileStmt();
                break;
            case Tag.REPEAT:
                repeatStmt();
                break;
            case Tag.IN:
                readStmt();
                break;
            case Tag.OUT:
                writeStmt();
                break;
            default:
                errorSyntax("início de comando inválido");
        }
    }

    /* assign-stmt ::= identifier "=" simple_expr */
    private void assignStmt() throws IOException {
        String id = look.toString();
        match(Tag.ID);
        Type idType = lookup(id);
        match(Tag.ASSIGN);
        Type exprType = simpleExpr();
        if (idType != exprType)
            errorSemantic("tipo da expressão (" + exprType +
                    ") incompatível com '" + id + "' (" + idType + ")");
    }

    /* ---------- estruturas de controle com escopos ---------- */

    private void ifStmt() throws IOException {
        match(Tag.IF);
        Type cond = condition();
        requireBool(cond, "condição do 'if'");
        match(Tag.THEN);

        enterScope();
        if (isTypeStarter())
            declList();
        stmtList();
        leaveScope();

        if (look != null && look.tag == Tag.ELSE) {
            match(Tag.ELSE);
            enterScope();
            if (isTypeStarter())
                declList();
            stmtList();
            leaveScope();
        }
        match(Tag.END);
    }

    private void whileStmt() throws IOException {
        match(Tag.WHILE);
        Type cond = condition();
        requireBool(cond, "condição do 'while'");
        match(Tag.DO);
        enterScope();
        if (isTypeStarter())
            declList();
        stmtList();
        leaveScope();
        match(Tag.END);
    }

    private void repeatStmt() throws IOException {
        match(Tag.REPEAT);
        enterScope();
        if (isTypeStarter())
            declList();
        stmtList();
        leaveScope();
        match(Tag.UNTIL);
        Type cond = condition();
        requireBool(cond, "condição do 'until'");
    }

    /* read-stmt ::= in "(" identifier ")" */
    private void readStmt() throws IOException {
        match(Tag.IN);
        match(Tag.LPAREN);
        String id = look.toString();
        match(Tag.ID);
        lookup(id); // existência garantida
        match(Tag.RPAREN);
    }

    /* write-stmt ::= out "(" writable ")" */
    private void writeStmt() throws IOException {
        match(Tag.OUT);
        match(Tag.LPAREN);
        writable();
        match(Tag.RPAREN);
    }

    private void writable() throws IOException {
        if (look.tag == Tag.LITERAL) {
            match(Tag.LITERAL);
        } else {
            simpleExpr(); // checa tipos internamente
        }
    }

    /* ---------- EXPRESSÕES ---------- */

    private Type condition() throws IOException {
        return expression();
    }

    /* expression ::= simple-expr | simple-expr relop simple-expr */
    private Type expression() throws IOException {
        Type left = simpleExpr();
        if (isRelop(look)) {
            relop();
            Type right = simpleExpr();
            if (!isComparable(left, right))
                errorSemantic("tipos incompatíveis em operador relacional (" +
                        left + " x " + right + ")");
            return Type.BOOL;
        }
        return left;
    }

    /* simple-expr ::= term {addop term} */
    private Type simpleExpr() throws IOException {
        Type t = term();
        while (isAddop(look)) {
            int op = look.tag;
            addop();
            Type rhs = term();
            if (op == Tag.OR) {
                if (t != Type.BOOL || rhs != Type.BOOL)
                    errorSemantic("'or' requer operandos booleanos");
                t = Type.BOOL;
            } else {
                t = arithmeticResult(t, rhs);
            }
        }
        return t;
    }

    /* term ::= factor-a {mulop factor-a} */
    private Type term() throws IOException {
        Type t = factorA();
        while (isMulop(look)) {
            int op = look.tag;
            mulop();
            Type rhs = factorA();
            if (op == Tag.AND) {
                if (t != Type.BOOL || rhs != Type.BOOL)
                    errorSemantic("'and' requer operandos booleanos");
                t = Type.BOOL;
            } else {
                t = arithmeticResult(t, rhs);
            }
        }
        return t;
    }

    /* factor-a ::= factor | "!" factor | "-" factor */
    private Type factorA() throws IOException {
        if (look.tag == '!') {
            match('!');
            Type t = factor();
            requireBool(t, "operando de '!'");
            return Type.BOOL;
        }
        if (look.tag == Tag.MINUS) {
            match(Tag.MINUS);
            Type t = factor();
            requireNumericOrChar(t, "operando de unário '-'");
            return t;
        }
        return factor();
    }

    /* factor ::= identifier | constant | "(" expression ")" */
    private Type factor() throws IOException {
        switch (look.tag) {
            case Tag.ID:
                String id = look.toString();
                match(Tag.ID);
                return lookup(id);

            case Tag.NUM:
            case Tag.REAL:
            case Tag.CHAR_CONST:
                return constant();

            case Tag.LPAREN:
                match(Tag.LPAREN);
                Type t = expression();
                match(Tag.RPAREN);
                return t;

            default:
                errorSyntax("fator esperado");
                return Type.ERROR;
        }
    }

    private Type constant() throws IOException {
        switch (look.tag) {
            case Tag.NUM:
                match(Tag.NUM);
                return Type.INT;
            case Tag.REAL:
                match(Tag.REAL);
                return Type.FLOAT;
            case Tag.CHAR_CONST:
                match(Tag.CHAR_CONST);
                return Type.CHAR;
            default:
                errorSyntax("constante esperada");
                return Type.ERROR;
        }
    }

    /*
     * ============================================================
     * Regras de tipo / promoção e utilidades semânticas
     * ============================================================
     */
    private static boolean isNumeric(Type t) {
        return t == Type.INT || t == Type.FLOAT;
    }

    private static Type arithmeticResult(Type a, Type b) {
        if (isNumeric(a) && isNumeric(b))
            return (a == Type.FLOAT || b == Type.FLOAT) ? Type.FLOAT : Type.INT;

        if ((a == Type.CHAR && b == Type.INT) || (a == Type.INT && b == Type.CHAR))
            return Type.INT;

        throw new SemanticException(
                "tipos incompatíveis em operação aritmética (" + a + " x " + b + ")",
                Lexer.line);
    }

    private static boolean isComparable(Type a, Type b) {
        try {
            arithmeticResult(a, b);
            return true;
        } catch (SemanticException e) {
            return false;
        }
    }

    private static void requireBool(Type t, String what) {
        if (t != Type.BOOL)
            throw new SemanticException(what + " deve ser booleana (encontrado: " + t + ")",
                    Lexer.line);
    }

    private static void requireNumericOrChar(Type t, String what) {
        if (!(isNumeric(t) || t == Type.CHAR))
            throw new SemanticException(what + " deve ser numérico ou caractere (encontrado: " + t + ")",
                    Lexer.line);
    }

    /*
     * ============================================================
     * Pequenos utilitários de gramática
     * ============================================================
     */
    private static boolean isRelop(Token t) {
        if (t == null)
            return false;
        int tg = t.tag;
        return tg == Tag.EQ || tg == Tag.GT || tg == Tag.GE ||
                tg == Tag.LT || tg == Tag.LE || tg == Tag.NE;
    }

    private static boolean isAddop(Token t) {
        if (t == null)
            return false;
        int tg = t.tag;
        return tg == Tag.PLUS || tg == Tag.MINUS || tg == Tag.OR;
    }

    private static boolean isMulop(Token t) {
        if (t == null)
            return false;
        int tg = t.tag;
        return tg == Tag.TIMES || tg == Tag.DIV || tg == Tag.AND;
    }

    private boolean isTypeStarter() {
        return look != null &&
                (look.tag == Tag.INT || look.tag == Tag.FLOAT || look.tag == Tag.CHAR);
    }

    private void relop() throws IOException {
        match(look.tag);
    }

    private void addop() throws IOException {
        match(look.tag);
    }

    private void mulop() throws IOException {
        match(look.tag);
    }

    /*
     * ------------------------------------------------------------
     * Nome amigável para TAGs (mensagens de erro)
     * ------------------------------------------------------------
     */
    private static String tagToString(int tag) {
        switch (tag) {
            case Tag.PROGRAM:
                return "program";
            case Tag.BEGIN:
                return "begin";
            case Tag.END:
                return "end";
            case Tag.ID:
                return "ID";
            case Tag.NUM:
                return "NUM";
            case Tag.REAL:
                return "REAL";
            case Tag.CHAR_CONST:
                return "CHAR_CONST";
            case Tag.LITERAL:
                return "LITERAL";
            case Tag.COLON:
                return ":";
            case Tag.SEMICOLON:
                return ";";
            case Tag.COMMA:
                return ",";
            case Tag.LPAREN:
                return "(";
            case Tag.RPAREN:
                return ")";
            case Tag.PLUS:
                return "+";
            case Tag.MINUS:
                return "-";
            case Tag.TIMES:
                return "*";
            case Tag.DIV:
                return "/";
            case Tag.ASSIGN:
                return "=";
            default:
                return tag < 128 ? Character.toString((char) tag)
                        : "TAG(" + tag + ")";
        }
    }
}
// ─── parser/Type.java ────────────────────────────────────────────────
package parser;

public enum Type { INT, FLOAT, CHAR, BOOL, ERROR }
package parser;

public class SemanticException extends RuntimeException {
    private final int line;

    public SemanticException(String msg, int line) {
        super("Erro semântico na linha " + line + ": " + msg);
        this.line = line;
    }

    public int getLine() { return line; }
}
package parser;

// import lexer.*;
import lexer.Lexer;
import lexer.Tag;
import lexer.Token;

import java.io.IOException;

/**
 * Analisador sintático recursivo-descendente para a gramática
 * fornecida no trabalho. Não gera AST – apenas valida a estrutura.
 */
public class Parser {

    private final Lexer lex;
    private Token look; // token corrente

    /* --------------------- utilidades --------------------- */

    public Parser(Lexer lex) throws IOException {
        this.lex = lex;
        move(); // carrega o primeiro token
    }

    /** Lê o próximo token do lexer. */
    private void move() throws IOException {
        look = lex.scan(); // pode vir null no EOF
    }

    /** Relata erro sintático e aborta. */
    private void error(String msg) {
        throw new ParserException(
                "Erro sintático na linha " + Lexer.line + ": " + msg +
                        " (encontrado: " + (look == null ? "EOF" : look) + ")");
    }

    /** Confere se o token atual possui a tag esperada e consome-o. */
    private void match(int tag) throws IOException {
        if (look != null && look.tag == tag) {
            move();
        } else {
            error("esperado '" + tagToString(tag) + "'");
        }
    }

    /** Apenas para mensagens de erro mais amigáveis. */
    private static String tagToString(int tag) {
        switch (tag) {
            case Tag.PROGRAM:
                return "program";
            case Tag.BEGIN:
                return "begin";
            case Tag.END:
                return "end";

            case Tag.ID:
                return "ID";
            case Tag.NUM:
                return "NUM";
            case Tag.REAL:
                return "REAL";
            case Tag.CHAR_CONST:
                return "CHAR_CONST";
            case Tag.LITERAL:
                return "LITERAL";

            case Tag.COLON:
                return ":";
            case Tag.SEMICOLON:
                return ";";
            case Tag.COMMA:
                return ",";
            case Tag.LPAREN:
                return "(";
            case Tag.RPAREN:
                return ")";
            case Tag.PLUS:
                return "+";
            case Tag.MINUS:
                return "-";
            case Tag.TIMES:
                return "*";
            case Tag.DIV:
                return "/";
            case Tag.ASSIGN:
                return "=";
            // adicione outros conforme precisar
            default:
                if (tag < 128)
                    return Character.toString((char) tag);
                return "TAG(" + tag + ")";
        }
    }

    /* ------------------------------------------------------ */
    /* ------------ métodos correspondentes à gramática ----- */
    /* ------------------------------------------------------ */

    // entry-point
    public void parse() throws IOException {
        program(); // <program>
        if (look != null)
            error("tokens adicionais após 'end'");
        System.out.println("Programa sintaticamente correto!");
    }

    /*
     * ------------------------------------------------------------------
     * GRAMMAR:
     * program ::= program [decl-list] begin stmt-list end
     * ------------------------------------------------------------------
     */
    private void program() throws IOException {
        match(Tag.PROGRAM);
        if (isTypeStarter()) { // opcional [decl-list]
            declList();
        }
        match(Tag.BEGIN);
        stmtList();
        match(Tag.END);
    }

    /* decl-list ::= decl {decl} */
    private void declList() throws IOException {
        do {
            decl();
        } while (isTypeStarter());
    }

    /* decl ::= type ":" ident-list ";" */
    private void decl() throws IOException {
        type();
        match(Tag.COLON);
        identList();
        match(Tag.SEMICOLON);
    }

    /* ident-list ::= identifier {"," identifier} */
    private void identList() throws IOException {
        match(Tag.ID);
        while (look != null && look.tag == Tag.COMMA) {
            match(Tag.COMMA);
            match(Tag.ID);
        }
    }

    /* type ::= int | float | char */
    private void type() throws IOException {
        switch (look.tag) {
            case Tag.INT:
                match(Tag.INT);
                break;
            case Tag.FLOAT:
                match(Tag.FLOAT);
                break;
            case Tag.CHAR:
                match(Tag.CHAR);
                break;
            default:
                error("tipo esperado");
        }
    }

    /* stmt-list ::= stmt {";" stmt} */
    private void stmtList() throws IOException {
        stmt();
        while (look != null && look.tag == Tag.SEMICOLON) {
            match(Tag.SEMICOLON);
            stmt();
        }
    }

    /*
     * stmt ::= assign-stmt | if-stmt | while-stmt | repeat-stmt
     * | read-stmt | write-stmt
     */
    private void stmt() throws IOException {
        if (look == null)
            error("instrução inesperada (EOF)");

        switch (look.tag) {
            case Tag.ID:
                assignStmt();
                break;
            case Tag.IF:
                ifStmt();
                break;
            case Tag.WHILE:
                whileStmt();
                break;
            case Tag.REPEAT:
                repeatStmt();
                break;
            case Tag.IN:
                readStmt();
                break;
            case Tag.OUT:
                writeStmt();
                break;
            default:
                error("início de comando inválido");
        }
    }

    /* assign-stmt ::= identifier "=" simple_expr */
    private void assignStmt() throws IOException {
        match(Tag.ID);
        match(Tag.ASSIGN);
        simpleExpr();
    }

    /*
     * if-stmt:
     * if condition then [decl-list] stmt-list end
     * | if condition then [decl-list] stmt-list else [decl-list] stmt-list end
     */
    private void ifStmt() throws IOException {
        match(Tag.IF);
        condition();
        match(Tag.THEN);
        if (isTypeStarter())
            declList();
        stmtList();
        if (look != null && look.tag == Tag.ELSE) {
            match(Tag.ELSE);
            if (isTypeStarter())
                declList();
            stmtList();
        }
        match(Tag.END);
    }

    /*
     * while-stmt ::= stmt-prefix [decl-list] stmt-list end
     * stmt-prefix ::= while condition do
     */
    private void whileStmt() throws IOException {
        match(Tag.WHILE);
        condition();
        match(Tag.DO);
        if (isTypeStarter())
            declList();
        stmtList();
        match(Tag.END);
    }

    /*
     * repeat-stmt ::= repeat [decl-list] stmt-list stmt-suffix
     * stmt-suffix ::= until condition
     */
    private void repeatStmt() throws IOException {
        match(Tag.REPEAT);
        if (isTypeStarter())
            declList();
        stmtList();
        match(Tag.UNTIL);
        condition();
    }

    /* read-stmt ::= in "(" identifier ")" */
    private void readStmt() throws IOException {
        match(Tag.IN);
        match(Tag.LPAREN);
        match(Tag.ID);
        match(Tag.RPAREN);
    }

    /* write-stmt ::= out "(" writable ")" */
    private void writeStmt() throws IOException {
        match(Tag.OUT);
        match(Tag.LPAREN);
        writable();
        match(Tag.RPAREN);
    }

    private void writable() throws IOException {
        if (look.tag == Tag.LITERAL) {
            match(Tag.LITERAL);
        } else {
            simpleExpr();
        }
    }

    /* condition ::= expression */
    private void condition() throws IOException {
        expression();
    }

    /* expression ::= simple-expr | simple-expr relop simple-expr */
    private void expression() throws IOException {
        simpleExpr();
        if (isRelop(look)) {
            relop();
            simpleExpr();
        }
    }

    /* simple-expr ::= term {addop term} */
    private void simpleExpr() throws IOException {
        term();
        while (isAddop(look)) {
            addop();
            term();
        }
    }

    /* term ::= factor-a {mulop factor-a} */
    private void term() throws IOException {
        factorA();
        while (isMulop(look)) {
            mulop();
            factorA();
        }
    }

    /* factor-a ::= factor | "!" factor | "-" factor */
    private void factorA() throws IOException {
        if (look.tag == '!') { // '!' é o próprio caractere
            match('!');
            factor();
        } else if (look.tag == Tag.MINUS) {
            match(Tag.MINUS);
            factor();
        } else {
            factor();
        }
    }

    /* factor ::= identifier | constant | "(" expression ")" */
    private void factor() throws IOException {
        switch (look.tag) {
            case Tag.ID:
                match(Tag.ID);
                break;
            case Tag.NUM:
            case Tag.REAL:
            case Tag.CHAR_CONST:
                constant();
                break;
            case Tag.LPAREN:
                match(Tag.LPAREN);
                expression();
                match(Tag.RPAREN);
                break;
            default:
                error("fator esperado");
        }
    }

    private void constant() throws IOException {
        switch (look.tag) {
            case Tag.NUM:
                match(Tag.NUM);
                break;
            case Tag.REAL:
                match(Tag.REAL);
                break;
            case Tag.CHAR_CONST:
                match(Tag.CHAR_CONST);
                break;
            default:
                error("constante esperada");
        }
    }

    /* ------------------------ auxiliares ------------------------ */

    private static boolean isRelop(Token t) {
        if (t == null)
            return false;
        int tg = t.tag;
        return tg == Tag.EQ || tg == Tag.GT || tg == Tag.GE ||
                tg == Tag.LT || tg == Tag.LE || tg == Tag.NE;
    }

    private static boolean isAddop(Token t) {
        if (t == null)
            return false;
        int tg = t.tag;
        return tg == Tag.PLUS || tg == Tag.MINUS || tg == Tag.OR;
    }

    private static boolean isMulop(Token t) {
        if (t == null)
            return false;
        int tg = t.tag;
        return tg == Tag.TIMES || tg == Tag.DIV || tg == Tag.AND;
    }

    /**
     * Retorna true se o token atual pode iniciar uma declaração (int|float|char).ds
     */
    private boolean isTypeStarter() {
        return look != null &&
                (look.tag == Tag.INT || look.tag == Tag.FLOAT || look.tag == Tag.CHAR);
    }

    /** Faz o consumo de um relop/addop/mulop (já verificado antes). */
    private void relop() throws IOException {
        match(look.tag);
    }

    private void addop() throws IOException {
        match(look.tag);
    }

    private void mulop() throws IOException {
        match(look.tag);
    }
}
