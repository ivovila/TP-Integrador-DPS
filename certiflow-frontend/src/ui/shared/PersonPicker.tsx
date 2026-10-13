import { useState } from 'react';
import type { Person } from '../../domain/shared';
import { roles, type Role } from '../app/roles';
import { roster } from '../app/roster';

export const someoneElse = '__someone-else__';

export const nobody: Person = { id: '', name: '' };

export function PersonPicker({
  label,
  person,
  onChange,
  role,
}: {
  label: string;
  person: Person;
  onChange(person: Person): void;
  role?: Role;
}) {
  const offeredMembers = roster.filter((member) => role === undefined || member.role === role);
  const isOffered = offeredMembers.some((member) => member.person.id === person.id);
  const [typingSomeoneElse, setTypingSomeoneElse] = useState(person.id !== '' && !isOffered);

  function choose(choice: string): void {
    setTypingSomeoneElse(choice === someoneElse);
    onChange(offeredMembers.find((member) => member.person.id === choice)?.person ?? nobody);
  }

  return (
    <div className="field">
      <label>
        {label}
        <select value={typingSomeoneElse ? someoneElse : person.id} onChange={(event) => choose(event.target.value)}>
          <option value="">Choose a person…</option>
          {offeredMembers.map((member) => (
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
            aria-label={`${label} id`}
            placeholder="Id (e.g. EMP-010)"
            value={person.id}
            onChange={(event) => onChange({ ...person, id: event.target.value })}
          />
          <input
            aria-label={`${label} name`}
            placeholder="Full name"
            value={person.name}
            onChange={(event) => onChange({ ...person, name: event.target.value })}
          />
        </div>
      )}
    </div>
  );
}
