package proyecto;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AnalizadorSemantico {

    public static class Simbolo { // Representa una entrada de la TS, que puede ser: variable, parámetro, función
        public final String nombre;
        public final String clase; // "var", "param", "funcion"
        public final int lineaDecl; // línea donde se declara explícitamente
        public final boolean implicita; // true si se creó por uso sin declarar

        // Para VAR/PARAM
        public final String tipo; // "entero", "real", "logico", "cadena"
        public final int despl;

        // Para FUNCION
        public final int numParam;
        public final ArrayList<String> tiposParam; // lista de tipos
        public final String tipoRetorno; // "entero", "real", "logico", "cadena", "vacio"
        public final String etiq; // EtiqFuncion

        private Simbolo(String nombre, String clase, String tipo, int despl, int lineaDecl, boolean implicita,
                int numParam, ArrayList<String> tiposParam, String tipoRetorno, String etiq) {
            this.nombre = nombre;
            this.clase = clase;
            this.tipo = tipo;
            this.despl = despl;
            this.lineaDecl = lineaDecl;
            this.implicita = implicita;
            this.numParam = numParam;
            this.tiposParam = tiposParam;
            this.tipoRetorno = tipoRetorno;
            this.etiq = etiq;
        }
    }

    public static class TablaSimbolos {
        public final int numero;
        public final String nombreTabla;
        public final Map<String, Simbolo> tabla = new LinkedHashMap<String, Simbolo>();
        private int desplActual = 0;

        public TablaSimbolos(int numero, String nombreTabla) {
            this.numero = numero;
            this.nombreTabla = nombreTabla;
        }

        private int sizeOf(String tipo) {
            if (tipo.equals("entero") || tipo.equals("logico"))
                return 1;
            if (tipo.equals("real"))
                return 2;
            if (tipo.equals("cadena"))
                return 64;
            return 0;
        }
    }

    private FileWriter escritorTS;
    private GestorErrores errores;
    private int numTabla = 0; // TSGlobal es #0
    private int contadorEtiq = 1;
    private final TablaSimbolos tsGlobal;
    private TablaSimbolos tsLocal = null;
    private String retornoFuncionActual = null; // "entero"/"real"/"logico"/"cadena"/"vacio"
    private String nombreFuncionActual = null;
    private final TablaLexemas tablaLexemas;

    public AnalizadorSemantico(GestorErrores errores, TablaLexemas tablaLexemas) throws IOException {
        this.errores = errores;
        this.tablaLexemas = tablaLexemas;
        escritorTS = new FileWriter("tablaDeSimbolos.txt");
        tsGlobal = new TablaSimbolos(numTabla, "TSGlobal");
    }

    public void cerrar() throws IOException {
        volcarTabla(tsGlobal);
        escritorTS.close();
    }

    public void crearAmbitoFuncion(String nombreFuncion, String tipoRetorno) { /* Crea TS local para una función. */
        numTabla++;
        tsLocal = new TablaSimbolos(numTabla, nombreFuncion);
        retornoFuncionActual = tipoRetorno;
        nombreFuncionActual = nombreFuncion;
    }

    public void destruirAmbitoFuncion() throws IOException { /*
                                                              * Vuelca la TS local actual en el fichero y la destruye.
                                                              */
        if (tsLocal != null) {
            volcarTabla(tsLocal);
        }
        tsLocal = null;
        retornoFuncionActual = null;
        nombreFuncionActual = null;
    }

    public String getRetornoFuncionActual() {
        return retornoFuncionActual;
    }

    public String getNombreFuncionActual() {
        return nombreFuncionActual;
    }

    public void declararVariable(String tipo, Token tokenId) throws IOException { // Declara una variable en el ámbito
                                                                                  // actual (local si existe, si no
                                                                                  // global)
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        TablaSimbolos t;
        if (tsLocal != null)
            t = tsLocal;
        else
            t = tsGlobal;

        if (t.tabla.containsKey(nombre)) {
            errores.errorSemantico(tokenId.linea, "La variable '" + nombre
                    + "' ya esta declarada en este ambito. Elimine una de las declaraciones o cambie el nombre.");
            return;
        }

        int despl = t.desplActual;
        t.desplActual += t.sizeOf(tipo);
        Simbolo s = new Simbolo(nombre, "var", tipo, despl, tokenId.linea, false, 0, null, null, null);
        t.tabla.put(s.nombre, s);
    }

    public void declararParametroFuncion(String tipo, Token tokenId) throws IOException { // Declara un parámetro de una
                                                                                          // funcion en la TS local
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        if (tsLocal == null)
            return;

        if (tsLocal.tabla.containsKey(nombre)) {
            errores.errorSemantico(tokenId.linea,
                    "El parametro '" + nombre + "' esta repetido en la lista de parametros de la funcion.");
            return;
        }

        int despl = tsLocal.desplActual;
        tsLocal.desplActual += tsLocal.sizeOf(tipo);
        Simbolo s = new Simbolo(nombre, "param", tipo, despl, tokenId.linea, false, 0, null, null, null);
        tsLocal.tabla.put(s.nombre, s);
    }

    public void declararFuncion(String tipoRetorno, Token tokenId, ArrayList<String> tiposParam) throws IOException { // Declara
                                                                                                                      // la
                                                                                                                      // función
                                                                                                                      // en
                                                                                                                      // la
                                                                                                                      // TS
                                                                                                                      // global
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        if (tsGlobal.tabla.containsKey(nombre)) {
            errores.errorSemantico(tokenId.linea,
                    "La funcion '" + nombre + "' ya esta declarada. Cambie el nombre o elimine una definicion.");
            return;
        }

        String etiq = "Et" + contadorEtiq;
        contadorEtiq++;
        Simbolo s = new Simbolo(nombre, "funcion", null, -1, tokenId.linea, false, tiposParam.size(),
                new ArrayList<>(tiposParam), tipoRetorno, etiq);
        tsGlobal.tabla.put(s.nombre, s);
    }

    private Simbolo declararImplicitaGlobalEntera(Token tokenId) throws IOException {
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        Simbolo yaExiste = tsGlobal.tabla.get(nombre);
        if (yaExiste != null)
            return yaExiste;

        int despl = tsGlobal.desplActual;
        tsGlobal.desplActual += tsGlobal.sizeOf("entero");
        Simbolo s = new Simbolo(nombre, "var", "entero", despl, tokenId.linea, true, 0, null, null, null);
        tsGlobal.tabla.put(s.nombre, s);
        return s;
    }

    public void comprobarAutoIncr(Token tokenId) throws IOException {
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        Simbolo s = resolverId(tokenId);
        if (s == null) {
            return;
        }
        if (s.clase.equals("funcion")) {
            errores.errorSemantico(tokenId.linea, "No se puede aplicar '++' a la funcion '" + nombre + "'.");
            return;
        }
        if (!s.tipo.equals("entero")) {
            errores.errorSemantico(tokenId.linea, "El operador '++' solo puede aplicarse a variables enteras. '"
                    + nombre + "' es de tipo '" + s.tipo + "'.");
        }
    }

    public void comprobarRead(Token tokenId) throws IOException {
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        Simbolo s = resolverId(tokenId);
        if (s == null) {
            return;
        }
        if (s.clase.equals("funcion")) {
            errores.errorSemantico(tokenId.linea, "No se puede usar read sobre una funcion ('" + nombre + "').");
            return;
        }
        if (!s.tipo.equals("entero") && !s.tipo.equals("real") && !s.tipo.equals("cadena")) {
            errores.errorSemantico(tokenId.linea,
                    "La instruccion read solo puede usarse con variables de tipo entero, real o cadena. '" + nombre
                            + "' es de tipo '" + s.tipo + "'.");
        }
    }

    private Simbolo resolverId(Token tokenId) throws IOException {
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        // 1) Si hay TS local, mirar primero ahí
        if (tsLocal != null) {
            Simbolo sLocal = tsLocal.tabla.get(nombre);
            if (sLocal != null) {
                if (!sLocal.implicita && tokenId.linea < sLocal.lineaDecl) {
                    errores.errorSemantico(tokenId.linea, "La variable local'" + nombre
                            + "' se usa antes de su declaracion (declarada en la linea " + sLocal.lineaDecl + ").");
                    return null;
                }
                return sLocal;
            }
        }

        // 2) Si no está en local, mirar global
        Simbolo sGlobal = tsGlobal.tabla.get(nombre);
        if (sGlobal == null) {
            return declararImplicitaGlobalEntera(tokenId);
        }

        // 3) Si es global EXPLÍCITA (implicita=false) y se usa antes de su declaración
        if (!sGlobal.implicita && tokenId.linea < sGlobal.lineaDecl) {
            errores.errorSemantico(tokenId.linea, "La variable '" + nombre
                    + "' se usa antes de su declaracion (declarada en la linea " + sGlobal.lineaDecl + ").");
            return null;
        }
        return sGlobal;
    }

    public String nombreDeId(Token tokenId) {
        return tablaLexemas.obtenerLexema(tokenId.posTS);
    }

    public String tipoDeId(Token tokenId) throws IOException {
        String nombre = tablaLexemas.obtenerLexema(tokenId.posTS);
        Simbolo s = resolverId(tokenId);
        if (s == null)
            return "error";

        // Si es función, error
        if (s.clase.equals("funcion")) {
            errores.errorSemantico(tokenId.linea,
                    "El identificador '" + nombre + "' es una funcion y no puede usarse como variable.");
            return "error";
        }
        return s.tipo;
    }

    private Simbolo buscarFuncion(String nombre) {
        Simbolo s = tsGlobal.tabla.get(nombre);
        if (s == null)
            return null;

        if (!s.clase.equals("funcion"))
            return null;

        return s;
    }

    public String comprobarLlamadaFuncion(Token idFun, ArrayList<String> tiposArgs, boolean usadaEnExpresion)
            throws IOException {
        String nombre = tablaLexemas.obtenerLexema(idFun.posTS);
        if (tsLocal != null) {
            Simbolo sLocal = tsLocal.tabla.get(nombre);
            if (sLocal != null && (sLocal.clase.equals("var") || sLocal.clase.equals("param"))) {
                errores.errorSemantico(idFun.linea, "El identificador '" + nombre + "' es una " + sLocal.clase
                        + " y no puede usarse como funcion en este ambito.");
                return "error";
            }
        }
        Simbolo g = tsGlobal.tabla.get(nombre);
        if (g != null && !g.clase.equals("funcion")) {
            errores.errorSemantico(idFun.linea,
                    "El identificador '" + nombre + "' es una " + g.clase + " y no puede usarse como funcion.");
            return "error";
        }
        Simbolo s = buscarFuncion(nombre);
        if (s == null) {
            errores.errorSemantico(idFun.linea, "La funcion '" + nombre + "' no esta declarada.");
            return "error";
        }
        if (idFun.linea < s.lineaDecl) {
            errores.errorSemantico(idFun.linea, "La funcion '" + nombre
                    + "' se usa antes de su definicion (definida en la linea " + s.lineaDecl + ").");
            return "error";
        }
        if (s.numParam != tiposArgs.size()) {
            errores.errorSemantico(idFun.linea, "Llamada incorrecta a '" + nombre + "': se esperaban " + s.numParam
                    + " argumentos y se han dado " + tiposArgs.size() + ".");
            return "error";
        }
        for (int i = 0; i < s.numParam; i++) {
            String esperado = s.tiposParam.get(i);
            String recibido = tiposArgs.get(i);
            if (!recibido.equals("error") && !esperado.equals(recibido)) {
                int aux = i + 1;
                errores.errorSemantico(idFun.linea, "Llamada incorrecta a '" + nombre + "': el argumento " + aux
                        + " deberia ser de tipo '" + esperado + "' pero es de tipo '" + recibido + "'.");
                return "error";
            }
        }
        if (usadaEnExpresion && "vacio".equals(s.tipoRetorno)) {
            errores.errorSemantico(idFun.linea,
                    "La funcion '" + nombre + "' no devuelve valor (void) y no puede usarse en una expresion.");
            return "error";
        }
        return s.tipoRetorno;
    }

    private void volcarTabla(TablaSimbolos ts) throws IOException {
        if (ts.numero == 0) {
            escritorTS.write("TSGlobal #" + ts.numero + ":\n");
        } else {
            escritorTS.write("TS de " + ts.nombreTabla + " #" + ts.numero + ":\n");
        }
        for (Simbolo s : ts.tabla.values()) {
            escritorTS.write("* '" + s.nombre + "'\n");

            if (s.clase.equals("var") || s.clase.equals("param")) {
                escritorTS.write("   + Tipo: '" + s.tipo + "'\n");
                escritorTS.write("   + Despl: " + s.despl + "\n");
            } else if (s.clase.equals("funcion")) {
                escritorTS.write("   + Tipo: 'funcion'\n");
                escritorTS.write("   + numParam: " + s.numParam + "\n");
                for (int i = 0; i < s.numParam; i++) {
                    int aux = i + 1;
                    escritorTS.write("   + TipoParam" + aux + ": '" + s.tiposParam.get(i) + "'\n");
                }
                escritorTS.write("   + TipoRetorno: '" + s.tipoRetorno + "'\n");
                escritorTS.write("   + EtiqFuncion: '" + s.etiq + "'\n");
            }
        }
        escritorTS.write("\n");
    }
}
