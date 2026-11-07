package proyecto;

public class Token {
    public final String tipo;     // "id", "let", "cteentera", "(", ...
    public final String lexema;   // nombre real para id, contenido para cadena, etc.
    public final int linea;
    public final int posTS;

    // Constructor general (para tokens sin posTS)
    public Token(String tipo, String lexema, int linea) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.posTS = -1;
    }

    // Constructor para id con posTS
    public Token(String tipo, String lexema, int linea, int posTS) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.posTS = posTS;
    }
}
