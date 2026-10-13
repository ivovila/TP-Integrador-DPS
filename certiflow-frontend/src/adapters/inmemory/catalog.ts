import type { Asset, AssetAction } from '../../domain/catalog';
import type { CatalogPort } from '../../ports/CatalogPort';
import { isKnownJurisdiction } from './policies';
import type { InMemorySystem, StoredAsset } from './store';
import { reject, requireText } from './support';

export function storedAssetWithCode(system: InMemorySystem, code: string): StoredAsset {
  return system.assets.get(code) ?? reject('AssetNotFoundException', `Asset ${code} does not exist`);
}

function assetView(system: InMemorySystem, asset: StoredAsset): Asset {
  const inspectable = system.schemas.has(asset.type);
  const actions: AssetAction[] = [
    ...(inspectable ? ['assignInspection' as const] : []),
    ...(inspectable && asset.subsystems.length > 0 ? ['issueGlobalCertificate' as const] : []),
  ];
  return structuredClone({ ...asset, actions });
}

export function createCatalogPort(system: InMemorySystem): CatalogPort {
  return {
    async registerAsset(command) {
      requireText(command.code, 'An asset needs a code');
      requireText(command.name, 'An asset needs a name');
      requireText(command.type, 'An asset needs a type');
      requireText(command.location, 'An asset needs a location');
      requireText(command.responsible.id, 'An asset needs a responsible person');
      command.characteristics.forEach((characteristic) => {
        requireText(characteristic.name, 'A characteristic needs a name');
        requireText(characteristic.value, `Characteristic ${characteristic.name} needs a value`);
      });
      const characteristicNames = command.characteristics.map((characteristic) => characteristic.name);
      if (new Set(characteristicNames).size !== characteristicNames.length) {
        reject('DuplicateCharacteristicException', 'An asset cannot have two characteristics with the same name');
      }
      requireText(command.jurisdictionCode, 'A jurisdiction code is required');
      if (!isKnownJurisdiction(command.jurisdictionCode)) {
        reject('RequiredValueMissingException', `No certification policy configured for ${command.jurisdictionCode}`);
      }
      command.subsystems.forEach((subsystem) => {
        requireText(subsystem.code, 'Subsystem code is required');
        requireText(subsystem.name, 'Subsystem name is required');
      });
      const subsystemCodes = command.subsystems.map((subsystem) => subsystem.code);
      if (new Set(subsystemCodes).size !== subsystemCodes.length) {
        reject('RequiredValueMissingException', 'Subsystem codes must be unique within an asset');
      }
      if (system.assets.has(command.code)) {
        reject('AssetAlreadyRegisteredException', `Asset ${command.code} is already registered`);
      }
      const { registeredBy, ...assetData } = command;
      const asset: StoredAsset = structuredClone(assetData);
      system.assets.set(asset.code, asset);
      system.auditLog.record({ kind: 'asset', code: asset.code }, 'REGISTERED', registeredBy, system.clock.now(), asset.name);
      return assetView(system, asset);
    },

    async findAsset(code) {
      return assetView(system, storedAssetWithCode(system, code));
    },

    async listAssets(filter) {
      return [...system.assets.values()]
        .filter((asset) => filter.type === undefined || asset.type === filter.type)
        .map((asset) => assetView(system, asset));
    },
  };
}
