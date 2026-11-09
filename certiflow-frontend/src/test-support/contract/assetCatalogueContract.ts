import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { administrator, assetResponsible } from './fixture';




export function assetCatalogueContract(context: ContractContext): void {
    describe('asset catalogue', () => {
      test('a registered asset can be consulted with its type, location, responsible and characteristics', async () => {
        await context.registerExtinguisher();
        const asset = await context.certiflow.catalog.findAsset(context.extinguisherCode);
        expect(asset).toMatchObject({
          name: 'Lobby extinguisher',
          type: context.extinguisherType,
          location: 'Lobby',
          responsible: assetResponsible,
          characteristics: [{ name: 'Agent', value: 'ABC powder' }],
        });
      });

      test('registering an asset is recorded in its history with who did it', async () => {
        await context.registerExtinguisher();
        const history = await context.certiflow.audit.auditHistory({ kind: 'asset', code: context.extinguisherCode });
        expect(history).toMatchObject([{ action: 'REGISTERED', performedBy: administrator }]);
      });

      test('the same asset code cannot be registered twice', async () => {
        await context.registerExtinguisher();
        await context.expectRejection(context.registerExtinguisher(), 'AssetAlreadyRegisteredException');
      });

      test('an asset cannot have two characteristics with the same name', async () => {
        const registration = context.certiflow.catalog.registerAsset({
          code: context.unique('EXT-0001'),
          name: 'Hall extinguisher',
          type: context.extinguisherType,
          location: 'Hall',
          responsible: assetResponsible,
          characteristics: [
            { name: 'Agent', value: 'ABC powder' },
            { name: 'Agent', value: 'CO2' },
          ],
          jurisdictionCode: 'DEMO-STANDARD',
          subsystems: [],
          registeredBy: administrator,
        });
        await context.expectRejection(registration, 'DuplicateCharacteristicException');
      });

      test('assets can be listed by type', async () => {
        await context.registerExtinguisher();
        await context.certiflow.catalog.registerAsset({
          code: context.unique('BLR-0007'),
          name: 'Boiler',
          type: context.unique('Pressure vessel'),
          location: 'Basement',
          responsible: assetResponsible,
          characteristics: [],
          jurisdictionCode: 'DEMO-STANDARD',
          subsystems: [],
          registeredBy: administrator,
        });
        const extinguishers = await context.certiflow.catalog.listAssets({ type: context.extinguisherType });
        expect(extinguishers.map((asset) => asset.code)).toEqual([context.extinguisherCode]);
        expect((await context.certiflow.catalog.listAssets({})).map((asset) => asset.code)).toEqual(
          expect.arrayContaining([context.extinguisherCode, context.unique('BLR-0007')]),
        );
      });

      test('an unknown asset is reported as not found', async () => {
        await context.expectRejection(context.certiflow.catalog.findAsset('NOPE'), 'AssetNotFoundException');
      });
    });

}
