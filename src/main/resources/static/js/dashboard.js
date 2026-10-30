(() => {
  const $ = (id) => document.getElementById(id);
  const values = { spo2: 98, breathingRate: 16, pulse: 72 };
  const baseline = { ...values };
  let tick = 0;
  let timer = null;

  function renderValues() {
    $('spo2-value').textContent = values.spo2;
    $('breathing-value').textContent = values.breathingRate;
    $('pulse-value').textContent = values.pulse;
    $('ventilation-value').textContent = (values.breathingRate * 0.5).toFixed(1);
  }
  function setState(text) { $('sim-state').textContent = text; }
  function simulateStep() {
    tick += 1;
    values.spo2 = 97 + Math.round(Math.sin(tick / 3));
    values.breathingRate = 14 + (tick % 6);
    values.pulse = 68 + (tick % 13);
    renderValues();
    setState(`Running · illustrative cycle ${tick}`);
  }
  function start() {
    if (timer) return;
    simulateStep();
    timer = window.setInterval(simulateStep, 1800);
  }
  function pause() {
    window.clearInterval(timer);
    timer = null;
    setState('Paused · simulated values held');
  }
  function reset() {
    pause();
    tick = 0;
    Object.assign(values, baseline);
    renderValues();
    setState('Reset to the illustrative baseline');
  }
  async function saveSample(sample = values, dataOrigin = 'SIMULATED') {
    const response = await fetch('/api/readings', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...sample, dataOrigin })
    });
    if (!response.ok) throw new Error('The sample was rejected. Check the value ranges and try again.');
    await loadReadings();
  }

  function formatDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
  }
  function drawTrend(readings) {
    const chart = $('respiratory-chart');
    const empty = $('trend-empty');
    const line = $('chart-line');
    const area = $('chart-area');
    const points = $('chart-points');
    const samples = readings.filter((reading) => reading.dataOrigin === 'USER_MEASURED').slice().sort((a, b) => a.id - b.id).slice(-7);
    $('trend-count').textContent = `${samples.length} sample${samples.length === 1 ? '' : 's'}`;
    points.replaceChildren();
    if (samples.length < 2) {
      chart.hidden = true;
      empty.hidden = false;
      empty.textContent = samples.length === 0
        ? 'No user-entered measurements saved yet. The chart uses only values you mark as measured, never simulated examples.'
        : 'One user-entered measurement is not a trend. Save another measurement to compare values over time.';
      line.setAttribute('points', '');
      area.setAttribute('d', '');
      return;
    }
    chart.hidden = false;
    empty.hidden = true;
    const left = 50, right = 580, top = 25, bottom = 145;
    const coords = samples.map((sample, index) => ({
      x: samples.length === 1 ? (left + right) / 2 : left + index * (right - left) / (samples.length - 1),
      y: bottom - ((Math.max(70, Math.min(100, sample.spo2)) - 70) / 30) * (bottom - top)
    }));
    line.setAttribute('points', coords.map(({ x, y }) => `${x},${y}`).join(' '));
    area.setAttribute('d', `M${coords[0].x} ${bottom} ${coords.map(({ x, y }) => `L${x} ${y}`).join(' ')} L${coords.at(-1).x} ${bottom} Z`);
    coords.forEach((point, index) => {
      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', point.x); circle.setAttribute('cy', point.y); circle.setAttribute('r', '5');
      const title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
      title.textContent = `Sample ${index + 1}: ${samples[index].spo2}% SpO₂`;
      circle.append(title); points.append(circle);
    });
  }
  function fillReadings(readings) {
    const table = $('recent-reading-table'), empty = $('recent-reading-empty'), body = $('recent-reading-body');
    const recent = readings.slice().sort((a, b) => b.id - a.id).slice(0, 5);
    body.replaceChildren();
    if (!recent.length) {
      table.hidden = true; empty.hidden = false; empty.textContent = 'No simulated readings saved yet. Save a sample to start your history.';
    } else {
      table.hidden = false; empty.hidden = true;
      recent.forEach((reading) => {
        const row = document.createElement('tr');
        [formatDate(reading.recordedAt), `${reading.spo2}%`, `${reading.breathingRate} /min`, `${reading.pulse} BPM`, reading.dataOrigin === 'USER_MEASURED' ? 'User-entered measurement · not device verified' : reading.dataOrigin === 'SIMULATED' ? 'Simulated example' : 'Legacy source unspecified', `${(reading.breathingRate * 0.5).toFixed(1)} L/min*`]
          .forEach((text) => { const cell = document.createElement('td'); cell.textContent = text; row.append(cell); });
        body.append(row);
      });
    }
  }
  async function renderAlert(readings) {
    const box = $('dashboard-alert');
    const latest = readings.slice().sort((a, b) => b.id - a.id)[0];
    box.classList.remove('warn');
    if (!latest) {
      box.innerHTML = '<strong>Waiting for a saved sample</strong>Save a simulated sample to see which configured demonstration rule matches.';
      return;
    }
    try {
      const payload = { ...latest,
        spo2Below: Number($('dash-alert-spo2').value), rateAbove: Number($('dash-alert-rate').value),
        pulseBelow: Number($('dash-alert-pulse-low').value), pulseAbove: Number($('dash-alert-pulse-high').value) };
      const response = await fetch('/api/alerts/evaluate', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      if (!response.ok) throw new Error('The configured alert rules could not be evaluated.');
      const data = await response.json();
      box.replaceChildren();
      const heading = document.createElement('strong'); heading.textContent = data.summary; box.append(heading);
      if (data.matchedRules) box.classList.add('warn');
      const list = document.createElement('ul'); list.className = 'tool-result-list';
      data.rules.forEach((rule) => { const item = document.createElement('li'); item.textContent = `${rule.name}: input ${rule.input}=${rule.inputValue}; rule ${rule.rule}; calculation ${rule.calculation}; reason: ${rule.reason}`; list.append(item); });
      box.append(list);
      const note = document.createElement('p'); note.className = 'lab-footnote'; note.textContent = data.limitation; box.append(note);
    } catch (error) { box.textContent = error.message; }
  }
  async function loadReadings() {
    const response = await fetch('/api/readings');
    if (!response.ok) throw new Error('Could not load saved readings. Refresh the page to retry.');
    const readings = await response.json();
    drawTrend(readings); fillReadings(readings); await renderAlert(readings);
  }
  async function loadExperiments() {
    const table = $('recent-experiment-table'), empty = $('recent-experiment-empty'), body = $('recent-experiment-body');
    try {
      const response = await fetch('/api/experiments');
      if (!response.ok) throw new Error('Could not load experiment history.');
      const runs = (await response.json()).sort((a, b) => b.id - a.id).slice(0, 5);
      body.replaceChildren();
      if (!runs.length) {
        table.hidden = true; empty.hidden = false; empty.textContent = 'No experiment runs saved yet. Open the Digital Twin lab to create a scenario.';
      } else {
        table.hidden = false; empty.hidden = true;
        runs.forEach((run) => {
          const row = document.createElement('tr');
          [run.name, `${run.respiratoryRate} /min`, `${run.tidalVolumeMl} mL`, `${Number(run.simulatedVentilation).toFixed(2)} L/min`, formatDate(run.createdAt)]
            .forEach((text) => { const cell = document.createElement('td'); cell.textContent = text; row.append(cell); });
          body.append(row);
        });
      }
    } catch (error) { table.hidden = true; empty.hidden = false; empty.textContent = 'Experiment history is temporarily unavailable.'; }
  }

  $('sim-start').addEventListener('click', start);
  $('sim-resume').addEventListener('click', start);
  $('sim-pause').addEventListener('click', pause);
  $('sim-reset').addEventListener('click', reset);
  $('sim-save').addEventListener('click', async () => {
    try { setState('Saving simulated sample…'); await saveSample(); setState('Simulated sample saved to history'); }
    catch (error) { setState(error.message); }
  });
  $('reading-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const sample = { spo2: Number(form.spo2.value), breathingRate: Number(form.breathingRate.value), pulse: Number(form.pulse.value) };
    $('reading-message').textContent = 'Saving simulated sample…';
    try { await saveSample(sample, $('reading-origin').value); $('reading-message').textContent = 'Saved with the selected source label. User-entered measurements are not independently device verified.'; }
    catch (error) { $('reading-message').textContent = error.message; }
  });

  renderValues();
  loadReadings().catch((error) => { $('trend-empty').textContent = error.message; $('recent-reading-empty').textContent = error.message; });
  loadExperiments();
})();
