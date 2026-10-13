import { Navigate, Route, Routes } from 'react-router';
import { AssetDetailPage } from '../catalog/AssetDetailPage';
import { AssetListPage } from '../catalog/AssetListPage';
import { FindingsSummaryPage } from '../catalog/FindingsSummaryPage';
import { RegisterAssetPage } from '../catalog/RegisterAssetPage';
import { CreateSchemaPage } from '../schema/CreateSchemaPage';
import { PublishVersionPage } from '../schema/PublishVersionPage';
import { SchemaDetailPage } from '../schema/SchemaDetailPage';
import { SchemaListPage } from '../schema/SchemaListPage';
import { SchemaVersionPage } from '../schema/SchemaVersionPage';
import { Layout } from './Layout';

export function ApplicationRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/assets" replace />} />
        <Route path="assets" element={<AssetListPage />} />
        <Route path="assets/new" element={<RegisterAssetPage />} />
        <Route path="assets/:code" element={<AssetDetailPage />} />
        <Route path="assets/:code/findings-summary" element={<FindingsSummaryPage />} />
        <Route path="schemas" element={<SchemaListPage />} />
        <Route path="schemas/new" element={<CreateSchemaPage />} />
        <Route path="schemas/:assetType" element={<SchemaDetailPage />} />
        <Route path="schemas/:assetType/versions/new" element={<PublishVersionPage />} />
        <Route path="schemas/:assetType/versions/:number" element={<SchemaVersionPage />} />
      </Route>
    </Routes>
  );
}
