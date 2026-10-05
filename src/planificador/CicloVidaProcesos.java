package planificador;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PSP - UD1 · Simulador didáctico del CICLO DE VIDA DE UN PROCESO (modelo de 5 estados).
 *
 *   NUEVO -> LISTO -> EJECUCIÓN -> TERMINADO
 *                ^        |
 *                |        +--> BLOQUEADO (espera de E/S) --> LISTO
 *                +-- fin de quantum (expropiación) --+
 *
 * Simula un sistema operativo con UNA sola CPU y planificación Round Robin.
 * Cada proceso alterna ráfagas de CPU y de E/S. En cada unidad de tiempo se pinta
 * una fila con el estado de cada proceso (con colores) y los cambios de estado.
 *
 * Para usarlo en clase:
 *  - Cambia QUANTUM y observa cómo aumentan o disminuyen las expropiaciones.
 *  - Pon PASO_A_PASO = true para avanzar con ENTER y comentar cada transición.
 *  - Modifica la lista de procesos en main() (CPU-bound vs. E/S-bound).
 */
public class CicloVidaProcesos {

    // ---------------- Configuración ----------------
    static final int QUANTUM = 2;            // unidades de CPU antes de expropiar (Round Robin)
    static final int PAUSA_MS = 900;         // velocidad de la animación
    static final boolean PASO_A_PASO = false; // true = avanzar pulsando ENTER

    // ---------------- Colores ANSI ----------------
    static final String RESET = "\u001B[0m";
    static final String NEGRITA = "\u001B[1m";
    static final String GRIS = "\u001B[90m";
    static final String CIAN = "\u001B[36m";

    enum Estado {
        NUEVO("NUEVO", "\u001B[44m\u001B[97m"),          // fondo azul
        LISTO("LISTO", "\u001B[43m\u001B[30m"),          // fondo amarillo
        EJECUCION("EJECUCIÓN", "\u001B[42m\u001B[30m"),  // fondo verde
        BLOQUEADO("BLOQUEADO", "\u001B[41m\u001B[97m"),  // fondo rojo
        TERMINADO("TERMINADO", "\u001B[100m\u001B[37m"); // fondo gris

        final String texto;
        final String color;

        Estado(String texto, String color) {
            this.texto = texto;
            this.color = color;
        }

        String celda(int ancho) {
            return color + centrar(texto, ancho) + RESET;
        }

        String etiqueta() {
            return color + " " + texto + " " + RESET;
        }
    }

    // ---------------- Proceso (equivalente simplificado a un PCB) ----------------
    static class Proceso {
        final String nombre;
        final int llegada;      // instante en que se crea el proceso
        final int[] rafagas;    // posiciones pares = CPU, impares = E/S. {3,2,2} -> CPU 3, E/S 2, CPU 2

        Estado estado = null;   // null = el proceso todavía no existe
        int fase = 0;           // ráfaga en la que está
        int restante;           // unidades que le quedan de la ráfaga actual
        int quantumUsado = 0;   // CPU consumida desde que entró en ejecución
        final List<Estado> historia = new ArrayList<>();

        Proceso(String nombre, int llegada, int... rafagas) {
            this.nombre = nombre;
            this.llegada = llegada;
            this.rafagas = rafagas;
            this.restante = rafagas[0];
        }

