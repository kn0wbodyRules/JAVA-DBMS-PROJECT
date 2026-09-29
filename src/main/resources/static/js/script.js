// Confirm before deleting any record
function confirmDelete(event) {
    const ok = confirm("Are you sure you want to delete this record?");
    if (!ok) {
        event.preventDefault();
    }
    return ok;
}

function gradeFor(score) {
    if (score >= 8) return "Excellent";
    if (score >= 5) return "Fair";
    return "Poor";
}

// Live readout next to the 0-10 segregation score slider
function setupScorePicker(picker) {
    const range = picker.querySelector("input[type=range]");
    const readout = picker.querySelector(".score-readout");
    if (!range || !readout) return;

    const update = function () {
        const score = parseInt(range.value, 10);
        const grade = gradeFor(score);
        readout.textContent = score + "/10 · " + grade;
        readout.className = "score-readout grade-" + grade.toLowerCase();
    };
    range.addEventListener("input", update);
    update();
}

// Ticking "wet and dry mixed" caps the suggested score at 4, since mixed waste cannot be processed
function setupViolationHints(form) {
    const range = form.querySelector("input[type=range]");
    if (!range) return;
    form.querySelectorAll("input[data-cap]").forEach(function (box) {
        box.addEventListener("change", function () {
            const cap = parseInt(box.dataset.cap, 10);
            if (box.checked && parseInt(range.value, 10) > cap) {
                range.value = cap;
                range.dispatchEvent(new Event("input"));
            }
        });
    });
}

// Pickup end time must come after its start time
function validateTimes(form) {
    const start = form.querySelector("input[name=startTime]");
    const end = form.querySelector("input[name=endTime]");
    if (start && end && start.value && end.value && end.value <= start.value) {
        alert("End time must be after start time.");
        return false;
    }
    return true;
}

// Colour the bars and score pills from their data attributes
function paintScores() {
    document.querySelectorAll("[data-score]").forEach(function (el) {
        const raw = el.dataset.score;
        if (raw === "" || raw === undefined) return;
        const score = parseFloat(raw);
        el.classList.add("grade-" + gradeFor(score).toLowerCase());
    });
    document.querySelectorAll(".bar > span[data-width]").forEach(function (bar) {
        const pct = Math.max(0, Math.min(100, parseFloat(bar.dataset.width) || 0));
        bar.style.width = pct + "%";
        if (bar.dataset.tone === "score") {
            const score = pct / 10;
            bar.style.background = score >= 8 ? "var(--good)" : score >= 5 ? "var(--fair)" : "var(--poor)";
        }
    });
    document.querySelectorAll(".spark span[data-value]").forEach(function (col) {
        const score = parseFloat(col.dataset.value);
        col.style.height = Math.max(4, score * 10) + "%";
        col.style.background = score >= 8 ? "var(--good)" : score >= 5 ? "var(--fair)" : "var(--poor)";
    });
}

// Auto-hide flash messages after a few seconds
function fadeAlerts() {
    document.querySelectorAll(".alert-success").forEach(function (alert) {
        setTimeout(function () {
            alert.style.transition = "opacity 0.4s";
            alert.style.opacity = "0";
            setTimeout(function () { alert.remove(); }, 400);
        }, 4000);
    });
}

document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("a.delete-link").forEach(function (link) {
        link.addEventListener("click", confirmDelete);
    });

    document.querySelectorAll(".score-picker").forEach(setupScorePicker);
    document.querySelectorAll("form.score-form").forEach(setupViolationHints);

    document.querySelectorAll("form.validate-times").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!validateTimes(form)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("select.auto-submit").forEach(function (select) {
        select.addEventListener("change", function () {
            select.form.submit();
        });
    });

    paintScores();
    fadeAlerts();
});
