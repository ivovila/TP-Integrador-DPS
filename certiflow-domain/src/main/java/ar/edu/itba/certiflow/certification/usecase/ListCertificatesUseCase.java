package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import java.util.List;
import java.util.Optional;

public interface ListCertificatesUseCase {

    List<Certificate> execute(Optional<AssetCode> asset);
}
