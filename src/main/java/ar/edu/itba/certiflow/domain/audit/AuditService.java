package ar.edu.itba.certiflow.domain.audit;

import java.util.List;

/**
 * Puerto del dominio: conserva y devuelve entradas de auditoría. No las genera;
 * las generan los decoradores de cada agregado, que son quienes saben qué ocurrió.
 * Las implementaciones deben devolver las entradas en el orden en que fueron registradas.
 */
public interface AuditService {

    void record(AuditEntry entry);

    List<AuditEntry> entriesFor(Object subject);
}
