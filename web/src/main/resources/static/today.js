let todayLessons = [];
let todayPayments = [];
let currentLessonId = null;
let currentPaymentId = null;
let lessonStudentChanges = {
    toAdd: [],    // [{studentId, studentName}]
    toRemove: []  // [studentId]
};
let addLessonStudents = [];
let selectedPaymentStudent = null;

async function loadTodayData() {
    try {
        const [lessonsRes, paymentsRes] = await Promise.all([
            authorizedFetch('/api/lessons'),
            authorizedFetch('/api/payments')
        ]);

        if (lessonsRes.ok) {
            todayLessons = await lessonsRes.json();
            displayTodayLessons();
        }

        if (paymentsRes.ok) {
            todayPayments = await paymentsRes.json();
            displayTodayPayments();
        }
    } catch (error) {
        console.error('Error loading today data:', error);
    }
}

function displayTodayLessons() {
    const container = document.getElementById('today-lessons-list');
    if (todayLessons.length === 0) {
        container.innerHTML = '<div class="no-data"><div class="no-data-icon">📚</div><p>Сегодня нет занятий</p></div>';
        return;
    }

    container.innerHTML = todayLessons.map(lesson => `
        <div class="card" style="margin-bottom: 12px; padding: 16px; background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 12px;">
                <div>
                    <div style="font-size: 18px; font-weight: 600; color: #2d3748; margin-bottom: 4px;">
                        ${lesson.dateTime} - ${lesson.subjectName}
                    </div>
                    <div style="color: #718096; font-size: 14px;">${lesson.studentName}</div>
                </div>
                <div style="font-size: 20px; font-weight: 700; color: #667eea;">${lesson.amount} ₽</div>
            </div>

            ${lesson.forceGroup || lesson.extraHalfHour ? `
                <div style="margin-bottom: 12px;">
                    ${lesson.forceGroup ? '<span class="badge group">Групповое</span>' : ''}
                    ${lesson.extraHalfHour ? '<span class="badge extra">1.5 часа</span>' : ''}
                </div>
            ` : ''}

            <div style="display: flex; gap: 8px;">
                <button class="button" onclick="openLessonModal('${lesson.id}')" style="flex: 1;">Изменить</button>
                <button class="button secondary" onclick="deleteLesson('${lesson.id}')" style="flex: 1;">Удалить</button>
            </div>
        </div>
    `).join('');
}

function displayTodayPayments() {
    const container = document.getElementById('today-payments-list');
    if (todayPayments.length === 0) {
        container.innerHTML = '<div class="no-data"><div class="no-data-icon">💳</div><p>Сегодня нет оплат</p></div>';
        return;
    }

    container.innerHTML = todayPayments.map(payment => `
        <div class="card" style="margin-bottom: 12px; padding: 16px; background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 12px;">
                <div>
                    <div style="font-size: 18px; font-weight: 600; color: #2d3748; margin-bottom: 4px;">
                        ${payment.dateTime} - ${payment.subjectName}
                    </div>
                    <div style="color: #718096; font-size: 14px;">${payment.studentName}</div>
                </div>
                <div style="font-size: 20px; font-weight: 700; color: #48bb78;">${payment.amount} ₽</div>
            </div>

            <div style="display: flex; gap: 8px;">
                <button class="button" onclick="openPaymentModal(${payment.id})" style="flex: 1;">Изменить</button>
                <button class="button secondary" onclick="deletePayment(${payment.id})" style="flex: 1;">Удалить</button>
            </div>
        </div>
    `).join('');
}

