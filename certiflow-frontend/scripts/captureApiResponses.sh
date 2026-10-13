#!/bin/bash
# Regenera src/adapters/http/__fixtures__ con respuestas reales del backend.
# Requiere el backend levantado sobre una base vacía. Uso: scripts/captureApiResponses.sh [URL base]
set -u
BASE=${1:-http://localhost:8080}
OUT=$(cd "$(dirname "$0")/.." && pwd)/src/adapters/http/__fixtures__
mkdir -p "$OUT"

call() {
  local name=$1 method=$2 path=$3 person_id=$4 person_name=$5 body=${6:-}
  local args=(-s -X "$method" -w '\n%{http_code}' -H 'Accept: application/hal+json, application/problem+json')
  [ -n "$person_id" ] && args+=(-H "X-Person-Id: $person_id" -H "X-Person-Name: $person_name")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  local response; response=$(curl "${args[@]}" "$BASE$path")
  local code=${response##*$'\n'}
  local payload=${response%$'\n'*}
  [ -n "$payload" ] && printf '%s' "$payload" | python3 -m json.tool > "$OUT/$name.json"
  printf '%-36s %-4s %s -> %s\n' "$name" "$method" "$path" "$code"
}
field() { python3 -c "import json; print(json.load(open('$OUT/$1.json'))$2)"; }

COORDINATOR=(EMP-001 "Paula Rios"); INSPECTOR=(EMP-002 "Ines Duarte"); VERIFIER=(EMP-004 "Vera Soto"); CERTIFIER=(EMP-005 "Carlos Vidal")
RESPONSIBLE='{"id":"EMP-003","name":"Raul Mendez"}'

call root GET /api/v1 "" ""
call vocabulary GET /api/v1/vocabulary "" ""
call schema-list-empty GET /api/v1/schemas "" ""

call asset-with-subsystems POST /api/v1/assets "${COORDINATOR[@]}" '{"code":"CAP-0001","name":"Capture plant","type":"Capture plant","location":"Plant 9","jurisdictionCode":"DEMO-CONDITIONAL","responsible":'"$RESPONSIBLE"',"characteristics":[{"name":"Area","value":"120 m2"}],"subsystems":[{"code":"ELECTRICAL","name":"Electrical installation"},{"code":"PRESSURE","name":"Pressure system"}]}'
call asset-without-subsystems POST /api/v1/assets "${COORDINATOR[@]}" '{"code":"CAP-0010","name":"Capture tank","type":"Capture tank","location":"Yard","jurisdictionCode":"DEMO-STRICT","responsible":'"$RESPONSIBLE"',"characteristics":[],"subsystems":[]}'
call asset-list-first-page GET '/api/v1/assets?page=0&size=1' "" ""
call asset-list-last-page GET '/api/v1/assets?page=1&size=1' "" ""

call schema-created POST /api/v1/schemas "${COORDINATOR[@]}" '{"name":"Capture plant inspection","assetType":"Capture plant","requiredSubsystems":["ELECTRICAL","PRESSURE"],"sections":[{"title":"Electrical","subsystemCode":"ELECTRICAL","criteria":[{"code":"EARTH","text":"Earthing resistance","severity":"MAJOR","rule":{"type":"numeric-range","unit":"ohm","minimum":0.00,"maximum":5.00},"requiredEvidence":[{"kind":"PHOTO","minimum":1}]},{"code":"PANEL","text":"Panel labelled","severity":"MINOR","rule":{"type":"yes-no","expected":"YES"},"requiredEvidence":[]}]},{"title":"Pressure","criteria":[{"code":"VALVE","text":"Relief valve state","severity":"CRITICAL","rule":{"type":"option-in-list","options":["Good","Acceptable"]},"requiredEvidence":[],"subsystemCode":"PRESSURE"}]}]}'
call schema-tank-created POST /api/v1/schemas "${COORDINATOR[@]}" '{"name":"Capture tank inspection","assetType":"Capture tank","sections":[{"title":"Tank","criteria":[{"code":"LEAKS","text":"No visible leaks","severity":"MAJOR","rule":{"type":"yes-no","expected":"YES"},"requiredEvidence":[]},{"code":"PAINT","text":"Paint condition","severity":"MINOR","rule":{"type":"option-in-list","options":["Good"]},"requiredEvidence":[]}]}]}'
call schema-list GET /api/v1/schemas "" ""
call schema GET '/api/v1/schemas/Capture%20plant' "" ""
call schema-versions GET '/api/v1/schemas/Capture%20plant/versions' "" ""

call inspection-assigned POST /api/v1/inspections "${COORDINATOR[@]}" '{"assetCode":"CAP-0001","inspector":{"id":"EMP-002","name":"Ines Duarte"},"plannedDate":"2026-10-20"}'
INSPECTION=$(field inspection-assigned "['id']")
call inspection-partial-assigned POST /api/v1/inspections "${COORDINATOR[@]}" '{"assetCode":"CAP-0001","inspector":{"id":"EMP-002","name":"Ines Duarte"},"plannedDate":"2026-10-21","sections":["Electrical"]}'
call inspection-list GET '/api/v1/inspections?asset=CAP-0001&status=open' "" ""
call answer-recorded PUT /api/v1/inspections/$INSPECTION/responses/EARTH/answer "${INSPECTOR[@]}" '{"value":2.50}'
call answer-yes-no PUT /api/v1/inspections/$INSPECTION/responses/PANEL/answer "${INSPECTOR[@]}" '{"value":false}'
call answer-option PUT /api/v1/inspections/$INSPECTION/responses/VALVE/answer "${INSPECTOR[@]}" '{"value":"Good"}'
call error-answer-type PUT /api/v1/inspections/$INSPECTION/responses/EARTH/answer "${INSPECTOR[@]}" '{"value":"abc"}'
call error-inspection-incomplete POST /api/v1/inspections/$INSPECTION/closure "${INSPECTOR[@]}"
call evidence-attached POST /api/v1/inspections/$INSPECTION/responses/EARTH/evidence "${INSPECTOR[@]}" '{"kind":"PHOTO","reference":"earth.jpg","description":"Meter reading"}'
call observation-added POST /api/v1/inspections/$INSPECTION/responses/PANEL/observations "${INSPECTOR[@]}" '{"text":"Two labels missing"}'
call inspection-open GET /api/v1/inspections/$INSPECTION "" ""
call responses-open GET /api/v1/inspections/$INSPECTION/responses "" ""
call revisions-empty GET /api/v1/inspections/$INSPECTION/revisions "" ""
call closure-one-finding POST /api/v1/inspections/$INSPECTION/closure "${INSPECTOR[@]}"
call inspection-closed GET /api/v1/inspections/$INSPECTION "" ""
call responses-closed GET /api/v1/inspections/$INSPECTION/responses "" ""
call rectification-no-finding POST /api/v1/inspections/$INSPECTION/rectifications "${INSPECTOR[@]}" '{"criterionCode":"EARTH","value":3.10,"reason":"Meter misread"}'
call revisions GET /api/v1/inspections/$INSPECTION/revisions "" ""
call act GET /api/v1/inspections/$INSPECTION/act "" ""
call audit-entries GET /api/v1/inspections/$INSPECTION/audit-entries "" ""

call findings-list GET '/api/v1/findings?asset=CAP-0001' "" ""
FINDING=$(field findings-list "['_embedded']['findings'][0]['id']")
call findings-summary GET /api/v1/assets/CAP-0001/findings-summary "" ""
call error-eligibility-whole-asset GET /api/v1/inspections/$INSPECTION/certification-eligibility "" ""
call eligibility-conditional GET "/api/v1/inspections/$INSPECTION/certification-eligibility?subsystem=ELECTRICAL" "" ""
call eligibility-approved GET "/api/v1/inspections/$INSPECTION/certification-eligibility?subsystem=PRESSURE" "" ""
call action-planned POST /api/v1/findings/$FINDING/corrective-actions "${COORDINATOR[@]}" '{"description":"Label the panel","responsible":'"$RESPONSIBLE"',"dueDate":"2030-12-01"}'
call action-second-planned POST /api/v1/findings/$FINDING/corrective-actions "${COORDINATOR[@]}" '{"description":"Replace the panel","responsible":'"$RESPONSIBLE"',"dueDate":"2030-12-15"}'
call verification-rejected POST /api/v1/findings/$FINDING/corrective-actions/1/verifications "${VERIFIER[@]}" '{"result":"REJECTED","notes":"Still missing"}'
call finding-open GET /api/v1/findings/$FINDING "" ""

call certificate-partial-issued POST /api/v1/certificates "${CERTIFIER[@]}" "{\"inspectionId\":\"$INSPECTION\",\"subsystemCode\":\"ELECTRICAL\"}"
call certificate-second-partial POST /api/v1/certificates "${CERTIFIER[@]}" "{\"inspectionId\":\"$INSPECTION\",\"subsystemCode\":\"PRESSURE\"}"
call error-already-certified POST /api/v1/certificates "${CERTIFIER[@]}" "{\"inspectionId\":\"$INSPECTION\",\"subsystemCode\":\"PRESSURE\"}"
PARTIAL=$(field certificate-partial-issued "['number']"); SECOND_PARTIAL=$(field certificate-second-partial "['number']")
call certificate-partial GET /api/v1/certificates/$PARTIAL "" ""
call certificate-global-issued POST /api/v1/assets/CAP-0001/global-certificates "${CERTIFIER[@]}" "{\"schemaVersion\":1,\"partialCertificateNumbers\":[\"$PARTIAL\",\"$SECOND_PARTIAL\"]}"
GLOBAL=$(field certificate-global-issued "['number']")
call certificate-global GET /api/v1/certificates/$GLOBAL "" ""
call certificate-document GET /api/v1/certificates/$GLOBAL/document "" ""
call certificates-list GET '/api/v1/certificates?asset=CAP-0001' "" ""
call suspension-created POST /api/v1/certificates/$SECOND_PARTIAL/suspensions "${CERTIFIER[@]}" '{"reason":"Valve recall"}'
call certificate-suspended GET /api/v1/certificates/$SECOND_PARTIAL "" ""
call certificate-global-dependency-invalid GET /api/v1/certificates/$GLOBAL "" ""
call verification-accepted POST /api/v1/findings/$FINDING/corrective-actions/1/verifications "${VERIFIER[@]}" '{"result":"ACCEPTED","notes":"Labels in place"}'
call finding-closed GET /api/v1/findings/$FINDING "" ""

call version-published-future POST '/api/v1/schemas/Capture%20plant/versions' "${COORDINATOR[@]}" '{"effectiveFrom":"2031-01-01T00:00:00-03:00","requiredSubsystems":["ELECTRICAL","PRESSURE"],"sections":[{"title":"Electrical","subsystemCode":"ELECTRICAL","criteria":[{"code":"EARTH","text":"Earthing resistance","severity":"MAJOR","rule":{"type":"numeric-range","unit":"ohm","minimum":0.00,"maximum":4.00},"requiredEvidence":[]}]},{"title":"Pressure","subsystemCode":"PRESSURE","criteria":[{"code":"VALVE","text":"Relief valve state","severity":"CRITICAL","rule":{"type":"option-in-list","options":["Good"]},"requiredEvidence":[]}]}]}'
call schema-with-scheduled-version GET '/api/v1/schemas/Capture%20plant' "" ""
call effective-version GET '/api/v1/schemas/Capture%20plant/effective-version?at=2031-06-01T00:00:00Z' "" ""
call error-no-effective-version GET '/api/v1/schemas/Capture%20plant/effective-version?at=2000-01-01T00:00:00Z' "" ""

call tank-inspection-assigned POST /api/v1/inspections "${COORDINATOR[@]}" '{"assetCode":"CAP-0010","inspector":{"id":"EMP-002","name":"Ines Duarte"},"plannedDate":"2026-10-22"}'
TANK=$(field tank-inspection-assigned "['id']")
call eligibility-rejected-open GET /api/v1/inspections/$TANK/certification-eligibility "" ""
call tank-answer-leaks PUT /api/v1/inspections/$TANK/responses/LEAKS/answer "${INSPECTOR[@]}" '{"value":false}'
call tank-answer-paint PUT /api/v1/inspections/$TANK/responses/PAINT/answer "${INSPECTOR[@]}" '{"value":"Flaking"}'
call closure-two-findings POST /api/v1/inspections/$TANK/closure "${INSPECTOR[@]}"
call eligibility-rejected GET /api/v1/inspections/$TANK/certification-eligibility "" ""
call error-not-eligible POST /api/v1/certificates "${CERTIFIER[@]}" "{\"inspectionId\":\"$TANK\"}"
call findings-list-empty GET '/api/v1/findings?asset=NO-FINDINGS' "" ""

call error-asset-not-found GET /api/v1/assets/NOPE "" ""
call error-asset-already-registered POST /api/v1/assets "${COORDINATOR[@]}" '{"code":"CAP-0001","name":"n","type":"t","location":"l","jurisdictionCode":"DEMO-STRICT","responsible":'"$RESPONSIBLE"',"characteristics":[],"subsystems":[]}'
call error-unknown-field POST /api/v1/assets "${COORDINATOR[@]}" '{"code":"CAP-0002","name":"n","type":"t","location":"l","jurisdictionCode":"DEMO-STRICT","responsible":'"$RESPONSIBLE"',"characteristics":[],"subsystems":[],"extra":1}'
call error-missing-person POST /api/v1/assets "" "" '{"code":"CAP-0003","name":"n","type":"t","location":"l","jurisdictionCode":"DEMO-STRICT","responsible":'"$RESPONSIBLE"',"characteristics":[],"subsystems":[]}'
