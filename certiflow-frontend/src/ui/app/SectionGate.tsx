import { Outlet } from 'react-router';
import { Card, PageHeader } from '../shared/Page';
import { roles, sections, type Section } from './roles';
import { useSession } from './SessionContext';

export function SectionGate({ section }: { section: Section }) {
  const { mayOpen, role } = useSession();
  if (mayOpen(section)) {
    return <Outlet />;
  }
  return (
    <>
      <PageHeader title="Not available for your role" />
      <Card>
        <p className="role-notice">
          {sections[section].label} are not part of the work of the {roles[role].label}. Use the menu to reach your sections.
        </p>
      </Card>
    </>
  );
}
