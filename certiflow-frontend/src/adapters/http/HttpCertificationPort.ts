import type { Certificate } from '../../domain/certification';
import type { CertificationPort } from '../../ports/CertificationPort';
import { certificateDocumentOf, certificateOf, eligibilityDecisionOf, scopeOf } from './certificationMapping';
import { type HalClient, query, segment } from './HalClient';
import { type SchemaReader, schemaVersionPath } from './HttpSchemaPort';
import type { WireCertificate, WireCertificateDocument, WireEligibility } from './wire';

function certificatePath(number: string): string {
  return `/api/v1/certificates/${segment(number)}`;
}

export function createHttpCertificationPort(client: HalClient, schemas: SchemaReader): CertificationPort {
  async function withRequiredSubsystems(certificate: WireCertificate): Promise<Certificate> {
    const reference = certificate.schemaReference;
    const version = await schemas.versionAt(schemaVersionPath(reference.assetType.name, reference.number));
    return certificateOf(certificate, version.requiredSubsystems);
  }

  return {
    async evaluateCertification(inspectionId, subsystemCode) {
      const eligibility = await client.read<WireEligibility>(
        `/api/v1/inspections/${segment(inspectionId)}/certification-eligibility${query({ subsystem: subsystemCode })}`,
      );
      return eligibilityDecisionOf(eligibility, inspectionId);
    },

    async issueCertificate(command) {
      const issued = await client.write<WireCertificate>('POST', '/api/v1/certificates', command.issuedBy, {
        inspectionId: command.inspectionId,
        ...(command.subsystemCode === undefined ? {} : { subsystemCode: command.subsystemCode }),
      });
      return issued.number;
    },

    async issueGlobalCertificate(command) {
      const issued = await client.write<WireCertificate>(
        'POST',
        `/api/v1/assets/${segment(command.assetCode)}/global-certificates`,
        command.issuedBy,
        { schemaVersion: command.schemaVersion, partialCertificateNumbers: command.partialCertificateNumbers },
      );
      return issued.number;
    },

    async listCertificates(filter) {
      const certificates = await client.readAll<WireCertificate>(
        `/api/v1/certificates${query({ asset: filter.assetCode, subsystem: filter.subsystemCode })}`,
        'certificates',
      );
      return Promise.all(certificates.map(withRequiredSubsystems));
    },

    async findCertificate(number) {
      return withRequiredSubsystems(await client.read<WireCertificate>(certificatePath(number)));
    },

    async suspendCertificate(command) {
      await client.write('POST', `${certificatePath(command.number)}/suspensions`, command.suspendedBy, { reason: command.reason });
    },

    async renewCertificate(command) {
      const currentScope = scopeOf((await client.read<WireCertificate>(certificatePath(command.currentNumber))).scope);
      const renewed = await client.write<WireCertificate>('POST', `${certificatePath(command.currentNumber)}/renewals`, command.renewedBy, {
        inspectionId: command.inspectionId,
        ...(currentScope.kind === 'subsystem' ? { subsystemCode: currentScope.subsystemCode } : {}),
      });
      return renewed.number;
    },

    async renewGlobalCertificate(command) {
      const renewed = await client.write<WireCertificate>(
        'POST',
        `${certificatePath(command.currentNumber)}/global-renewals`,
        command.renewedBy,
        { schemaVersion: command.schemaVersion, partialCertificateNumbers: command.partialCertificateNumbers },
      );
      return renewed.number;
    },

    async generateCertificateDocument(number) {
      return certificateDocumentOf(await client.read<WireCertificateDocument>(`${certificatePath(number)}/document`));
    },
  };
}
