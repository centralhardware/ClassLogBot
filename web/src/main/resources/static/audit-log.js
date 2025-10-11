let loadedLogsCount = 0;
let totalAuditLogs = 0;
let isLoadingMore = false;
let hasMoreLogs = true;
const auditLogPageSize = 50;
let currentFilters = {};

async function loadAuditLog() {
    const loadingEl = document.getElementById('audit-log-loading');
    const errorEl = document.getElementById('audit-log-error');
    const contentEl = document.getElementById('audit-log-content');
    const filtersEl = document.getElementById('audit-log-filters');
    const listEl = document.getElementById('audit-log-list');

    try {
        loadingEl.classList.remove('hidden');
        errorEl.classList.add('hidden');

        await loadFiltersData();

        const params = new URLSearchParams({ limit: auditLogPageSize, offset: 0 });
        Object.entries(currentFilters).forEach(([key, value]) => {
            if (value) params.append(key, value);
        });

        const response = await authorizedFetch(`/api/audit-log?${params}`);

        if (!response.ok) {
            throw new Error('Failed to load audit log');
        }

        const data = await response.json();
        totalAuditLogs = data.total;
        loadedLogsCount = 0;

        listEl.innerHTML = '';

        if (data.logs && data.logs.length > 0) {
            data.logs.forEach(log => appendLogCard(log, listEl));
            loadedLogsCount = data.logs.length;
            hasMoreLogs = loadedLogsCount < totalAuditLogs;
        } else {
            listEl.innerHTML = '<div class="no-data"><div class="no-data-icon">📋</div><p>Нет записей в журнале</p></div>';
            hasMoreLogs = false;
        }
        loadingEl.classList.add('hidden');
        contentEl.classList.remove('hidden');
        filtersEl.classList.remove('hidden');
        
        setupAuditLogFilters();
    } catch (error) {
        console.error('Error loading audit log:', error);
        loadingEl.classList.add('hidden');
        errorEl.textContent = 'Ошибка загрузки журнала действий';
        errorEl.classList.remove('hidden');
    }
}

async function loadFiltersData() {
    if (isAdmin) {
        try {
            const tutorsResponse = await authorizedFetch('/api/tutors');
            if (tutorsResponse.ok) {
                const tutors = await tutorsResponse.json();
                const tutorSelect = document.getElementById('audit-log-tutor-filter');
                tutorSelect.innerHTML = '<option value="">Все учителя</option>';
                tutors.forEach(tutor => {
                    const option = document.createElement('option');
                    option.value = tutor.id;
                    option.textContent = tutor.name;
                    tutorSelect.appendChild(option);
                });
                document.getElementById('audit-log-tutor-filter-container').classList.remove('hidden');
            }
        } catch (e) {
            console.error('Error loading tutors:', e);
        }

        try {
            const subjectsResponse = await authorizedFetch('/api/subjects/all');
            if (subjectsResponse.ok) {
                const subjects = await subjectsResponse.json();
                const subjectSelect = document.getElementById('audit-log-subject-filter');
                subjectSelect.innerHTML = '<option value="">Все предметы</option>';
                subjects.forEach(subject => {
                    const option = document.createElement('option');
                    option.value = subject.id || subject.subjectId;
                    option.textContent = subject.name || subject.subjectName;
                    subjectSelect.appendChild(option);
                });
            }
        } catch (e) {
            console.error('Error loading subjects:', e);
        }
    } else {
        const subjectSelect = document.getElementById('audit-log-subject-filter');
        subjectSelect.innerHTML = '<option value="">Все предметы</option>';
        subjectsData.forEach(subject => {
            const option = document.createElement('option');
            option.value = subject.id || subject.subjectId;
            option.textContent = subject.name || subject.subjectName;
            subjectSelect.appendChild(option);
        });
    }
}

