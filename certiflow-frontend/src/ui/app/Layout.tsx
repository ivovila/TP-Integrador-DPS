import { NavLink, Outlet } from 'react-router';
import { PersonPicker } from '../shared/PersonPicker';
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
          <PersonPicker label="Acting as" person={actingPerson} onChange={actAs} />
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}