function openLessonModal(lessonId) {
    currentLessonId = lessonId;
    const lesson = todayLessons.find(l => l.id === lessonId);
    if (!lesson) return;

    lessonStudentChanges = {
        toAdd: [],
        toRemove: []
    };

    document.getElementById('lesson-modal-info').textContent =
        `${lesson.subjectName} (${lesson.dateTime})`;
    document.getElementById('lesson-amount').value = lesson.amount;
    document.getElementById('lesson-force-group').checked = lesson.forceGroup;
    document.getElementById('lesson-extra-half-hour').checked = lesson.extraHalfHour;

    const forceGroupContainer = document.getElementById('lesson-force-group').closest('.form-group');
    const extraHalfHourContainer = document.getElementById('lesson-extra-half-hour').closest('.form-group');

    if (userPermissions.includes('FORCE_GROUP')) {
        forceGroupContainer.style.display = 'block';
    } else {
        forceGroupContainer.style.display = 'none';
    }

    if (userPermissions.includes('EXTRA_HALF_HOUR')) {
        extraHalfHourContainer.style.display = 'block';
    } else {
        extraHalfHourContainer.style.display = 'none';
    }

    const photoContainer = document.getElementById('lesson-photo-container');
    const photoImg = document.getElementById('lesson-photo');
    if (lesson.photoReport) {
        photoImg.src = lesson.photoReport;
        photoImg.onerror = () => {
            photoContainer.style.display = 'none';
        };
        photoContainer.style.display = 'block';
    } else {
        photoContainer.style.display = 'none';
    }

    displayLessonStudents();

    document.getElementById('add-student-search').value = '';
    document.getElementById('add-student-results').innerHTML = '';

    const searchInput = document.getElementById('add-student-search');
    const resultsContainer = document.getElementById('add-student-results');

    searchInput.oninput = searchStudentForLesson;

    const modal = document.getElementById('lesson-modal');
    modal.addEventListener('click', (e) => {
        if (e.target !== searchInput && !resultsContainer.contains(e.target)) {
            resultsContainer.innerHTML = '';
        }
    });

    document.getElementById('lesson-modal').classList.add('active');
}

function displayLessonStudents() {
    const lesson = todayLessons.find(l => l.id === currentLessonId);
    if (!lesson) return;

    const container = document.getElementById('lesson-students-list');

    let currentStudents = [...lesson.students];

    lessonStudentChanges.toAdd.forEach(({studentId, studentName}) => {
        currentStudents.push({id: studentId, name: studentName});
    });

    currentStudents = currentStudents.filter(s => !lessonStudentChanges.toRemove.includes(s.id));

    const canDelete = currentStudents.length > 1;

    container.innerHTML = currentStudents.map(student => {
        const isPending = lessonStudentChanges.toAdd.some(s => s.studentId === student.id);
        const studentName = student.name;
        return `
            <div style="display: flex; align-items: center; justify-content: space-between; padding: 8px; background: ${isPending ? '#e6fffa' : '#f7fafc'}; border-radius: 8px; margin-bottom: 4px; ${isPending ? 'border: 1px solid #81e6d9;' : ''}">
                <span style="font-weight: 500;">${studentName}${isPending ? ' (новый)' : ''}</span>
                ${canDelete ? `
                    <button onclick="markStudentForRemoval(${student.id}, '${studentName.replace(/'/g, "\\'")}')"
                            style="padding: 4px 8px; background: #fc8181; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px;">
                        Удалить
                    </button>
                ` : ''}
            </div>
        `;
    }).join('');
}

function closeLessonModal() {
    document.getElementById('lesson-modal').classList.remove('active');
    currentLessonId = null;
    lessonStudentChanges = {
        toAdd: [],
        toRemove: []
    };
}

async function searchStudentForLesson(e) {
    const query = e.target.value.trim().toLowerCase();
    const resultsContainer = document.getElementById('add-student-results');

    if (query.length < 2) {
        resultsContainer.innerHTML = '';
        return;
    }

    try {
        const response = await authorizedFetch(`/api/student/search?q=${encodeURIComponent(query)}`);

        if (!response.ok) throw new Error('Ошибка поиска');

        const students = await response.json();

        if (students.length === 0) {
            resultsContainer.innerHTML = '<div style="padding: 10px; color: #718096;">Ничего не найдено</div>';
            return;
        }

        resultsContainer.innerHTML = students.map(student => `
            <div class="student-card" style="cursor: pointer; padding: 8px; border: 1px solid #e2e8f0; margin-bottom: 4px; border-radius: 8px;"
                 onclick="addStudentToLesson(${student.id}, '${student.secondName} ${student.name} ${student.lastName}')">
                <div style="font-weight: 500;">${student.secondName} ${student.name} ${student.lastName}</div>
                <div style="font-size: 12px; color: #718096;">Класс: ${student.schoolClass || 'не указан'}</div>
            </div>
        `).join('');
    } catch (error) {
        resultsContainer.innerHTML = '<div style="padding: 10px; color: #e53e3e;">Ошибка поиска</div>';
    }
}