function applyAuditLogFilters() {
    currentFilters = {};
    
    const tutorFilter = document.getElementById('audit-log-tutor-filter').value;
    if (tutorFilter) currentFilters.tutorId = tutorFilter;
    
    const subjectFilter = document.getElementById('audit-log-subject-filter').value;
    if (subjectFilter) currentFilters.subjectId = subjectFilter;
    
    const actionFilter = document.getElementById('audit-log-action-filter').value;
    if (actionFilter) currentFilters.action = actionFilter;
    
    loadedLogsCount = 0;
    totalAuditLogs = 0;
    hasMoreLogs = true;
    
    const listEl = document.getElementById('audit-log-list');
    listEl.innerHTML = '';
    
    loadAuditLogWithFilters();
}

async function loadAuditLogWithFilters() {
    const loadingEl = document.getElementById('audit-log-loading');
    const errorEl = document.getElementById('audit-log-error');
    const listEl = document.getElementById('audit-log-list');

    try {
        loadingEl.classList.remove('hidden');
        errorEl.classList.add('hidden');

        const params = new URLSearchParams({ limit: auditLogPageSize, offset: 0 });
        Object.entries(currentFilters).forEach(([key, value]) => {
            if (value) params.append(key, value);
        });

        const response = await authorizedFetch(`/api/audit-log?${params}`);

        if (!response.ok) {
            throw new Error('Failed to load audit log');
        }

        const data = await response.json();
        totalAuditLogs = data.total;
        loadedLogsCount = 0;

        listEl.innerHTML = '';

        if (data.logs && data.logs.length > 0) {
            data.logs.forEach(log => appendLogCard(log, listEl));
            loadedLogsCount = data.logs.length;
            hasMoreLogs = loadedLogsCount < totalAuditLogs;
        } else {
            listEl.innerHTML = '<div class="no-data"><div class="no-data-icon">📋</div><p>Нет записей в журнале</p></div>';
            hasMoreLogs = false;
        }
        
        loadingEl.classList.add('hidden');
    } catch (error) {
        console.error('Error loading audit log:', error);
        loadingEl.classList.add('hidden');
        errorEl.textContent = 'Ошибка загрузки журнала действий';
        errorEl.classList.remove('hidden');
    }
}

async function loadMoreAuditLogs() {
    if (isLoadingMore || !hasMoreLogs) return;

    const listEl = document.getElementById('audit-log-list');
    const loadingMoreEl = document.getElementById('audit-log-loading-more');
    const endEl = document.getElementById('audit-log-end');

    try {
        isLoadingMore = true;
        loadingMoreEl.classList.remove('hidden');
        endEl.classList.add('hidden');

        const params = new URLSearchParams({ limit: auditLogPageSize, offset: loadedLogsCount });
        Object.entries(currentFilters).forEach(([key, value]) => {
            if (value) params.append(key, value);
        });

        const response = await authorizedFetch(`/api/audit-log?${params}`);

        if (!response.ok) {
            throw new Error('Failed to load more logs');
        }

        const data = await response.json();

        if (data.logs && data.logs.length > 0) {
            data.logs.forEach(log => appendLogCard(log, listEl));
            loadedLogsCount += data.logs.length;
            hasMoreLogs = loadedLogsCount < totalAuditLogs;
        } else {
            hasMoreLogs = false;
        }

        loadingMoreEl.classList.add('hidden');

        if (!hasMoreLogs) {
            endEl.classList.remove('hidden');
        }
    } catch (error) {
        console.error('Error loading more logs:', error);
        loadingMoreEl.classList.add('hidden');
    } finally {
        isLoadingMore = false;
    }
}

