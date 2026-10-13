import type { Asset, AssetAction } from '../../domain/catalog';
import { hasLink } from './HalClient';
import type { WireAsset } from './wire';

export function assetOf(wire: WireAsset, assetTypesWithSchema: string[]): Asset {
  const actions: AssetAction[] = [
    ...(assetTypesWithSchema.includes(wire.type) ? ['assignInspection' as const] : []),
    ...(hasLink(wire, 'global-certificates') ? ['issueGlobalCertificate' as const] : []),
  ];
  return {
    code: wire.code,
    name: wire.name,
    type: wire.type,
    location: wire.location,
    responsible: wire.responsible,
    characteristics: wire.characteristics,
    jurisdictionCode: wire.jurisdictionCode,
    subsystems: wire.subsystems,
    actions,
  };
}
