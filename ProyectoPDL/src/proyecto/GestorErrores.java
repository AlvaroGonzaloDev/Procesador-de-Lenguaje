package proyecto;

import java.io.FileWriter;
import java.io.IOException;

public class GestorErrores {
    private FileWriter escritorGE;

    public GestorErrores() throws IOException {
        escritorGE = new FileWriter("errores.txt");
    }

    public void errorLexico(int linea, String msg) throws IOException {
        escritorGE.write("[Linea " + linea + "] Error lexico: " + msg + "\n");
    }

    public void errorSintactico(int linea, String msg) throws IOException {
        escritorGE.write("[Linea " + linea + "] Error sintactico: " + msg + "\n");
    }

    public void errorSemantico(int linea, String msg) throws IOException {
        escritorGE.write("[Linea " + linea + "] Error semantico: " + msg + "\n");
    }

    public void cerrar() throws IOException {
        escritorGE.close();
    }
}
