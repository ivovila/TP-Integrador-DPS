import type { ReactNode } from 'react';

export function PageHeader({ title, subtitle, children }: { title: ReactNode; subtitle?: ReactNode; children?: ReactNode }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle !== undefined && <p className="muted">{subtitle}</p>}
      </div>
      <div className="toolbar">{children}</div>
    </div>
  );
}

export function Card({ title, children }: { title?: ReactNode; children: ReactNode }) {
  return (
    <section className="card">
      {title !== undefined && <h2>{title}</h2>}
      {children}
    </section>
  );
}

export function Details({ entries }: { entries: [string, ReactNode][] }) {
  return (
    <dl className="details">
      {entries.map(([term, description]) => (
        <div key={term}>
          <dt>{term}</dt>
          <dd>{description}</dd>
        </div>
      ))}
    </dl>
  );
}

export function TextField({
  label,
  value,
  onChange,
  type = 'text',
  placeholder,
}: {
  label: string;
  value: string;
  onChange(value: string): void;
  type?: 'text' | 'date' | 'number';
  placeholder?: string;
}) {
  return (
    <div className="field">
      <label>
        {label}
        <input type={type} value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} />
      </label>
    </div>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <p className="muted empty">{children}</p>;
}
