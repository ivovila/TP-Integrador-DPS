package ar.edu.itba.certiflow.support;

import ar.edu.itba.certiflow.domain.model.audit.AuditLog;
import ar.edu.itba.certiflow.domain.model.certificate.CertificationPolicy;
import ar.edu.itba.certiflow.domain.model.certificate.SeverityCertificationPolicy;
import ar.edu.itba.certiflow.domain.model.finding.Severity;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.usecase.AssignInspection;
import ar.edu.itba.certiflow.domain.usecase.CloseFinding;
import ar.edu.itba.certiflow.domain.usecase.CloseInspection;
import ar.edu.itba.certiflow.domain.usecase.ExpireCertificate;
import ar.edu.itba.certiflow.domain.usecase.IssueCertificate;
import ar.edu.itba.certiflow.domain.usecase.PlanCorrectiveAction;
import ar.edu.itba.certiflow.domain.usecase.PublishSchemaVersion;
import ar.edu.itba.certiflow.domain.usecase.RaiseFinding;
import ar.edu.itba.certiflow.domain.usecase.RectifyInspection;
import ar.edu.itba.certiflow.domain.usecase.RegisterAsset;
import ar.edu.itba.certiflow.domain.usecase.RegisterResponse;
import ar.edu.itba.certiflow.domain.usecase.RenewCertificate;
import ar.edu.itba.certiflow.domain.usecase.StartInspection;
import ar.edu.itba.certiflow.domain.usecase.SuspendForOverdueActions;
import ar.edu.itba.certiflow.domain.usecase.VerifyCorrectiveAction;

public class TestContext {

    public final FixedClock clock = new FixedClock(Fixtures.NOW);
    public final AuditLog audit = new AuditLog();
    public final EventPublisher events = audit::record;
    public final CertificationPolicy policy = new SeverityCertificationPolicy(Severity.MAJOR);

    public final InMemoryAssetRepository assets = new InMemoryAssetRepository();
    public final InMemorySchemaRepository schemas = new InMemorySchemaRepository();
    public final InMemoryInspectionRepository inspections = new InMemoryInspectionRepository();
    public final InMemoryFindingRepository findings = new InMemoryFindingRepository();
    public final InMemoryCertificateRepository certificates = new InMemoryCertificateRepository();

    public final RegisterAsset registerAsset = new RegisterAsset(assets);
    public final PublishSchemaVersion publishSchema = new PublishSchemaVersion(schemas, clock, events);
    public final AssignInspection assignInspection = new AssignInspection(assets, schemas, inspections);
    public final StartInspection startInspection = new StartInspection(inspections, schemas, clock, events);
    public final RegisterResponse registerResponse = new RegisterResponse(inspections);
    public final CloseInspection closeInspection = new CloseInspection(inspections, clock, events);
    public final RectifyInspection rectifyInspection = new RectifyInspection(inspections, clock, events);
    public final RaiseFinding raiseFinding = new RaiseFinding(inspections, findings, clock, events);
    public final PlanCorrectiveAction planAction = new PlanCorrectiveAction(findings, clock, events);
    public final VerifyCorrectiveAction verifyAction = new VerifyCorrectiveAction(findings, clock, events);
    public final CloseFinding closeFinding = new CloseFinding(findings, clock, events);
    public final IssueCertificate issueCertificate =
            new IssueCertificate(inspections, findings, certificates, policy, clock, events);
    public final RenewCertificate renewCertificate =
            new RenewCertificate(certificates, inspections, findings, policy, clock, events);
    public final ExpireCertificate expireCertificate = new ExpireCertificate(certificates, clock, events);
    public final SuspendForOverdueActions suspendForOverdueActions =
            new SuspendForOverdueActions(certificates, findings, clock, events);
}
