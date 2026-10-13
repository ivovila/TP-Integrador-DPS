import { Link, useParams } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { StatusBadge } from '../shared/Badge';
import { formatDate, formatInstant, formatPerson } from '../shared/format';
import { Card, Details, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';
import { SuspensionList } from './CertificateDetailPage';

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
                  ['Valid from', formatDate(document.validFrom)],
                  ['Valid until', formatDate(document.validUntil)],
                  ['Issued by', formatPerson(document.issuedBy)],
                  ['Issued at', formatInstant(document.issuedAt)],
                  [`Status on ${formatDate(document.statusDate)}`, <StatusBadge status={document.status} />],
                ]}
              />
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
