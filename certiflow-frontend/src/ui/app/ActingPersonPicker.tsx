import { useState } from 'react';
import { nobody, someoneElse } from '../shared/PersonPicker';
import { everyRole, roles, type Role } from './roles';
import { roster } from './roster';
import { useSession } from './SessionContext';

export function ActingPersonPicker() {
  const { actingPerson, role, actAs } = useSession();
  const isOnRoster = roster.some((member) => member.person.id === actingPerson.id && member.role === role);
  const [typingSomeoneElse, setTypingSomeoneElse] = useState(!isOnRoster);

  function choose(choice: string): void {
    const chosenMember = roster.find((member) => member.person.id === choice);
    setTypingSomeoneElse(chosenMember === undefined);
    actAs(chosenMember?.person ?? nobody, chosenMember?.role ?? role);
  }

  return (
    <div className="field">
      <label>
        Acting as
        <select value={typingSomeoneElse ? someoneElse : actingPerson.id} onChange={(event) => choose(event.target.value)}>
          {roster.map((member) => (
            <option key={member.person.id} value={member.person.id}>
              {member.person.name} · {roles[member.role].label}
            </option>
          ))}
          <option value={someoneElse}>Someone else…</option>
        </select>
      </label>
      {typingSomeoneElse && (
        <div className="inline-fields">
          <input
            aria-label="Acting as id"
            placeholder="Id (e.g. EMP-010)"
            value={actingPerson.id}
            onChange={(event) => actAs({ ...actingPerson, id: event.target.value }, role)}
          />
          <input
            aria-label="Acting as name"
            placeholder="Full name"
            value={actingPerson.name}
            onChange={(event) => actAs({ ...actingPerson, name: event.target.value }, role)}
          />
          <select aria-label="Acting as role" value={role} onChange={(event) => actAs(actingPerson, event.target.value as Role)}>
            {everyRole.map((candidate) => (
              <option key={candidate} value={candidate}>
                {roles[candidate].label}
              </option>
            ))}
          </select>
        </div>
      )}
    </div>
  );
}
