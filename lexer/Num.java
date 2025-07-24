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
