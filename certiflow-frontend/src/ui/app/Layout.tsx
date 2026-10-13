import { NavLink, Outlet } from 'react-router';
import { roles, sections } from './roles';
import { useSession } from './SessionContext';

export function Layout() {
  const { actingPerson, role, logOut } = useSession();

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
          <span className="signed-in">
            {actingPerson.name} · {roles[role].label}
          </span>
          <button type="button" className="secondary" onClick={() => void logOut()}>
            Logout
          </button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}