function markStudentForRemoval(studentId, studentName) {
    const lesson = todayLessons.find(l => l.id === currentLessonId);
    if (!lesson) return;

    const addIndex = lessonStudentChanges.toAdd.findIndex(s => s.studentId === studentId);
    if (addIndex !== -1) {
        lessonStudentChanges.toAdd.splice(addIndex, 1);
    } else {
        if (!lessonStudentChanges.toRemove.includes(studentId)) {
            lessonStudentChanges.toRemove.push(studentId);
        }
    }

    displayLessonStudents();
}

function addStudentToLesson(studentId, studentName) {
    if (!currentLessonId) return;

    const lesson = todayLessons.find(l => l.id === currentLessonId);
    if (!lesson) return;

    const alreadyExists = lesson.students.some(s => s.id === studentId);
    const alreadyAdded = lessonStudentChanges.toAdd.some(s => s.studentId === studentId);

    if (alreadyExists && !lessonStudentChanges.toRemove.includes(studentId)) {
        tg.showPopup({
            message: 'Этот ученик уже в занятии',
            buttons: [{type: 'ok'}]
        });
        return;
    }

    if (alreadyAdded) {
        tg.showPopup({
            message: 'Этот ученик уже добавлен',
            buttons: [{type: 'ok'}]
        });
        return;
    }

    const removeIndex = lessonStudentChanges.toRemove.indexOf(studentId);
    if (removeIndex !== -1) {
        lessonStudentChanges.toRemove.splice(removeIndex, 1);
    } else {
        lessonStudentChanges.toAdd.push({ studentId, studentName });
    }

    document.getElementById('add-student-search').value = '';
    document.getElementById('add-student-results').innerHTML = '';

    displayLessonStudents();
}

