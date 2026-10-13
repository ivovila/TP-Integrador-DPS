import { expect, test } from 'vitest';
import { actionsOf, everyRole, homeOf, roleMayDo, roles, type Role, type RoleAction, type Section } from './roles';
import { roster } from './roster';

const whatEachRoleDoes: [Role, RoleAction[]][] = [
  ['administrator', ['registerAsset', 'createInspectionSchema', 'publishSchemaVersion']],
  ['inspectionCoordinator', ['assignInspection']],
  ['inspector', ['recordAnswer', 'attachEvidence', 'addObservation', 'closeInspection', 'rectifyInspection']],
  ['assetResponsible', ['planCorrectiveAction']],
  ['verifier', ['verifyCorrectiveAction']],
  [
    'certificationOfficer',
    ['issueCertificate', 'issueGlobalCertificate', 'suspendCertificate', 'renewCertificate', 'renewGlobalCertificate'],
  ],
];

test.each(whatEachRoleDoes)('the %s does exactly its own actions', (role, expectedActions) => {
  expect(actionsOf(role)).toEqual(expectedActions);
  expectedActions.forEach((action) => expect(roleMayDo(role, action)).toBe(true));
});

test.each(whatEachRoleDoes)('nobody but the %s does its actions', (role, itsActions) => {
  const otherRoles = everyRole.filter((candidate) => candidate !== role);
  itsActions.forEach((action) => otherRoles.forEach((otherRole) => expect(roleMayDo(otherRole, action)).toBe(false)));
});

test('every role can read the inspection act', () => {
  everyRole.forEach((role) => expect(roleMayDo(role, 'generateInspectionAct')).toBe(true));
});

const whatEachRoleSees: [Role, Section[], string][] = [
  ['administrator', ['assets', 'schemas'], '/assets'],
  ['inspectionCoordinator', ['inspections', 'assets'], '/inspections'],
  ['inspector', ['inspections'], '/inspections'],
  ['assetResponsible', ['findings', 'assets'], '/findings'],
  ['verifier', ['findings'], '/findings'],
  ['certificationOfficer', ['certificates', 'inspections', 'assets'], '/certificates'],
];

test.each(whatEachRoleSees)('the %s has its own sections and starts on the first', (role, expectedSections, expectedHome) => {
  expect(roles[role].sections).toEqual(expectedSections);
  expect(homeOf(role)).toBe(expectedHome);
});

test('the sample people cover every role, one each', () => {
  expect(roster.map((member) => member.role).sort()).toEqual([...everyRole].sort());
});
