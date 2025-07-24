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
