// Authentication and Session Management

const API_BASE = '/api/auth';

function switchTab(tab) {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const tabLogin = document.getElementById('tab-login');
    const tabRegister = document.getElementById('tab-register');
    const alertBox = document.getElementById('auth-alert');

    if (alertBox) alertBox.style.display = 'none';

    if (tab === 'login') {
        loginForm.style.display = 'block';
        registerForm.style.display = 'none';
        tabLogin.classList.add('active');
        tabRegister.classList.remove('active');
    } else {
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
        tabLogin.classList.remove('active');
        tabRegister.classList.add('active');
    }
}

function showAlert(message, isError = true) {
    const alertBox = document.getElementById('auth-alert');
    if (!alertBox) return;
    alertBox.textContent = message;
    alertBox.className = 'alert-box ' + (isError ? 'alert-danger' : 'alert-success');
    alertBox.style.display = 'block';
}

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();
        if (response.ok && data.success) {
            localStorage.setItem('currentUser', JSON.stringify(data.data));
            showAlert('Login successful! Redirecting...', false);
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 600);
        } else {
            showAlert(data.message || 'Login failed. Please verify credentials.');
        }
    } catch (err) {
        console.error(err);
        showAlert('Could not connect to backend server. Make sure Java server is running.');
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const fullName = document.getElementById('reg-fullname').value.trim();
    const username = document.getElementById('reg-username').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;

    try {
        const response = await fetch(`${API_BASE}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, username, email, password })
        });

        const data = await response.json();
        if (response.ok && data.success) {
            showAlert('Registration successful! Please sign in with your credentials.', false);
            setTimeout(() => {
                switchTab('login');
                document.getElementById('login-username').value = username;
            }, 1200);
        } else {
            showAlert(data.message || 'Registration failed.');
        }
    } catch (err) {
        console.error(err);
        showAlert('Server connection error. Please make sure the Java backend is running.');
    }
}

function getCurrentUser() {
    const raw = localStorage.getItem('currentUser');
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

function checkAuth() {
    const user = getCurrentUser();
    const isLoginPage = window.location.pathname.endsWith('login.html');

    if (!user && !isLoginPage) {
        window.location.href = 'login.html';
    } else if (user && isLoginPage) {
        window.location.href = 'index.html';
    }
}

function logout() {
    localStorage.removeItem('currentUser');
    window.location.href = 'login.html';
}

// Auto-run auth verification on page load
document.addEventListener('DOMContentLoaded', checkAuth);
