const tg = window.Telegram.WebApp;
tg.expand();

// Authorization helpers
function getAuthHeaders() {
    return {
        'Authorization': `tma ${tg.initData}`
    };
}

async function authorizedFetch(url, options = {}) {
    const headers = {
        ...getAuthHeaders(),
        ...options.headers
    };
    
    return fetch(url, {
        ...options,
        headers
    });
}

// Global state
let isAdmin = false;
let tutorsData = [];
let subjectsData = [];
let userPermissions = [];
let currentTutorId = null;

// Loading log
function addLoadingLog(message, isSuccess = false) {
    const log = document.getElementById('loading-log');
    if (!log) return;

    const item = document.createElement('div');
    item.className = `loading-log-item${isSuccess ? ' success' : ''}`;
    item.textContent = message;
    log.appendChild(item);
}

// Show access denied page
function showAccessDenied() {
    const template = document.getElementById('template-access-denied');
    const content = template.content.cloneNode(true);
    document.body.innerHTML = '';
    document.body.appendChild(content);
}

// App version checking based on files hash
const VERSION_CHECK_INTERVAL = 10000; // Check every minute
let currentHash = null;

async function checkForUpdates() {
    try {
        const response = await fetch('/api/version');
        if (!response.ok) return;

        const data = await response.json();
        const serverHash = data.hash;

        if (currentHash === null) {
            // First load - save hash
            currentHash = serverHash;
            localStorage.setItem('appHash', serverHash);
        } else if (currentHash !== serverHash) {
            // Hash changed - force reload
            console.log('New version detected, reloading...');
            window.location.reload(true);
        }
    } catch (error) {
        console.error('Failed to check for updates:', error);
    }
}

// Check for updates on load
checkForUpdates();

// Periodic version check
setInterval(checkForUpdates, VERSION_CHECK_INTERVAL);

// Initialize
async function init() {
    try {
        addLoadingLog('Инициализация приложения...');

        // Load user info from /api/me
        try {
            addLoadingLog('Загрузка информации о пользователе...');
            const meResponse = await authorizedFetch('/api/me');

            if (meResponse.status === 403) {
                // No WEB_INTERFACE permission - show access denied page
                showAccessDenied();
                return;
            }

            if (meResponse.ok) {
                const userData = await meResponse.json();
                currentTutorId = userData.tutorId;
                userPermissions = userData.permissions || [];
                isAdmin = userData.isAdmin;
                subjectsData = userData.subjects || [];
                addLoadingLog('Информация о пользователе загружена', true);

                // Populate period selects BEFORE subject selects to avoid calling loadReport with empty period
                populatePeriodSelects();

                // Populate subject selects with loaded data
                populateSubjectSelects();
            }
        } catch (e) {
            console.log('Could not load user info');
            addLoadingLog('Ошибка загрузки пользователя');
        }

        // Load tutors list for admin
        if (isAdmin) {
            try {
                addLoadingLog('Загрузка списка учителей...');
                const tutorsResponse = await authorizedFetch('/api/tutors');
                if (tutorsResponse.ok) {
                    tutorsData = await tutorsResponse.json();
                    addLoadingLog('Режим администратора активен', true);
                    document.getElementById('tutor-select-row').style.display = 'flex';
                    document.getElementById('stats-tutor-select-row').style.display = 'flex';
                    const teachersOption = document.getElementById('teachers-nav-option');
                    if (teachersOption) teachersOption.style.display = 'block';
                    populateTutorSelects();
                    // Load subjects for current tutor
                    if (currentTutorId) {
                        addLoadingLog('Загрузка предметов...');
                        await loadSubjectsForTutor('reports');
                        addLoadingLog('Предметы загружены', true);
                    }
                }
            } catch (e) {
                console.log('Error loading tutors');
                addLoadingLog('Ошибка загрузки учителей');
            }
        } else {
            addLoadingLog('Стандартный режим');
        }

        // Load today's data
        addLoadingLog('Загрузка данных за сегодня...');
        await loadTodayData();
        addLoadingLog('Данные загружены', true);

        addLoadingLog('Настройка интерфейса...');
        setupEventListeners();
        addLoadingLog('Готово!', true);

        // Small delay to show final message
        await new Promise(resolve => setTimeout(resolve, 300));

        document.getElementById('loading').classList.add('hidden');
        document.getElementById('content').classList.remove('hidden');
    } catch (error) {
        addLoadingLog('❌ Ошибка: ' + error.message);
        showError('Не удалось загрузить данные: ' + error.message);
    }
}

