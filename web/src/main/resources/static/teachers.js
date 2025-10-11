// Teachers page state
let teachers = [];
let allSubjects = [];
let currentTeacherId = null;
let currentTeacherPermissions = [];
let currentTeacherSubjects = [];

const permissionNames = {
    'ADD_TIME': 'Добавление времени',
    'ADD_PAYMENT': 'Добавление оплаты',
    'ADD_CLIENT': 'Добавление учеников',
    'ADMIN': 'Администратор',
    'FORCE_GROUP': 'Групповые занятия',
    'EXTRA_HALF_HOUR': 'Дополнительные 30 минут',
    'ADD_PAYMENT_FOR_OTHERS': 'Оплата за других',
    'ADD_TIME_FOR_OTHERS': 'Время за других'
};

// Teachers page functions
async function loadTeachers() {
    try {
        const response = await authorizedFetch('/api/tutors');
        if (!response.ok) throw new Error('Failed to load teachers');
        teachers = await response.json();
        // Sort teachers by name (ФИО)
        teachers.sort((a, b) => a.name.localeCompare(b.name, 'ru'));
        renderTeachers();
    } catch (error) {
        showError('Ошибка загрузки преподавателей: ' + error.message);
    }
}

async function loadAllSubjects() {
    try {
        const response = await authorizedFetch('/api/subjects/all');
        if (!response.ok) throw new Error('Failed to load subjects');
        allSubjects = await response.json();
    } catch (error) {
        showError('Ошибка загрузки предметов: ' + error.message);
    }
}

function renderTeachers() {
    const container = document.getElementById('teachers-list');
    if (!container) return;

    if (teachers.length === 0) {
        container.innerHTML = '<div class="no-teachers">Преподаватели не найдены</div>';
        return;
    }

    container.innerHTML = teachers.map(teacher => `
        <div class="teacher-card">
            <div class="teacher-header">
                <div>
                    <span class="teacher-name">${teacher.name}</span>
                    <span class="teacher-id">ID: ${teacher.id}</span>
                </div>
                <button class="edit-button" data-teacher-id="${teacher.id}">Изменить</button>
            </div>
            <div class="teacher-section">
                <div class="teacher-section-title">Разрешения</div>
                <div>
                    ${teacher.permissions.length > 0
                        ? teacher.permissions.map(p =>
                            `<span class="permission-tag ${p === 'ADMIN' ? 'admin' : ''}">${permissionNames[p] || p}</span>`
                          ).join('')
                        : '<span style="color: #a0aec0; font-size: 13px;">Нет разрешений</span>'
                    }
                </div>
            </div>
            <div class="teacher-section">
                <div class="teacher-section-title">Предметы</div>
                <div>
                    ${teacher.subjects.length > 0
                        ? teacher.subjects.map(s => `<span class="subject-tag">${s.name}</span>`).join('')
                        : '<span style="color: #a0aec0; font-size: 13px;">Нет предметов</span>'
                    }
                </div>
            </div>
        </div>
    `).join('');

    // Add event listeners to edit buttons
    container.querySelectorAll('.edit-button').forEach(button => {
        button.addEventListener('click', () => {
            const teacherId = parseInt(button.dataset.teacherId);
            openTeacherModal(teacherId);
        });
    });
}

