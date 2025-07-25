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
