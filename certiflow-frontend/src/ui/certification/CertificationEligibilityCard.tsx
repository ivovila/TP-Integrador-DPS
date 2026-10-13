import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { Badge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { Card, TextField } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';

function IssueCertificateForm({ inspectionId }: { inspectionId: string }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const navigate = useNavigate();
  const issuance = useCommand();
  const [validUntil, setValidUntil] = useState('');

  function issue(event: FormEvent): void {
    event.preventDefault();
    let certificateNumber = '';
    issuance.run(
      async () => {
        certificateNumber = await certiflow.certification.issueCertificate({ inspectionId, validUntil, issuedBy: actingPerson });
      },
      () => navigate(`/certificates/${certificateNumber}`),
    );
  }

  return (
    <form onSubmit={issue}>
      <ErrorBanner error={issuance.error} />
      <div className="row">
        <TextField label="Valid until" type="date" value={validUntil} onChange={setValidUntil} />
        <button className="primary" disabled={issuance.running}>
          Issue certificate
        </button>
      </div>
    </form>
  );
}

export function CertificationEligibilityCard({ inspectionId, refreshKey }: { inspectionId: string; refreshKey: number }) {
  const certiflow = useCertiflow();
  const decision = useQuery(() => certiflow.certification.evaluateCertification(inspectionId), [inspectionId, refreshKey]);

  return (
    <Card title="Certification eligibility">
      <QueryView query={decision}>
        {(certificationDecision) => (
          <div className="stacked">
            <div>
              {certificationDecision.eligible ? (
                <Badge tone="positive">eligible</Badge>
              ) : (
                <Badge tone="negative">not eligible</Badge>
              )}
            </div>
            {certificationDecision.ineligibilities.length > 0 && (
              <ul className="plain-list">
                {certificationDecision.ineligibilities.map((ineligibility) => (
                  <li key={ineligibility.reason}>{ineligibility.detail}</li>
                ))}
              </ul>
            )}
            {certificationDecision.actions.includes('issueCertificate') && <IssueCertificateForm inspectionId={inspectionId} />}
          </div>
        )}
      </QueryView>
    </Card>
  );
}
