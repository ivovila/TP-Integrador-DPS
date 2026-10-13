import { Navigate, Route, Routes } from 'react-router';
import { AssetDetailPage } from '../catalog/AssetDetailPage';
import { AssetListPage } from '../catalog/AssetListPage';
import { FindingsSummaryPage } from '../catalog/FindingsSummaryPage';
import { RegisterAssetPage } from '../catalog/RegisterAssetPage';
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
      </Route>
    </Routes>
  );
}