function setupEventListeners() {
    // Navigation - custom dropdown
    const selectorButton = document.getElementById('page-selector-button');
    const dropdown = document.getElementById('page-dropdown');

    if (selectorButton && dropdown) {
        // Toggle dropdown on button click
        selectorButton.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdown.classList.toggle('hidden');
            selectorButton.classList.toggle('open');
        });

        // Handle page option clicks
        document.querySelectorAll('.page-option').forEach(option => {
            option.addEventListener('click', () => {
                const pageName = option.dataset.page;
                switchPage(pageName);
                dropdown.classList.add('hidden');
                selectorButton.classList.remove('open');
            });
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!selectorButton.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.classList.add('hidden');
                selectorButton.classList.remove('open');
            }
        });
    }

    // Tabs
    document.querySelectorAll('[data-tab]').forEach(button => {
        button.addEventListener('click', () => switchTab(button));
    });

    // Reports filters
    document.getElementById('reports-subject').addEventListener('change', loadReport);
    document.getElementById('reports-period').addEventListener('change', loadReport);
    if (isAdmin) {
        document.getElementById('reports-tutor-select').addEventListener('change', async () => {
            await loadSubjectsForTutor('reports');
            loadReport();
        });
    }

    // Statistics filters
    document.getElementById('stats-subject').addEventListener('change', loadStatistics);
    document.getElementById('stats-period').addEventListener('change', loadStatistics);
    if (isAdmin) {
        document.getElementById('stats-tutor-select').addEventListener('change', async () => {
            await loadSubjectsForTutor('stats');
            loadStatistics();
        });
    }

    // Student search - live results
    document.getElementById('student-search').addEventListener('input', (e) => {
        searchStudents(e.target.value);
    });

    // Phone formatting
    setupPhoneFormatting('student-phone');
    setupPhoneFormatting('student-responsible-phone');
}

// Phone formatting function
function setupPhoneFormatting(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;

    input.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\D/g, ''); // Remove all non-digits

        // Ensure it starts with 7 or 8
        if (value.length > 0 && value[0] !== '7' && value[0] !== '8') {
            value = '7' + value;
        }

        // Convert 8 to 7
        if (value[0] === '8') {
            value = '7' + value.substring(1);
        }

        // Limit to 11 digits
        if (value.length > 11) {
            value = value.substring(0, 11);
        }

        // Format the number
        let formatted = '';
        if (value.length > 0) {
            formatted = '+7';
            if (value.length > 1) {
                formatted += ' (' + value.substring(1, 4);
            }
            if (value.length >= 4) {
                formatted += ') ' + value.substring(4, 7);
            }
            if (value.length >= 7) {
                formatted += '-' + value.substring(7, 9);
            }
            if (value.length >= 9) {
                formatted += '-' + value.substring(9, 11);
            }
        }

        e.target.value = formatted;
    });

    input.addEventListener('keydown', (e) => {
        // Allow: backspace, delete, tab, escape, enter
        if ([8, 9, 27, 13, 46].indexOf(e.keyCode) !== -1 ||
            // Allow: Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X
            (e.keyCode === 65 && e.ctrlKey === true) ||
            (e.keyCode === 67 && e.ctrlKey === true) ||
            (e.keyCode === 86 && e.ctrlKey === true) ||
            (e.keyCode === 88 && e.ctrlKey === true) ||
            // Allow: home, end, left, right
            (e.keyCode >= 35 && e.keyCode <= 39)) {
            return;
        }
        // Ensure that it is a number and stop the keypress if not
        if ((e.shiftKey || (e.keyCode < 48 || e.keyCode > 57)) && (e.keyCode < 96 || e.keyCode > 105)) {
            e.preventDefault();
        }
    });
}

function populateTutorSelects() {
    const selects = ['reports-tutor-select', 'stats-tutor-select'];
    selects.forEach(selectId => {
        const select = document.getElementById(selectId);
        tutorsData.forEach(tutor => {
            const option = document.createElement('option');
            option.value = tutor.id;
            option.textContent = tutor.name;
            select.appendChild(option);
        });
        // Set current tutor as selected by default
        if (currentTutorId) {
            select.value = currentTutorId;
        }
    });
}

function populateSubjectSelects() {
    const select = document.getElementById('reports-subject');
    select.innerHTML = '<option value="">Выберите предмет</option>';

    // For admin, if no tutor is selected, show placeholder message
    if (isAdmin && !document.getElementById('reports-tutor-select').value) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'Сначала выберите преподавателя';
        option.disabled = true;
        select.appendChild(option);
        select.disabled = true;
        return;
    }

    select.disabled = false;
    subjectsData.forEach(subject => {
        const option = document.createElement('option');
        option.value = subject.id;
        option.textContent = subject.name;
        select.appendChild(option);
    });

    if (subjectsData.length === 1) {
        select.value = subjectsData[0].id;
        // Don't auto-load report here, let the user trigger it or let switchPage handle it
    }

    // Also populate stats subject select
    populateStatsSubjectSelect();
}

function populateStatsSubjectSelect() {
    const statsSelect = document.getElementById('stats-subject');
    if (!statsSelect) return;

    statsSelect.innerHTML = '<option value="all">Все предметы</option>';

    // For admin, if no tutor is selected, show placeholder message
    if (isAdmin && !document.getElementById('stats-tutor-select').value) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'Сначала выберите преподавателя';
        option.disabled = true;
        statsSelect.appendChild(option);
        statsSelect.disabled = true;
        return;
    }

    statsSelect.disabled = false;
    subjectsData.forEach(subject => {
        const option = document.createElement('option');
        option.value = subject.name;
        option.textContent = subject.name;
        statsSelect.appendChild(option);
    });
}

