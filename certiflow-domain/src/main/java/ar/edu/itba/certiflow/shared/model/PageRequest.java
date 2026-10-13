package ar.edu.itba.certiflow.shared.model;

/** Qué página de un listado se pide: el número, desde cero, y cuántos elementos trae. */
public record PageRequest(int number, int size) {

    public PageRequest {
        if (number < 0 || size < 1) {
            throw new IllegalArgumentException("page must be 0 or more and size 1 or more");
        }
    }

    public long offset() {
        return (long) number * size;
    }
}
