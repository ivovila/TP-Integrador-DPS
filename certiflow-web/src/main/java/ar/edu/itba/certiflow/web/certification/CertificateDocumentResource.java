package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.usecase.CertificateDocument;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.BasisPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.DecisionPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ScopePayload;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class CertificateDocumentResource extends RepresentationModel<CertificateDocumentResource> {

    public final String number;
    public final String assetCode;
    public final String assetName;
    public final LocalDate validFrom;
    public final LocalDate validUntil;
    public final LocalDate statusDate;
    public final String status;
    public final DecisionPayload decision;
    public final ScopePayload scope;
    public final BasisPayload basis;
    public final PersonPayload issuedBy;
    public final Instant issuedAt;
    public final List<CertificateResource.SuspensionView> suspensions;

    CertificateDocumentResource(CertificateDocument document) {
        this.number = document.number().value();
        this.assetCode = document.assetCode().value();
        this.assetName = document.assetName();
        this.validFrom = document.validity().validFrom();
        this.validUntil = document.validity().validUntil();
        this.statusDate = document.statusDate();
        this.status = document.status().name();
        this.decision = DecisionPayload.of(document.decision());
        this.scope = ScopePayload.of(document.scope());
        this.basis = BasisPayload.of(document.basis());
        this.issuedBy = PersonPayload.of(document.issuedBy());
        this.issuedAt = document.issuedAt();
        this.suspensions = document.suspensions().stream()
                .map(suspension -> new CertificateResource.SuspensionView(suspension.reason(), PersonPayload.of(suspension.suspendedBy()),
                        suspension.suspendedAt()))
                .toList();
    }
}