async function saveLessonChanges() {
    if (!currentLessonId) return;

    const amount = parseInt(document.getElementById('lesson-amount').value);
    if (!amount || amount <= 0) {
        tg.showPopup({ message: 'Введите корректную сумму', buttons: [{type: 'ok'}] });
        return;
    }

    try {
        const updateResponse = await authorizedFetch(`/api/lessons/${currentLessonId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                amount: amount,
                forceGroup: document.getElementById('lesson-force-group').checked,
                extraHalfHour: document.getElementById('lesson-extra-half-hour').checked
            })
        });

        if (!updateResponse.ok) throw new Error('Ошибка сохранения');

        for (const studentId of lessonStudentChanges.toRemove) {
            const removeResponse = await authorizedFetch(`/api/lessons/${currentLessonId}/student/${studentId}`, {
                method: 'DELETE'
            });
            if (!removeResponse.ok) {
                console.error(`Failed to remove student ${studentId}`);
            }
        }

        for (const {studentId} of lessonStudentChanges.toAdd) {
            const addResponse = await authorizedFetch(`/api/lessons/${currentLessonId}/student`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ studentId })
            });
            if (!addResponse.ok) {
                console.error(`Failed to add student ${studentId}`);
            }
        }

        await loadTodayData();
        closeLessonModal();
        tg.showPopup({ message: 'Изменения сохранены', buttons: [{type: 'ok'}] });
    } catch (error) {
        showError('Не удалось сохранить: ' + error.message);
    }
}

async function deleteLesson(lessonId) {
    if (!confirm('Вы уверены, что хотите удалить это занятие?')) return;

    try {
        const response = await authorizedFetch(`/api/lessons/${lessonId}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Ошибка удаления');

        await loadTodayData();
        tg.showPopup({ message: 'Занятие удалено', buttons: [{type: 'ok'}] });
    } catch (error) {
        showError('Не удалось удалить: ' + error.message);
    }
}

function openPaymentModal(paymentId) {
    currentPaymentId = paymentId;
    const payment = todayPayments.find(p => p.id === paymentId);
    if (!payment) return;

    document.getElementById('payment-modal-info').textContent =
        `${payment.studentName} - ${payment.subjectName} (${payment.dateTime})`;
    document.getElementById('payment-amount').value = payment.amount;

    const photoContainer = document.getElementById('payment-photo-container');
    const photoImg = document.getElementById('payment-photo');
    if (payment.photoReport) {
        photoImg.src = payment.photoReport;
        photoImg.onerror = () => {
            photoContainer.style.display = 'none';
        };
        photoContainer.style.display = 'block';
    } else {
        photoContainer.style.display = 'none';
    }

    document.getElementById('payment-modal').classList.add('active');
}

function closePaymentModal() {
    document.getElementById('payment-modal').classList.remove('active');
    currentPaymentId = null;
}

async function savePaymentChanges() {
    if (!currentPaymentId) return;

    const amount = parseInt(document.getElementById('payment-amount').value);
    if (!amount || amount <= 0) {
        tg.showPopup({ message: 'Введите корректную сумму', buttons: [{type: 'ok'}] });
        return;
    }

    try {
        const response = await authorizedFetch(`/api/payments/${currentPaymentId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ amount })
        });

        if (!response.ok) throw new Error('Ошибка сохранения');

        await loadTodayData();
        closePaymentModal();
        tg.showPopup({ message: 'Изменения сохранены', buttons: [{type: 'ok'}] });
    } catch (error) {
        showError('Не удалось сохранить: ' + error.message);
    }
}

async function deletePayment(paymentId) {
    if (!confirm('Вы уверены, что хотите удалить эту оплату?')) return;

    try {
        const response = await authorizedFetch(`/api/payments/${paymentId}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Ошибка удаления');

        await loadTodayData();
        tg.showPopup({ message: 'Оплата удалена', buttons: [{type: 'ok'}] });
    } catch (error) {
        showError('Не удалось удалить: ' + error.message);
    }
}

async function openAddLessonModal() {
    addLessonStudents = [];
    document.getElementById('add-lesson-students-list').innerHTML = '';
    document.getElementById('add-lesson-student-search').value = '';
    document.getElementById('add-lesson-student-results').innerHTML = '';
    document.getElementById('add-lesson-amount').value = '';
    document.getElementById('add-lesson-force-group').checked = false;
    document.getElementById('add-lesson-extra-half-hour').checked = false;

    const subjectSelect = document.getElementById('add-lesson-subject');
    subjectSelect.innerHTML = '<option value="">Выберите предмет</option>';

    subjectsData.forEach(subject => {
        const option = document.createElement('option');
        option.value = subject.id || subject.subjectId;
        option.textContent = subject.name || subject.subjectName;
        subjectSelect.appendChild(option);
    });

    if (subjectsData.length === 1) {
        subjectSelect.value = subjectsData[0].id || subjectsData[0].subjectId;
        subjectSelect.disabled = true;
    }

    const forceGroupContainer = document.getElementById('add-lesson-force-group').closest('.form-group');
    const extraHalfHourContainer = document.getElementById('add-lesson-extra-half-hour').closest('.form-group');

    if (userPermissions.includes('FORCE_GROUP')) {
        forceGroupContainer.style.display = 'block';
    } else {
        forceGroupContainer.style.display = 'none';
    }

    if (userPermissions.includes('EXTRA_HALF_HOUR')) {
        extraHalfHourContainer.style.display = 'block';
    } else {
        extraHalfHourContainer.style.display = 'none';
    }

    const searchInput = document.getElementById('add-lesson-student-search');
    const resultsContainer = document.getElementById('add-lesson-student-results');

    searchInput.oninput = searchStudentForNewLesson;

    const modal = document.getElementById('add-lesson-modal');
    modal.addEventListener('click', (e) => {
        if (e.target !== searchInput && !resultsContainer.contains(e.target)) {
            resultsContainer.innerHTML = '';
        }
    });

    document.getElementById('add-lesson-modal').classList.add('active');
}

function closeAddLessonModal() {
    document.getElementById('add-lesson-modal').classList.remove('active');
}

async function searchStudentForNewLesson(e) {
    const query = e.target.value.trim().toLowerCase();
    const resultsContainer = document.getElementById('add-lesson-student-results');

    if (query.length < 2) {
        resultsContainer.innerHTML = '';
        return;
    }

    try {
        const response = await authorizedFetch(`/api/student/search?q=${encodeURIComponent(query)}`);

        if (!response.ok) throw new Error('Ошибка поиска');

        const students = await response.json();

        if (students.length === 0) {
            resultsContainer.innerHTML = '<div style="padding: 10px; color: #718096;">Ничего не найдено</div>';
            return;
        }

        resultsContainer.innerHTML = students.map(student => `
            <div class="student-card" style="cursor: pointer; padding: 8px; border: 1px solid #e2e8f0; margin-bottom: 4px; border-radius: 8px;"
                 onclick="addStudentToNewLesson(${student.id}, '${student.secondName} ${student.name} ${student.lastName}')">
                <div style="font-weight: 500;">${student.secondName} ${student.name} ${student.lastName}</div>
                <div style="font-size: 12px; color: #718096;">Класс: ${student.schoolClass || 'не указан'}</div>
            </div>
        `).join('');
    } catch (error) {
        resultsContainer.innerHTML = '<div style="padding: 10px; color: #e53e3e;">Ошибка поиска</div>';
    }
}

function addStudentToNewLesson(studentId, studentName) {
    if (addLessonStudents.some(s => s.id === studentId)) {
        tg.showPopup({ message: 'Этот ученик уже добавлен', buttons: [{type: 'ok'}] });
        return;
    }

    addLessonStudents.push({ id: studentId, name: studentName });

    document.getElementById('add-lesson-student-search').value = '';
    document.getElementById('add-lesson-student-results').innerHTML = '';

    displayAddLessonStudents();
}

function removeStudentFromNewLesson(studentId) {
    addLessonStudents = addLessonStudents.filter(s => s.id !== studentId);
    displayAddLessonStudents();
}

function displayAddLessonStudents() {
    const container = document.getElementById('add-lesson-students-list');

    if (addLessonStudents.length === 0) {
        container.innerHTML = '<div style="color: #718096; font-size: 14px;">Добавьте учеников...</div>';
        return;
    }

    container.innerHTML = addLessonStudents.map(student => `
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 8px; background: #f7fafc; border-radius: 8px; margin-bottom: 4px;">
            <span style="font-weight: 500;">${student.name}</span>
            <button onclick="removeStudentFromNewLesson(${student.id})"
                    style="padding: 4px 8px; background: #fc8181; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px;">
                Удалить
            </button>
        </div>
    `).join('');
}

async function saveNewLesson() {
    const saveBtn = document.getElementById('save-new-lesson-btn');

    if (saveBtn.disabled) return;

    const subjectId = document.getElementById('add-lesson-subject').value;
    const amount = parseInt(document.getElementById('add-lesson-amount').value);
    const photoFile = document.getElementById('add-lesson-photo').files[0];

    if (!subjectId) {
        tg.showPopup({ message: 'Выберите предмет', buttons: [{type: 'ok'}] });
        return;
    }

    if (addLessonStudents.length === 0) {
        tg.showPopup({ message: 'Добавьте хотя бы одного ученика', buttons: [{type: 'ok'}] });
        return;
    }

    if (!amount || amount <= 0) {
        tg.showPopup({ message: 'Введите корректную сумму', buttons: [{type: 'ok'}] });
        return;
    }

    if (!photoFile) {
        tg.showPopup({ message: 'Загрузите фото отчет', buttons: [{type: 'ok'}] });
        return;
    }

    saveBtn.disabled = true;
    saveBtn.textContent = 'Сохранение...';

    const progressContainer = document.getElementById('add-lesson-progress');
    const progressBar = document.getElementById('add-lesson-progress-bar');
    const progressText = document.getElementById('add-lesson-progress-text');
    const progressLabel = progressContainer.querySelector('.progress-label');

    progressContainer.classList.remove('hidden');
    progressBar.style.width = '0%';
    progressText.textContent = '0%';
    progressLabel.textContent = 'Загрузка изображения...';

    try {
        const formData = new FormData();
        formData.append('file', photoFile);

        const path = await new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();

            xhr.upload.addEventListener('progress', (e) => {
                if (e.lengthComputable) {
                    const percentComplete = Math.round((e.loaded / e.total) * 90);
                    progressBar.style.width = percentComplete + '%';
                    progressText.textContent = percentComplete + '%';
                }
            });

            xhr.addEventListener('load', () => {
                if (xhr.status === 200) {
                    try {
                        const response = JSON.parse(xhr.responseText);
                        progressBar.style.width = '90%';
                        progressText.textContent = '90%';
                        resolve(response.path);
                    } catch (e) {
                        reject(new Error('Ошибка парсинга ответа'));
                    }
                } else {
                    reject(new Error('Ошибка загрузки фото'));
                }
            });

            xhr.addEventListener('error', () => {
                reject(new Error('Ошибка загрузки фото'));
            });

            xhr.open('POST', '/api/image/upload');
            Object.entries(getAuthHeaders()).forEach(([key, value]) => {
                xhr.setRequestHeader(key, value);
            });
            xhr.send(formData);
        });

        progressLabel.textContent = 'Создание занятия...';

        const response = await authorizedFetch('/api/lessons', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                subjectId: parseInt(subjectId),
                studentIds: addLessonStudents.map(s => s.id),
                amount: amount,
                forceGroup: document.getElementById('add-lesson-force-group').checked,
                extraHalfHour: document.getElementById('add-lesson-extra-half-hour').checked,
                photoReport: path
            })
        });

        if (!response.ok) throw new Error('Ошибка создания');

        progressBar.style.width = '95%';
        progressText.textContent = '95%';
        progressLabel.textContent = 'Обновление данных...';

        await loadTodayData();

        progressBar.style.width = '100%';
        progressText.textContent = '100%';
        progressLabel.textContent = 'Готово!';

        await new Promise(resolve => setTimeout(resolve, 300));

        closeAddLessonModal();
        tg.showPopup({ message: 'Занятие создано', buttons: [{type: 'ok'}] });
    } catch (error) {
        showError('Не удалось создать: ' + error.message);
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = 'Создать';
        progressContainer.classList.add('hidden');
    }
}