// Load subjects for selected tutor (admin only)
async function loadSubjectsForTutor(prefix) {
    const tutorId = document.getElementById(`${prefix}-tutor-select`).value;
    if (!tutorId) {
        subjectsData = [];
        if (prefix === 'reports') {
            document.getElementById('reports-subject').innerHTML = '<option value="">Выберите предмет</option>';
            document.getElementById('reports-container').classList.add('hidden');
            document.getElementById('reports-no-data').classList.remove('hidden');
        } else if (prefix === 'stats') {
            document.getElementById('stats-subject').innerHTML = '<option value="all">Все предметы</option>';
            document.getElementById('stats-container').innerHTML = '';
        }
        return;
    }

    // Get subjects from tutorsData loaded in app.js
    const tutor = tutorsData.find(t => t.id === parseInt(tutorId));
    if (tutor) {
        subjectsData = tutor.subjects || [];
        if (prefix === 'reports') {
            populateSubjectSelects();
        } else if (prefix === 'stats') {
            populateStatsSubjectSelect();
        }
    }
}

function populatePeriodSelects() {
    const months = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
        'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
    const now = new Date();
    const selects = ['reports-period', 'stats-period'];

    selects.forEach(selectId => {
        const select = document.getElementById(selectId);
        for (let i = 0; i < 12; i++) {
            const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
            const year = date.getFullYear();
            const month = date.getMonth() + 1;
            const monthStr = month < 10 ? `0${month}` : month;
            const value = `${year}-${monthStr}`;
            const label = `${months[date.getMonth()]} ${year}`;

            const option = document.createElement('option');
            option.value = value;
            option.textContent = label;
            select.appendChild(option);
        }

        // Select current month by default
        const currentMonth = now.getMonth() + 1;
        const currentMonthStr = currentMonth < 10 ? `0${currentMonth}` : currentMonth;
        select.value = `${now.getFullYear()}-${currentMonthStr}`;
    });
}

// Page Navigation
function switchPage(pageName) {
    // Update current page label
    const pageLabels = {
        'today': '📅 Сегодня',
        'reports': '📊 Отчеты',
        'statistics': '📈 Статистика',
        'students': '👥 Ученики',
        'teachers': '👨‍🏫 Учителя',
        'audit-log': '📋 Журнал действий'
    };

    const currentPageLabel = document.getElementById('current-page-label');
    if (currentPageLabel && pageLabels[pageName]) {
        currentPageLabel.textContent = pageLabels[pageName];
    }

    document.querySelectorAll('.page-section').forEach(section => section.classList.remove('active'));
    document.getElementById(`page-${pageName}`).classList.add('active');

    // Load data for specific pages
    if (pageName === 'today' && todayLessons.length === 0) {
        loadTodayData();
    } else if (pageName === 'reports') {
        // Auto-select first subject if only one available and nothing selected
        const subjectSelect = document.getElementById('reports-subject');
        const periodSelect = document.getElementById('reports-period');
        if (!subjectSelect.value && subjectsData.length === 1) {
            subjectSelect.value = subjectsData[0].subjectId;
        }
        // Load report if both subject and period are selected
        if (subjectSelect.value && periodSelect.value && !currentReport) {
            loadReport();
        }
    } else if (pageName === 'statistics' && !currentStats) {
        loadStatistics();
    } else if (pageName === 'students') {
        searchStudents('');
    } else if (pageName === 'teachers' && teachers.length === 0) {
        loadTeachers();
    } else if (pageName === 'audit-log' && loadedLogsCount === 0) {
        loadAuditLog();
    }
}

function switchTab(button) {
    const tabName = button.dataset.tab;
    const parent = button.closest('.page-section') || button.closest('.card');

    parent.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
    button.classList.add('active');

    parent.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    const targetTab = document.getElementById(tabName);
    if (targetTab) {
        targetTab.classList.add('active');
    }
}

function showError(message) {
    const errorContainer = document.getElementById('error-container');
    errorContainer.innerHTML = `<div class="error">${message}</div>`;
    errorContainer.classList.remove('hidden');
    setTimeout(() => errorContainer.classList.add('hidden'), 5000);
}

// Close modals on outside click
['lesson-modal', 'payment-modal', 'student-modal', 'student-details-modal', 'add-lesson-modal', 'add-payment-modal'].forEach(modalId => {
    document.getElementById(modalId).addEventListener('click', (e) => {
        if (e.target.id === modalId) {
            if (modalId === 'lesson-modal') closeLessonModal();
            else if (modalId === 'payment-modal') closePaymentModal();
            else if (modalId === 'student-modal') closeStudentModal();
            else if (modalId === 'student-details-modal') closeStudentDetailsModal();
            else if (modalId === 'add-lesson-modal') closeAddLessonModal();
            else if (modalId === 'add-payment-modal') closeAddPaymentModal();
        }
    });
});

// Initialize app
init();
