import { Link, useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { StatusBadge } from '../shared/Badge';
import { formatDate, formatInstant, formatPerson, formatScope } from '../shared/format';
import { Card, Details, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';
import { BasisDescription, SuspensionList } from './CertificateDetailPage';
import { DecisionDetails } from './DecisionDetails';

export function CertificateDocumentPage() {
  const certiflow = useCertiflow();
  const number = useParams().number!;
  const certificateDocument = useQuery(() => certiflow.certification.generateCertificateDocument(number), [number]);

  return (
    <>
      <PageHeader title="Certificate" subtitle={<Link to={`/certificates/${number}`}>Certificate {number}</Link>}>
        <button onClick={() => window.print()}>Print</button>
      </PageHeader>
      <QueryView query={certificateDocument}>
        {(document) => (
          <>
            <Card>
              <Details
                entries={[
                  ['Number', document.number],
                  ['Asset', `${document.assetCode} · ${document.assetName}`],
                  ['Scope', formatScope(document.scope)],
                  ['Basis', <BasisDescription basis={document.basis} />],
                  ['Valid from', formatDate(document.validFrom)],
                  ['Valid until', formatDate(document.validUntil)],
                  ['Modality', <StatusBadge status={document.modality} />],
                  ['Issued by', formatPerson(document.issuedBy)],
                  ['Issued at', formatInstant(document.issuedAt)],
                  [`Status on ${formatDate(document.statusDate)}`, <StatusBadge status={document.status} />],
                ]}
              />
            </Card>
            <Card title="Decision">
              <DecisionDetails decision={document.decision} />
            </Card>
            {document.suspensions.length > 0 && (
              <Card title="Suspensions">
                <SuspensionList suspensions={document.suspensions} />
              </Card>
            )}
          </>
        )}
      </QueryView>
    </>
  );
}
