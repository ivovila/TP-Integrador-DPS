package ar.edu.itba.certiflow.domain.shared;

/**
 * Raíz de las violaciones a reglas de negocio. Los argumentos mal formados (nulos, textos vacíos)
 * usan las excepciones estándar de Java; estas se reservan para reglas del dominio.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
