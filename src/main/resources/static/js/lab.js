(() => {
  const moduleId = document.body.dataset.module;
  const byId = (id) => document.getElementById(id);
  const setText = (id, text) => { const node = byId(id); if (node) node.textContent = text; };
  const escapeCsv = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`;
  const downloadText = (filename, text, type = 'text/csv;charset=utf-8') => {
    const url = URL.createObjectURL(new Blob([text], { type }));
    const link = document.createElement('a'); link.href = url; link.download = filename; link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  function setupAnatomy() {
    const details = {
      'Nose and airway entrance': ['Nose and airway entrance', 'The nose filters, warms, and humidifies incoming air. Air can also enter through the mouth.', 'The nasal cavity is part of the upper respiratory tract.'],
      Trachea: ['Trachea', 'The trachea is a conducting airway that carries air from the larynx toward the right and left main bronchi.', 'Cartilage rings help keep the airway open.'],
      Bronchi: ['Bronchi', 'The two main bronchi branch from the trachea and carry air into each lung.', 'The airways branch repeatedly as they extend into the lungs.'],
      Lungs: ['Lungs', 'The lungs contain branching airways and millions of tiny alveolar air spaces where gas exchange takes place.', 'Right and left lungs have different numbers of lobes.'],
      Bronchioles: ['Bronchioles', 'Bronchioles are smaller branching airways that distribute air through lung tissue toward alveolar regions.', 'Unlike the larger bronchi, small bronchioles do not have cartilage rings.'],
      Alveoli: ['Alveoli', 'Alveoli are microscopic air sacs next to pulmonary capillaries. Oxygen and carbon dioxide diffuse across the thin interface.', 'A single alveolus is tiny; the lungs contain many millions of them.']
    };
    function choose(name) {
      const item = details[name]; if (!item) return;
      setText('anatomy-title', item[0]); setText('anatomy-description', item[1]); setText('anatomy-fact', item[2]);
      document.querySelectorAll('[data-structure]').forEach((node) => node.classList.toggle('is-selected', node.dataset.structure === name));
      byId('alveoli-detail').hidden = name !== 'Alveoli';
    }
    document.querySelectorAll('[data-anatomy]').forEach((button) => button.addEventListener('click', () => choose(button.dataset.anatomy)));
    document.querySelectorAll('[data-structure]').forEach((node) => node.addEventListener('click', () => choose(node.dataset.structure)));
    const steps = [
      ['Inhale · nose', 'Air enters through the nose, where it is filtered, warmed, and humidified.'],
      ['Conduct · trachea', 'The trachea conducts air down toward the main bronchi.'],
      ['Branch · bronchi and bronchioles', 'Air travels through progressively smaller branches within the lungs.'],
      ['Exchange · alveoli', 'At the alveolar-capillary interface, oxygen and carbon dioxide diffuse in opposite directions.'],
      ['Exhale · return path', 'Air moves back out through the branching airways and leaves the body.']
    ];
    let step = -1;
    const showStep = () => {
      step = (step + 1) % steps.length;
      setText('airflow-step-label', steps[step][0]); setText('airflow-step-copy', steps[step][1]);
      const path = byId('airflow-path'); if (path) path.classList.add('journey-active');
    };
    byId('airflow-start').addEventListener('click', () => { step = -1; showStep(); });
    byId('airflow-next').addEventListener('click', showStep);
    byId('airflow-reset').addEventListener('click', () => {
      step = -1; setText('airflow-step-label', 'Ready');
      setText('airflow-step-copy', 'Start the guide to follow one conceptual breath from the nose to the alveoli and back out.');
      byId('airflow-path').classList.remove('journey-active');
    });
    let asked = 0, correct = 0;
    document.querySelectorAll('#anatomy-quiz [data-answer]').forEach((button) => button.addEventListener('click', () => {
      asked += 1; const right = button.dataset.answer === 'alveoli'; if (right) correct += 1;
      const feedback = byId('anatomy-quiz-feedback');
      feedback.className = `quiz-feedback ${right ? 'correct' : 'incorrect'}`;
      feedback.textContent = right ? 'Correct. Gas exchange occurs across alveolar-capillary surfaces.' : 'Not quite. Follow the airways to the tiny alveolar sacs.';
      setText('anatomy-score', `Score: ${correct} / ${asked}`);
    }));
  }

  function setupGasExchange() {
    let phase = 0, timer = null;
    const oxygen = byId('alveolar-oxygen'), co2 = byId('blood-co2');
    function render() {
      const o = Number(oxygen.value), c = Number(co2.value);
      byId('oxygen-output').value = o; byId('co2-output').value = c;
      const oxygenIndex = Math.round(o * (100 - c) / 100);
      const co2Index = Math.round(c * (100 - o) / 100);
      setText('oxygen-index', `${oxygenIndex}/100`); setText('co2-index', `${co2Index}/100`);
      byId('oxygen-bar').style.width = `${oxygenIndex}%`; byId('co2-bar').style.width = `${co2Index}%`;
      const note = oxygenIndex >= 35 && co2Index >= 20
        ? 'In this teaching setup, both conceptual gradients support visible transfer.'
        : oxygenIndex < 20 ? 'The oxygen transfer index is lower because the modeled oxygen availability is low or the opposing CO₂ condition is high.'
          : 'The carbon dioxide transfer index is lower because the two selected model inputs create a smaller relative gradient.';
      setText('gas-explanation', `${note} Indices use O₂ × (100 − CO₂)/100 and CO₂ × (100 − O₂)/100.`);
      setText('gas-result', `Current preset values: oxygen availability ${o}/100 and blood-side CO₂ burden ${c}/100. Relative transfer indices: O₂ ${oxygenIndex}/100; CO₂ ${co2Index}/100.`);
    }
    function step() {
      phase = (phase + 1) % 2;
      const inhale = phase === 0;
      setText('gas-phase', inhale ? 'Inhalation phase' : 'Exhalation phase');
      setText('gas-step-copy', inhale
        ? 'In this conceptual phase, fresh air reaches alveolar spaces and oxygen diffuses toward capillary blood.'
        : 'In this conceptual phase, carbon dioxide diffuses from capillary blood toward alveolar air for exhalation.');
      byId('gas-illustration').classList.add('is-running');
    }
    oxygen.addEventListener('input', render); co2.addEventListener('input', render);
    byId('gas-step').addEventListener('click', step);
    byId('gas-play').addEventListener('click', () => { if (timer) return; step(); timer = window.setInterval(step, 1700); });
    byId('gas-pause').addEventListener('click', () => { window.clearInterval(timer); timer = null; byId('gas-illustration').classList.add('is-paused'); setText('gas-phase', 'Paused'); });
    byId('gas-reset').addEventListener('click', () => { window.clearInterval(timer); timer = null; phase = 0; oxygen.value = 80; co2.value = 40; render(); setText('gas-phase', 'Paused'); setText('gas-step-copy', 'Ready to model inhalation and diffusion.'); byId('gas-illustration').classList.remove('is-running', 'is-paused'); });
    const presets = { balanced: [80, 40], low: [40, 40], high: [80, 80] };
    document.querySelectorAll('[data-gas-preset]').forEach((button) => button.addEventListener('click', () => {
      [oxygen.value, co2.value] = presets[button.dataset.gasPreset]; render();
      setText('gas-step-copy', `Preset loaded: ${button.textContent}. Adjust either teaching condition to compare.`);
    }));
    render();
  }

  function setupCirculation() {
    const rate = byId('heart-rate'), oxygen = byId('blood-oxygen');
    const explanations = {
      Lungs: 'Pulmonary circulation brings oxygen-poor blood to the lungs; gas exchange concepts are explored in the gas-exchange lab.',
      Heart: 'The right heart sends blood toward the lungs. The left heart sends oxygen-rich blood toward modeled body tissues.',
      'Body tissues': 'Systemic circulation delivers oxygen-rich blood to body tissues and returns oxygen-poor blood to the heart.'
    };
    function render() {
      rate.nextElementSibling.value = rate.value; oxygen.nextElementSibling.value = oxygen.value;
      setText('cycle-pace', `${rate.value}/min`); setText('oxygen-level', `${oxygen.value}/100`);
      setText('circulation-explanation', `Teaching inputs: ${rate.value} modeled cycles/min and oxygen availability ${oxygen.value}/100. These values only alter this diagram's labels and are not physiological outputs.`);
    }
    rate.addEventListener('input', render); oxygen.addEventListener('input', render);
    document.querySelectorAll('[data-circ]').forEach((node) => node.addEventListener('click', () => {
      document.querySelectorAll('[data-circ]').forEach((item) => item.classList.toggle('is-selected', item === node));
      setText('circulation-explanation', explanations[node.dataset.circ]);
    }));
    const guide = [
      ['1 · Tissues to heart', 'Oxygen-poor blood returns from modeled body tissues to the right side of the heart.'],
      ['2 · Heart to lungs', 'The right side sends blood through the pulmonary pathway toward the lungs.'],
      ['3 · Lungs to heart', 'After conceptual gas exchange, oxygen-rich blood returns to the left side of the heart.'],
      ['4 · Heart to tissues', 'The left side sends oxygen-rich blood through the systemic pathway toward body tissues.']
    ];
    let index = -1, timer = null;
    function next() { index = (index + 1) % guide.length; setText('circ-guide-title', guide[index][0]); setText('circ-guide-copy', guide[index][1]); setText('circ-step-count', `Step ${index + 1} / ${guide.length}`); }
    byId('circ-play').addEventListener('click', () => { next(); byId('circulation-status').textContent = 'Guided model running'; document.querySelector('.circulation-art').classList.add('is-running'); if (!timer) timer = window.setInterval(next, 2600); });
    byId('circ-next').addEventListener('click', next);
    byId('circ-reset').addEventListener('click', () => { window.clearInterval(timer); timer = null; index = -1; setText('circ-guide-title', 'Ready to begin'); setText('circ-guide-copy', 'Step through the pulmonary and systemic pathways.'); setText('circ-step-count', 'Step 0 / 4'); setText('circulation-status', 'Paused'); document.querySelector('.circulation-art').classList.remove('is-running'); });
    let asked = 0, rightCount = 0;
    document.querySelectorAll('#circulation-quiz [data-answer]').forEach((button) => button.addEventListener('click', () => {
      asked += 1; const correct = button.dataset.answer === 'right'; if (correct) rightCount += 1;
      const feedback = byId('circulation-quiz-feedback'); feedback.className = `quiz-feedback ${correct ? 'correct' : 'incorrect'}`;
      feedback.textContent = `${correct ? 'Correct.' : 'Try again.'} The right ventricle pumps blood toward the lungs. Score ${rightCount}/${asked}.`;
    }));
    render();
  }

  async function setupAnalytics() {
    const tbody = byId('analytics-history'), range = byId('analytics-range');
    let readings = [];
    const timeFor = (item) => new Date(item.recordedAt).getTime();
    const triggerText = (item) => {
      const triggers = [];
      const spo2Floor = Number(byId('alert-spo2-below').value), rateCeiling = Number(byId('alert-rate-above').value);
      const pulseFloor = Number(byId('alert-pulse-below').value), pulseCeiling = Number(byId('alert-pulse-above').value);
      if (item.spo2 < spo2Floor) triggers.push(`SpO₂ ${item.spo2} < ${spo2Floor} (${item.spo2} < ${spo2Floor} = true)`);
      if (item.breathingRate > rateCeiling) triggers.push(`Rate ${item.breathingRate}/min > ${rateCeiling} (${item.breathingRate} > ${rateCeiling} = true)`);
      if (item.pulse < pulseFloor) triggers.push(`Pulse ${item.pulse} < ${pulseFloor} (${item.pulse} < ${pulseFloor} = true)`);
      if (item.pulse > pulseCeiling) triggers.push(`Pulse ${item.pulse} > ${pulseCeiling} (${item.pulse} > ${pulseCeiling} = true)`);
      return triggers.length ? triggers.join('; ') : 'No configured caution rule matched';
    };
    function filtered() {
      const span = { '24h': 86400000, '7d': 604800000, '30d': 2592000000 }[range.value];
      if (!span) return readings.slice();
      const cutoff = Date.now() - span;
      return readings.filter((item) => timeFor(item) >= cutoff);
    }
    function updateChart(samples) {
      const empty = byId('analytics-empty'), line = byId('analytics-line'), group = byId('analytics-points');
      group.replaceChildren();
      if (!samples.length) { line.setAttribute('points', ''); empty.hidden = false; return; }
      empty.hidden = true;
      const ordered = samples.slice().sort((a, b) => a.id - b.id).slice(-20);
      const points = ordered.map((item, i) => ({ x: ordered.length === 1 ? 397 : 58 + i * 680 / (ordered.length - 1), y: 180 - (Math.max(70, Math.min(100, item.spo2)) - 70) * 5 }));
      line.setAttribute('points', points.map((p) => `${p.x},${p.y}`).join(' '));
      points.forEach((p, i) => {
        const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle'); circle.setAttribute('cx', p.x); circle.setAttribute('cy', p.y); circle.setAttribute('r', '4'); circle.setAttribute('fill', '#07111f'); circle.setAttribute('stroke', '#54e4e2'); circle.setAttribute('stroke-width', '2');
        const title = document.createElementNS('http://www.w3.org/2000/svg', 'title'); title.textContent = `${ordered[i].spo2}% SpO₂, ${ordered[i].breathingRate} breaths/min`; circle.append(title); group.append(circle);
      });
    }
    function updateCompareOptions(samples) {
      const a = byId('compare-reading-a'), b = byId('compare-reading-b');
      [a, b].forEach((select) => { const prior = select.value; select.replaceChildren(); samples.slice().sort((x, y) => y.id - x.id).forEach((item) => { const option = document.createElement('option'); option.value = item.id; option.textContent = `#${item.id} · ${new Date(timeFor(item)).toLocaleString()}`; select.append(option); }); if (samples.some((item) => String(item.id) === prior)) select.value = prior; });
      compare();
    }
    async function compare() {
      const a = readings.find((item) => String(item.id) === byId('compare-reading-a').value);
      const b = readings.find((item) => String(item.id) === byId('compare-reading-b').value);
      if (!a || !b || a.id === b.id) { setText('reading-comparison', 'Choose two different saved samples to compare their modeled values.'); return; }
      try {
        const response = await fetch('/api/readings/compare', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ firstId: a.id, secondId: b.id }) });
        const data = await response.json(); if (!response.ok) throw new Error(data.detail || 'Could not compare these saved sessions.');
        const changes = data.changes;
        const signed = (value) => value > 0 ? `+${value}` : String(value);
        setText('reading-comparison', `Sample A #${a.id}: SpO₂ ${a.spo2}%, rate ${a.breathingRate}/min, pulse ${a.pulse} BPM. Sample B #${b.id}: SpO₂ ${b.spo2}%, rate ${b.breathingRate}/min, pulse ${b.pulse} BPM. Changes (B − A): SpO₂ ${signed(changes.spo2PercentagePoints)} percentage points; rate ${signed(changes.breathingRatePerMinute)}/min; pulse ${signed(changes.pulseBpm)} BPM. ${data.explanation}`);
      } catch (error) { setText('reading-comparison', error.message); }
    }
    function render() {
      const samples = filtered().sort((a, b) => b.id - a.id);
      const alerts = samples.filter((item) => triggerText(item) !== 'No configured caution rule matched');
      const average = samples.length ? Math.round(samples.reduce((sum, item) => sum + item.spo2, 0) / samples.length) : null;
      setText('analytics-count', samples.length); setText('analytics-alert-count', alerts.length); setText('analytics-average', average === null ? '—' : `${average}%`);
      setText('analytics-latest', samples.length ? `${samples[0].spo2}% · ${samples[0].breathingRate}/min` : '—');
      const windowText = range.options[range.selectedIndex].textContent;
      setText('analytics-interpretation', !samples.length ? `No readings match “${windowText}”. Save simulated samples from the dashboard to build a trend.` : alerts.length ? `${alerts.length} of ${samples.length} sample(s) match at least one displayed teaching rule. The rule names below identify the exact input and threshold; this is not a medical assessment.` : `${samples.length} saved sample(s) in “${windowText}”; none match the displayed teaching rules. This is not a clinical interpretation.`);
      tbody.replaceChildren();
      if (!samples.length) { const row = document.createElement('tr'), cell = document.createElement('td'); cell.colSpan = 6; cell.textContent = 'No saved samples for this time range.'; row.append(cell); tbody.append(row); }
      else samples.forEach((item) => { const row = document.createElement('tr'); [new Date(timeFor(item)).toLocaleString(), `${item.spo2}%`, `${item.breathingRate}/min`, `${item.pulse} BPM`, `${(item.breathingRate * 0.5).toFixed(1)} L/min*`, triggerText(item)].forEach((text) => { const cell = document.createElement('td'); cell.textContent = text; row.append(cell); }); tbody.append(row); });
      updateChart(samples); updateCompareOptions(samples);
    }
    function exportCsv() {
      const samples = filtered();
      if (!samples.length) { setText('analytics-interpretation', 'There are no samples in this time range to export.'); return; }
      const rows = [['recordedAt', 'spo2Percent', 'breathingRatePerMinute', 'pulseBpm', 'modeledVentilationEstimateLPerMin', 'educationalAlert']];
      samples.forEach((item) => rows.push([item.recordedAt, item.spo2, item.breathingRate, item.pulse, (item.breathingRate * 0.5).toFixed(2), triggerText(item)]));
      downloadText('aero-ai-simulated-readings.csv', rows.map((row) => row.map(escapeCsv).join(',')).join('\r\n'));
    }
    range.addEventListener('change', render);
    ['alert-spo2-below', 'alert-rate-above', 'alert-pulse-below', 'alert-pulse-above'].forEach((id) => byId(id).addEventListener('change', render));
    byId('compare-reading-a').addEventListener('change', compare); byId('compare-reading-b').addEventListener('change', compare); byId('export-readings').addEventListener('click', exportCsv);
    try { const response = await fetch('/api/readings'); if (!response.ok) throw new Error('Saved readings are unavailable. Sign in again or refresh the page.'); readings = await response.json(); render(); }
    catch (error) { tbody.innerHTML = `<tr><td colspan="6">${error.message}</td></tr>`; setText('analytics-interpretation', error.message); setText('analytics-count', '—'); setText('analytics-alert-count', '—'); }
  }

  function setupDevice() {
    const configs = {
      oximeter: { title: 'Pulse oximeter concept', kicker: 'Non-invasive sensor concept', description: 'A pulse oximeter estimates oxygen saturation and pulse from light absorption. This panel generates sample values only.', limitation: 'A real device can be affected by motion, perfusion, placement, and device-specific accuracy. This lab has no optical sensor.', main: ['Oxygen sample', 50, 100, 98, '%'], secondary: ['Pulse sample', 30, 180, 72, 'BPM'], primaryLabel: 'Oxygen sample' },
      spirometer: { title: 'Spirometer concept', kicker: 'Airflow and volume concept', description: 'A spirometer measures breathed air volume and flow during a maneuver. Here the values are adjustable teaching inputs, not a performed test.', limitation: 'A clinical spirometry test depends on calibrated equipment, technique, and interpretation. This lab measures no airflow.', main: ['Breath volume input', 200, 1000, 500, 'mL'], secondary: ['Flow concept', 1, 12, 4, 'L/s'], primaryLabel: 'Modeled sample volume' },
      oxygen: { title: 'Oxygen delivery concept', kicker: 'Delivery interface overview', description: 'Explore how delivery interfaces differ as an engineering and physiology topic. The display does not choose a therapy or control a flow meter.', limitation: 'Oxygen settings require clinical context and equipment. This simulation cannot recommend oxygen use or deliver a gas.', main: ['Flow concept', 0, 15, 2, 'L/min'], secondary: ['Inspired oxygen index', 21, 100, 35, '/100'], primaryLabel: 'Flow concept' },
      ventilator: { title: 'Ventilator concepts', kicker: 'Virtual mechanical support model', description: 'Adjust a software-only rate and tidal-volume scenario to see its minute-ventilation estimate.', limitation: 'Real ventilators have safety systems, pressure/volume modes, alarms, and patient-specific settings absent from this educational model.', main: ['Respiratory rate', 6, 40, 16, '/min'], secondary: ['Tidal volume', 200, 1000, 500, 'mL'], primaryLabel: 'Minute ventilation estimate' }
    };
    let selected = 'oximeter', running = false, tick = 0, timer = null;
    function renderDevice() {
      const config = configs[selected];
      setText('device-title', config.title); setText('device-panel-title', config.title.replace(' concept', ''));
      setText('device-kicker', config.kicker); setText('device-description', config.description); setText('device-limitation', config.limitation);
      document.querySelectorAll('[data-device]').forEach((button) => button.classList.toggle('active', button.dataset.device === selected));
      const controls = byId('device-controls'); controls.replaceChildren();
      [[config.main, 'device-main'], [config.secondary, 'device-secondary']].forEach(([field, id]) => {
        const wrapper = document.createElement('div'); wrapper.className = 'lab-field device-panel-control';
        const label = document.createElement('label'); label.htmlFor = id; label.textContent = field[0];
        const input = document.createElement('input'); input.className = 'lab-input'; input.id = id; input.type = 'range'; input.min = field[1]; input.max = field[2]; input.value = field[3]; input.setAttribute('aria-label', field[0]);
        wrapper.append(label, input); controls.append(wrapper); input.addEventListener('input', updateOutput);
      });
      const wave = byId('device-wave');
      if (wave) wave.setAttribute('d', selected === 'spirometer' ? 'M212 138h16l12-45 20 93 21-67 19 38 22-19h36' : selected === 'oxygen' ? 'M211 136h40m0 0c20-40 38-40 58 0s38 40 58 0' : selected === 'ventilator' ? 'M212 138h28v-35h28v35h28v-35h28v35h28' : 'M212 138h20l14-32 19 64 20-42 17 18h50');
      updateOutput();
    }
    function updateOutput() {
      const config = configs[selected], main = Number(byId('device-main').value), secondary = Number(byId('device-secondary').value);
      const output = selected === 'ventilator' ? `${(main * secondary / 1000).toFixed(1)} L/min` : selected === 'oximeter' ? `${main}% · ${secondary} BPM` : selected === 'spirometer' ? `${main} mL · ${secondary} L/s` : `${main} L/min · O₂ index ${secondary}/100`;
      setText('device-output-label', selected === 'oximeter' ? 'Simulated outputs' : config.primaryLabel); setText('device-output', output);
      if (selected === 'ventilator') setText('device-event', `Modeled minute ventilation = ${main} breaths/min × ${secondary} mL ÷ 1000 = ${output}.`);
      else setText('device-event', `${config.title}: sample inputs updated. No physical device or sensor is connected.`);
    }
    document.querySelectorAll('[data-device]').forEach((button) => button.addEventListener('click', () => { selected = button.dataset.device; renderDevice(); }));
    byId('device-start').addEventListener('click', () => { running = true; tick += 1; byId('device-status').textContent = 'Running · simulated'; setText('device-event', `${configs[selected].title}: virtual sample cycle ${tick} started.`); if (!timer) timer = window.setInterval(() => { tick += 1; setText('device-event', `${configs[selected].title}: virtual sample cycle ${tick}; all values remain simulated.`); }, 2200); });
    byId('device-pause').addEventListener('click', () => { running = false; window.clearInterval(timer); timer = null; byId('device-status').textContent = 'Paused · simulation'; setText('device-event', 'Virtual sample updates paused. No physical sensor was used.'); });
    byId('device-reset').addEventListener('click', () => { running = false; window.clearInterval(timer); timer = null; tick = 0; byId('device-status').textContent = 'Idle · simulation'; renderDevice(); setText('device-event', 'Device model reset to its educational starting values.'); });
    renderDevice();
  }

  function setupIntegration() {
    const rate = byId('integration-rate'), volume = byId('integration-volume'), log = byId('integration-log');
    let timer = null;
    function logEvent(message) {
      const empty = log.querySelector('.empty-state'); if (empty) empty.remove();
      const row = document.createElement('div'); row.className = 'log-entry';
      const time = document.createElement('time'); time.textContent = new Date().toLocaleTimeString();
      const text = document.createElement('span'); text.textContent = message; row.append(time, text); log.prepend(row);
      while (log.children.length > 8) log.lastElementChild.remove();
    }
    function update(source = 'Model inputs changed') {
      const r = Number(rate.value), v = Number(volume.value), estimate = r * v / 1000;
      rate.nextElementSibling.value = r; volume.nextElementSibling.value = v;
      setText('integration-formula', `${r} breaths/min × ${(v / 1000).toFixed(2)} L/breath`);
      setText('integration-ventilation', `${estimate.toFixed(2)} L/min`);
      const warn = estimate > 20 || r > 30 || v < 250;
      byId('integration-warning').hidden = !warn;
      setText('integration-status', warn ? 'Review model range' : timer ? 'Running software model' : 'Ready');
      setText('integration-interpretation', warn ? 'Example model range warning' : 'Teaching calculation updated');
      setText('integration-description', warn ? `The inputs estimate ${estimate.toFixed(2)} L/min, which is outside this lab’s preferred teaching range. The warning is not a clinical alarm.` : `Minute ventilation estimate: ${r} × (${v} ÷ 1000) = ${estimate.toFixed(2)} L/min. This simplified software model does not represent a person.`);
      if (source) logEvent(`${source}: rate ${r}/min, tidal volume ${v} mL, estimate ${estimate.toFixed(2)} L/min${warn ? ' · teaching-range warning' : ''}.`);
    }
    rate.addEventListener('input', () => update('Rate adjusted')); volume.addEventListener('input', () => update('Tidal volume adjusted'));
    byId('integration-play').addEventListener('click', () => { if (timer) return; setText('integration-status', 'Running software model'); logEvent('Software-only integration model started.'); timer = window.setInterval(() => logEvent(`Model cycle: ${(Number(rate.value) * Number(volume.value) / 1000).toFixed(2)} L/min estimate.`), 2500); });
    byId('integration-pause').addEventListener('click', () => { window.clearInterval(timer); timer = null; setText('integration-status', 'Paused'); logEvent('Integration model paused.'); });
    byId('integration-reset').addEventListener('click', () => { window.clearInterval(timer); timer = null; rate.value = 16; volume.value = 500; update('Model reset to baseline'); setText('integration-status', 'Ready'); });
    byId('integration-clear').addEventListener('click', () => { log.replaceChildren(); const empty = document.createElement('div'); empty.className = 'empty-state'; empty.textContent = 'Event log cleared. New model actions will appear here.'; log.append(empty); });
    update('Integration model initialized');
  }

  function setupExperiments() {
    const selected = new Map(); let runs = [];
    const fields = ['rr', 'volume', 'compliance'];
    function preview() {
      fields.forEach((id) => { byId(`${id}-out`).value = byId(id).value; });
      setText('twin-preview', `${(Number(byId('rr').value) * Number(byId('volume').value) / 1000).toFixed(2)} L/min`);
    }
    fields.forEach((id) => byId(id).addEventListener('input', preview));
    document.querySelectorAll('[data-experiment-preset]').forEach((button) => button.addEventListener('click', () => {
      const presets = { baseline: [16, 500, 60, 'Baseline scenario'], slow: [10, 650, 70, 'Slower pattern'], shallow: [24, 300, 45, 'Shallower pattern'] };
      const values = presets[button.dataset.experimentPreset];
      [byId('rr').value, byId('volume').value, byId('compliance').value, byId('exp-name').value] = values;
      preview(); setText('experiment-message', `${values[3]} preset loaded. Review the inputs, then run and save it.`);
    }));
    function renderCompare() {
      const pair = Array.from(selected.values()).slice(-2);
      setText('compare-status', `${pair.length} selected`); byId('export-experiment').disabled = pair.length !== 2;
      if (pair.length < 2) { setText('compare-summary', pair.length ? `Selected “${pair[0].name}”. Choose another saved scenario to compare.` : 'Save at least two scenarios, then select their compare buttons in the history.'); byId('compare-bar-a').setAttribute('width', '0'); byId('compare-bar-b').setAttribute('width', '0'); return; }
      const [a, b] = pair, av = Number(a.simulatedVentilation), bv = Number(b.simulatedVentilation), max = Math.max(av, bv, 1);
      setText('compare-label-a', a.name.slice(0, 24)); setText('compare-label-b', b.name.slice(0, 24));
      setText('compare-value-a', `${av.toFixed(2)} L/min`); setText('compare-value-b', `${bv.toFixed(2)} L/min`);
      byId('compare-bar-a').setAttribute('width', String(390 * av / max)); byId('compare-bar-b').setAttribute('width', String(390 * bv / max));
      const diff = bv - av;
      setText('compare-summary', `${a.name}: ${a.respiratoryRate}/min × ${a.tidalVolumeMl} mL = ${av.toFixed(2)} L/min. ${b.name}: ${b.respiratoryRate}/min × ${b.tidalVolumeMl} mL = ${bv.toFixed(2)} L/min. Difference: ${diff >= 0 ? '+' : ''}${diff.toFixed(2)} L/min.`);
    }
    function renderRuns() {
      const body = byId('experiment-history'), empty = byId('experiment-empty'); body.replaceChildren();
      setText('experiment-count', runs.length);
      if (!runs.length) { empty.hidden = false; body.parentElement.hidden = true; return; }
      empty.hidden = true; body.parentElement.hidden = false;
      runs.slice().sort((a, b) => b.id - a.id).forEach((run) => {
        const row = document.createElement('tr');
        const selectCell = document.createElement('td'), button = document.createElement('button'); button.type = 'button'; button.className = 'lab-button'; button.textContent = selected.has(run.id) ? 'Selected' : 'Compare'; button.setAttribute('aria-pressed', String(selected.has(run.id)));
        button.addEventListener('click', () => { if (selected.has(run.id)) selected.delete(run.id); else { selected.set(run.id, run); if (selected.size > 2) selected.delete(selected.keys().next().value); } renderRuns(); renderCompare(); }); selectCell.append(button); row.append(selectCell);
        [run.name, `${run.respiratoryRate}/min`, `${run.tidalVolumeMl} mL`, `${run.compliance}/100`, `${Number(run.simulatedVentilation).toFixed(2)} L/min`, new Date(run.createdAt).toLocaleString()].forEach((value) => { const cell = document.createElement('td'); cell.textContent = value; row.append(cell); });
        body.append(row);
      });
    }
    async function loadRuns() {
      const response = await fetch('/api/experiments'); if (!response.ok) throw new Error('Could not load saved scenarios. Refresh the page or sign in again.');
      runs = await response.json(); renderRuns();
      if (runs.length) setText('latest-run-output', `${runs.slice().sort((a, b) => b.id - a.id)[0].name} · ${(Number(runs.slice().sort((a, b) => b.id - a.id)[0].simulatedVentilation)).toFixed(2)} L/min`);
    }
    byId('run-experiment').addEventListener('click', async () => {
      const name = byId('exp-name').value.trim();
      if (!name) { setText('experiment-message', 'Enter a scenario name before saving.'); byId('exp-name').focus(); return; }
      const payload = { name, respiratoryRate: Number(byId('rr').value), tidalVolumeMl: Number(byId('volume').value), compliance: Number(byId('compliance').value) };
      setText('experiment-message', 'Running and saving the educational scenario…');
      try {
        const response = await fetch('/api/experiments', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        if (!response.ok) throw new Error('The scenario was not saved. Check the values and try again.');
        const run = await response.json(); selected.clear(); await loadRuns();
        setText('latest-run-output', `${run.name} · ${Number(run.simulatedVentilation).toFixed(2)} L/min`);
        setText('experiment-message', `Saved ${run.name}. The calculation is ${run.respiratoryRate} × (${run.tidalVolumeMl} ÷ 1000) = ${Number(run.simulatedVentilation).toFixed(2)} L/min.`); renderCompare();
      } catch (error) { setText('experiment-message', error.message); }
    });
    byId('reset-experiment').addEventListener('click', () => { byId('rr').value = 16; byId('volume').value = 500; byId('compliance').value = 60; byId('exp-name').value = 'Baseline scenario'; preview(); setText('experiment-message', 'Inputs reset to the baseline teaching scenario.'); });
    byId('export-experiment').addEventListener('click', () => {
      const pair = Array.from(selected.values()); if (pair.length !== 2) return;
      const rows = [['name', 'respiratoryRatePerMinute', 'tidalVolumeMl', 'complianceIndex', 'modeledVentilationLPerMinute', 'savedAt'], ...pair.map((run) => [run.name, run.respiratoryRate, run.tidalVolumeMl, run.compliance, Number(run.simulatedVentilation).toFixed(2), run.createdAt])];
      downloadText('aero-ai-experiment-comparison.csv', rows.map((row) => row.map(escapeCsv).join(',')).join('\r\n'));
    });
    document.addEventListener('aero-experiments-updated', () => loadRuns().catch(() => setText('experiment-message', 'Comparison saved, but history could not be refreshed.')));
    preview(); renderCompare(); loadRuns().catch((error) => { byId('experiment-history').innerHTML = `<tr><td colspan="7">${error.message}</td></tr>`; setText('experiment-message', error.message); });
  }

  const initializers = { anatomy: setupAnatomy, 'gas-exchange': setupGasExchange, circulation: setupCirculation, analytics: setupAnalytics, device: setupDevice, integration: setupIntegration, experiments: setupExperiments };
  if (initializers[moduleId]) initializers[moduleId]();
})();
