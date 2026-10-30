(() => {
  const $ = (id) => document.getElementById(id);
  const set = (id, value) => { const node = $(id); if (node) node.textContent = value; };
  const postJson = async (url, body) => {
    const response = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.detail || data.message || `Request failed (${response.status}).`);
    return data;
  };
  const showRules = (target, data) => {
    target.replaceChildren();
    const summary = document.createElement('strong'); summary.textContent = data.summary; target.append(summary);
    const list = document.createElement('ul'); list.className = 'tool-result-list';
    data.rules.forEach((rule) => {
      const item = document.createElement('li');
      item.textContent = `${rule.name}: input ${rule.input}=${rule.inputValue}; rule ${rule.rule}; calculation ${rule.calculation}; reason: ${rule.reason}`;
      list.append(item);
    });
    target.append(list);
    const note = document.createElement('p'); note.className = 'lab-footnote'; note.textContent = data.limitation; target.append(note);
  };
  const alertInputs = (prefix) => ({
    spo2Below: Number(($(prefix + 'spo2') || $(prefix + 'spo2-below')).value),
    rateAbove: Number(($(prefix + 'rate') || $(prefix + 'rate-above')).value),
    pulseBelow: Number(($(prefix + 'pulse-low') || $(prefix + 'pulse-below')).value),
    pulseAbove: Number(($(prefix + 'pulse-high') || $(prefix + 'pulse-above')).value)
  });
  const latestSample = async () => {
    const response = await fetch('/api/readings');
    if (!response.ok) throw new Error('Could not load your saved sessions. Sign in again and retry.');
    const samples = await response.json();
    if (!samples.length) throw new Error('Save a user-created simulation sample before evaluating its rules.');
    return samples.slice().sort((a, b) => b.id - a.id)[0];
  };

  if ($('check-reliability')) $('check-reliability').addEventListener('click', async () => {
    const result = $('reliability-results'); result.textContent = 'Evaluating the selected teaching scenario…';
    try {
      const issues = Array.from(document.querySelectorAll('[name="reliability-issue"]:checked')).map((input) => input.value);
      const data = await postJson('/api/reliability/check', { issues });
      result.replaceChildren(); const summary = document.createElement('strong'); summary.textContent = data.summary; result.append(summary);
      const list = document.createElement('ul'); list.className = 'tool-result-list';
      data.scenarios.forEach((scenario) => { const item = document.createElement('li'); item.textContent = `${scenario.condition}: ${scenario.possibleEffect} Suggested action: ${scenario.recommendedAction}`; list.append(item); });
      if (data.recheckSuggested) result.append(list);
      const note = document.createElement('p'); note.className = 'lab-footnote'; note.textContent = data.limitation; result.append(note);
    } catch (error) { result.textContent = error.message; }
  });

  if ($('evaluate-aqi')) $('evaluate-aqi').addEventListener('click', async () => {
    const result = $('aqi-result');
    try {
      const data = await postJson('/api/air-quality/advice', { aqi: Number($('manual-aqi').value), scale: $('aqi-scale').value });
      result.replaceChildren();
      for (const [label, value] of [['Entered AQI', `${data.inputAqi} · ${data.category} (${data.scale})`], ['Possible concern', data.possibleConcern], ['Practical precaution', data.practicalPrecaution], ['Source', data.sourceNote], ['Scale reference', data.scaleReference], ['Limitation', data.limitation]]) {
        const row = document.createElement('p'); const strong = document.createElement('strong'); strong.textContent = `${label}: `; row.append(strong, document.createTextNode(value)); result.append(row);
      }
    } catch (error) { result.textContent = error.message; }
  });

  async function evaluateSavedSample(input, output) {
    output.textContent = 'Evaluating configured educational rules…';
    try {
      const sample = await latestSample();
      const data = await postJson('/api/alerts/evaluate', { ...sample, ...input });
      showRules(output, data);
    } catch (error) { output.textContent = error.message; }
  }
  if ($('evaluate-alerts')) $('evaluate-alerts').addEventListener('click', () => evaluateSavedSample(alertInputs('alert-'), $('alert-results')));
  if ($('run-dashboard-alerts')) $('run-dashboard-alerts').addEventListener('click', () => evaluateSavedSample(alertInputs('dash-alert-'), $('dashboard-alert')));

  if ($('run-whatif')) {
    let latestReport = null;
    const result = $('whatif-result'), exportButton = $('export-whatif');
    $('run-whatif').addEventListener('click', async () => {
      const scenario = (key) => ({
        name: $(`whatif-${key}-name`).value.trim(),
        respiratoryRate: Number($(`whatif-${key}-rate`).value),
        tidalVolumeMl: Number($(`whatif-${key}-volume`).value),
        compliance: Number($(`whatif-${key}-compliance`).value)
      });
      result.textContent = 'Running both scenarios and saving the results…'; exportButton.disabled = true;
      try {
        latestReport = await postJson('/api/experiments/compare', { scenarioA: scenario('a'), scenarioB: scenario('b') });
        result.replaceChildren();
        for (const [label, scenarioData] of [['Scenario A', latestReport.scenarioA], ['Scenario B', latestReport.scenarioB]]) {
          const row = document.createElement('p'); const strong = document.createElement('strong'); strong.textContent = `${label} — ${scenarioData.name}: `; row.append(strong, document.createTextNode(scenarioData.calculation)); result.append(row);
        }
        const delta = document.createElement('p'); delta.textContent = `Difference (B − A): ${latestReport.differenceFormula} L/min.`; result.append(delta);
        const assumptions = document.createElement('ul'); assumptions.className = 'tool-result-list';
        latestReport.assumptions.forEach((assumption) => { const item = document.createElement('li'); item.textContent = assumption; assumptions.append(item); }); result.append(assumptions);
        const note = document.createElement('p'); note.className = 'lab-footnote'; note.textContent = latestReport.limitation; result.append(note);
        exportButton.disabled = false;
        document.dispatchEvent(new CustomEvent('aero-experiments-updated'));
      } catch (error) { latestReport = null; result.textContent = error.message; }
    });
    exportButton.addEventListener('click', () => {
      if (!latestReport) return;
      const query = new URLSearchParams({ firstId: latestReport.scenarioA.id, secondId: latestReport.scenarioB.id });
      const link = document.createElement('a'); link.href = `/api/experiments/report?${query}`; link.hidden = true;
      document.body.append(link); link.click(); window.setTimeout(() => link.remove(), 1000);
    });
  }

  if ($('alert-results')) evaluateSavedSample(alertInputs('alert-'), $('alert-results'));
})();
