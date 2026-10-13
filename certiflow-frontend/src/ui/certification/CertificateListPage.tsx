import { useState } from 'react';
import { Link } from 'react-router';
import { useCertiflow } from '../app/CertiflowContext';
import { StatusBadge } from '../shared/Badge';
import { formatDate } from '../shared/format';
import { Card, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';

export function CertificateListPage() {
  const certiflow = useCertiflow();
  const [assetCode, setAssetCode] = useState('');
  const assets = useQuery(() => certiflow.catalog.listAssets({}), []);
  const certificates = useQuery(
    () => certiflow.certification.listCertificates(assetCode === '' ? {} : { assetCode }),
    [assetCode],
  );

  return (
    <>
      <PageHeader title="Certificates" subtitle="A certificate is issued from the eligibility decision of a closed inspection">
        <div className="field">
          <label>
            Asset
            <select value={assetCode} onChange={(event) => setAssetCode(event.target.value)}>
              <option value="">All assets</option>
              {(assets.data ?? []).map((asset) => (
                <option key={asset.code} value={asset.code}>
                  {asset.code} · {asset.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      </PageHeader>
      <Card>
        <QueryView query={certificates}>
          {(listedCertificates) =>
            listedCertificates.length === 0 ? (
              <EmptyState>No certificates issued.</EmptyState>
            ) : (
              <table className="data">
                <thead>
                  <tr>
                    <th>Number</th>
                    <th>Asset</th>
                    <th>Based on</th>
                    <th>Valid from</th>
                    <th>Valid until</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {listedCertificates.map((certificate) => (
                    <tr key={certificate.number}>
                      <td>
                        <Link to={`/certificates/${certificate.number}`}>{certificate.number}</Link>
                      </td>
                      <td>
                        <Link to={`/assets/${encodeURIComponent(certificate.assetCode)}`}>{certificate.assetCode}</Link>
                      </td>
                      <td>
                        <Link to={`/inspections/${certificate.inspectionId}`}>{certificate.inspectionId}</Link>
                      </td>
                      <td>{formatDate(certificate.validFrom)}</td>
                      <td>{formatDate(certificate.validUntil)}</td>
                      <td>
                        <StatusBadge status={certificate.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
          }
        </QueryView>
      </Card>
    </>
  );
}
