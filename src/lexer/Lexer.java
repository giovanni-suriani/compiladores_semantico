package lexer;

import java.io.*;
import java.util.*;

// A classe Lexer mapeia cadeias em palavras
public class Lexer {
    public static int line = 1;              // contador de linhas
    private char ch = ' ';                   // caractere lido do arquivo
    private final FileReader file;

    private final Hashtable<String, Word> words = new Hashtable<>();

    /* ----------------------------------------------------------
     *  Construtor ­– carrega palavras‑chave na tabela
     * ---------------------------------------------------------- */
    private void reserve(Word w) { words.put(w.getLexeme(), w); }

    public Lexer(String fileName) throws FileNotFoundException {
        try { file = new FileReader(fileName); }
        catch (FileNotFoundException e) {
            System.err.println("Arquivo não encontrado");
            throw e;
        }

        // Palavras reservadas
        reserve(new Word("if",      Tag.IF));
        reserve(new Word("program", Tag.PROGRAM));
        reserve(new Word("begin",   Tag.BEGIN));
        reserve(new Word("end",     Tag.END));
        reserve(new Word("type",    Tag.TYPE));
        reserve(new Word("int",     Tag.INT));
        reserve(new Word("float",   Tag.FLOAT));
        reserve(new Word("char",    Tag.CHAR));
        reserve(new Word("bool",    Tag.BOOL));
        reserve(new Word("then",    Tag.THEN));
        reserve(new Word("else",    Tag.ELSE));
        reserve(new Word("while",   Tag.WHILE));
        reserve(new Word("do",      Tag.DO));
        reserve(new Word("repeat",  Tag.REPEAT));
        reserve(new Word("until",   Tag.UNTIL));
        reserve(new Word("in",      Tag.IN));
        reserve(new Word("out",     Tag.OUT));
    }

    /* ----------------------------------------------------------
     *  Utilidades de leitura
     * ---------------------------------------------------------- */
    private void readch() throws IOException {
        int r = file.read();
        ch = (r == -1) ? (char) -1 : (char) r;
    }

    /** Cria token de 1 caractere e já avança o ponteiro. */
    private Token single(int tag) throws IOException {
        Token t = new Token(tag);
        readch();
        return t;
    }

    /** Avança um caractere e retorna true se ele for ‘c’. */
    private boolean readch(char c) throws IOException {
        readch();
        if (ch != c) return false;
        ch = ' ';
        return true;
    }

    /* ==========================================================
     *  Principal: devolve o próximo Token ou null (EOF)
     * ========================================================== */
    public Token scan() throws IOException {

        /* ----- ignora espaços, tabs, CR, etc. ----- */
        for (;; readch()) {
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\b') continue;
            else if (ch == '\n') line++;
            else break;
        }

        /* ----- comentários { … } ou % linha ----- */
        if (ch == '{') {
            do {
                readch();
                if (ch == (char) -1)
                    throw new IOException("Erro léxico: comentário de bloco não fechado");
            } while (ch != '}');
            readch();            // consome '}'
            return scan();       // recomeça
        }
        if (ch == '%') {         // comentário de uma linha
            do { readch(); }
            while (ch != '\n' && ch != (char) -1);
            line++; readch();
            return scan();
        }

        /* ------------------------------------------------------
         *  Operadores & pontuação
         *  (✅ corrigido: não usa single() depois de readch(c))
         * ------------------------------------------------------ */
        switch (ch) {
            case '&':
                if (readch('&')) return Word.and;
                return new Token('&');
            case '|':
                if (readch('|')) return Word.or;
                return new Token('|');
            case '=':
                if (readch('=')) return Word.eq;
                return new Token(Tag.ASSIGN);
            case '!':
                if (readch('=')) return Word.ne;
                return new Token('!');
            case '<':
                if (readch('=')) return Word.le;
                return new Token(Tag.LT);
            case '>':
                if (readch('=')) return Word.ge;
                return new Token(Tag.GT);

            /* um único caractere (pode usar single) */
            case '+': return single(Tag.PLUS);
            case '-': return single(Tag.MINUS);
            case '*': return single(Tag.TIMES);
            case '/': return single(Tag.DIV);
            case ';': return single(Tag.SEMICOLON);
            case ':': return single(Tag.COLON);
            case ',': return single(Tag.COMMA);
            case '(': return single(Tag.LPAREN);
            case ')': return single(Tag.RPAREN);
        }

        /* ----- constantes de caractere 'x' ----- */
        if (ch == '\'') {
            readch();
            char valor = ch;
            readch();
            if (ch == '\'') { readch(); return new CharConst(valor); }
            throw new IOException("Erro léxico: caractere mal formado");
        }

        /* ----- literais de string "..." ----- */
        if (ch == '"') {
            StringBuilder sb = new StringBuilder();
            readch();
            while (ch != '"' && ch != '\n' && ch != (char) -1) {
                sb.append(ch); readch();
            }
            if (ch == '"') { readch(); return new Literal(sb.toString()); }
            throw new IOException("Erro léxico: string mal formada");
        }

        /* ----- números ----- */
        if (Character.isDigit(ch)) {
            int val = 0;
            do { val = 10*val + Character.digit(ch,10); readch(); }
            while (Character.isDigit(ch));

            if (ch != '.') return new Num(val);

            /* ponto flutuante */
            float x = val, d = 10;
            readch();                    // consome '.'
            if (!Character.isDigit(ch))
                throw new IOException("Erro léxico: ponto sem dígitos em float");
            while (Character.isDigit(ch)) {
                x += Character.digit(ch,10) / d; d *= 10; readch();
            }
            return new Real(x);
        }

        /* ----- identificadores / palavras‑chave ----- */
        if (Character.isLetter(ch) || ch == '_') {
            StringBuilder sb = new StringBuilder();
            do { sb.append(ch); readch(); }
            while (Character.isLetterOrDigit(ch) || ch == '_');

            String s = sb.toString().toLowerCase();
            Word w = words.get(s);
            if (w != null) return w;              // palavra reservada
            w = new Word(s, Tag.ID); words.put(s, w);
            return w;
        }

        /* ----- fim de arquivo ----- */
        if (ch == (char) -1) return null;

        /* caractere desconhecido isolado */
        Token t = new Token(ch);
        readch();
        return t;
    }
}
