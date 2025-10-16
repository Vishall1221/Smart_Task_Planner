const form = document.getElementById("goalForm");
const goalInput = document.getElementById("goalInput");
const statusBox = document.getElementById("status");
const resultCard = document.getElementById("result");
const tasksBody = document.getElementById("tasksBody");
const downloadBtn = document.getElementById("downloadBtn");

let lastPlanJson = null;

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const goal = goalInput.value.trim();
  if (!goal) return;

  clearResult();
  setStatus("Generating plan…", "info");

  try {
    const res = await fetch("/api/plan", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ goal }),
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Server ${res.status}: ${text}`);
    }

    const data = await res.json();
    lastPlanJson = data;

    renderPlan(data);
    setStatus("Plan generated successfully ✅", "success");
  } catch (err) {
    console.error(err);
    setStatus(`Error: ${err.message}`, "error");
  }
});

downloadBtn.addEventListener("click", () => {
  if (!lastPlanJson) return;
  const blob = new Blob([JSON.stringify(lastPlanJson, null, 2)], {
    type: "application/json",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `plan-${lastPlanJson.id || "latest"}.json`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
});

function renderPlan(plan) {
  if (!plan || !Array.isArray(plan.tasks)) {
    setStatus("No tasks returned.", "error");
    return;
  }
  tasksBody.innerHTML = "";
  plan.tasks.forEach((t, idx) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${idx + 1}</td>
      <td>${escapeHtml(t.description || "")}</td>
      <td>${escapeHtml(t.duration || "")}</td>
      <td>${escapeHtml(t.dependencies || "")}</td>
    `;
    tasksBody.appendChild(tr);
  });
  resultCard.classList.remove("hidden");
}

function clearResult() {
  tasksBody.innerHTML = "";
  resultCard.classList.add("hidden");
  setStatus("", "idle");
}

function setStatus(msg, type) {
  statusBox.textContent = msg;
  statusBox.className = `status ${type}`;
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}
