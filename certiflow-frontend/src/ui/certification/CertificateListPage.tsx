import { useState } from 'react';
import { useCertiflow } from '../app/CertiflowContext';
import { StatusBadge } from '../shared/Badge';
import { formatDate, formatScope } from '../shared/format';
import { Card, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';
import { BasisDescription } from './CertificateDetailPage';
import { SectionLink } from '../shared/SectionLink';

export function CertificateListPage() {
  const certiflow = useCertiflow();
  const [assetCode, setAssetCode] = useState('');
  const [subsystemCode, setSubsystemCode] = useState('');
  const assets = useQuery(() => certiflow.catalog.listAssets({}), []);
  const certificates = useQuery(
    () =>
      certiflow.certification.listCertificates({
        ...(assetCode === '' ? {} : { assetCode }),
        ...(subsystemCode === '' ? {} : { subsystemCode }),
      }),
    [assetCode, subsystemCode],
  );
  const subsystemCodes = [...new Set((assets.data ?? []).flatMap((asset) => asset.subsystems.map((subsystem) => subsystem.code)))];

  return (
    <>
      <PageHeader
        title="Certificates"
        subtitle="Issued from the eligibility decision of a closed inspection, or derived from partial certificates"
      >
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
        <div className="field">
          <label>
            Subsystem
            <select value={subsystemCode} onChange={(event) => setSubsystemCode(event.target.value)}>
              <option value="">Any scope</option>
              {subsystemCodes.map((code) => (
                <option key={code} value={code}>
                  {code}
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
                    <th>Scope</th>
                    <th>Basis</th>
                    <th>Valid from</th>
                    <th>Valid until</th>
                    <th>Modality</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {listedCertificates.map((certificate) => (
                    <tr key={certificate.number}>
                      <td>
                        <SectionLink to={`/certificates/${certificate.number}`}>{certificate.number}</SectionLink>
                      </td>
                      <td>
                        <SectionLink to={`/assets/${encodeURIComponent(certificate.assetCode)}`}>{certificate.assetCode}</SectionLink>
                      </td>
                      <td>{formatScope(certificate.scope)}</td>
                      <td>
                        <BasisDescription basis={certificate.basis} />
                      </td>
                      <td>{formatDate(certificate.validFrom)}</td>
                      <td>{formatDate(certificate.validUntil)}</td>
                      <td>
                        <StatusBadge status={certificate.modality} />
                      </td>
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
