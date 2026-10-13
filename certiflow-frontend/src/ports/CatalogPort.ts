import type { Asset, Characteristic, Subsystem } from '../domain/catalog';
import type { Person } from '../domain/shared';

export interface RegisterAssetCommand {
  code: string;
  name: string;
  type: string;
  location: string;
  responsible: Person;
  characteristics: Characteristic[];
  jurisdictionCode: string;
  subsystems: Subsystem[];
  registeredBy: Person;
}

export interface AssetFilter {
  type?: string;
}

export interface CatalogPort {
  registerAsset(command: RegisterAssetCommand): Promise<Asset>;
  findAsset(code: string): Promise<Asset>;
  listAssets(filter: AssetFilter): Promise<Asset[]>;
}