function appendLogCard(log, container) {
    const actionText = getActionText(log.action);
    const userName = log.userName || `User #${log.userId}`;

    const timePart = log.timestamp.split(' ')[1] || log.timestamp;
    const datePart = log.timestamp.split(' ')[0] || '';

    const card = document.createElement('div');
    card.className = 'card';
    card.style.cssText = 'margin-bottom: 8px; padding: 12px; background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); cursor: pointer;';
    card.onclick = () => openAuditDetailsModal(log);

    let studentSubjectLine = '';
    if (log.studentName || log.subject) {
        const parts = [];
        if (log.studentName) parts.push(log.studentName);
        if (log.subject) parts.push(log.subject);
        studentSubjectLine = `<div style="font-size: 13px; color: #4a5568; margin-top: 2px;">${parts.join(' • ')}</div>`;
    }

    card.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 6px;">
            <div style="flex: 1; min-width: 0;">
                <div style="font-size: 13px; color: #718096; margin-bottom: 2px;">
                    ${timePart} • ${datePart}
                </div>
                <div style="font-size: 14px; font-weight: 600; color: #2d3748;">
                    ${userName}
                </div>
                ${studentSubjectLine}
            </div>
            <span class="action-badge action-${log.action.toLowerCase()}" style="font-size: 11px; padding: 4px 8px; white-space: nowrap;">
                ${actionText}
            </span>
        </div>
    `;

    container.appendChild(card);
}

function openAuditDetailsModal(log) {
    const actionText = getActionText(log.action);
    const userName = log.userName || `User #${log.userId}`;

    document.getElementById('audit-details-timestamp').textContent = log.timestamp;
    document.getElementById('audit-details-user').textContent = userName;
    document.getElementById('audit-details-action').innerHTML = `<span class="action-badge action-${log.action.toLowerCase()}">${actionText}</span>`;
    
    const detailsContent = document.getElementById('audit-details-content');
    if (log.details) {
        detailsContent.innerHTML = log.details;
    } else {
        detailsContent.innerHTML = '<span style="color: #718096;">Нет деталей</span>';
    }

    document.getElementById('audit-log-details-modal').classList.add('active');
}

function closeAuditDetailsModal() {
    document.getElementById('audit-log-details-modal').classList.remove('active');
}



function getActionText(action) {
    const actionMap = {
        'CREATE_STUDENT': '+ Ученик',
        'UPDATE_STUDENT': 'Изм. Ученик',
        'DELETE_STUDENT': 'Удал. Ученик',
        'CREATE_LESSON': '+ Занятие',
        'UPDATE_LESSON': 'Изм. Занятие',
        'DELETE_LESSON': 'Удал. Занятие',
        'ADD_STUDENT_TO_LESSON': '+ Ученик в занятие',
        'REMOVE_STUDENT_FROM_LESSON': '- Ученик из занятия',
        'CREATE_PAYMENT': '+ Оплата',
        'UPDATE_PAYMENT': 'Изм. Оплата',
        'DELETE_PAYMENT': 'Удал. Оплата',
        'CREATE_TEACHER': '+ Учитель',
        'UPDATE_TEACHER': 'Изм. Учитель',
        'DELETE_TEACHER': 'Удал. Учитель'
    };
    
    return actionMap[action] || action;
}

let scrollTimeout;
function setupInfiniteScroll() {
    window.addEventListener('scroll', () => {
        clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(() => {
            const auditLogPage = document.getElementById('page-audit-log');
            if (!auditLogPage || !auditLogPage.classList.contains('active')) {
                return;
            }

            const scrollPosition = window.innerHeight + window.scrollY;
            const threshold = document.documentElement.scrollHeight - 500;

            if (scrollPosition >= threshold && hasMoreLogs && !isLoadingMore) {
                loadMoreAuditLogs();
            }
        }, 100);
    });
}

setupInfiniteScroll();

function setupAuditLogFilters() {
    const applyButton = document.getElementById('audit-log-apply-filters');
    if (applyButton) {
        applyButton.removeEventListener('click', applyAuditLogFilters);
        applyButton.addEventListener('click', applyAuditLogFilters);
    }
}

setupAuditLogFilters();

if (typeof window.pageLoaders !== 'undefined') {
    window.pageLoaders['audit-log'] = loadAuditLog;
}
