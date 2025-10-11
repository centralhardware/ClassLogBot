let currentStudentId = null;

async function searchStudents(query) {
    const container = document.getElementById('student-search-results');

    try {
        const url = (!query || query.length < 2)
            ? '/api/student/search'
            : `/api/student/search?q=${encodeURIComponent(query)}`;

        const response = await authorizedFetch(url);

        if (!response.ok) throw new Error('Ошибка поиска');

        const students = await response.json();

        if (students.length === 0) {
            container.innerHTML = '<div class="no-data"><div class="no-data-icon">🔍</div><p>Ничего не найдено</p></div>';
            return;
        }

        container.innerHTML = students.map(student => `
            <div class="student-card" onclick="openStudentModal(${student.id})">
                <div class="student-card-header">
                    <div class="student-card-title">${student.secondName} ${student.name} ${student.lastName}</div>
                    <div class="student-card-id">${student.schoolClass ? `${student.schoolClass} класс` : ''}</div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showError('Не удалось выполнить поиск: ' + error.message);
    }
}

async function openStudentModal(studentId) {
    currentStudentId = studentId;

    try {
        const response = await authorizedFetch(`/api/student/${studentId}`);

        if (!response.ok) throw new Error('Ошибка загрузки данных');

        const student = await response.json();

        document.getElementById('student-name').value = student.name || '';
        document.getElementById('student-second-name').value = student.secondName || '';
        document.getElementById('student-last-name').value = student.lastName || '';
        document.getElementById('student-school-class').value = student.schoolClass || '';
        document.getElementById('student-phone').value = formatPhoneForDisplay(student.phone || '');
        document.getElementById('student-responsible-phone').value = formatPhoneForDisplay(student.responsiblePhone || '');
        document.getElementById('student-source').value = student.source || '';
        document.getElementById('student-birth-date').value = student.birthDate || '';

        const searchInput = document.getElementById('student-search');
        if (searchInput) searchInput.blur();

        document.getElementById('student-modal').classList.add('active');
    } catch (error) {
        showError('Не удалось загрузить данные ученика: ' + error.message);
    }
}

function formatPhoneForDisplay(phone) {
    if (!phone) return '';

    const digits = phone.replace(/\D/g, '');

    if (digits.length === 0) return '';
    if (digits.length !== 11) return phone;

    return `+7 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9, 11)}`;
}

function unformatPhone(formattedPhone) {
    if (!formattedPhone) return null;

    const digits = formattedPhone.replace(/\D/g, '');

    if (digits.length !== 11) return null;

    return digits;
}

function closeStudentModal() {
    document.getElementById('student-modal').classList.remove('active');
    currentStudentId = null;
}

async function saveStudentChanges() {
    if (!currentStudentId) return;

    const phone = unformatPhone(document.getElementById('student-phone').value.trim());
    const responsiblePhone = unformatPhone(document.getElementById('student-responsible-phone').value.trim());

    const formData = {
        name: document.getElementById('student-name').value.trim(),
        secondName: document.getElementById('student-second-name').value.trim(),
        lastName: document.getElementById('student-last-name').value.trim() || null,
        schoolClass: document.getElementById('student-school-class').value || null,
        phone: phone,
        responsiblePhone: responsiblePhone,
        source: document.getElementById('student-source').value || null,
        birthDate: document.getElementById('student-birth-date').value || null
    };

    if (!formData.name || !formData.secondName) {
        tg.showPopup({ message: 'Заполните обязательные поля (Имя и Фамилия)', buttons: [{type: 'ok'}] });
        return;
    }

    if (phone !== null && phone.length !== 11) {
        tg.showPopup({ message: 'Некорректный формат телефона', buttons: [{type: 'ok'}] });
        return;
    }
    if (responsiblePhone !== null && responsiblePhone.length !== 11) {
        tg.showPopup({ message: 'Некорректный формат телефона ответственного', buttons: [{type: 'ok'}] });
        return;
    }

    try {
        const response = await authorizedFetch(`/api/student/${currentStudentId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) throw new Error('Ошибка сохранения');

        closeStudentModal();
        tg.showPopup({ message: 'Данные сохранены', buttons: [{type: 'ok'}] });

        const query = document.getElementById('student-search').value;
        if (query) searchStudents(query);
    } catch (error) {
        showError('Не удалось сохранить: ' + error.message);
    }
}

async function openStudentDetailsModal(studentId, studentName) {
    try {
        const period = currentReportPeriod || document.getElementById('reports-period').value;

        if (!period || !period.match(/^\d{4}-\d{2}$/)) {
            showError('Период отчета некорректен. Пожалуйста, выберите период из списка.');
            return;
        }

        const subjectId = currentReportSubjectId || document.getElementById('reports-subject').value;

        let url = `/api/student/${studentId}/details?period=${period}`;
        if (subjectId) {
            url += `&subjectId=${subjectId}`;
        }

        const response = await authorizedFetch(url);

        if (!response.ok) throw new Error('Ошибка загрузки данных');

        const data = await response.json();

        document.getElementById('student-details-title').textContent = studentName;

        const lessonsContainer = document.getElementById('student-lessons-list');
        if (data.lessons.length === 0) {
            lessonsContainer.innerHTML = '<div class="no-data">Занятий не найдено</div>';
        } else {
            lessonsContainer.innerHTML = data.lessons.map(lesson => {
                const details = [
                    lesson.forceGroup ? 'Групп.' : '',
                    lesson.extraHalfHour ? '1.5 часа' : ''
                ].filter(Boolean).join(' • ');

                return `
                    <div style="background: #f7fafc; padding: 12px; border-radius: 8px; margin-bottom: 8px;">
                        <div style="display: flex; justify-content: space-between; align-items: start;">
                            <div>
                                <div style="font-weight: 600; color: #2d3748;">${lesson.dateTime}</div>
                                <div style="color: #4a5568; font-size: 14px; margin-top: 4px;">${lesson.subjectName}</div>
                                ${details ? `<div style="color: #718096; font-size: 13px; margin-top: 2px;">${details}</div>` : ''}
                            </div>
                            <div style="font-weight: 600; color: #667eea; white-space: nowrap;">${lesson.amount} ₽</div>
                        </div>
                    </div>
                `;
            }).join('');
        }

        // Display payments
        const paymentsContainer = document.getElementById('student-payments-list');
        if (data.payments.length === 0) {
            paymentsContainer.innerHTML = '<div class="no-data">Оплат не найдено</div>';
        } else {
            paymentsContainer.innerHTML = data.payments.map(payment => `
                <div style="background: #f7fafc; padding: 12px; border-radius: 8px; margin-bottom: 8px;">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 600; color: #2d3748;">${payment.dateTime}</div>
                        </div>
                        <div style="font-weight: 600; color: #48bb78;">${payment.amount} ₽</div>
                    </div>
                </div>
            `).join('');
        }

        setupTabsForModal('student-details-modal');

        document.getElementById('student-details-modal').classList.add('active');
    } catch (error) {
        showError('Не удалось загрузить данные студента: ' + error.message);
    }
}

function closeStudentDetailsModal() {
    document.getElementById('student-details-modal').classList.remove('active');
}

function setupTabsForModal(modalId) {
    const modal = document.getElementById(modalId);
    const tabButtons = modal.querySelectorAll('.tab-button');
    const tabContents = modal.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const targetTab = button.getAttribute('data-tab');

            tabButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');

            tabContents.forEach(content => {
                if (content.id === targetTab) {
                    content.classList.add('active');
                } else {
                    content.classList.remove('active');
                }
            });
        });
    });
}
