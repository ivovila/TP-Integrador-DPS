import type { ReactNode } from 'react';
import { actionsReservedToARole, roles, roleThatMayDo, type RoleAction, type SystemAction } from '../app/roles';
import { useSession } from '../app/SessionContext';

export function ReadOnlyNotice({ offeredActions, record }: { offeredActions: SystemAction[]; record: string }) {
  const { role, permitted } = useSession();
  const reservedActions = actionsReservedToARole(offeredActions);
  if (reservedActions.length === 0 || permitted(reservedActions).length > 0) {
    return null;
  }
  const rolesThatAct = [...new Set(reservedActions.map((action) => roles[roleThatMayDo[action]].label))];
  return (
    <p className="role-notice">
      Acting as {roles[role].label} you can read this {record} but not change it. Its actions belong to the{' '}
      {rolesThatAct.join(' and the ')}.
    </p>
  );
}

export function RequiresRoleFor({ action, children }: { action: RoleAction; children: ReactNode }) {
  const { actingPerson, role, mayDo } = useSession();
  if (mayDo(action)) {
    return <>{children}</>;
  }
  return (
    <section className="card">
      <h2>This needs another role</h2>
      <p className="role-notice">
        Only the {roles[roleThatMayDo[action]].label} can do this. You are acting as {actingPerson.name}, {roles[role].label}.
        Log out and log in as the {roles[roleThatMayDo[action]].label} to continue.
      </p>
    </section>
  );
}
