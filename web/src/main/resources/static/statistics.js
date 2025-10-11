// STATISTICS PAGE - Aggregated statistics with comparison

// State variables
let currentStats = null;

// Load and display statistics
async function loadStatistics() {
    const subjectId = document.getElementById('stats-subject').value;
    const period = document.getElementById('stats-period').value;

    if (!period || period === '') {
        document.getElementById('stats-container').innerHTML = '';
        return;
    }

    try {
        let url;
        // If "all" is selected or subject is empty, use aggregated endpoint
        if (!subjectId || subjectId === 'all') {
            url = `/api/report/aggregated/${period}`;
        } else {
            // Otherwise use subject-specific endpoint
            url = `/api/report/${subjectId}/aggregated/${period}`;
        }

        if (isAdmin) {
            const tutorId = document.getElementById('stats-tutor-select').value;
            if (tutorId) url += `?tutorId=${tutorId}`;
        }

        const response = await authorizedFetch(url);

        if (!response.ok) throw new Error('Ошибка загрузки статистики');

        currentStats = await response.json();
        displayStatistics();
    } catch (error) {
        showError('Не удалось загрузить статистику: ' + error.message);
    }
}

function displayStatistics() {
    const months = {
        'JANUARY': 'Январь', 'FEBRUARY': 'Февраль', 'MARCH': 'Март',
        'APRIL': 'Апрель', 'MAY': 'Май', 'JUNE': 'Июнь',
        'JULY': 'Июль', 'AUGUST': 'Август', 'SEPTEMBER': 'Сентябрь',
        'OCTOBER': 'Октябрь', 'NOVEMBER': 'Ноябрь', 'DECEMBER': 'Декабрь'
    };

    const container = document.getElementById('stats-container');
    const selectedSubject = document.getElementById('stats-subject').value;

    const current = currentStats.period;
    const previous = currentStats.previousPeriod;
    const comp = currentStats.comparison;

    // Subject name for title (if specific subject is selected)
    const subjectTitle = (selectedSubject && selectedSubject !== 'all' && current.subjectStats.length > 0)
        ? ' - ' + current.subjectStats[0].subjectName
        : '';

    let subjectBreakdownHtml = '';
    if (selectedSubject === 'all' && current.subjectStats.length > 0) {
        subjectBreakdownHtml = `
            <div class="card">
                <h2>По предметам</h2>
                <div style="display: grid; gap: 12px;">
                    ${current.subjectStats.map(subject => `
                        <div style="background: #f7fafc; padding: 12px; border-radius: 8px;">
                            <div style="font-weight: 600; color: #2d3748; margin-bottom: 8px;">${subject.subjectName}</div>
                            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(100px, 1fr)); gap: 8px; font-size: 14px;">
                                <div>
                                    <div style="color: #718096;">Занятий</div>
                                    <div style="font-weight: 600; color: #4a5568;">${subject.lessons}</div>
                                </div>
                                <div>
                                    <div style="color: #718096;">Инд/Групп</div>
                                    <div style="font-weight: 600; color: #4a5568;">${subject.individual}/${subject.group}</div>
                                </div>
                                <div>
                                    <div style="color: #718096;">Оплачено</div>
                                    <div style="font-weight: 600; color: #667eea;">${subject.payments} ₽</div>
                                </div>
                                <div>
                                    <div style="color: #718096;">Учеников</div>
                                    <div style="font-weight: 600; color: #4a5568;">${subject.students}</div>
                                </div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    container.innerHTML = `
        <div class="card">
            <h2>${months[current.month]} ${current.year}${subjectTitle}</h2>
            <div class="summary">
                <div class="summary-item">
                    <div class="value">${current.totalLessons}</div>
                    <div class="label">Занятий</div>
                </div>
                <div class="summary-item">
                    <div class="value">${current.totalIndividual}</div>
                    <div class="label">Индивидуальных</div>
                </div>
                <div class="summary-item">
                    <div class="value">${current.totalGroup}</div>
                    <div class="label">Групповых</div>
                </div>
                <div class="summary-item">
                    <div class="value">${current.totalPayments}</div>
                    <div class="label">Оплачено (₽)</div>
                </div>
                <div class="summary-item">
                    <div class="value">${current.totalStudents}</div>
                    <div class="label">Учеников</div>
                </div>
            </div>
        </div>

        <div class="card">
            <h2>Сравнение с ${months[previous.month]} ${previous.year}</h2>
            <div class="summary">
                <div class="summary-item" style="background: ${comp.lessonsChange >= 0 ? '#10b981' : '#ef4444'}">
                    <div class="value">${comp.lessonsChange >= 0 ? '+' : ''}${comp.lessonsChange}</div>
                    <div class="label">Занятий (${comp.lessonsChangePercent.toFixed(1)}%)</div>
                </div>
                <div class="summary-item" style="background: ${comp.paymentsChange >= 0 ? '#10b981' : '#ef4444'}">
                    <div class="value">${comp.paymentsChange >= 0 ? '+' : ''}${comp.paymentsChange}</div>
                    <div class="label">Оплат (${comp.paymentsChangePercent.toFixed(1)}%)</div>
                </div>
                <div class="summary-item" style="background: ${comp.studentsChange >= 0 ? '#10b981' : '#ef4444'}">
                    <div class="value">${comp.studentsChange >= 0 ? '+' : ''}${comp.studentsChange}</div>
                    <div class="label">Учеников (${comp.studentsChangePercent.toFixed(1)}%)</div>
                </div>
            </div>
        </div>

        ${subjectBreakdownHtml}
    `;
}
