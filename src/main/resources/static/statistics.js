// STATISTICS PAGE - Aggregated statistics with comparison

// State variables
let currentStats = null;

// Load and display statistics
async function loadStatistics() {
    const period = document.getElementById('stats-period').value;

    if (!period) return;

    try {
        let url = `/api/report/aggregated/${period}`;
        if (isAdmin) {
            const tutorId = document.getElementById('stats-tutor-select').value;
            if (tutorId) url += `?tutorId=${tutorId}`;
        }

        const response = await fetch(url, {
            headers: { 'Authorization': `tma ${tg.initData}` }
        });

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
    const current = currentStats.period;
    const previous = currentStats.previousPeriod;
    const comp = currentStats.comparison;

    container.innerHTML = `
        <div class="card">
            <h2>${months[current.month]} ${current.year}</h2>
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
    `;
}
