import { NavLink, Outlet } from 'react-router';
import { ActingPersonPicker } from './ActingPersonPicker';
import { roles, sections } from './roles';
import { useSession } from './SessionContext';

export function Layout() {
  const { role } = useSession();

  return (
    <>
      <header className="app-header">
        <span className="brand">CertiFlow</span>
        <nav>
          {roles[role].sections.map((section) => (
            <NavLink key={section} to={sections[section].path}>
              {sections[section].label}
            </NavLink>
          ))}
        </nav>
        <div className="session">
          <ActingPersonPicker />
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}
