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