async function openAddPaymentModal() {
    selectedPaymentStudent = null;
    document.getElementById('add-payment-student-search').value = '';
    document.getElementById('add-payment-student-results').innerHTML = '';
    document.getElementById('add-payment-selected-student').innerHTML = '';
    document.getElementById('add-payment-amount').value = '';

    const subjectSelect = document.getElementById('add-payment-subject');
    subjectSelect.innerHTML = '<option value="">Выберите предмет</option>';
    subjectsData.forEach(subject => {
        const option = document.createElement('option');
        option.value = subject.id || subject.subjectId;
        option.textContent = subject.name || subject.subjectName;
        subjectSelect.appendChild(option);
    });

    if (subjectsData.length === 1) {
        subjectSelect.value = subjectsData[0].id || subjectsData[0].subjectId;
        subjectSelect.disabled = true;
    }

    const searchInput = document.getElementById('add-payment-student-search');
    const resultsContainer = document.getElementById('add-payment-student-results');

    searchInput.oninput = searchStudentForNewPayment;

    const modal = document.getElementById('add-payment-modal');
    modal.addEventListener('click', (e) => {
        if (e.target !== searchInput && !resultsContainer.contains(e.target)) {
            resultsContainer.innerHTML = '';
        }
    });

    document.getElementById('add-payment-modal').classList.add('active');
}

