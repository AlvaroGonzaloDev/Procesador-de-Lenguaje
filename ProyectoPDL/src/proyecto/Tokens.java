package proyecto;

import java.util.ArrayList;
import java.util.Set;

public class Tokens {
    private static final Set<String> palabrasReservadas = Set.of("if", "else", "int", "boolean", "string", "float", "return", "function", "let", "read", "write", "void");

    public static String[] token(char caracter, ArrayList<Character> word) {
        String token = "";
        String tokenDel = "";
        String tokenPuro = "";
        String tokenPuroDel = "";
        String lexemaProcesado = null;
        String[] tokens = new String[5];

        if (word.size() > 0) { // Procesa la palabra acumulada
            String lex = convertirALexema(word);
            lexemaProcesado = lex;
            if (palabrasReservadas.contains(lex)) {
                token = "<" + palabraReservada(lex) + ",>\n";
                tokenPuro = lex;
            }
            else if (esCteEntera(lex)) {
                if (lex.length() > 5) // más de 5 dígitos, seguro esta fuera de rango
                    token = "<errorlexnumerofuerarango," + lex + ">\n";
                else {
                    int valorNum = Integer.parseInt(lex);
                    if (valorNum > 32767)
                        token = "<errorlexnumerofuerarango," + lex + ">\n";
                    else {
                        token = "<cteentera," + lex + ">\n";
                        tokenPuro = "cteentera";
                    }
                }
            } 
            else if (esCteReal(lex)) {
                double valorNum = Double.parseDouble(lex);
                if (valorNum > 117549436.0)
                    token = "<errorlexnumerofuerarango," + lex + ">\n";
                else {
                    token = "<ctereal," + lex + ">\n";
                    tokenPuro = "ctereal";
                }
            }
            else if (esId(lex)) {
                token = "";
                tokenPuro = "id";
            } 
            else {
                token = "<errorlex," + lex + ">\n";
                tokenPuro = "error";
            }
        }

        // Procesa delimitadores u operadores de un solo carácter
        if (caracter == '=') {
            tokenDel = "<codasig,>\n";
            tokenPuroDel = "=";
        } else if (caracter == '{') {
            tokenDel = "<codcorcheteA,>\n";
            tokenPuroDel = "{";
        } else if (caracter == '}') {
            tokenDel = "<codcorcheteC,>\n";
            tokenPuroDel = "}";
        } else if (caracter == '(') {
            tokenDel = "<codparentesisA,>\n";
            tokenPuroDel = "(";
        } else if (caracter == ')') {
            tokenDel = "<codparentesisC,>\n";
            tokenPuroDel = ")";
        } else if (caracter == ',') {
            tokenDel = "<codcoma,>\n";
            tokenPuroDel = ",";
        } else if (caracter == ';') {
            tokenDel = "<codpuntoycoma,>\n";
            tokenPuroDel = ";";
        } else if (caracter == '<') {
            tokenDel = "<codmenor,>\n";
            tokenPuroDel = "<";
        } else if (caracter == '!') {
            tokenDel = "<codnegado,>\n";
            tokenPuroDel = "!";
        } else if (caracter == '-') {
            tokenDel = "<codresta,>\n";
            tokenPuroDel = "-";
        } else if (caracter == ' ' || caracter == '\n' || caracter == '\t' || caracter == '\r') {

        } else {
            tokenDel = "<errorlex," + caracter + ">\n";
            tokenPuroDel = "error";
        }

        tokens[0] = token;
        tokens[1] = tokenDel;
        tokens[2] = tokenPuro;
        tokens[3] = tokenPuroDel;
        tokens[4] = lexemaProcesado;
        return tokens;
    }

    private static String palabraReservada(String palReservada) {
        switch (palReservada) {
            case "if":
                return "codif";
            case "else":
                return "codelse";
            case "int":
                return "codint";
            case "boolean":
                return "codboolean";
            case "string":
                return "codstring";
            case "float":
                return "codfloat";
            case "return":
                return "codreturn";
            case "function":
                return "codfunction";
            case "let":
                return "codlet";
            case "read":
                return "codread";
            case "write":
                return "codwrite";
            case "void":
                return "codvoid";
            default:
                return "codid";
        }
    }

    public static String convertirALexema(ArrayList<Character> word) {
        String lex = "";
        int tam = word.size();
        for (int j = 0; j < tam; j++) {
            char letra = word.get(0);
            word.remove(0);
            lex = lex + letra;
        }
        return lex;
    }

    private static boolean esId(String s) {
        if (s.isEmpty())
            return false;

        char c0 = s.charAt(0);
        if (!(Character.isLetter(c0) || c0 == '_'))
            return false;

        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_'))
                return false;
        }
        return true;
    }

    public static boolean esCteEntera(String s) {
        if (s.isEmpty())
            return false;

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }
        return true;
    }

    public static boolean esCteReal(String s) {
        int posPunto = s.indexOf('.');
        if (posPunto <= 0 || posPunto == s.length() - 1) 
            return false;

        for (int i = 0; i < s.length(); i++) {
            if (i != posPunto && !Character.isDigit(s.charAt(i))) 
                return false;
        }
        return true;
    }
}
