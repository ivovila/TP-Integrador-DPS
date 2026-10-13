import { useState, type FormEvent } from 'react';
import type { CriterionResponse } from '../../domain/inspection';
import type { RawAnswer } from '../../domain/schema';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { ErrorBanner } from '../shared/ErrorBanner';
import { RuleAnswerInput } from '../shared/ruleKinds';
import { useCommand } from '../shared/useCommand';

export function RectificationForm({
  inspectionId,
  responses,
  onRectified,
}: {
  inspectionId: string;
  responses: CriterionResponse[];
  onRectified(raisedFindingIds: string[]): void;
}) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const rectification = useCommand();
  const [criterionCode, setCriterionCode] = useState(responses[0]!.criterion.code);
  const [answer, setAnswer] = useState<RawAnswer | undefined>(undefined);
  const [reason, setReason] = useState('');
  const chosenResponse = responses.find((response) => response.criterion.code === criterionCode)!;

  function rectify(event: FormEvent): void {
    event.preventDefault();
    if (answer === undefined) {
      return;
    }
    let raisedFindingIds: string[] = [];
    rectification.run(
      async () => {
        raisedFindingIds = await certiflow.inspections.rectifyInspection({
          inspectionId,
          criterionCode,
          answer,
          reason,
          rectifiedBy: actingPerson,
        });
      },
      () => {
        setAnswer(undefined);
        setReason('');
        onRectified(raisedFindingIds);
      },
    );
  }

  return (
    <form className="stacked" onSubmit={rectify}>
      <ErrorBanner error={rectification.error} />
      <div className="row">
        <div className="field">
          <label>
            Criterion
            <select
              value={criterionCode}
              onChange={(event) => {
                setCriterionCode(event.target.value);
                setAnswer(undefined);
              }}
            >
              {responses.map((response) => (
                <option key={response.criterion.code} value={response.criterion.code}>
                  {response.criterion.code} · {response.criterion.text}
                </option>
              ))}
            </select>
          </label>
        </div>
        <RuleAnswerInput key={criterionCode} rule={chosenResponse.criterion.rule} answer={answer} onChange={setAnswer} />
        <div className="field">
          <label>
            Reason
            <input value={reason} onChange={(event) => setReason(event.target.value)} />
          </label>
        </div>
        <button disabled={rectification.running || answer === undefined}>Rectify</button>
      </div>
    </form>
  );
}
