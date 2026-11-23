// Dashboard Logic, Pomodoro Timer, Analytics & PDF Export

let allTasks = [];
let currentUser = null;

// ========================================================
// 1. Initialization & Theme Handling
// ========================================================
document.addEventListener('DOMContentLoaded', () => {
    currentUser = getCurrentUser();
    if (!currentUser) return;

    initTheme();
    initUserInterface();
    startLiveClock();
    loadDashboardData();
    initPomodoro();
    loadDailyQuote();
});

function initTheme() {
    const savedTheme = localStorage.getItem('schedule_optimizer_theme') || 'light';
    applyTheme(savedTheme);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    applyTheme(newTheme);
    localStorage.setItem('schedule_optimizer_theme', newTheme);
    renderAnalyticsCharts(allTasks); // Re-draw charts with theme colors
}

function applyTheme(theme) {
    if (theme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        const btn = document.getElementById('theme-btn');
        if (btn) btn.textContent = '☀️';
    } else {
        document.documentElement.removeAttribute('data-theme');
        const btn = document.getElementById('theme-btn');
        if (btn) btn.textContent = '🌙';
    }
}

function initUserInterface() {
    const nameEl = document.getElementById('user-name');
    const avatarEl = document.getElementById('user-avatar');

    if (nameEl && currentUser) {
        nameEl.textContent = currentUser.fullName || currentUser.username;
    }
    if (avatarEl && currentUser) {
        avatarEl.textContent = (currentUser.fullName || currentUser.username).charAt(0).toUpperCase();
    }
}

function startLiveClock() {
    const clockEl = document.getElementById('live-clock');
    if (!clockEl) return;

    function update() {
        const now = new Date();
        clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }
    update();
    setInterval(update, 1000);
}

// ========================================================
// 2. Load Tasks, Summary & Analytics
// ========================================================
async function loadDashboardData() {
    await Promise.all([fetchTasks(), fetchSummary()]);
}

async function fetchTasks() {
    if (!currentUser) return;
    try {
        const res = await fetch(`/api/tasks`, {
            headers: { 'X-User-Id': currentUser.id }
        });
        const json = await res.json();
        if (json.success && Array.isArray(json.data)) {
            allTasks = json.data;
            renderTasks(allTasks);
            renderTimelineFromTasks(allTasks);
            renderAnalyticsCharts(allTasks);
        }
    } catch (err) {
        console.error('Failed to load tasks:', err);
        showToast('Error loading tasks from server');
    }
}

async function fetchSummary() {
    if (!currentUser) return;
    try {
        const res = await fetch(`/api/schedule/summary`, {
            headers: { 'X-User-Id': currentUser.id }
        });
        const json = await res.json();
        if (json.success && json.data) {
            updateSummaryCards(json.data);
        }
    } catch (err) {
        console.error('Failed to load summary:', err);
    }
}

function updateSummaryCards(summary) {
    document.getElementById('stat-total').textContent = summary.totalTasks || 0;
    document.getElementById('stat-completed').textContent = summary.completedTasks || 0;
    document.getElementById('stat-pending').textContent = summary.pendingTasks || 0;

    const rate = Math.round(summary.completionRate || 0);
    const progressBar = document.getElementById('stat-progress-bar');
    if (progressBar) progressBar.style.width = rate + '%';

    const hoursPending = ((summary.pendingEstimatedMinutes || 0) / 60).toFixed(1);
    document.getElementById('stat-pending-time').textContent = `${hoursPending} hrs pending`;

    const totalHours = ((summary.totalEstimatedMinutes || 0) / 60).toFixed(1);
    document.getElementById('stat-total-time').textContent = `${totalHours} hrs`;
    document.getElementById('stat-priority-mix').textContent = 
        `High: ${summary.highPriorityCount || 0} | Med: ${summary.mediumPriorityCount || 0} | Low: ${summary.lowPriorityCount || 0}`;

    const badge = document.getElementById('task-count-badge');
    if (badge) badge.textContent = `${summary.totalTasks || 0} Tasks`;
}

