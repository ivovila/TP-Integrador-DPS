import { type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { Card } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { ReadOnlyNotice } from '../shared/RoleNotices';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';
import { DecisionDetails } from './DecisionDetails';

function IssueCertificateForm({ inspectionId, subsystemCode }: { inspectionId: string; subsystemCode?: string }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const issuance = useCommand();

  function issue(event: FormEvent): void {
    event.preventDefault();
    let certificateNumber = '';
    issuance.run(
      async () => {
        certificateNumber = await certiflow.certification.issueCertificate({ inspectionId, subsystemCode, issuedBy: actingPerson });
      },
      () => navigate(`/certificates/${certificateNumber}`),
    );
  }

  return (
    <form onSubmit={issue}>
      <ErrorBanner error={issuance.error} />
      <button className="primary" disabled={issuance.running}>
        {subsystemCode === undefined ? 'Issue certificate' : `Issue ${subsystemCode} certificate`}
      </button>
    </form>
  );
}

function ScopeDecision({ inspectionId, subsystemCode, refreshKey }: { inspectionId: string; subsystemCode?: string; refreshKey: number }) {
  const certiflow = useCertiflow();
  const { permitted } = useSession();
  const decision = useQuery(
    () => certiflow.certification.evaluateCertification(inspectionId, subsystemCode),
    [inspectionId, subsystemCode, refreshKey],
  );

  return (
    <div className="nested">
      {subsystemCode !== undefined && <h3>Subsystem {subsystemCode}</h3>}
      <QueryView query={decision}>
        {(certificationDecision) => (
          <div className="stacked">
            <DecisionDetails decision={certificationDecision} />
            <ReadOnlyNotice offeredActions={certificationDecision.actions} record="decision" />
            {permitted(certificationDecision.actions).includes('issueCertificate') && (
              <IssueCertificateForm inspectionId={inspectionId} subsystemCode={subsystemCode} />
            )}
          </div>
        )}
      </QueryView>
    </div>
  );
}

/** Una decisión por subsistema si la versión de la inspección los tiene; si no, una sola por el activo entero. */
export function CertificationEligibilityCard({ inspectionId, refreshKey }: { inspectionId: string; refreshKey: number }) {
  const certiflow = useCertiflow();
  const requiredSubsystems = useQuery(async () => {
    const inspection = await certiflow.inspections.findInspection(inspectionId);
    const asset = await certiflow.catalog.findAsset(inspection.assetCode);
    const schema = await certiflow.schemas.findSchema(asset.type);
    return schema.versions.find((version) => version.number === inspection.schemaVersion)!.requiredSubsystems;
  }, [inspectionId]);

  return (
    <Card title="Certification eligibility">
      <p className="muted">The asset's jurisdiction policy decides the outcome and the validity; the certifier cannot choose them.</p>
      <QueryView query={requiredSubsystems}>
        {(subsystemCodes) =>
          subsystemCodes.length === 0 ? (
            <ScopeDecision inspectionId={inspectionId} refreshKey={refreshKey} />
          ) : (
            subsystemCodes.map((subsystemCode) => (
              <ScopeDecision key={subsystemCode} inspectionId={inspectionId} subsystemCode={subsystemCode} refreshKey={refreshKey} />
            ))
          )
        }
      </QueryView>
    </Card>
  );
}