function closeAddPaymentModal() {
    document.getElementById('add-payment-modal').classList.remove('active');
}

async function searchStudentForNewPayment(e) {
    const query = e.target.value.trim().toLowerCase();
    const resultsContainer = document.getElementById('add-payment-student-results');

    if (query.length < 2) {
        resultsContainer.innerHTML = '';
        return;
    }

    try {
        const response = await authorizedFetch(`/api/student/search?q=${encodeURIComponent(query)}`);

        if (!response.ok) throw new Error('Ошибка поиска');

        const students = await response.json();

        if (students.length === 0) {
            resultsContainer.innerHTML = '<div style="padding: 10px; color: #718096;">Ничего не найдено</div>';
            return;
        }

        resultsContainer.innerHTML = students.map(student => `
            <div class="student-card" style="cursor: pointer; padding: 8px; border: 1px solid #e2e8f0; margin-bottom: 4px; border-radius: 8px;"
                 onclick="selectStudentForNewPayment(${student.id}, '${student.secondName} ${student.name} ${student.lastName}')">
                <div style="font-weight: 500;">${student.secondName} ${student.name} ${student.lastName}</div>
                <div style="font-size: 12px; color: #718096;">Класс: ${student.schoolClass || 'не указан'}</div>
            </div>
        `).join('');
    } catch (error) {
        resultsContainer.innerHTML = '<div style="padding: 10px; color: #e53e3e;">Ошибка поиска</div>';
    }
}

