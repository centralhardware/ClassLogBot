let currentReport = null;
let currentReportPeriod = null;
let currentReportSubjectId = null;

async function loadReport() {
    const subjectId = document.getElementById('reports-subject').value;
    const period = document.getElementById('reports-period').value;

    if (!subjectId || !period || subjectId === '' || period === '') {
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

        const response = await authorizedFetch(url);

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

async function displayReport() {
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

    const studentsTbody = document.getElementById('reports-students-tbody');
    studentsTbody.innerHTML = '';

    const studentsFragment = TemplateUtils.createMany('template-report-student-card', currentReport.students, (el, student, index) => {
        TemplateUtils.setData(el, null, 'studentId', student.studentId);
        TemplateUtils.setText(el, '.student-index', `${index + 1}. ${student.fio}`);
        TemplateUtils.setText(el, '.student-class', `Класс: ${student.schoolClass}`);
        TemplateUtils.setText(el, '.student-stats', `Инд: ${student.individual} | Групп: ${student.group}`);
        TemplateUtils.setText(el, '.student-payment', `${student.payment} ₽`);
        el.addEventListener('click', () => openStudentDetailsModal(student.studentId, student.fio));
    });

    studentsTbody.appendChild(studentsFragment);

    const lessonsTbody = document.getElementById('reports-lessons-tbody');
    lessonsTbody.innerHTML = '';
    const lessonsFragment = TemplateUtils.createMany('template-report-lesson-card', currentReport.lessons, (el, lesson) => {
        const details = [
            lesson.forceGroup ? 'Групп.' : '',
            lesson.extraHalfHour ? '1.5 часа' : ''
        ].filter(Boolean).join(' • ');

        TemplateUtils.setText(el, '.lesson-datetime', lesson.dateTime);
        TemplateUtils.setText(el, '.lesson-students', lesson.students.join(', '));
        TemplateUtils.setText(el, '.lesson-details', details);
        TemplateUtils.setText(el, '.lesson-amount', `${lesson.amount} ₽`);
    });
    lessonsTbody.appendChild(lessonsFragment);

    const paymentsTbody = document.getElementById('reports-payments-tbody');
    paymentsTbody.innerHTML = '';
    const paymentsFragment = TemplateUtils.createMany('template-report-payment-card', currentReport.payments, (el, payment) => {
        TemplateUtils.setText(el, '.payment-datetime', payment.dateTime);
        TemplateUtils.setText(el, '.payment-student', payment.studentFio);
        TemplateUtils.setText(el, '.payment-amount', `${payment.amount} ₽`);
    });
    paymentsTbody.appendChild(paymentsFragment);
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
            lessonsContainer.innerHTML = '';
            const lessonsFragment = TemplateUtils.createMany('template-student-detail-lesson', data.lessons, (el, lesson) => {
                const details = [
                    lesson.forceGroup ? 'Групп.' : '',
                    lesson.extraHalfHour ? '1.5 часа' : ''
                ].filter(Boolean).join(' • ');

                TemplateUtils.setText(el, '.lesson-datetime', lesson.dateTime);
                TemplateUtils.setText(el, '.lesson-subject', lesson.subjectName);
                TemplateUtils.setText(el, '.lesson-details', details);
                TemplateUtils.setText(el, '.lesson-amount', `${lesson.amount} ₽`);
            });
            lessonsContainer.appendChild(lessonsFragment);
        }

        const paymentsContainer = document.getElementById('student-payments-list');
        if (data.payments.length === 0) {
            paymentsContainer.innerHTML = '<div class="no-data">Оплат не найдено</div>';
        } else {
            paymentsContainer.innerHTML = '';
            const paymentsFragment = TemplateUtils.createMany('template-student-detail-payment', data.payments, (el, payment) => {
                TemplateUtils.setText(el, '.payment-datetime', payment.dateTime);
                TemplateUtils.setText(el, '.payment-amount', `${payment.amount} ₽`);
            });
            paymentsContainer.appendChild(paymentsFragment);
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
