function todayValue() {
    var now = new Date();
    return now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0');
}

function currentTimeValue() {
    var now = new Date();
    return String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
}

function timeToMinutes(value) {
    var parts = value.split(':');
    return Number(parts[0]) * 60 + Number(parts[1]);
}

function updateTimeLimit(dateInput) {
    var container = dateInput.closest('form');
    var timeInput = container ? container.querySelector('.reservation-time-input') : null;
    var durationInput = container ? container.querySelector('.reservation-duration-input') : null;
    var durationHours = durationInput ? Number(durationInput.value || 1) : 1;
    var closingMinutes = 19 * 60;

    if (!timeInput) {
        return;
    }

    Array.prototype.forEach.call(timeInput.options, function (option) {
        if (!option.value) {
            return;
        }

        var isPastToday = dateInput.value === todayValue() && option.value < currentTimeValue();
        var exceedsClosing = timeToMinutes(option.value) + (durationHours * 60) > closingMinutes;
        option.disabled = isPastToday || exceedsClosing;
    });

    if (timeInput.selectedOptions.length && timeInput.selectedOptions[0].disabled) {
        timeInput.value = '';
    }
}

function initReservationTimeLimits() {
    document.querySelectorAll('.reservation-date-input').forEach(function (dateInput) {
        dateInput.min = todayValue();
        updateTimeLimit(dateInput);
        dateInput.addEventListener('change', function () {
            updateTimeLimit(dateInput);
        });
    });

    document.querySelectorAll('.reservation-duration-input').forEach(function (durationInput) {
        durationInput.addEventListener('change', function () {
            var form = durationInput.closest('form');
            var dateInput = form ? form.querySelector('.reservation-date-input') : null;
            if (dateInput) {
                updateTimeLimit(dateInput);
            }
        });
    });
}

function initBookSelectionLimit() {
    document.querySelectorAll('.reservation-selection-form').forEach(function (form) {
        var checkboxes = form.querySelectorAll('.book-select-input');
        var count = form.querySelector('.selected-books-count');
        var submitButton = form.querySelector('.reserve-selected-button');

        if (!count || !submitButton) {
            return;
        }

        function updateSelectedBooks() {
            var selected = form.querySelectorAll('.book-select-input:checked').length;
            count.textContent = selected;
            submitButton.disabled = selected === 0;

            checkboxes.forEach(function (checkbox) {
                checkbox.disabled = !checkbox.checked && selected >= 3;
            });
        }

        checkboxes.forEach(function (checkbox) {
            checkbox.addEventListener('change', updateSelectedBooks);
        });
        updateSelectedBooks();
    });
}

function initShowMoreBooks() {
    var button = document.getElementById('showMoreBooks');
    if (!button) {
        return;
    }

    button.addEventListener('click', function () {
        document.querySelectorAll('.more-book-item').forEach(function (item) {
            item.classList.remove('d-none');
            item.classList.remove('more-book-item');
        });
        button.closest('.text-center').classList.add('d-none');
    });
}

function initSanctionDays() {
    document.querySelectorAll('.sanction-type-input').forEach(function (typeInput) {
        function updateDaysField() {
            var form = typeInput.closest('form');
            var daysGroup = form ? form.querySelector('.sanction-days-group') : null;
            var daysInput = form ? form.querySelector('.sanction-days-input') : null;
            var isTemporal = typeInput.value === 'SUSPENSION_TEMPORAL';

            if (daysGroup) {
                daysGroup.classList.toggle('d-none', !isTemporal);
            }
            if (daysInput) {
                daysInput.required = isTemporal;
                daysInput.disabled = !isTemporal;
                daysInput.value = isTemporal ? (daysInput.value || '3') : '';
            }
        }

        typeInput.addEventListener('change', updateDaysField);
        updateDaysField();
    });
}

function moveProfileModalToBody() {
    var modal = document.getElementById('perfilModal');

    if (modal && modal.parentElement !== document.body) {
        document.body.appendChild(modal);
    }
}

function initLibrarySearchScroll() {
    var storageKey = 'librarySearchScrollY';

    if (window.location.pathname.endsWith('/libros')) {
        var scrollY = window.sessionStorage.getItem(storageKey);
        if (scrollY !== null) {
            window.sessionStorage.removeItem(storageKey);
            window.scrollTo(0, Number(scrollY));
        }
    }

    document.querySelectorAll('.library-search-form').forEach(function (form) {
        form.addEventListener('submit', function () {
            window.sessionStorage.setItem(storageKey, String(window.scrollY));
        });
    });
}

function initLibraryScripts() {
    initShowMoreBooks();
    initReservationTimeLimits();
    initBookSelectionLimit();
    initSanctionDays();
    initLibrarySearchScroll();
    moveProfileModalToBody();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initLibraryScripts);
} else {
    initLibraryScripts();
}
