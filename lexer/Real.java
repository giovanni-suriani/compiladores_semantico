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
