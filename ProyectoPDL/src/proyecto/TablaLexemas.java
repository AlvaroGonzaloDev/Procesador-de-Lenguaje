package proyecto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class TablaLexemas {
    private int sigPosTS = 1;
    private final Map<String, Integer> posPorLexema = new LinkedHashMap<>();
    private final ArrayList<String> lexemasPorPos = new ArrayList<>();

    // Asigna un identificador (posición tabla de símbolos) a cada lexema
    public int insertarLexema(String lexema) {
        Integer pos = posPorLexema.get(lexema);
        if (pos != null) 
            return pos;

        int nuevaPos = sigPosTS++;
        posPorLexema.put(lexema, nuevaPos);
        lexemasPorPos.add(lexema);
        return nuevaPos;
    }

    // Recupera el lexema a partir de un identificador (posición tabla de símbolos)
    public String obtenerLexema(int posTS) {
        if (posTS <= 0 || posTS > lexemasPorPos.size()) 
            return null;

        return lexemasPorPos.get(posTS - 1);
    }
}
