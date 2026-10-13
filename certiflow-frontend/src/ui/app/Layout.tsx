import { useState } from 'react';
import { NavLink, Outlet } from 'react-router';
import { PersonPicker } from '../shared/PersonPicker';
import { useDemoControls } from './CertiflowContext';
import { useSession } from './SessionContext';

const navigation = [
  { to: '/assets', label: 'Assets' },
  { to: '/schemas', label: 'Schemas' },
  { to: '/inspections', label: 'Inspections' },
  { to: '/findings', label: 'Findings' },
  { to: '/certificates', label: 'Certificates' },
];

export function Layout() {
  const { actingPerson, actAs } = useSession();
  const demoControls = useDemoControls();
  const [today, setToday] = useState(demoControls?.today() ?? '');

  function changeToday(date: string): void {
    if (date === '') {
      return;
    }
    demoControls?.setToday(date);
    setToday(date);
  }

  return (
    <>
      <header className="app-header">
        <span className="brand">CertiFlow</span>
        <nav>
          {navigation.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="session">
          {demoControls !== undefined && (
            <div className="field">
              <label>
                Today (demo)
                <input type="date" value={today} onChange={(event) => changeToday(event.target.value)} />
              </label>
            </div>
          )}
          <PersonPicker label="Acting as" person={actingPerson} onChange={actAs} />
        </div>
      </header>
      <main className="page">
        <Outlet key={today} />
      </main>
    </>
  );
}
