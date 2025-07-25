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
