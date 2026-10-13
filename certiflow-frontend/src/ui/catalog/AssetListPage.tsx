import { useState } from 'react';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { Card, EmptyState, PageHeader } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useQuery } from '../shared/useQuery';
import { SectionLink } from '../shared/SectionLink';

export function AssetListPage() {
  const certiflow = useCertiflow();
  const { mayDo } = useSession();
  const [typeFilter, setTypeFilter] = useState('');
  const knownTypes = useQuery(
    async () => [...new Set((await certiflow.catalog.listAssets({})).map((asset) => asset.type))].sort(),
    [],
  );
  const assets = useQuery(() => certiflow.catalog.listAssets(typeFilter === '' ? {} : { type: typeFilter }), [typeFilter]);

  return (
    <>
      <PageHeader title="Assets" subtitle="Installations, laboratories and equipment under inspection">
        <div className="field">
          <label>
            Type
            <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
              <option value="">All types</option>
              {(knownTypes.data ?? []).map((type) => (
                <option key={type}>{type}</option>
              ))}
            </select>
          </label>
        </div>
        {mayDo('registerAsset') && (
          <SectionLink className="button primary" to="/assets/new">
            Register asset
          </SectionLink>
        )}
      </PageHeader>
      <Card>
        <QueryView query={assets}>
          {(listedAssets) =>
            listedAssets.length === 0 ? (
              <EmptyState>No assets registered.</EmptyState>
            ) : (
              <table className="data">
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Location</th>
                    <th>Responsible</th>
                  </tr>
                </thead>
                <tbody>
                  {listedAssets.map((asset) => (
                    <tr key={asset.code}>
                      <td>
                        <SectionLink to={`/assets/${encodeURIComponent(asset.code)}`}>{asset.code}</SectionLink>
                      </td>
                      <td>{asset.name}</td>
                      <td>{asset.type}</td>
                      <td>{asset.location}</td>
                      <td>{asset.responsible.name}</td>
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
