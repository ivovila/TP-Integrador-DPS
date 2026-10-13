package ar.edu.itba.certiflow.catalog.repository;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;

/** Lo que el catálogo necesita saber de una jurisdicción para aceptar un activo: si el sistema la conoce. */
public interface KnownJurisdictions {

    boolean isKnown(JurisdictionCode jurisdictionCode);
}
