package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.BasisPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.DecisionPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.SchemaReferencePayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ScopePayload;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "certificates", itemRelation = "certificate")
public class CertificateResource extends RepresentationModel<CertificateResource> {

    public record SuspensionView(String reason, PersonPayload suspendedBy, Instant suspendedAt) {
    }

    public final String number;
    public final String assetCode;
    public final String inspectionId;
    public final ScopePayload scope;
    public final BasisPayload basis;
    public final SchemaReferencePayload schemaReference;
    public final LocalDate validFrom;
    public final LocalDate validUntil;
    public final PersonPayload issuedBy;
    public final Instant issuedAt;
    public final String status;
    public final DecisionPayload decision;
    public final List<SuspensionView> suspensions;

    CertificateResource(Certificate certificate, CertificateStatus status) {
        this.number = certificate.number().value();
        this.assetCode = certificate.assetCode().value();
        this.inspectionId = certificate.basis() instanceof CertificateBasis.InspectionBasis inspection ? inspection.inspection().id().value() : null;
        this.scope = ScopePayload.of(certificate.scope());
        this.basis = BasisPayload.of(certificate.basis());
        this.schemaReference = SchemaReferencePayload.of(certificate.schemaReference());
        this.validFrom = certificate.validity().validFrom();
        this.validUntil = certificate.validity().validUntil();
        this.issuedBy = PersonPayload.of(certificate.issuedBy());
        this.issuedAt = certificate.issuedAt();
        this.status = status.name();
        this.decision = DecisionPayload.of(certificate.decision());
        this.suspensions = certificate.suspensions().stream()
                .map(suspension -> new SuspensionView(suspension.reason(), PersonPayload.of(suspension.suspendedBy()),
                        suspension.suspendedAt()))
                .toList();
    }
}
