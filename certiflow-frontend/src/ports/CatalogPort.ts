import type { Asset, Characteristic } from '../domain/catalog';
import type { Person } from '../domain/shared';

export interface RegisterAssetCommand {
  code: string;
  name: string;
  type: string;
  location: string;
  responsible: Person;
  characteristics: Characteristic[];
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
