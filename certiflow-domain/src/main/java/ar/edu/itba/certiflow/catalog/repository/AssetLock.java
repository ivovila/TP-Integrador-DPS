package ar.edu.itba.certiflow.catalog.repository;

import ar.edu.itba.certiflow.catalog.model.AssetCode;

/**
 * Deja un activo bloqueado hasta el fin de la transacción. Un caso de uso que cambia algo de un activo lo bloquea
 * antes de leer lo que va a cambiar, así dos comandos sobre el mismo activo se ejecutan de a uno.
 */
public interface AssetLock {

    void lock(AssetCode assetCode);
}