        String describir() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rafagas.length; i++) {
                if (i > 0) sb.append(" -> ");
                sb.append(i % 2 == 0 ? "CPU " : "E/S ").append(rafagas[i]);
            }
            return sb.toString();
        }
    }

    static final int ANCHO = 11;

    public static void main(String[] args) throws InterruptedException {
        List<Proceso> procesos = List.of(
                new Proceso("P1", 0, 3, 2, 2),   // mixto
                new Proceso("P2", 1, 4),         // CPU-bound: solo calcula
                new Proceso("P3", 2, 1, 3, 1)    // E/S-bound: casi siempre esperando
        );

        Scanner teclado = new Scanner(System.in);

        mostrarDiagrama();
        mostrarProcesos(procesos);
        System.out.println(NEGRITA + "Pulsa ENTER para arrancar la simulación..." + RESET);
        teclado.nextLine();

        simular(procesos, teclado);
        mostrarGantt(procesos);
        mostrarEstadisticas(procesos);
    }

    // ---------------- Núcleo de la simulación ----------------
    static void simular(List<Proceso> procesos, Scanner teclado) throws InterruptedException {
        Deque<Proceso> colaListos = new ArrayDeque<>();
        Proceso enCPU = null;
        List<String> eventos = new ArrayList<>();

        imprimirCabecera(procesos);

        for (int t = 0; ; t++) {
            // 1. Admisión: los procesos creados en el instante anterior pasan a LISTO
            for (Proceso p : procesos) {
                if (p.estado == Estado.NUEVO) {
                    cambiar(p, Estado.LISTO, "admitido por el SO", eventos);
                    colaListos.add(p);
                }
            }

            // 2. Creación de procesos que llegan ahora
            for (Proceso p : procesos) {
                if (p.estado == null && p.llegada == t) {
                    cambiar(p, Estado.NUEVO, "se crea el proceso", eventos);
                }
            }

            // 3. Planificador: si la CPU está libre, se la da al primero de la cola
            if (enCPU == null && !colaListos.isEmpty()) {
                enCPU = colaListos.poll();
                enCPU.quantumUsado = 0;
                cambiar(enCPU, Estado.EJECUCION, "el planificador le asigna la CPU", eventos);
            }

            // 4. Pintar el estado actual
            for (Proceso p : procesos) p.historia.add(p.estado);
            imprimirFila(t, procesos, colaListos, eventos);
            eventos.clear();

            if (procesos.stream().allMatch(p -> p.estado == Estado.TERMINADO)) break;

            if (PASO_A_PASO) teclado.nextLine();
            else Thread.sleep(PAUSA_MS);

            // 5a. Los procesos BLOQUEADOS avanzan su E/S (en paralelo a la CPU)
            for (Proceso p : procesos) {
                if (p.estado == Estado.BLOQUEADO && --p.restante == 0) {
                    p.fase++;
                    p.restante = p.rafagas[p.fase];
                    cambiar(p, Estado.LISTO, "termina su E/S", eventos);
                    colaListos.add(p);
                }
            }

            // 5b. El proceso en EJECUCIÓN consume una unidad de CPU
            if (enCPU != null) {
                Proceso p = enCPU;
                p.restante--;
                p.quantumUsado++;

                if (p.restante == 0) {                 // fin de la ráfaga de CPU
                    p.fase++;
                    enCPU = null;
                    if (p.fase == p.rafagas.length) {
                        cambiar(p, Estado.TERMINADO, "fin de ejecución (exit)", eventos);
                    } else {
                        p.restante = p.rafagas[p.fase];
                        cambiar(p, Estado.BLOQUEADO, "solicita E/S de " + p.restante + " u.", eventos);
                    }
                } else if (p.quantumUsado == QUANTUM) { // expropiación por Round Robin
                    enCPU = null;
                    cambiar(p, Estado.LISTO, "agota su quantum (expropiado)", eventos);
                    colaListos.add(p);
                }
            }
        }
    }

    static void cambiar(Proceso p, Estado nuevo, String motivo, List<String> eventos) {
        String origen = (p.estado == null) ? "" : p.estado.texto + " -> ";
        eventos.add(NEGRITA + p.nombre + RESET + GRIS + ": " + origen + RESET
                + nuevo.etiqueta() + GRIS + "  (" + motivo + ")" + RESET);
        p.estado = nuevo;
    }

    // ---------------- Salida por consola ----------------
    static void mostrarDiagrama() {
        String[] lineas = {
                "   NUEVO --admitido--> LISTO --planificador--> EJECUCIÓN --exit--> TERMINADO",
                "                        ^ ^                      |   |",
                "                        | +--fin de quantum------+   |",
                "                        | fin E/S           pide E/S |",
                "                        +-------- BLOQUEADO <--------+"
        };
        System.out.println();
        System.out.println(NEGRITA + CIAN + "=== CICLO DE VIDA DE UN PROCESO (modelo de 5 estados) ===" + RESET);
        System.out.println();
        for (String l : lineas) {
            for (Estado e : Estado.values()) l = l.replace(e.texto, e.color + e.texto + RESET);
            System.out.println(l);
        }
        System.out.println();
    }

    static void mostrarProcesos(List<Proceso> procesos) {
        System.out.println(NEGRITA + "Procesos (1 CPU, Round Robin, quantum = " + QUANTUM + "):" + RESET);
        for (Proceso p : procesos) {
            System.out.printf("  %s  llega en t=%d   %s%n", p.nombre, p.llegada, p.describir());
        }
        System.out.println();
    }

    static void imprimirCabecera(List<Proceso> procesos) {
        StringBuilder sb = new StringBuilder(NEGRITA + "   t |");
        for (Proceso p : procesos) sb.append(' ').append(centrar(p.nombre, ANCHO)).append(" |");
        sb.append(" Cola de LISTOS").append(RESET);
        System.out.println(sb);
        System.out.println("-".repeat(6 + procesos.size() * (ANCHO + 3) + 16));
    }

    static void imprimirFila(int t, List<Proceso> procesos, Deque<Proceso> cola, List<String> eventos) {
        StringBuilder sb = new StringBuilder(String.format("%4d |", t));
        for (Proceso p : procesos) {
            String celda = (p.estado == null) ? " ".repeat(ANCHO) : p.estado.celda(ANCHO);
            sb.append(' ').append(celda).append(" |");
        }
        String colaTxt = cola.isEmpty() ? GRIS + "(vacía)" + RESET
                : cola.stream().map(p -> p.nombre).collect(Collectors.joining(" <- "));
        sb.append(' ').append(colaTxt);
        System.out.println(sb);
        for (String e : eventos) System.out.println("     |    > " + e);
    }

    static void mostrarGantt(List<Proceso> procesos) {
        int ticks = procesos.get(0).historia.size();
        System.out.println();
        System.out.println(NEGRITA + CIAN + "=== DIAGRAMA DE GANTT (estado en cada instante) ===" + RESET);
        StringBuilder cab = new StringBuilder("     ");
        for (int t = 0; t < ticks; t++) cab.append(String.format("%3d", t));
        System.out.println(cab);
        for (Proceso p : procesos) {
            StringBuilder fila = new StringBuilder(String.format("%4s ", p.nombre));
            // Inicial del estado dentro del bloque de color (se lee incluso sin colores)
            for (Estado e : p.historia) fila.append(e == null ? "   " : e.color + " " + e.texto.charAt(0) + " " + RESET);
            System.out.println(fila);
        }
        System.out.print("\nLeyenda: ");
        for (Estado e : Estado.values()) System.out.print(e.etiqueta() + " ");
        System.out.println();
    }

    static void mostrarEstadisticas(List<Proceso> procesos) {
        System.out.println();
        System.out.println(NEGRITA + CIAN + "=== ESTADÍSTICAS ===" + RESET);
        System.out.println("Proceso | Llegada | Fin | T. retorno | T. espera (LISTO) | T. bloqueado");
        for (Proceso p : procesos) {
            int fin = p.historia.indexOf(Estado.TERMINADO);
            long espera = p.historia.stream().filter(e -> e == Estado.LISTO).count();
            long bloqueado = p.historia.stream().filter(e -> e == Estado.BLOQUEADO).count();
            System.out.printf("  %-5s | %7d | %3d | %10d | %17d | %12d%n",
                    p.nombre, p.llegada, fin, fin - p.llegada, espera, bloqueado);
        }
    }

    static String centrar(String s, int ancho) {
        int izq = (ancho - s.length()) / 2;
        return " ".repeat(Math.max(0, izq)) + s + " ".repeat(Math.max(0, ancho - s.length() - izq));
    }
}
