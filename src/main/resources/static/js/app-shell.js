(() => {
  const themeButton = document.getElementById('theme-toggle');
  let storedTheme = 'dark';
  try {
    const savedTheme = localStorage.getItem('aero-theme');
    if (savedTheme === 'light' || savedTheme === 'dark') storedTheme = savedTheme;
  } catch (_) { /* Storage can be unavailable in restricted browser contexts. */ }
  document.documentElement.dataset.theme = storedTheme;
  if (themeButton) {
    themeButton.textContent = storedTheme === 'dark' ? 'Light mode' : 'Dark mode';
    themeButton.addEventListener('click', () => {
      const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
      document.documentElement.dataset.theme = next;
      try { localStorage.setItem('aero-theme', next); } catch (_) { /* Theme still applies for this page. */ }
      themeButton.textContent = next === 'dark' ? 'Light mode' : 'Dark mode';
    });
  }
  const shell = document.querySelector('.app-layout');
  const toggle = document.querySelector('.app-menu-toggle');
  const scrim = document.querySelector('.app-sidebar-scrim');
  if (!shell || !toggle || !scrim) return;
  const close = () => { shell.classList.remove('sidebar-open'); toggle.setAttribute('aria-expanded', 'false'); };
  toggle.addEventListener('click', () => {
    const open = shell.classList.toggle('sidebar-open');
    toggle.setAttribute('aria-expanded', String(open));
  });
  scrim.addEventListener('click', close);
  shell.querySelectorAll('.app-nav-link').forEach(link => link.addEventListener('click', close));
  document.addEventListener('keydown', event => { if (event.key === 'Escape') close(); });
})();
