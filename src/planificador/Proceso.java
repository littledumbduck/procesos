package planificador;

public class Proceso {
    private final String nombre;
    private final int llegada;
    private final int rafaga;
    private final int ordenFichero;

    private int restante;
    private EstadoProceso estado;
    private int instanteInicio;
    private int instanteFin;

    public Proceso(String nombre, int llegada, int rafaga, int ordenFichero) {
        this.nombre = nombre;
        this.llegada = llegada;
        this.rafaga = rafaga;
        this.ordenFichero = ordenFichero;
        this.restante = rafaga;
        this.estado = EstadoProceso.NUEVO;
        this.instanteInicio = -1;
        this.instanteFin = -1;
    }

    public Proceso copiar() {
        return new Proceso(this.nombre, this.llegada, this.rafaga, this.ordenFichero);
    }

    // Métricas del PDF (Página 4)
    public int getRetorno() { return instanteFin - llegada; }

    public int getEspera() { return getRetorno() - rafaga; }

    public int getRespuesta() { return instanteInicio - llegada; }

    public void decrementarRestante() { this.restante--; }

    // Getters y Setters
    public String getNombre() { return nombre; }

    public int getLlegada() { return llegada; }

    public int getRafaga() { return rafaga; }

    public int getOrdenFichero() { return ordenFichero; }

    public int getRestante() { return restante; }

    public EstadoProceso getEstado() { return estado; }

    public void setEstado(EstadoProceso estado) { this.estado = estado; }

    public int getInstanteInicio() { return instanteInicio; }

    public void setInstanteInicio(int instanteInicio) { this.instanteInicio = instanteInicio; }

    public int getInstanteFin() { return instanteFin; }

    public void setInstanteFin(int instanteFin) { this.instanteFin = instanteFin; }

}