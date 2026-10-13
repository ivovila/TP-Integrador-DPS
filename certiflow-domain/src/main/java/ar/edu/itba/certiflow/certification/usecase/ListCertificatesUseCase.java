package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import java.util.List;

public interface ListCertificatesUseCase {

    List<CertificateView> execute();

    List<CertificateView> execute(AssetCode asset);
}
