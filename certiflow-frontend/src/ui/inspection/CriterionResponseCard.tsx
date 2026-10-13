import { useState, type FormEvent } from 'react';
import type { CriterionResponse, InspectionAction } from '../../domain/inspection';
import type { EvidenceKind, RawAnswer } from '../../domain/schema';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { describeRequiredEvidence } from '../schema/SectionsView';
import { StatusBadge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatInstant } from '../shared/format';
import { describeRule, formatAnswer, RuleAnswerInput } from '../shared/ruleKinds';
import { useCommand } from '../shared/useCommand';

interface ResponseFormProps {
  inspectionId: string;
  response: CriterionResponse;
  onRecorded(): void;
}

function AnswerForm({ inspectionId, response, onRecorded }: ResponseFormProps) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const recording = useCommand();
  const [answer, setAnswer] = useState<RawAnswer | undefined>(response.given?.value);

  function record(event: FormEvent): void {
    event.preventDefault();
    if (answer === undefined) {
      return;
    }
    recording.run(
      () =>
        certiflow.inspections.recordAnswer({
          inspectionId,
          criterionCode: response.criterion.code,
          answer,
          answeredBy: actingPerson,
        }),
      onRecorded,
    );
  }

  return (
    <form onSubmit={record}>
      <ErrorBanner error={recording.error} />
      <div className="row">
        <RuleAnswerInput rule={response.criterion.rule} answer={answer} onChange={setAnswer} />
        <button disabled={recording.running || answer === undefined}>Record answer</button>
      </div>
    </form>
  );
}

function EvidenceForm({ inspectionId, response, onRecorded, evidenceKinds }: ResponseFormProps & { evidenceKinds: EvidenceKind[] }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const attaching = useCommand();
  const [kindName, setKindName] = useState(evidenceKinds[0]!.name);
  const [reference, setReference] = useState('');
  const [description, setDescription] = useState('');

  function attach(event: FormEvent): void {
    event.preventDefault();
    attaching.run(
      () =>
        certiflow.inspections.attachEvidence({
          inspectionId,
          criterionCode: response.criterion.code,
          evidence: { kind: evidenceKinds.find((kind) => kind.name === kindName)!, reference, description },
          attachedBy: actingPerson,
        }),
      () => {
        setReference('');
        setDescription('');
        onRecorded();
      },
    );
  }

  return (
    <form onSubmit={attach}>
      <ErrorBanner error={attaching.error} />
      <div className="row">
        <div className="field">
          <label>
            Evidence
            <select value={kindName} onChange={(event) => setKindName(event.target.value)}>
              {evidenceKinds.map((kind) => (
                <option key={kind.name} value={kind.name}>
                  {kind.label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="field">
          <label>
            File reference
            <input value={reference} placeholder="gauge.jpg" onChange={(event) => setReference(event.target.value)} />
          </label>
        </div>
        <div className="field">
          <label>
            Description
            <input value={description} onChange={(event) => setDescription(event.target.value)} />
          </label>
        </div>
        <button disabled={attaching.running}>Attach evidence</button>
      </div>
    </form>
  );
}

function ObservationForm({ inspectionId, response, onRecorded }: ResponseFormProps) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const observing = useCommand();
  const [text, setText] = useState('');

  function observe(event: FormEvent): void {
    event.preventDefault();
    observing.run(
      () =>
        certiflow.inspections.addObservation({
          inspectionId,
          criterionCode: response.criterion.code,
          text,
          observedBy: actingPerson,
        }),
      () => {
        setText('');
        onRecorded();
      },
    );
  }

  return (
    <form onSubmit={observe}>
      <ErrorBanner error={observing.error} />
      <div className="row">
        <div className="field">
          <label>
            Observation
            <input value={text} onChange={(event) => setText(event.target.value)} />
          </label>
        </div>
        <button disabled={observing.running}>Add observation</button>
      </div>
    </form>
  );
}

export function CriterionResponseCard({
  inspectionId,
  response,
  actions,
  evidenceKinds,
  onRecorded,
}: ResponseFormProps & { actions: InspectionAction[]; evidenceKinds: EvidenceKind[] }) {
  const criterion = response.criterion;
  return (
    <div className="criterion">
      <div className="criterion-heading">
        <div>
          <span className="criterion-code">{criterion.code}</span> <strong>{criterion.text}</strong>
          <p className="muted">
            {describeRule(criterion.rule)} · Severity: {criterion.severity.label} · Required evidence:{' '}
            {describeRequiredEvidence(criterion)}
          </p>
        </div>
        <StatusBadge status={response.outcome} />
      </div>
      <ul className="plain-list">
        {response.given !== undefined && (
          <li>
            Answer: <strong>{formatAnswer(criterion.rule, response.given.value)}</strong>{' '}
            <span className="muted">
              by {response.given.answeredBy.name}, {formatInstant(response.given.answeredAt)}
            </span>
          </li>
        )}
        {response.attachments.map((attached) => (
          <li key={attached.evidence.reference}>
            {attached.evidence.kind.label}: {attached.evidence.reference}
            {attached.evidence.description !== '' && ` (${attached.evidence.description})`}{' '}
            <span className="muted">
              by {attached.attachedBy.name}, {formatInstant(attached.attachedAt)}
            </span>
          </li>
        ))}
        {response.observations.map((observation) => (
          <li key={observation.observedAt}>
            Observation: {observation.text}{' '}
            <span className="muted">
              by {observation.observedBy.name}, {formatInstant(observation.observedAt)}
            </span>
          </li>
        ))}
      </ul>
      {actions.includes('recordAnswer') && <AnswerForm inspectionId={inspectionId} response={response} onRecorded={onRecorded} />}
      {(actions.includes('attachEvidence') || actions.includes('addObservation')) && (
        <details className="more">
          <summary>Attach evidence or add an observation</summary>
          {actions.includes('attachEvidence') && (
            <EvidenceForm inspectionId={inspectionId} response={response} onRecorded={onRecorded} evidenceKinds={evidenceKinds} />
          )}
          {actions.includes('addObservation') && (
            <ObservationForm inspectionId={inspectionId} response={response} onRecorded={onRecorded} />
          )}
        </details>
      )}
    </div>
  );
}
