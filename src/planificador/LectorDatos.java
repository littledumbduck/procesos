package planificador;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LectorDatos {
    public static List<Proceso> leer(String ruta) throws IOException {
        List<Proceso> lista = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            int nLinea = 0;
            int orden = 0;
            while ((linea = br.readLine()) != null) {
                nLinea++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                String[] partes = linea.split(";");
                if (partes.length != 3) {
                    throw new IllegalArgumentException("Error en línea " + nLinea + ": formato incorrecto. Se esperaba nombre;llegada;ráfaga");
                }
                String nombre = partes[0].trim();
                int llegada, rafaga;
                try {
                    llegada = Integer.parseInt(partes[1].trim());
                    rafaga = Integer.parseInt(partes[2].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Error en línea " + nLinea + ": la llegada y la ráfaga deben ser números enteros.");
                }

                if (llegada < 0) {
                    throw new IllegalArgumentException("Error en línea " + nLinea + ": el instante de llegada no puede ser negativo (" + llegada + ").");
                }
                if (rafaga <= 0) {
                    throw new IllegalArgumentException("Error en línea " + nLinea + ": la ráfaga debe ser mayor que 0 (" + rafaga + ").");
                }

                lista.add(new Proceso(nombre, llegada, rafaga, orden++));
            }
        }
        return lista;
    }
}