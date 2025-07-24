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
