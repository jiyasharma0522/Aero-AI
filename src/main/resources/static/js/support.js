(() => {
  const $ = (id) => document.getElementById(id);
  const section = document.body.dataset.section;
  async function post(url, data) {
    const response = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.detail || result.message || 'Request failed.');
    return result;
  }

  const assessmentForm = $('assessment-form');
  if (assessmentForm) {
    const emergencyChecks = [...assessmentForm.querySelectorAll('.emergency-checks input')];
    const responseBox = $('emergency-response');
    const urgent = () => emergencyChecks.filter(input => input.checked);
    const severeBreathlessness = () => $('assessment-severity')?.value === 'severe';
    const showEmergency = () => {
      if (!urgent().length && !severeBreathlessness()) {
        responseBox.classList.remove('urgent-result');
        responseBox.textContent = 'Check any warning signs that apply, then continue if none apply.';
        if ($('save-emergency-assessment')) $('save-emergency-assessment').hidden = true;
        return;
      }
      responseBox.replaceChildren();
      const heading = document.createElement('strong'); heading.textContent = 'Possible emergency warning sign selected';
      const text = document.createElement('p'); text.textContent = 'Call India’s emergency number now. Do not wait for this questionnaire or a reading.';
      const call = document.createElement('a'); call.href = 'tel:112'; call.textContent = 'Call 112'; call.className = 'call';
      responseBox.append(heading, text, call);
      responseBox.classList.add('urgent-result');
      let saveButton = $('save-emergency-assessment');
      if (!saveButton) {
        saveButton = document.createElement('button'); saveButton.id = 'save-emergency-assessment'; saveButton.type = 'button'; saveButton.className = 'btn secondary'; saveButton.textContent = 'Save summary after seeking urgent help';
        $('continue-assessment').after(saveButton);
        saveButton.addEventListener('click', () => saveAssessment([...urgent().map(input => input.value), ...(severeBreathlessness() ? ['breathlessness'] : [])]));
      }
      saveButton.hidden = false;
    };
    emergencyChecks.forEach(input => input.addEventListener('change', showEmergency));
    $('assessment-severity')?.addEventListener('change', showEmergency);
    $('continue-assessment').addEventListener('click', () => {
      if (urgent().length || severeBreathlessness()) { showEmergency(); return; }
      $('followup-intake').hidden = false;
      $('followup-intake').scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    function assessmentPayload(symptoms) {
      return { symptoms, severity: $('assessment-severity')?.value || 'none', onset: $('assessment-onset')?.value || 'unknown',
        duration: $('assessment-duration')?.value || 'unknown', progression: $('assessment-progression')?.value || 'stable', history: $('assessment-history')?.value || 'none' };
    }
    async function saveAssessment(symptoms) {
      const out = $('assessment-result'); out.textContent = 'Saving your user-reported symptom summary…';
      try {
        const result = await post('/api/assessment', assessmentPayload(symptoms));
        out.replaceChildren();
        const heading = document.createElement('h3'); heading.textContent = result.urgency === 'URGENT' ? 'Emergency guidance' : result.urgency === 'PROMPT_REVIEW' ? 'Consider prompt professional advice' : 'No listed emergency sign selected';
        const guidance = document.createElement('p'); guidance.textContent = result.guidance;
        const summary = document.createElement('p'); summary.textContent = result.doctorSummary;
        out.append(heading, guidance, summary);
        out.classList.toggle('urgent-result', result.urgency === 'URGENT');
      } catch (error) { out.textContent = error.message; }
    }
    assessmentForm.addEventListener('submit', (event) => {
      event.preventDefault();
      if (!$('followup-intake').hidden && (urgent().length || severeBreathlessness())) { showEmergency(); return; }
      const symptoms = [...assessmentForm.querySelectorAll('input[name="symptom"]:checked')].map(input => input.value);
      if ($('followup-intake').hidden && (urgent().length || severeBreathlessness())) { showEmergency(); return; }
      if (severeBreathlessness()) {
        responseBox.replaceChildren();
        const warning = document.createElement('strong'); warning.textContent = 'Severe breathing difficulty needs immediate emergency care. Call 112 now.';
        const link = document.createElement('a'); link.href = 'tel:112'; link.className = 'call'; link.textContent = 'Call 112';
        responseBox.append(warning, link); responseBox.classList.add('urgent-result');
      }
      if (severeBreathlessness() && !symptoms.includes('breathlessness')) symptoms.push('breathlessness');
      if (!symptoms.length) { $('assessment-result').textContent = 'Select at least one symptom before creating a summary.'; return; }
      saveAssessment(symptoms);
    });
  }

  if ($('assistant-form')) $('assistant-form').addEventListener('submit', async (event) => {
    event.preventDefault(); const output = $('assistant-result'); output.textContent = 'Preparing educational response…';
    try { const result = await post('/api/assistant', { question: $('assistant-question').value }); output.replaceChildren(); const label = document.createElement('b'); label.textContent = result.label; const answer = document.createElement('p'); answer.textContent = result.answer; output.append(label, answer); }
    catch (error) { output.textContent = error.message; }
  });

  if ($('aqi-form')) $('aqi-form').addEventListener('submit', async (event) => {
    event.preventDefault(); const output = $('aqi-result'); output.textContent = 'Interpreting entered AQI…';
    try {
      const result = await post('/api/air-quality/advice', { aqi: Number($('aqi-value').value), scale: $('aqi-scale').value }); output.replaceChildren();
      [['Category', `${result.category} (${result.scale})`], ['Possible concern', result.possibleConcern], ['Practical step', result.practicalPrecaution], ['Source', result.sourceNote], ['Limitation', result.limitation]].forEach(([key, value]) => { const row = document.createElement('p'); const label = document.createElement('b'); label.textContent = `${key}: `; row.append(label, document.createTextNode(value)); output.append(row); });
    } catch (error) { output.textContent = error.message; }
  });

  if (section === 'notifications') loadNotifications();
  async function loadNotifications() {
    const root = $('notifications-list'), key = `aero-notifications-${document.body.dataset.user}`;
    try {
      const [readingResponse, assessmentResponse] = await Promise.all([fetch('/api/readings'), fetch('/api/assessment/history')]);
      if (!readingResponse.ok || !assessmentResponse.ok) throw new Error('Could not load private saved activity.');
      const [readings, assessments] = await Promise.all([readingResponse.json(), assessmentResponse.json()]);
      const items = [...readings.map(item => ({ id: `reading-${item.id}`, title: 'Saved respiratory reading', detail: `SpO₂ ${item.spo2}%, rate ${item.breathingRate}/min, pulse ${item.pulse} bpm · user-entered`, time: item.recordedAt })), ...assessments.map(item => ({ id: `assessment-${item.id}`, title: `Assessment result: ${item.urgency}`, detail: item.symptoms, time: item.createdAt }))].sort((a, b) => new Date(b.time) - new Date(a.time));
      const read = new Set(JSON.parse(localStorage.getItem(key) || '[]')); root.replaceChildren();
      if (!items.length) { const empty = document.createElement('p'); empty.className = 'muted'; empty.textContent = 'No saved-reading or assessment notifications yet.'; root.append(empty); return; }
      items.forEach(item => {
        const row = document.createElement('article'); row.className = `notification${read.has(item.id) ? ' is-read' : ''}`;
        const title = document.createElement('b'); title.textContent = item.title; const detail = document.createElement('p'); detail.textContent = item.detail;
        const time = document.createElement('time'); time.textContent = new Date(item.time).toLocaleString(); const button = document.createElement('button');
        button.type = 'button'; button.className = 'btn secondary'; button.textContent = read.has(item.id) ? 'Read' : 'Mark as read';
        button.addEventListener('click', () => { read.add(item.id); localStorage.setItem(key, JSON.stringify([...read])); row.classList.add('is-read'); button.textContent = 'Read'; });
        row.append(title, detail, time, button); root.append(row);
      });
      $('mark-all-read').onclick = () => { items.forEach(item => read.add(item.id)); localStorage.setItem(key, JSON.stringify([...read])); loadNotifications(); };
    } catch (error) { root.textContent = error.message; }
  }
})();