function selectStudentForNewPayment(studentId, studentName) {
    selectedPaymentStudent = { id: studentId, name: studentName };

    document.getElementById('add-payment-student-search').value = '';
    document.getElementById('add-payment-student-results').innerHTML = '';
    document.getElementById('add-payment-selected-student').innerHTML = `
        <div style="padding: 8px; background: #e6fffa; border: 1px solid #81e6d9; border-radius: 8px;">
            <strong>Выбран:</strong> ${studentName}
        </div>
    `;
}

async function saveNewPayment() {
    const saveBtn = document.getElementById('save-new-payment-btn');

    if (saveBtn.disabled) return;

    const subjectId = document.getElementById('add-payment-subject').value;
    const amount = parseInt(document.getElementById('add-payment-amount').value);
    const photoFile = document.getElementById('add-payment-photo').files[0];

    if (!subjectId) {
        tg.showPopup({ message: 'Выберите предмет', buttons: [{type: 'ok'}] });
        return;
    }

    if (!selectedPaymentStudent) {
        tg.showPopup({ message: 'Выберите ученика', buttons: [{type: 'ok'}] });
        return;
    }

    if (!amount || amount <= 0) {
        tg.showPopup({ message: 'Введите корректную сумму', buttons: [{type: 'ok'}] });
        return;
    }

    if (!photoFile) {
        tg.showPopup({ message: 'Загрузите фото отчет', buttons: [{type: 'ok'}] });
        return;
    }

    saveBtn.disabled = true;
    saveBtn.textContent = 'Сохранение...';

    const progressContainer = document.getElementById('add-payment-progress');
    const progressBar = document.getElementById('add-payment-progress-bar');
    const progressText = document.getElementById('add-payment-progress-text');
    const progressLabel = progressContainer.querySelector('.progress-label');

    progressContainer.classList.remove('hidden');
    progressBar.style.width = '0%';
    progressText.textContent = '0%';
    progressLabel.textContent = 'Загрузка изображения...';

    try {
        const formData = new FormData();
        formData.append('file', photoFile);

        const path = await new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();

            xhr.upload.addEventListener('progress', (e) => {
                if (e.lengthComputable) {
                    const percentComplete = Math.round((e.loaded / e.total) * 90);
                    progressBar.style.width = percentComplete + '%';
                    progressText.textContent = percentComplete + '%';
                }
            });

            xhr.addEventListener('load', () => {
                if (xhr.status === 200) {
                    try {
                        const response = JSON.parse(xhr.responseText);
                        progressBar.style.width = '90%';
                        progressText.textContent = '90%';
                        resolve(response.path);
                    } catch (e) {
                        reject(new Error('Ошибка парсинга ответа'));
                    }
                } else {
                    reject(new Error('Ошибка загрузки фото'));
                }
            });

            xhr.addEventListener('error', () => {
                reject(new Error('Ошибка загрузки фото'));
            });

            xhr.open('POST', '/api/image/upload');
            Object.entries(getAuthHeaders()).forEach(([key, value]) => {
                xhr.setRequestHeader(key, value);
            });
            xhr.send(formData);
        });

        progressLabel.textContent = 'Создание оплаты...';

        const response = await authorizedFetch('/api/payments', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                subjectId: parseInt(subjectId),
                studentId: selectedPaymentStudent.id,
                amount: amount,
                photoReport: path
            })
        });

        if (!response.ok) throw new Error('Ошибка создания');

        progressBar.style.width = '95%';
        progressText.textContent = '95%';
        progressLabel.textContent = 'Обновление данных...';

        await loadTodayData();

        progressBar.style.width = '100%';
        progressText.textContent = '100%';
        progressLabel.textContent = 'Готово!';

        await new Promise(resolve => setTimeout(resolve, 300));

        closeAddPaymentModal();
        tg.showPopup({ message: 'Оплата создана', buttons: [{type: 'ok'}] });
    } catch (error) {
        showError('Не удалось создать: ' + error.message);
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = 'Создать';
        progressContainer.classList.add('hidden');
    }
}
