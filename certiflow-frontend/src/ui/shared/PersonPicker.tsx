import { useState } from 'react';
import type { Person } from '../../domain/shared';
import { roster } from '../app/roster';

const someoneElse = '__someone-else__';

export const nobody: Person = { id: '', name: '' };

export function PersonPicker({ label, person, onChange }: { label: string; person: Person; onChange(person: Person): void }) {
  const isOnRoster = roster.some((member) => member.person.id === person.id);
  const [typingSomeoneElse, setTypingSomeoneElse] = useState(person.id !== '' && !isOnRoster);

  function choose(choice: string): void {
    setTypingSomeoneElse(choice === someoneElse);
    onChange(roster.find((member) => member.person.id === choice)?.person ?? nobody);
  }

  return (
    <div className="field">
      <label>
        {label}
        <select value={typingSomeoneElse ? someoneElse : person.id} onChange={(event) => choose(event.target.value)}>
          <option value="">Choose a person…</option>
          {roster.map((member) => (
            <option key={member.person.id} value={member.person.id}>
              {member.person.name} · {member.role}
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