async function openTeacherModal(teacherId) {
    const teacher = teachers.find(t => t.id === teacherId);
    if (!teacher) return;

    if (allSubjects.length === 0) {
        await loadAllSubjects();
    }

    currentTeacherId = teacherId;
    currentTeacherPermissions = [...teacher.permissions];
    currentTeacherSubjects = teacher.subjects.map(s => ({ id: s.id, name: s.name }));

    document.getElementById('teacher-modal-title').textContent = `Редактирование: ${teacher.name}`;
    document.getElementById('teacher-name-input').value = teacher.name;

    // Render permissions as cards
    const permissionsContainer = document.getElementById('permissions-grid');
    permissionsContainer.innerHTML = Object.entries(permissionNames).map(([key, label]) => `
        <div class="permission-card ${currentTeacherPermissions.includes(key) ? 'active' : ''}"
             data-permission="${key}"
             onclick="togglePermission('${key}')">
            <div class="permission-card-content">
                <span class="permission-label">${label}</span>
                <span class="permission-checkbox-indicator">
                    ${currentTeacherPermissions.includes(key) ? '✓' : ''}
                </span>
            </div>
        </div>
    `).join('');

    // Render subjects select
    renderSubjectSelect();
    renderSelectedSubjects();

    document.getElementById('teacher-modal').classList.add('active');
}

function renderSubjectSelect() {
    const select = document.getElementById('subject-select');
    if (!select) return;

    const availableSubjects = allSubjects.filter(s =>
        !currentTeacherSubjects.some(ts => ts.id === s.id)
    );

    select.innerHTML = '<option value="">Выберите предмет для добавления</option>' +
        availableSubjects.map(s => `<option value="${s.id}">${s.name}</option>`).join('');

    select.onchange = function() {
        if (this.value) {
            const subjectId = parseInt(this.value);
            const subject = allSubjects.find(s => s.id === subjectId);
            if (subject && !currentTeacherSubjects.some(s => s.id === subjectId)) {
                currentTeacherSubjects.push({ id: subject.id, name: subject.name });
                renderSubjectSelect();
                renderSelectedSubjects();
            }
            this.value = '';
        }
    };
}

function renderSelectedSubjects() {
    const container = document.getElementById('selected-subjects');
    if (!container) return;

    if (currentTeacherSubjects.length === 0) {
        container.innerHTML = '<div class="no-subjects">Предметы не выбраны</div>';
        return;
    }

    container.innerHTML = currentTeacherSubjects.map(s => `
        <div class="subject-chip">
            <span class="subject-chip-name">${s.name}</span>
            <button class="subject-chip-remove" onclick="removeSubject(${s.id})" type="button">×</button>
        </div>
    `).join('');
}

function removeSubject(subjectId) {
    currentTeacherSubjects = currentTeacherSubjects.filter(s => s.id !== subjectId);
    renderSubjectSelect();
    renderSelectedSubjects();
}

function closeTeacherModal() {
    document.getElementById('teacher-modal').classList.remove('active');
    currentTeacherId = null;
    currentTeacherPermissions = [];
    currentTeacherSubjects = [];
}

function togglePermission(permissionKey) {
    const index = currentTeacherPermissions.indexOf(permissionKey);
    if (index > -1) {
        currentTeacherPermissions.splice(index, 1);
    } else {
        currentTeacherPermissions.push(permissionKey);
    }

    // Update UI
    const card = document.querySelector(`.permission-card[data-permission="${permissionKey}"]`);
    const indicator = card.querySelector('.permission-checkbox-indicator');

    if (currentTeacherPermissions.includes(permissionKey)) {
        card.classList.add('active');
        indicator.textContent = '✓';
    } else {
        card.classList.remove('active');
        indicator.textContent = '';
    }
}

async function saveTeacherChanges() {
    try {
        const newName = document.getElementById('teacher-name-input').value.trim();
        
        if (!newName) {
            showError('Имя преподавателя не может быть пустым');
            return;
        }

        const updateData = {
            name: newName,
            permissions: currentTeacherPermissions,
            subjectIds: currentTeacherSubjects.map(s => s.id)
        };

        const response = await authorizedFetch(`/api/tutors/${currentTeacherId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updateData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to update teacher');
        }

        closeTeacherModal();
        await loadTeachers();
        tg.showPopup({ message: 'Преподаватель успешно обновлен', buttons: [{type: 'ok'}] });
    } catch (error) {
        showError('Ошибка сохранения: ' + error.message);
    }
}
