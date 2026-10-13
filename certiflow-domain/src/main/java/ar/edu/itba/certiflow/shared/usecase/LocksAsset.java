package ar.edu.itba.certiflow.shared.usecase;

/** Un comando que cambia algo de un activo: se ejecuta de a uno por activo. */
public interface LocksAsset {

    AssetLockKey assetLockKey();
}
