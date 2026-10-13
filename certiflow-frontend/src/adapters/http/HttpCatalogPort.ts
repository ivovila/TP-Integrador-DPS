import type { CatalogPort } from '../../ports/CatalogPort';
import { assetOf } from './catalogMapping';
import { type HalClient, query, segment } from './HalClient';
import type { SchemaReader } from './HttpSchemaPort';
import type { WireAsset } from './wire';

export function createHttpCatalogPort(client: HalClient, schemas: SchemaReader): CatalogPort {
  return {
    async registerAsset(command) {
      const registered = await client.write<WireAsset>('POST', '/api/v1/assets', command.registeredBy, {
        code: command.code,
        name: command.name,
        type: command.type,
        location: command.location,
        jurisdictionCode: command.jurisdictionCode,
        responsible: { id: command.responsible.id, name: command.responsible.name },
        characteristics: command.characteristics.map((characteristic) => ({ name: characteristic.name, value: characteristic.value })),
        subsystems: command.subsystems.map((subsystem) => ({ code: subsystem.code, name: subsystem.name })),
      });
      return assetOf(registered, await schemas.assetTypesWithSchema());
    },

    async findAsset(code) {
      const [asset, assetTypesWithSchema] = await Promise.all([
        client.read<WireAsset>(`/api/v1/assets/${segment(code)}`),
        schemas.assetTypesWithSchema(),
      ]);
      return assetOf(asset, assetTypesWithSchema);
    },

    async listAssets(filter) {
      const [assets, assetTypesWithSchema] = await Promise.all([
        client.readAll<WireAsset>(`/api/v1/assets${query({ type: filter.type })}`, 'assets'),
        schemas.assetTypesWithSchema(),
      ]);
      return assets.map((asset) => assetOf(asset, assetTypesWithSchema));
    },
  };
}
