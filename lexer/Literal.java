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