// ========================================================
// 3. Render Tasks Table
// ========================================================
function renderTasks(tasks) {
    const tbody = document.getElementById('tasks-table-body');
    if (!tbody) return;

    if (!tasks || tasks.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty-state">No tasks found. Click "Add New Task" to get started!</td></tr>`;
        return;
    }

    tbody.innerHTML = tasks.map(task => {
        const isCompleted = task.status === 'COMPLETED';
        const priorityBadgeClass = `badge-${(task.priority || 'medium').toLowerCase()}`;
        const cat = (task.category || 'general').toLowerCase();
        const catBadgeClass = `badge-${cat}`;
        const slotText = task.scheduledStart && task.scheduledEnd ? 
            `${formatShortTime(task.scheduledStart)} - ${formatShortTime(task.scheduledEnd)}` : 
            `<span style="color: #94a3b8; font-style: italic;">Not scheduled</span>`;

        return `
            <tr class="${isCompleted ? 'task-completed' : ''}">
                <td style="text-align: center;">
                    <input type="checkbox" class="task-checkbox" ${isCompleted ? 'checked' : ''} 
                           onchange="toggleTaskStatus(${task.id})">
                </td>
                <td class="task-title-cell">
                    <strong>${escapeHtml(task.title)}</strong>
                    <span>${escapeHtml(task.description || 'No description provided')}</span>
                </td>
                <td>
                    <span class="badge ${catBadgeClass}">${escapeHtml(task.category || 'GENERAL')}</span>
                </td>
                <td>
                    <span class="badge ${priorityBadgeClass}">${task.priority}</span>
                </td>
                <td>
                    ${formatDeadline(task.deadline)}
                </td>
                <td>
                    ${task.estimatedMinutes} mins
                </td>
                <td>
                    ${slotText}
                </td>
                <td style="text-align: right;">
                    <button class="btn btn-sm btn-outline" onclick="startFocusOnTask(${task.id})" title="Start Focus Session">🎯 Focus</button>
                    <button class="btn btn-sm btn-outline" onclick="openEditTaskModal(${task.id})">Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteTask(${task.id})">Delete</button>
                </td>
            </tr>
        `;
    }).join('');
}

// ========================================================
// 4. Intelligent Schedule Optimizer Trigger
// ========================================================
async function runOptimization() {
    if (!currentUser) return;
    showToast('Running intelligent schedule optimization...');

    try {
        const res = await fetch(`/api/schedule/optimize`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-User-Id': currentUser.id 
            }
        });
        const json = await res.json();

        if (json.success && Array.isArray(json.data)) {
            showToast('Schedule successfully optimized!');
            renderTimelineFromTasks(json.data);
            await loadDashboardData();
        } else {
            showToast(json.message || 'Optimization failed');
        }
    } catch (err) {
        console.error(err);
        showToast('Error executing schedule optimizer');
    }
}

