package proyecto;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class AnalizadorSintactico2 {
    private static AnalizadorLexico aLex;
    private static AnalizadorSemantico aSem;
    private static Token sigToken;
    private static GestorErrores errores;

    public static void main(String[] args) {
        try (FileWriter escritor = new FileWriter("parse.txt")) {
            errores = new GestorErrores();
            TablaLexemas tablaLexemas = new TablaLexemas();
            aSem = new AnalizadorSemantico(errores, tablaLexemas);
            aLex = new AnalizadorLexico(errores, tablaLexemas);
            aLex.ALexicoInit();
            sigToken = aLex.ALexico();
            escritor.write("Descendente ");
            if (sigToken == null)
                sigToken = new Token("$", null, 0);

            P(escritor);
            if (!sigToken.tipo.equals("$")) {
                errorEsperado("fin de fichero");
            }
            aSem.cerrar();
            errores.cerrar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* FIRST */

    public static boolean FirstB() { // FIRST(B) = { if, let } U FIRST(S)
        return sigToken.tipo.equals("if") || sigToken.tipo.equals("let") || FirstS();
    }

    public static boolean FirstF() { // FIRST(F) = { function }
        return sigToken.tipo.equals("function");
    }

    private static boolean FirstS() { // FIRST(S) = { id, write, read, return, ++ }
        return sigToken.tipo.equals("id") || sigToken.tipo.equals("write") || sigToken.tipo.equals("read")
                || sigToken.tipo.equals("return") || sigToken.tipo.equals("++");
    }

    private static boolean FirstT() { // FIRST(T) = { int, float, boolean, string }
        return sigToken.tipo.equals("int") || sigToken.tipo.equals("float")
                || sigToken.tipo.equals("boolean") || sigToken.tipo.equals("string");
    }

    public static boolean FirstE() { // FIRST(E) = { ! } U FIRST(R) = { !, id, ++, (, cteentera, ctereal, cadena }
        return sigToken.tipo.equals("!") || sigToken.tipo.equals("id") || sigToken.tipo.equals("++")
                || sigToken.tipo.equals("(") || sigToken.tipo.equals("cteentera")
                || sigToken.tipo.equals("ctereal") || sigToken.tipo.equals("cadena");
    }

    /* 1,2,3: P */

    public static void P(FileWriter escritor) throws IOException {
        if (FirstB()) {
            escritor.write("1 ");
            B(escritor);
            P(escritor);
        } else if (FirstF()) {
            escritor.write("2 ");
            F(escritor);
            P(escritor);
        } else if (sigToken.tipo.equals("$")) {
            escritor.write("3 ");
        } else {
            errorEsperado("'if', 'let', 'function' o fin de fichero");
        }
    }

    /* 4,5,6: B */

    public static void B(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals("if")) {
            escritor.write("4 "); // B -> if ( E ) B2
            Token tokIf = sigToken;
            equipara("if");
            if (!sigToken.tipo.equals("(")) {
                errorEsperado("'(' despues de if");
            }
            equipara("(");
            String tCond = E(escritor);
            if (!tCond.equals("error") && !tCond.equals("logico")) {
                errores.errorSemantico(tokIf.linea, "La condicion del if debe ser de tipo logico.");
            }
            if (!sigToken.tipo.equals(")")) {
                errorEsperado("')' despues de la condicion del if");
            }
            equipara(")");
            if (!FirstS() && !sigToken.tipo.equals("{")) {
                errorEsperado("una sentencia o '{' despues del if");
            }
            B2(escritor);
        } else if (sigToken.tipo.equals("let")) {
            escritor.write("5 "); // B -> let T id ;
            equipara("let");
            if (!FirstT()) {
                errorEsperado("un tipo ('int', 'float', 'boolean' o 'string') despues de 'let'");
            }
            String tipo = T(escritor);
            if (!sigToken.tipo.equals("id")) {
                errorEsperado("un identificador despues del tipo");
            }
            Token tokenId = sigToken;
            equipara("id");
            aSem.declararVariable(tipo, tokenId);
            if (!sigToken.tipo.equals(";")) {
                errorEsperado("';' al final de la declaracion");
            }
            equipara(";");
        } else if (FirstS()) {
            escritor.write("6 "); // B -> S
            S(escritor);
        } else {
            errorEsperado("'if', 'let' o una sentencia");
        }
    }

    /* 7,8: B2 */

    private static void B2(FileWriter escritor) throws IOException {
        // B2 -> S B1 | B4 B1
        if (FirstS()) {
            escritor.write("7 "); // B2 -> S B1
            S(escritor);
            B1(escritor);
        } else if (sigToken.tipo.equals("{")) {
            escritor.write("8 "); // B2 -> B4 B1
            B4(escritor);
            B1(escritor);
        } else {
            errorEsperado("una sentencia o '{' para iniciar un bloque");
        }
    }

    /* 9,10: B1 */

    private static void B1(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals("else")) {
            escritor.write("9 "); // B1 -> else B3
            equipara("else");
            if (!FirstS() && !sigToken.tipo.equals("{")) {
                errorEsperado("una sentencia o '{' despues de else");
            }
            B3(escritor);
        } else {
            escritor.write("10 "); // B1 -> lambda
        }
    }

    /* 11,12: B3 */

    private static void B3(FileWriter escritor) throws IOException {
        // B3 -> S | B4
        if (FirstS()) {
            escritor.write("11 "); // B3 -> S
            S(escritor);
        } else if (sigToken.tipo.equals("{")) {
            escritor.write("12 "); // B3 -> B4
            B4(escritor);
        } else {
            errorEsperado("una sentencia o '{' para iniciar un bloque");
        }
    }

    /* 13: B4 */

    private static void B4(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals("{")) {
            escritor.write("13 "); // B4 -> { C }
            equipara("{");
            C(escritor);
            if (!sigToken.tipo.equals("}")) {
                errorEsperado("'}' para cerrar el bloque");
            }
            equipara("}");
        } else {
            errorEsperado("'{' para iniciar un bloque");
        }
    }

    /* 14-17: T */

    private static String T(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals("int")) {
            escritor.write("14 ");
            equipara("int");
            return "entero";
        } else if (sigToken.tipo.equals("float")) {
            escritor.write("15 ");
            equipara("float");
            return "real";
        } else if (sigToken.tipo.equals("boolean")) {
            escritor.write("16 ");
            equipara("boolean");
            return "logico";
        } else if (sigToken.tipo.equals("string")) {
            escritor.write("17 ");
            equipara("string");
            return "cadena";
        } else {
            errorEsperado("un tipo ('int', 'float', 'boolean' o 'string')");
            return null;
        }
    }

    /* 18-22: S */

    public static void S(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals("id")) {
            escritor.write("18 "); // S -> id S1
            Token idTok = sigToken;
            equipara("id");
            S1(escritor, idTok);
        } else if (sigToken.tipo.equals("write")) {
            escritor.write("19 "); // S -> write E ;
            Token tokWrite = sigToken;
            equipara("write");
            if (!FirstE()) {
                errorEsperado("una expresion despues de 'write'");
            }
            String t = E(escritor);
            if (!t.equals("error") && !t.equals("entero") && !t.equals("real") && !t.equals("cadena")) {
                errores.errorSemantico(tokWrite.linea,
                        "La instruccion write solo admite expresiones de tipo entero, real o cadena.");
            }
            if (!sigToken.tipo.equals(";")) {
                errorEsperado("';' al final de la sentencia write");
            }
            equipara(";");
        } else if (sigToken.tipo.equals("read")) {
            escritor.write("20 "); // S -> read id ;
            equipara("read");
            if (!sigToken.tipo.equals("id")) {
                errorEsperado("un identificador despues de 'read'");
            }
            Token idTok = sigToken;
            equipara("id");
            aSem.comprobarRead(idTok);
            if (!sigToken.tipo.equals(";")) {
                errorEsperado("';' al final de la sentencia read");
            }
            equipara(";");
        } else if (sigToken.tipo.equals("return")) {
            escritor.write("21 "); // S -> return X ;
            Token tokReturn = sigToken;
            equipara("return");
            String tipoFun = aSem.getRetornoFuncionActual();
            String nombreFun = aSem.getNombreFuncionActual();
            if (tipoFun == null) {
                errores.errorSemantico(tokReturn.linea, "La sentencia return solo puede usarse dentro de una funcion.");
            }
            if (!FirstE() && !sigToken.tipo.equals(";")) {
                errorEsperado("una expresion o ';' despues de 'return'");
            }
            String tipoDevuelto = X(escritor);
            if (tipoFun != null) {
                if (tipoFun.equals("vacio")) {
                    if (!tipoDevuelto.equals("vacio"))
                        errores.errorSemantico(tokReturn.linea,
                                "En la funcion '" + nombreFun + "' (void) no se puede devolver un valor con return.");
                } else {
                    if (tipoDevuelto.equals("vacio")) {
                        errores.errorSemantico(tokReturn.linea, "En la funcion '" + nombreFun
                                + "' se debe devolver un valor de tipo '" + tipoFun + "'.");
                    } else if (!tipoDevuelto.equals("error") && !tipoFun.equals(tipoDevuelto)) {
                        errores.errorSemantico(tokReturn.linea, "Tipo de retorno incorrecto en la funcion '" + nombreFun
                                + "': se esperaba '" + tipoFun + "' pero el return devuelve '" + tipoDevuelto + "'.");
                    }
                }
            }
            if (!sigToken.tipo.equals(";")) {
                errorEsperado("';' al final del return");
            }
            equipara(";");
        } else if (sigToken.tipo.equals("++")) {
            escritor.write("22 "); // S -> ++ id ;
            equipara("++");
            if (!sigToken.tipo.equals("id")) {
                errorEsperado("un identificador despues de '++'");
            }
            Token idTok = sigToken;
            equipara("id");
            aSem.comprobarAutoIncr(idTok);
            if (!sigToken.tipo.equals(";")) {
                errorEsperado("';' al final de la sentencia");
            }
            equipara(";");
        } else {
            errorEsperado("una sentencia (id, write, read, return o '++')");
        }
    }

    /* 23,24: S1 */

    private static void S1(FileWriter escritor, Token idIzq) throws IOException {
        if (sigToken.tipo.equals("=")) {
            escritor.write("23 "); // S1 -> = E ;
            equipara("=");
            String tipoExpr = E(escritor);
            String tipoId = aSem.tipoDeId(idIzq);
            String nombreId = aSem.nombreDeId(idIzq);
            if (!tipoExpr.equals("error") && !tipoId.equals("error") && !tipoId.equals(tipoExpr)) {
                errores.errorSemantico(idIzq.linea, "Asignacion invalida: no se puede asignar una expresion de tipo '"
                        + tipoExpr + "' a la variable '" + nombreId + "' de tipo '" + tipoId + "'.");
            }
            if (!sigToken.tipo.equals(";")) {
                errorEsperado("';' al final de la asignacion");
            }
            equipara(";");
        } else if (sigToken.tipo.equals("(")) {
            escritor.write("24 "); // S1 -> ( L ) ;
            equipara("(");
            ArrayList<String> tiposArgs = L(escritor);
            if (!sigToken.tipo.equals(")")) {
                errorEsperado("')' para cerrar la lista de argumentos de la funcion");
            }
            equipara(")");
            aSem.comprobarLlamadaFuncion(idIzq, tiposArgs, false);
            if (!sigToken.tipo.equals(";")) {
                errorEsperado("';' al final de la llamada a funcion");
            }
            equipara(";");
        } else {
            errorEsperado("'=' para asignacion o '(' para llamada a funcion despues de un identificador");
        }
    }

    /* 25,26: L */

    public static ArrayList<String> L(FileWriter escritor) throws IOException {
        if (FirstE()) {
            escritor.write("25 "); // L -> E Q
            ArrayList<String> lista = new ArrayList<>();
            lista.add(E(escritor));
            lista.addAll(Q(escritor));
            return lista;
        } else {
            escritor.write("26 "); // L -> lambda
            return new ArrayList<>();
        }
    }

    /* 27,28: Q */

    private static ArrayList<String> Q(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals(",")) {
            escritor.write("27 "); // Q -> , E Q
            equipara(",");
            ArrayList<String> lista = new ArrayList<>();
            if (!FirstE()) {
                errorEsperado("una expresion despues de ','");
            }
            lista.add(E(escritor));
            lista.addAll(Q(escritor));
            return lista;
        } else {
            escritor.write("28 "); // Q -> lambda
            return new ArrayList<>();
        }
    }

    /* 29,30: X */

    private static String X(FileWriter escritor) throws IOException {
        if (FirstE()) {
            escritor.write("29 "); // X -> E
            return E(escritor);
        } else {
            escritor.write("30 "); // X -> lambda
            return "vacio";
        }
    }

    /* 31,32: E */

    public static String E(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals("!")) {
            escritor.write("32 "); // E -> ! R
            Token tokNegado = sigToken;
            equipara("!");
            if (!FirstE()) {
                errorEsperado("una expresion despues de '!'");
            }
            String t = R(escritor);
            if (!t.equals("error") && !t.equals("logico")) {
                errores.errorSemantico(tokNegado.linea, "El operador '!' solo puede aplicarse a expresiones logicas.");
                return "error";
            }
            return "logico";
        } else if (FirstE()) {
            escritor.write("31 "); // E -> R
            return R(escritor);
        } else {
            errorEsperado("una expresion");
            return "error";
        }
    }

    /* 33: R */

    public static String R(FileWriter escritor) throws IOException {
        escritor.write("33 "); // R -> U R1
        String tIzq = U(escritor);
        return R1(escritor, tIzq);
    }

    /* 34,35: R1 */

    public static String R1(FileWriter escritor, String tipoIzq) throws IOException {
        if (sigToken.tipo.equals("<")) {
            Token tokOp = sigToken;
            escritor.write("34 "); // R1 -> < U R1
            equipara("<");
            String tipoDer = U(escritor);
            boolean izqNum = tipoIzq.equals("entero") || tipoIzq.equals("real");
            boolean derNum = tipoDer.equals("entero") || tipoDer.equals("real");
            if (!izqNum || !derNum || !tipoIzq.equals(tipoDer)) {
                errores.errorSemantico(tokOp.linea,
                        "El operador '<' solo puede compararse entre numeros del mismo tipo (entero con entero o real con real).");
                return "error";
            }
            return R1(escritor, "logico");
        } else {
            escritor.write("35 "); // R1 -> lambda
            return tipoIzq;
        }
    }

    /* 36: U */

    public static String U(FileWriter escritor) throws IOException {
        escritor.write("36 "); // U -> V U1
        String tipoIzq = V(escritor);
        return U1(escritor, tipoIzq);
    }

    /* 37,38: U1 */

    public static String U1(FileWriter escritor, String tipoIzq) throws IOException {
        if (sigToken.tipo.equals("-")) {
            Token tokOp = sigToken;
            escritor.write("37 "); // U1 -> - V U1
            equipara("-");
            String tipoDer = V(escritor);
            boolean izqNum = tipoIzq.equals("entero") || tipoIzq.equals("real");
            boolean derNum = tipoDer.equals("entero") || tipoDer.equals("real");
            if (!izqNum || !derNum || !tipoIzq.equals(tipoDer)) {
                errores.errorSemantico(tokOp.linea,
                        "El operador '-' solo puede aplicarse a numeros del mismo tipo (entero-entero o real-real).");
                return "error";
            }
            return U1(escritor, tipoIzq);
        } else {
            escritor.write("38 "); // U1 -> lambda
            return tipoIzq;
        }
    }

    /* 39-44: V */

    public static String V(FileWriter escritor) throws IOException {
        if (sigToken.tipo.equals("id")) {
            escritor.write("39 "); // V -> id V1
            Token idTok = sigToken;
            equipara("id");
            return V1(escritor, idTok);
        } else if (sigToken.tipo.equals("++")) {
            escritor.write("40 "); // V -> ++ id
            equipara("++");
            if (!sigToken.tipo.equals("id")) {
                errorEsperado("un identificador despues de '++' en una expresion");
            }
            Token idTok = sigToken;
            equipara("id");
            aSem.comprobarAutoIncr(idTok);
            return "entero";
        } else if (sigToken.tipo.equals("(")) {
            escritor.write("41 "); // V -> ( E )
            equipara("(");
            String t = E(escritor);
            if (!sigToken.tipo.equals(")")) {
                errorEsperado("')' para cerrar la expresion entre parentesis");
            }
            equipara(")");
            return t;
        } else if (sigToken.tipo.equals("cteentera")) {
            escritor.write("42 "); // V -> cteentera
            equipara("cteentera");
            return "entero";
        } else if (sigToken.tipo.equals("ctereal")) {
            escritor.write("43 "); // V -> ctereal
            equipara("ctereal");
            return "real";
        } else if (sigToken.tipo.equals("cadena")) {
            escritor.write("44 "); // V -> cadena
            equipara("cadena");
            return "cadena";
        } else {
            errorEsperado("un identificador, '++', '(', una constante entera, real o una cadena");
            return "error";
        }
    }

    /* 45,46: V1 */

    private static String V1(FileWriter escritor, Token idTok) throws IOException {
        if (sigToken.tipo.equals("(")) {
            escritor.write("45 "); // V1 -> ( L )
            equipara("(");
            ArrayList<String> tiposArgs = L(escritor);
            if (!sigToken.tipo.equals(")")) {
                errorEsperado("')' para cerrar la lista de argumentos");
            }
            equipara(")");
            return aSem.comprobarLlamadaFuncion(idTok, tiposArgs, true);
        } else {
            escritor.write("46 "); // V1 -> lambda
            return aSem.tipoDeId(idTok);
        }
    }

    /* 47: F */

    private static void F(FileWriter escritor) throws IOException {
        if (!sigToken.tipo.equals("function")) {
            errorEsperado("'function' para iniciar una definicion de funcion");
        }
        escritor.write("47 "); // F -> function H id ( A ) { C }
        equipara("function");
        String tipoRetorno = H(escritor);
        if (!sigToken.tipo.equals("id")) {
            errorEsperado("un identificador como nombre de funcion");
        }
        Token idFun = sigToken;
        equipara("id");
        aSem.crearAmbitoFuncion(aSem.nombreDeId(idFun), tipoRetorno);
        ArrayList<String> tiposParam = new ArrayList<>();
        if (!sigToken.tipo.equals("(")) {
            errorEsperado("'(' despues del nombre de funcion");
        }
        equipara("(");
        A(escritor, tiposParam);
        if (!sigToken.tipo.equals(")")) {
            errorEsperado("')' para cerrar la lista de parametros");
        }
        equipara(")");
        aSem.declararFuncion(tipoRetorno, idFun, tiposParam);
        if (!sigToken.tipo.equals("{")) {
            errorEsperado("'{' para abrir el cuerpo de la funcion");
        }
        equipara("{");
        C(escritor);
        if (!sigToken.tipo.equals("}")) {
            errorEsperado("'}' para cerrar el cuerpo de la funcion");
        }
        equipara("}");
        aSem.destruirAmbitoFuncion();
    }

    /* 48,49: H */

    private static String H(FileWriter escritor) throws IOException {
        if (FirstT()) {
            escritor.write("48 "); // H -> T
            return T(escritor);
        } else if (sigToken.tipo.equals("void")) {
            escritor.write("49 "); // H -> void
            equipara("void");
            return "vacio";
        } else {
            errorEsperado("un tipo o 'void' como tipo de retorno");
            return null;
        }
    }

    /* 50,51: A */

    private static void A(FileWriter escritor, ArrayList<String> tiposParam) throws IOException {
        if (FirstT()) {
            escritor.write("50 "); // A -> T id K
            String tipo = T(escritor);
            if (!sigToken.tipo.equals("id")) {
                errorEsperado("un identificador como nombre de parametro");
            }
            Token idParam = sigToken;
            equipara("id");
            aSem.declararParametroFuncion(tipo, idParam);
            tiposParam.add(tipo);
            K(escritor, tiposParam);
        } else if (sigToken.tipo.equals("void")) {
            escritor.write("51 "); // A -> void
            equipara("void");
        } else {
            errorEsperado("un tipo para el parametro o 'void' si no hay parametros");
        }
    }

    /* 52,53: K */

    private static void K(FileWriter escritor, ArrayList<String> tiposParam) throws IOException {
        if (sigToken.tipo.equals(",")) {
            escritor.write("52 "); // K -> , T id K
            equipara(",");
            String tipo = T(escritor);
            if (!sigToken.tipo.equals("id")) {
                errorEsperado("un identificador como nombre de parametro");
            }
            Token idParam = sigToken;
            equipara("id");
            aSem.declararParametroFuncion(tipo, idParam);
            tiposParam.add(tipo);
            K(escritor, tiposParam);
        } else {
            escritor.write("53 "); // K -> lambda
        }
    }

    /* 54,55: C */

    private static void C(FileWriter escritor) throws IOException {
        if (FirstB()) {
            escritor.write("54 "); // C -> B C
            B(escritor);
            C(escritor);
        } else {
            escritor.write("55 "); // C -> lambda
        }
    }

    public static Token equipara(String t) {
        if (sigToken.tipo.equals(t)) {
            Token consumido = sigToken;
            sigToken = aLex.ALexico();
            if (sigToken == null) {
                sigToken = new Token("$", null, 0);
            }
            return consumido;
        } else {
            errorEsperado("'" + t + "'");
            return null;
        }
    }

    public static void errorEsperado(String esperado) {
        try {
            errores.errorSintactico(sigToken.linea,
                    "Se esperaba " + esperado + ", pero se encontro '" + sigToken.tipo + "'.");
            errores.cerrar();
        } catch (Exception e) {
            System.out.println("Error sintactico en linea " + sigToken.linea + ": se esperaba " + esperado
                    + " pero se encontro " + sigToken.tipo);
        }
        System.exit(1);
    }
}