package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.web.schema.CriterionPayload;
import ar.edu.itba.certiflow.web.schema.SchemaPayloads;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "findings", itemRelation = "finding")
public class FindingResource extends RepresentationModel<FindingResource> {

    public final String id;
    public final String inspectionId;
    public final String assetCode;
    public final CriterionPayload criterion;
    public final String severity;
    public final PersonPayload responsible;
    public final boolean open;
    public final boolean blocksCertification;
    public final List<CriterionResponseResource.EvidenceView> evidence;
    public final List<CorrectiveActionResource> actions;

    FindingResource(Finding finding, List<CorrectiveActionResource> actions) {
        this.id = finding.id().value();
        this.inspectionId = finding.inspectionId().value();
        this.assetCode = finding.assetCode().value();
        this.criterion = SchemaPayloads.payload(finding.criterion());
        this.severity = ((Enum<?>) finding.severity()).name();
        this.responsible = PersonPayload.of(finding.responsible());
        this.open = finding.isOpen();
        this.blocksCertification = finding.blocksCertification();
        this.evidence = finding.evidence().stream()
                .map(attached -> new CriterionResponseResource.EvidenceView(attached.evidence().kind().label(),
                        attached.evidence().reference(), attached.evidence().description(), PersonPayload.of(attached.attachedBy()),
                        attached.attachedAt()))
                .toList();
        this.actions = actions;
    }
}
