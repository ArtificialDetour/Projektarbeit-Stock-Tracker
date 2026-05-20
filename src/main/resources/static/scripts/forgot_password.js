document.addEventListener('DOMContentLoaded', function() {
  // Wire auth form toggle.
  var btnShowForgot = document.getElementById('btn-show-forgot');
  var btnShowLogin = document.getElementById('btn-show-login');
  var loginForm = document.getElementById('login-form');
  var forgotForm = document.getElementById('forgot-password-form');
  var titleEl = document.querySelector('h1');
  var subtitleEl = document.querySelector('p');

  if (btnShowForgot) {
    btnShowForgot.addEventListener('click', function(e) {
      e.preventDefault();
      if (loginForm) loginForm.classList.add('hidden');
      if (forgotForm) forgotForm.classList.remove('hidden');
      if (titleEl) titleEl.textContent = 'Reset Password';
      if (subtitleEl) subtitleEl.textContent = 'Enter your email, security code, and new password';
    });
  }

  if (btnShowLogin) {
    btnShowLogin.addEventListener('click', function(e) {
      e.preventDefault();
      if (forgotForm) forgotForm.classList.add('hidden');
      if (loginForm) loginForm.classList.remove('hidden');
      if (titleEl) titleEl.textContent = 'Welcome Back';
      if (subtitleEl) subtitleEl.textContent = 'Enter your credentials to access your dashboard';
    });
  }
});
