package parser;

public class SemanticException extends RuntimeException {
    private final int line;

    public SemanticException(String msg, int line) {
        super("Erro semântico na linha " + line + ": " + msg);
        this.line = line;
    }

    public int getLine() { return line; }
}
