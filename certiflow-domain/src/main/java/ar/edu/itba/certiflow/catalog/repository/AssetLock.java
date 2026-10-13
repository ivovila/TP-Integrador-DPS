package ar.edu.itba.certiflow.catalog.repository;

import ar.edu.itba.certiflow.catalog.model.AssetCode;

public interface AssetLock { void lock(AssetCode code); }
