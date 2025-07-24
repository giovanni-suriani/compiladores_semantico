package parser;

/** Exceção lançada quando ocorre erro sintático. */
public class ParserException extends RuntimeException {
    public ParserException(String msg) { super(msg); }
}
