// REPORTS PAGE - Monthly reports by subject

// State variables
let currentReport = null;
let currentReportPeriod = null;
let currentReportSubjectId = null;

// Load and display report
async function loadReport() {
    const subjectId = document.getElementById('reports-subject').value;
    const period = document.getElementById('reports-period').value;

    if (!subjectId || !period) {
        document.getElementById('reports-container').classList.add('hidden');
        document.getElementById('reports-no-data').classList.remove('hidden');
        return;
    }

    try {
        let url = `/api/report/${subjectId}/${period}`;
        if (isAdmin) {
            const tutorId = document.getElementById('reports-tutor-select').value;
            if (tutorId) url += `?tutorId=${tutorId}`;
        }

        const response = await fetch(url, {
            headers: { 'Authorization': `tma ${tg.initData}` }
        });

        if (!response.ok) throw new Error('Ошибка загрузки отчета');

        currentReport = await response.json();
        currentReportPeriod = period;
        currentReportSubjectId = subjectId;

        if (currentReport.students.length === 0) {
            document.getElementById('reports-container').classList.add('hidden');
            document.getElementById('reports-no-data').classList.remove('hidden');
        } else {
            displayReport();
            document.getElementById('reports-container').classList.remove('hidden');
            document.getElementById('reports-no-data').classList.add('hidden');
        }
    } catch (error) {
        showError('Не удалось загрузить отчет: ' + error.message);
    }
}

function displayReport() {
    const months = {
        'JANUARY': 'Январь', 'FEBRUARY': 'Февраль', 'MARCH': 'Март',
        'APRIL': 'Апрель', 'MAY': 'Май', 'JUNE': 'Июнь',
        'JULY': 'Июль', 'AUGUST': 'Август', 'SEPTEMBER': 'Сентябрь',
        'OCTOBER': 'Октябрь', 'NOVEMBER': 'Ноябрь', 'DECEMBER': 'Декабрь'
    };

    document.getElementById('reports-title').textContent =
        `${currentReport.tutorName} - ${currentReport.subjectName} - ${months[currentReport.month]} ${currentReport.year}`;

    document.getElementById('reports-total-individual').textContent = currentReport.totalIndividual;
    document.getElementById('reports-total-group').textContent = currentReport.totalGroup;
    document.getElementById('reports-total-payments').textContent = currentReport.totalPayments;

    // Students - Cards instead of table
    const studentsTbody = document.getElementById('reports-students-tbody');
    studentsTbody.innerHTML = currentReport.students.map((student, index) => `
        <div class="student-card" onclick="openStudentDetailsModal(${student.studentId}, '${student.fio}')"
             style="background: #f7fafc; padding: 12px; border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <span style="font-weight: 600; color: #2d3748;">${index + 1}. ${student.fio}</span>
                    <span style="color: #718096; font-size: 14px; margin-left: 8px;">Класс: ${student.schoolClass}</span>
                </div>
                <div style="text-align: right;">
                    <div style="font-size: 14px; color: #4a5568;">
                        Инд: ${student.individual} | Групп: ${student.group}
                    </div>
                    <div style="font-weight: 600; color: #667eea;">${student.payment} ₽</div>
                </div>
            </div>
        </div>
    `).join('');

    // Lessons - Cards
    const lessonsTbody = document.getElementById('reports-lessons-tbody');
    lessonsTbody.innerHTML = currentReport.lessons.map(lesson => {
        const details = [
            lesson.forceGroup ? 'Групп.' : '',
            lesson.extraHalfHour ? '1.5 часа' : ''
        ].filter(Boolean).join(' • ');

        return `
            <div style="background: #f7fafc; padding: 12px; border-radius: 8px; margin-bottom: 8px;">
                <div style="display: flex; justify-content: space-between; align-items: start;">
                    <div>
                        <div style="font-weight: 600; color: #2d3748;">${lesson.dateTime}</div>
                        <div style="color: #4a5568; font-size: 14px; margin-top: 4px;">${lesson.students.join(', ')}</div>
                        ${details ? `<div style="color: #718096; font-size: 13px; margin-top: 2px;">${details}</div>` : ''}
                    </div>
                    <div style="font-weight: 600; color: #667eea; white-space: nowrap;">${lesson.amount} ₽</div>
                </div>
            </div>
        `;
    }).join('');

    // Payments - Cards
    const paymentsTbody = document.getElementById('reports-payments-tbody');
    paymentsTbody.innerHTML = currentReport.payments.map(payment => `
        <div style="background: #f7fafc; padding: 12px; border-radius: 8px; margin-bottom: 8px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <div style="font-weight: 600; color: #2d3748;">${payment.dateTime}</div>
                    <div style="color: #4a5568; font-size: 14px; margin-top: 4px;">${payment.studentFio}</div>
                </div>
                <div style="font-weight: 600; color: #48bb78;">${payment.amount} ₽</div>
            </div>
        </div>
    `).join('');
}

// STUDENT DETAILS MODAL
async function openStudentDetailsModal(studentId, studentName) {
    try {
        const period = currentReportPeriod || document.getElementById('reports-period').value;

        // Validate period format (must be YYYY-MM)
        if (!period || !period.match(/^\d{4}-\d{2}$/)) {
            showError('Период отчета некорректен. Пожалуйста, выберите период из списка.');
            return;
        }

        const subjectId = currentReportSubjectId || document.getElementById('reports-subject').value;

        console.log('Opening student details:', studentId, 'period:', period, 'subjectId:', subjectId);

        let url = `/api/student/${studentId}/details?period=${period}`;
        if (subjectId) {
            url += `&subjectId=${subjectId}`;
        }

        const response = await fetch(url, {
            headers: {
                'Authorization': `tma ${tg.initData}`
            }
        });

        if (!response.ok) throw new Error('Ошибка загрузки данных');

        const data = await response.json();

        document.getElementById('student-details-title').textContent = studentName;

        // Display lessons
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

        // Setup tabs
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
