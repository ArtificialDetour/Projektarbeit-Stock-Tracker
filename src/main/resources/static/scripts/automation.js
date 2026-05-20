(function () {
    // Wire standalone toggles.
    const switches = document.querySelectorAll('[data-toggle-switch]');
    switches.forEach((toggle) => {
        toggle.addEventListener('click', () => {
            const knob = toggle.querySelector('[data-toggle-knob]');
            if (!knob) return;
            const isActive = toggle.getAttribute('aria-pressed') === 'true';
            const nextState = !isActive;
            const isCompact = toggle.classList.contains('w-10');
            toggle.setAttribute('aria-pressed', String(nextState));
            toggle.classList.toggle('bg-tertiary-fixed', nextState);
            toggle.classList.toggle('bg-outline-variant', !nextState);
            knob.classList.toggle(isCompact ? 'translate-x-5' : 'translate-x-6', nextState);
        });
    });
})();
