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
