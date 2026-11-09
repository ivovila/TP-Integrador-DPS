/** Las anclas del detalle de la inspección: el panel de IA y cada criterio se enlazan entre sí. */
export const assistedReviewAnchor = 'assisted-review';

export function criterionAnchor(criterionCode: string): string {
  return `criterion-${criterionCode}`;
}
