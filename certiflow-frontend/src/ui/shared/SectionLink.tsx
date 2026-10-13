import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { sectionOfPath } from '../app/roles';
import { useSession } from '../app/SessionContext';

export function SectionLink({ to, className, children }: { to: string; className?: string; children: ReactNode }) {
  const { mayOpen } = useSession();
  const section = sectionOfPath(to);
  if (section === undefined || mayOpen(section)) {
    return (
      <Link to={to} className={className}>
        {children}
      </Link>
    );
  }
  return className === undefined ? <span>{children}</span> : null;
}