// ========================================================
// 5. Render Timeline
// ========================================================
function renderTimelineFromTasks(tasks) {
    const container = document.getElementById('timeline-container');
    if (!container) return;

    const scheduled = tasks.filter(t => t.scheduledStart && t.status === 'PENDING')
                           .sort((a, b) => new Date(a.scheduledStart) - new Date(b.scheduledStart));

    if (scheduled.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                No active schedule generated yet. Click <strong>"Run Schedule Optimizer"</strong> above to auto-arrange today's tasks!
            </div>
        `;
        return;
    }

    container.innerHTML = scheduled.map((t) => {
        const startTime = formatShortTime(t.scheduledStart);
        const endTime = formatShortTime(t.scheduledEnd);
        const hasRisk = t.deadlineRisk;
        const priorityBadge = `<span class="badge badge-${t.priority.toLowerCase()}">${t.priority}</span>`;
        const catBadge = `<span class="badge badge-${(t.category || 'general').toLowerCase()}">${t.category || 'GENERAL'}</span>`;
        const riskBadge = hasRisk ? `<span class="badge badge-risk">⚠️ Deadline Risk</span>` : `<span class="badge badge-ok">✓ On Track</span>`;

        return `
            <div class="timeline-item ${hasRisk ? 'risk' : ''}">
                <div class="time-slot">
                    🕒 ${startTime} - ${endTime}
                </div>
                <div class="timeline-content">
                    <div class="timeline-title">${escapeHtml(t.title)}</div>
                    <div class="timeline-meta">
                        ${catBadge} &nbsp;|&nbsp; Duration: <strong>${t.estimatedMinutes} mins</strong> | Deadline: ${formatShortDate(t.deadline)}
                    </div>
                </div>
                <div class="timeline-actions">
                    <button class="btn btn-sm btn-outline" onclick="startFocusOnTask(${t.id})">🎯 Focus</button>
                    ${priorityBadge}
                    ${riskBadge}
                </div>
            </div>
        `;
    }).join('');
}

// ========================================================
// 6. Visual Productivity Analytics Charts (Pure Canvas)
// ========================================================
function renderAnalyticsCharts(tasks) {
    drawPriorityChart(tasks);
    drawCategoryChart(tasks);
}

function drawPriorityChart(tasks) {
    const canvas = document.getElementById('priorityChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    let high = 0, med = 0, low = 0;
    tasks.forEach(t => {
        const p = (t.priority || '').toUpperCase();
        if (p === 'HIGH') high++;
        else if (p === 'LOW') low++;
        else med++;
    });

    const total = tasks.length || 1;
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const textColor = isDark ? '#f1f5f9' : '#1e293b';

    const data = [
        { label: 'High Priority', count: high, color: '#ef4444' },
        { label: 'Medium Priority', count: med, color: '#f59e0b' },
        { label: 'Low Priority', count: low, color: '#10b981' }
    ];

    const barWidth = 42;
    const startX = 60;
    const maxBarHeight = 110;
    const maxVal = Math.max(high, med, low, 5);

    data.forEach((item, index) => {
        const x = startX + index * 95;
        const barH = (item.count / maxVal) * maxBarHeight;
        const y = height - 45 - barH;

        // Draw Bar
        ctx.fillStyle = item.color;
        ctx.beginPath();
        ctx.roundRect(x, y, barWidth, barH, 4);
        ctx.fill();

        // Count above bar
        ctx.fillStyle = textColor;
        ctx.font = 'bold 12px Inter, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(item.count, x + barWidth / 2, y - 6);

        // Label below bar
        ctx.font = '11px Inter, sans-serif';
        ctx.fillStyle = isDark ? '#94a3b8' : '#64748b';
        ctx.fillText(item.label.split(' ')[0], x + barWidth / 2, height - 20);
    });
}

function drawCategoryChart(tasks) {
    const canvas = document.getElementById('categoryChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    const counts = {};
    tasks.forEach(t => {
        const c = (t.category || 'GENERAL').toUpperCase();
        counts[c] = (counts[c] || 0) + 1;
    });

    const categories = Object.keys(counts);
    if (categories.length === 0) categories.push('GENERAL');

    const colors = ['#4f46e5', '#7c3aed', '#c2410c', '#10b981', '#0284c7', '#64748b'];
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const textColor = isDark ? '#f1f5f9' : '#1e293b';

    const maxCount = Math.max(...Object.values(counts), 4);
    const startY = 30;
    const rowHeight = 24;

    categories.slice(0, 5).forEach((cat, idx) => {
        const y = startY + idx * rowHeight;
        const count = counts[cat] || 0;
        const barWidth = Math.max(10, (count / maxCount) * 160);

        // Label
        ctx.fillStyle = isDark ? '#94a3b8' : '#64748b';
        ctx.font = '11px Inter, sans-serif';
        ctx.textAlign = 'right';
        ctx.fillText(cat, 80, y + 12);

        // Horizontal bar
        ctx.fillStyle = colors[idx % colors.length];
        ctx.beginPath();
        ctx.roundRect(90, y + 2, barWidth, 12, 3);
        ctx.fill();

        // Value
        ctx.fillStyle = textColor;
        ctx.font = 'bold 11px Inter, sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(count, 95 + barWidth + 6, y + 12);
    });
}

// ========================================================
// 7. Integrated Pomodoro Focus Timer
// ========================================================
let pomodoroSeconds = 25 * 60;
let pomodoroTimer = null;
let pomodoroRunning = false;
let currentPomodoroMode = 'work'; // work, short, long

function initPomodoro() {
    updatePomodoroDisplay();
}

function updatePomodoroDisplay() {
    const timeEl = document.getElementById('pomodoro-time');
    if (!timeEl) return;
    const mins = Math.floor(pomodoroSeconds / 60);
    const secs = pomodoroSeconds % 60;
    timeEl.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function togglePomodoro() {
    if (pomodoroRunning) {
        pausePomodoro();
    } else {
        startPomodoro();
    }
}

function startPomodoro() {
    pomodoroRunning = true;
    document.getElementById('pomodoro-toggle-btn').textContent = 'Pause';
    document.getElementById('pomodoro-time').classList.add('running');

    pomodoroTimer = setInterval(() => {
        if (pomodoroSeconds > 0) {
            pomodoroSeconds--;
            updatePomodoroDisplay();
        } else {
            clearInterval(pomodoroTimer);
            pomodoroRunning = false;
            document.getElementById('pomodoro-toggle-btn').textContent = 'Start Focus';
            document.getElementById('pomodoro-time').classList.remove('running');
            playPomodoroChime();
            showToast('🎉 Focus Session Completed! Take a well-deserved break.');
        }
    }, 1000);
}

function pausePomodoro() {
    pomodoroRunning = false;
    clearInterval(pomodoroTimer);
    document.getElementById('pomodoro-toggle-btn').textContent = 'Resume';
    document.getElementById('pomodoro-time').classList.remove('running');
}

function resetPomodoro() {
    pausePomodoro();
    setPomodoroMode(currentPomodoroMode);
    document.getElementById('pomodoro-toggle-btn').textContent = 'Start Focus';
}

function setPomodoroMode(mode) {
    currentPomodoroMode = mode;
    document.querySelectorAll('.pomodoro-mode-btn').forEach(b => b.classList.remove('active'));

    if (mode === 'work') {
        pomodoroSeconds = 25 * 60;
        document.getElementById('mode-work').classList.add('active');
    } else if (mode === 'short') {
        pomodoroSeconds = 5 * 60;
        document.getElementById('mode-short').classList.add('active');
    } else {
        pomodoroSeconds = 15 * 60;
        document.getElementById('mode-long').classList.add('active');
    }
    updatePomodoroDisplay();
}

function startFocusOnTask(taskId) {
    const task = allTasks.find(t => t.id === taskId);
    if (!task) return;

    document.getElementById('pomodoro-task-name').textContent = `🎯 ${task.title}`;
    resetPomodoro();
    startPomodoro();
    showToast(`Focus session started for: ${task.title}`);
}

function togglePomodoroWidget() {
    const body = document.getElementById('pomodoro-body');
    body.style.display = body.style.display === 'none' ? 'block' : 'none';
}

function playPomodoroChime() {
    try {
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5
        osc.frequency.setValueAtTime(880.00, audioCtx.currentTime + 0.2); // A5
        gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.8);
        osc.start();
        osc.stop(audioCtx.currentTime + 0.8);
    } catch (ignored) {}
}

// ========================================================
// 8. 🖨️ Export Schedule as PDF / Print
// ========================================================
function exportSchedulePDF() {
    const dateEl = document.getElementById('print-date');
    if (dateEl) {
        dateEl.textContent = 'Date: ' + new Date().toLocaleDateString([], { 
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' 
        });
    }
    window.print();
}

// ========================================================
// 9. Task CRUD Modals & Filters
// ========================================================
function openAddTaskModal() {
    document.getElementById('modal-title').textContent = 'Add New Task';
    document.getElementById('task-id').value = '';
    document.getElementById('task-title').value = '';
    document.getElementById('task-desc').value = '';
    document.getElementById('task-priority').value = 'MEDIUM';
    document.getElementById('task-category').value = 'STUDY';
    document.getElementById('task-est').value = 45;

    const defaultDate = new Date(Date.now() + 4 * 3600 * 1000);
    document.getElementById('task-deadline').value = formatDateTimeLocal(defaultDate);

    document.getElementById('task-modal').style.display = 'flex';
}

function openEditTaskModal(id) {
    const task = allTasks.find(t => t.id === id);
    if (!task) return;

    document.getElementById('modal-title').textContent = 'Edit Task';
    document.getElementById('task-id').value = task.id;
    document.getElementById('task-title').value = task.title;
    document.getElementById('task-desc').value = task.description || '';
    document.getElementById('task-priority').value = task.priority;
    document.getElementById('task-category').value = task.category || 'GENERAL';
    document.getElementById('task-est').value = task.estimatedMinutes || 30;

    if (task.deadline) {
        const d = new Date(task.deadline.replace(' ', 'T'));
        document.getElementById('task-deadline').value = formatDateTimeLocal(d);
    }

    document.getElementById('task-modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('task-modal').style.display = 'none';
}

async function handleSaveTask(event) {
    event.preventDefault();
    if (!currentUser) return;

    const id = document.getElementById('task-id').value;
    const title = document.getElementById('task-title').value.trim();
    const description = document.getElementById('task-desc').value.trim();
    const priority = document.getElementById('task-priority').value;
    const category = document.getElementById('task-category').value;
    const estimatedMinutes = parseInt(document.getElementById('task-est').value) || 30;
    const deadlineInput = document.getElementById('task-deadline').value;

    const deadline = deadlineInput.replace('T', ' ') + ':00';

    const payload = {
        title,
        description,
        priority,
        category,
        estimatedMinutes,
        deadline
    };

    const isEdit = id && id !== '';
    const url = isEdit ? `/api/tasks/${id}` : `/api/tasks`;
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': currentUser.id
            },
            body: JSON.stringify(payload)
        });

        const json = await res.json();
        if (json.success) {
            closeModal();
            showToast(isEdit ? 'Task updated!' : 'Task created successfully!');
            await loadDashboardData();
        } else {
            alert(json.message || 'Operation failed');
        }
    } catch (err) {
        console.error(err);
        showToast('Error saving task');
    }
}

async function toggleTaskStatus(id) {
    if (!currentUser) return;
    try {
        const res = await fetch(`/api/tasks/${id}/toggle`, {
            method: 'POST',
            headers: { 'X-User-Id': currentUser.id }
        });
        const json = await res.json();
        if (json.success) {
            showToast('Task status updated');
            await loadDashboardData();
        }
    } catch (err) {
        console.error(err);
        showToast('Failed to toggle status');
    }
}

async function deleteTask(id) {
    if (!confirm('Are you sure you want to delete this task?')) return;
    if (!currentUser) return;

    try {
        const res = await fetch(`/api/tasks/${id}`, {
            method: 'DELETE',
            headers: { 'X-User-Id': currentUser.id }
        });
        const json = await res.json();
        if (json.success) {
            showToast('Task removed');
            await loadDashboardData();
        }
    } catch (err) {
        console.error(err);
        showToast('Error deleting task');
    }
}

// Filters
function filterTasks() {
    const priorityFilter = document.getElementById('filter-priority').value;
    const statusFilter = document.getElementById('filter-status').value;
    const categoryFilter = document.getElementById('filter-category').value;

    let filtered = allTasks;

    if (priorityFilter !== 'ALL') {
        filtered = filtered.filter(t => t.priority === priorityFilter);
    }
    if (statusFilter !== 'ALL') {
        filtered = filtered.filter(t => t.status === statusFilter);
    }
    if (categoryFilter !== 'ALL') {
        filtered = filtered.filter(t => (t.category || 'GENERAL').toUpperCase() === categoryFilter);
    }

    renderTasks(filtered);
}

function toggleTimelineView() {
    const section = document.getElementById('timeline-container');
    const btn = document.getElementById('btn-toggle-timeline');
    if (section.style.display === 'none') {
        section.style.display = 'flex';
        btn.textContent = 'Collapse';
    } else {
        section.style.display = 'none';
        btn.textContent = 'Expand';
    }
}

// ========================================================
// 10. 💡 Live Productivity Quote REST API Integration
// ========================================================
async function loadDailyQuote() {
    const textEl = document.getElementById('quote-text');
    const authorEl = document.getElementById('quote-author');
    if (!textEl || !authorEl) return;

    textEl.style.opacity = '0.4';

    try {
        // Attempt 1: Fetch from External Public REST API
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 2500);

        const res = await fetch('https://dummyjson.com/quotes/random', { signal: controller.signal });
        clearTimeout(timeoutId);

        if (res.ok) {
            const data = await res.json();
            textEl.textContent = `"${data.quote}"`;
            authorEl.textContent = `— ${data.author}`;
            textEl.style.opacity = '1';
            return;
        }
    } catch (err) {
        // Fallback to internal Java Backend REST API
    }

    try {
        // Attempt 2: Fetch from our own Java Backend API (/api/quote)
        const res = await fetch('/api/quote');
        const data = await res.json();
        if (data.success && data.data) {
            textEl.textContent = `"${data.data.quote}"`;
            authorEl.textContent = `— ${data.data.author}`;
            textEl.style.opacity = '1';
            return;
        }
    } catch (err) {
        // Fallback 3: Built-in default
        textEl.textContent = '"Focus on being productive instead of busy."';
        authorEl.textContent = '— Tim Ferriss';
    }

    textEl.style.opacity = '1';
}

// ========================================================
// 11. 🤖 NVIDIA NIM &bull; AI Schedule Advisor
// ========================================================
async function askNvidiaAI() {
    if (!currentUser) return;

    const modal = document.getElementById('ai-modal');
    const contentEl = document.getElementById('ai-advice-content');
    const badgeEl = document.getElementById('ai-model-badge');

    if (modal) modal.style.display = 'flex';
    if (contentEl) {
        contentEl.innerHTML = `
            <div class="ai-loading">
                <div class="ai-spinner"></div>
                <p>Analyzing your tasks, deadlines & priorities with NVIDIA Llama 3.1 AI...</p>
            </div>
        `;
    }

    try {
        const res = await fetch(`/api/ai/advisor`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': currentUser.id
            }
        });

        const json = await res.json();
        if (json.success && json.advice) {
            if (badgeEl && json.model) {
                badgeEl.textContent = `Powered by ${json.model} • NVIDIA Cloud AI`;
            }
            contentEl.innerHTML = formatMarkdownToHtml(json.advice);
        } else {
            contentEl.innerHTML = `<p style="color: #ef4444;">Could not generate AI advice. Please verify connection.</p>`;
        }
    } catch (err) {
        console.error('NVIDIA AI Error:', err);
        contentEl.innerHTML = `
            <div style="padding: 10px; color: #ef4444;">
                <p><strong>⚠️ Connection Issue:</strong> Could not connect to Java backend or NVIDIA API.</p>
                <p style="font-size: 12px; color: var(--text-secondary); margin-top: 6px;">
                    Make sure the Java server is running in your VS Code terminal (run <code>.\\run.ps1</code>) and your internet connection is active.
                </p>
            </div>
        `;
    }
}

function closeAIModal() {
    const modal = document.getElementById('ai-modal');
    if (modal) modal.style.display = 'none';
}

function formatMarkdownToHtml(text) {
    if (!text) return '';
    // Format bold **text**
    let formatted = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    
    // Split into lines
    const lines = formatted.split('\n');
    let html = '';
    let inList = false;

    lines.forEach(line => {
        const trimmed = line.trim();
        if (trimmed.startsWith('- ') || trimmed.startsWith('* ') || /^[0-9]+\.\s/.test(trimmed)) {
            if (!inList) {
                html += '<ul style="margin: 8px 0; padding-left: 20px;">';
                inList = true;
            }
            const cleanLine = trimmed.replace(/^[-*]\s+|^[0-9]+\.\s+/, '');
            html += `<li style="margin-bottom: 8px;">${cleanLine}</li>`;
        } else if (trimmed.length > 0) {
            if (inList) {
                html += '</ul>';
                inList = false;
            }
            html += `<p style="margin-bottom: 8px;">${trimmed}</p>`;
        }
    });

    if (inList) html += '</ul>';
    return html;
}

// Helpers
function formatDeadline(raw) {
    if (!raw) return '-';
    const date = new Date(raw.replace(' ', 'T'));
    const now = new Date();
    const diffHours = (date - now) / (1000 * 3600);
    const formattedDate = date.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

    if (diffHours < 0) {
        return `<span style="color: #ef4444; font-weight: 600;">⚠️ Overdue (${formattedDate})</span>`;
    } else if (diffHours < 3) {
        return `<span style="color: #f59e0b; font-weight: 600;">⏳ Due in ${Math.round(diffHours * 60)}m (${formattedDate})</span>`;
    }
    return formattedDate;
}

function formatShortTime(raw) {
    if (!raw) return '';
    const d = new Date(raw.replace(' ', 'T'));
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatShortDate(raw) {
    if (!raw) return '';
    const d = new Date(raw.replace(' ', 'T'));
    return d.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function formatDateTimeLocal(date) {
    const pad = n => n < 10 ? '0' + n : n;
    return date.getFullYear() + '-' +
        pad(date.getMonth() + 1) + '-' +
        pad(date.getDate()) + 'T' +
        pad(date.getHours()) + ':' +
        pad(date.getMinutes());
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.style.display = 'block';
    setTimeout(() => {
        toast.style.display = 'none';
    }, 2800);
}
