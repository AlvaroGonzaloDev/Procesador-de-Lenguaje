package proyecto;

import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;

public class AnalizadorLexico extends Tokens {
    private static FileWriter escritor;
    private static PushbackReader lector;
    private static ArrayList<Token> canalAl;
    private static int linea;
    private TablaLexemas tablaLexemas;
    private GestorErrores errores;

    public AnalizadorLexico(GestorErrores errores, TablaLexemas tablaLexemas) throws IOException {
        this.errores = errores;
        this.tablaLexemas = tablaLexemas;
        escritor = new FileWriter("tokens.txt");
        lector = new PushbackReader(new BufferedReader(new FileReader("prueba.txt")), 2);
        canalAl = new ArrayList<>();
        linea = 1;
    }

    public void ALexicoInit() {
        try {
            int caracter;
            ArrayList<Character> word = new ArrayList<>();
            String tokenPalabra;
            String tokenDelimitador;
            String tipoPalabra;
            String tipoDel;
            String lexema;

            while ((caracter = lector.read()) != -1) {
                // Blancos (solo cierran palabra y no generan token)
                if (caracter == ' ' || caracter == '\t' || caracter == '\n' || caracter == '\r') {
                    escribirTokenPrevio(word, escritor);
                    if (caracter == '\n')
                        linea++;
                }

                // Cadena con comilla simple
                else if (caracter == '\'') {
                    escribirTokenPrevio(word, escritor);
                    ArrayList<Character> arr = new ArrayList<>();
                    int siguiente;
                    boolean cadenaCerrada = false;
                    while ((siguiente = lector.read()) != -1 && siguiente != '\'') {
                        if (siguiente == '\n') {
                            errores.errorLexico(linea, "No se permiten saltos de linea dentro de cadenas.");
                            linea++;
                            int c;
                            while ((c = lector.read()) != -1 && c != '\'') { // Consumir hasta cierre o EOF para no
                                                                             // dejar basura en el stream
                                if (c == '\n')
                                    linea++;
                            }
                            break;
                        }
                        arr.add((char) siguiente);
                    }
                    if (siguiente == '\'') {
                        cadenaCerrada = true;
                    }
                    if (cadenaCerrada) {
                        String lex = convertirALexema(arr);
                        if (lex.length() > 64) {
                            escritor.write("<errorlexcadenafuerarango,\"" + lex + "\">\n");
                            errores.errorLexico(linea, "Cadena demasiado larga (maximo 64 caracteres).");
                        } else {
                            escritor.write("<codcadena,\"" + lex + "\">\n");
                            canalAl.add(new Token("cadena", lex, linea));
                        }
                    } else {
                        if (siguiente == -1)
                            errores.errorLexico(linea, "Cadena sin cerrar (falta comilla de cierre ').");
                    }
                }

                // Comentarios /*...*/ (leer y descartar, sin token ni error)
                else if (caracter == '/') {
                    int siguiente = lector.read();
                    if (siguiente == '*') {
                        escribirTokenPrevio(word, escritor);
                        int c1 = lector.read();
                        int c2 = lector.read();
                        while (!(c1 == '*' && c2 == '/') && (c1 != -1) && (c2 != -1)) {
                            if (c2 == '\n')
                                linea++;
                            c1 = c2;
                            c2 = lector.read();
                        }
                    } else {
                        escribirTokenPrevio(word, escritor);
                        escritor.write("<errorlex,/>\n");
                        errores.errorLexico(linea,
                                "Simbolo '/' no valido. Solo se permiten comentarios de bloque '/* ... */'.");
                        if (siguiente != -1)
                            lector.unread(siguiente);
                    }
                }

                // Palabra (identificador, palabra reservada, cte entera/real)
                else if (Character.isLetter(caracter) || Character.isDigit(caracter) || caracter == '_')
                    word.add((char) caracter);

                // Posible numero real valido
                else if (caracter == '.') {
                    if (word.isEmpty()) { // CASO .7 O .a O . SUELTO
                        escritor.write("<errorlex,.>\n");
                        errores.errorLexico(linea,
                                "Uso invalido de '.' en un numero real. Debe haber digitos antes y despues del punto.");
                    } else if (esCteEntera(convertirALexema(new ArrayList<>(word)))) { // POSIBLE NUMERO REAL VALIDO
                        int siguiente = lector.read();
                        if (siguiente != -1 && Character.isDigit(siguiente)) { // NUMERO REAL VALIDO EJ: 14.7
                            word.add('.');
                            lector.unread(siguiente);
                        } else { // CASO 14.
                            escribirTokenPrevio(word, escritor);
                            escritor.write("<errorlex,.>\n");
                            errores.errorLexico(linea,
                                    "Uso invalido de '.' en un numero real. Debe haber digitos antes y despues del punto.");
                            if (siguiente != -1)
                                lector.unread(siguiente);
                        }
                    } else { // ALGO COMO "a.b" O "12.3.4"
                        escribirTokenPrevio(word, escritor);
                        escritor.write("<errorlex,.>\n");
                        errores.errorLexico(linea,
                                "Uso invalido de '.' en un numero real. Debe haber digitos antes y despues del punto.");
                    }
                }

                // Operador de dos caracteres: ++
                else if (caracter == '+') {
                    int siguiente = lector.read();
                    if (siguiente == '+') {
                        escribirTokenPrevio(word, escritor);
                        escritor.write("<codautoincrem,>\n");
                        canalAl.add(new Token("++", null, linea));
                    } else {
                        escribirTokenPrevio(word, escritor);
                        escritor.write("<errorlex,+>\n");
                        errores.errorLexico(linea, "Simbolo '+' no valido. En este lenguaje solo se permite '++'.");
                        if (siguiente != -1)
                            lector.unread(siguiente);
                    }
                }

                // Operadores de un carácter
                else {
                    String[] tokens = token((char) caracter, word);
                    tokenPalabra = tokens[0];
                    tokenDelimitador = tokens[1];
                    tipoPalabra = tokens[2];
                    tipoDel = tokens[3];
                    lexema = tokens[4];
                    if (tokenPalabra != null && !tokenPalabra.isEmpty()) {
                        if (tokenPalabra.contains("errorlexnumerofuerarango"))
                            errores.errorLexico(linea, "Número fuera de rango: '" + lexema + "'.");
                        else if ("error".equals(tipoPalabra))
                            errores.errorLexico(linea, "Lexema no valido: '" + lexema + "'.");
                    }
                    if (tipoPalabra != null && !tipoPalabra.isEmpty()) {
                        if (tipoPalabra.equals("id")) {
                            int pos = tablaLexemas.insertarLexema(lexema);
                            escritor.write("<codid," + pos + ">\n");
                            canalAl.add(new Token("id", lexema, linea, pos));
                        } else {
                            if (tokenPalabra != null && !tokenPalabra.isEmpty()) {
                                escritor.write(tokenPalabra);
                            }
                            if (!tipoPalabra.equals("error")) {
                                String lexToken = null;
                                if (tipoPalabra.equals("cteentera") || tipoPalabra.equals("ctereal")) {
                                    lexToken = lexema; // para ctes guardamos lexema/valor
                                }
                                canalAl.add(new Token(tipoPalabra, lexToken, linea));
                            }
                        }
                    }
                    if (!tokenDelimitador.isEmpty()) {
                        escritor.write(tokenDelimitador);
                        if (tipoDel != null && !tipoDel.isEmpty() && !tipoDel.equals("error"))
                            canalAl.add(new Token(tipoDel, null, linea));
                    }
                    if (tokenDelimitador != null && !tokenDelimitador.isEmpty()) {
                        if ("error".equals(tipoDel))
                            errores.errorLexico(linea, "Caracter no reconocido: '" + (char) caracter + "'.");
                    }
                }
            }

            escribirTokenPrevio(word, escritor);
            escritor.close();
            lector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void escribirTokenPrevio(ArrayList<Character> word, FileWriter escritor) throws IOException {
        String[] tokens = token(' ', word);
        String tokenPalabra = tokens[0];
        String tipo = tokens[2];
        String lexema = tokens[4];
        if (tipo == null || tipo.isEmpty())
            return;
        if (tokenPalabra.contains("errorlexnumerofuerarango"))
            errores.errorLexico(linea, "Numero fuera de rango: '" + lexema + "'.");
        else if (tipo.equals("error"))
            errores.errorLexico(linea, "Lexema no valido: '" + lexema + "'.");
        if (tipo.equals("id")) {
            int pos = tablaLexemas.insertarLexema(lexema);
            escritor.write("<codid," + pos + ">\n");
            canalAl.add(new Token("id", lexema, linea, pos));
            return;
        }
        if (tokenPalabra != null && !tokenPalabra.isEmpty()) {
            escritor.write(tokenPalabra);
        }
        if (!tipo.equals("error")) {
            String lexToken = null;
            if (tipo.equals("cteentera") || tipo.equals("ctereal"))
                lexToken = lexema;
            canalAl.add(new Token(tipo, lexToken, linea));
        }
    }

    public Token ALexico() {
        if (canalAl.isEmpty()) {
            return new Token("$", null, linea);
        }
        return canalAl.remove(0);
    }
}