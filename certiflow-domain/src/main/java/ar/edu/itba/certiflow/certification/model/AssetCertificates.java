package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import java.time.LocalDate;
import java.util.List;

/**
 * Los certificados de un activo. Un alcance tiene a lo sumo un certificado vigente: uno suspendido dentro de
 * su vigencia también lo ocupa, así que no se evade una suspensión emitiendo otro.
 */
public record AssetCertificates(AssetCode assetCode, List<Certificate> certificates) {

    public AssetCertificates {
        certificates = List.copyOf(certificates);
    }

    public void ensureScopeAvailable(CertificationScope scope, LocalDate date) {
        ensureNoneOf(currentFor(scope, date));
    }

    /** Al renovar, el certificado que se reemplaza no cuenta como ocupante de su propio alcance. */
    public void ensureScopeAvailableToReplace(Certificate replacedCertificate, CertificationScope scope, LocalDate date) {
        ensureNoneOf(currentFor(scope, date).stream()
                .filter(occupant -> !occupant.hasNumber(replacedCertificate.number()))
                .toList());
    }

    /** El estado de un certificado depende de los demás del activo cuando es un global respaldado por parciales. */
    public CertificateStatus statusOf(Certificate certificate, LocalDate date) {
        return certificate.statusOn(date, certificates);
    }

    private List<Certificate> currentFor(CertificationScope scope, LocalDate date) {
        return certificates.stream().filter(certificate -> certificate.isCurrentFor(scope, date)).toList();
    }

    private void ensureNoneOf(List<Certificate> occupants) {
        if (!occupants.isEmpty()) {
            throw new AssetAlreadyCertifiedException(assetCode, occupants.getFirst().number());
        }
    }
}
