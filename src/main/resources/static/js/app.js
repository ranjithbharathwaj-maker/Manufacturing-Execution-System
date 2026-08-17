// App State
let currentUser = null;
let dashboardData = {};
let orderChart = null;
let defectChart = null;
let dailyChart = null;
let machineChart = null;

const API_BASE = "/api";

// Page Loads
document.addEventListener("DOMContentLoaded", () => {
    initClock();
    checkCachedAuth();
    setupEventListeners();
});

// Clock widget
function initClock() {
    setInterval(() => {
        const now = new Date();
        document.getElementById("live-clock").textContent = now.toTimeString().split(' ')[0];
    }, 1000);
}

// Check if logged in previously
function checkCachedAuth() {
    const cached = localStorage.getItem("mes_user");
    if (cached) {
        try {
            currentUser = JSON.parse(cached);
            showMainApp();
        } catch (e) {
            localStorage.removeItem("mes_user");
            showLogin();
        }
    } else {
        showLogin();
    }
}

function showLogin() {
    document.getElementById("login-overlay").classList.remove("hide");
    document.getElementById("app-container").classList.add("hide");

    // Reset view to Login state
    document.getElementById("login-form").classList.remove("hide");
    document.getElementById("register-form").classList.add("hide");
    document.getElementById("auth-title").textContent = "MES Login";
    document.getElementById("auth-subtitle").textContent = "Manufacturing Execution System Portal";

    // Clear errors / inputs / success alerts
    document.getElementById("login-error").classList.add("hide");
    document.getElementById("register-error").classList.add("hide");
    document.getElementById("register-success").classList.add("hide");
    document.getElementById("login-form").reset();
    document.getElementById("register-form").reset();
}

function showMainApp() {
    document.getElementById("login-overlay").classList.add("hide");
    document.getElementById("app-container").classList.remove("hide");

    // Set user headers and UI widgets
    document.getElementById("user-name").textContent = currentUser.name;
    document.getElementById("user-role").textContent = currentUser.role;
    document.getElementById("user-initials").textContent = currentUser.name.charAt(0).toUpperCase();

    // Apply Role-Based Access Control on navigation items
    applyRBAC();

    // Default Tab
    switchTab("dashboard-tab");
    fetchDashboardData();
}

function applyRBAC() {
    const role = currentUser.role; // ADMIN, MANAGER, OPERATOR, INSPECTOR

    // Hide all navigation elements first
    document.querySelectorAll(".nav-item").forEach(item => {
        let visible = false;
        if (role === "ADMIN") {
            visible = true; // Admin gets everything
        } else if (role === "MANAGER") {
            if (item.classList.contains("limit-manager")) visible = true;
        } else if (role === "OPERATOR") {
            if (item.classList.contains("limit-operator")) visible = true;
        } else if (role === "INSPECTOR") {
            if (item.classList.contains("limit-inspector")) visible = true;
        }

        // Base modules every user sees
        if (item.dataset.tab === "dashboard-tab" || item.dataset.tab === "reports-tab" || item.dataset.tab === "logs-tab") {
            visible = true;
        }

        if (visible) {
            item.classList.remove("hide");
        } else {
            item.classList.add("hide");
        }
    });
}

// Navigation Tab switcher
function setupEventListeners() {
    // Toggle views to Register
    document.getElementById("toggle-to-register").addEventListener("click", (e) => {
        e.preventDefault();
        document.getElementById("login-form").classList.add("hide");
        document.getElementById("register-form").classList.remove("hide");
        document.getElementById("auth-title").textContent = "MES Register";
        document.getElementById("auth-subtitle").textContent = "Create your personnel account";
        document.getElementById("login-error").classList.add("hide");
        document.getElementById("register-error").classList.add("hide");
        document.getElementById("register-success").classList.add("hide");
        document.getElementById("register-form").reset();
    });

    // Toggle views to Login
    document.getElementById("toggle-to-login").addEventListener("click", (e) => {
        e.preventDefault();
        document.getElementById("register-form").classList.add("hide");
        document.getElementById("login-form").classList.remove("hide");
        document.getElementById("auth-title").textContent = "MES Login";
        document.getElementById("auth-subtitle").textContent = "Manufacturing Execution System Portal";
        document.getElementById("login-error").classList.add("hide");
        document.getElementById("register-error").classList.add("hide");
        document.getElementById("register-success").classList.add("hide");
        document.getElementById("login-form").reset();
    });

    // Login Submit
    document.getElementById("login-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const empId = document.getElementById("login-emp-id").value;
        const password = document.getElementById("login-password").value;

        fetch(`${API_BASE}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ employeeId: empId, password: password })
        })
            .then(res => {
                if (!res.ok) throw new Error("Invalid Employee ID or Password");
                return res.json();
            })
            .then(user => {
                currentUser = user;
                localStorage.setItem("mes_user", JSON.stringify(user));
                document.getElementById("login-error").classList.add("hide");
                // Clear password input
                document.getElementById("login-password").value = "";
                showMainApp();
            })
            .catch(err => {
                const errEl = document.getElementById("login-error");
                errEl.textContent = err.message || "Invalid sign in";
                errEl.classList.remove("hide");
            });
    });

    // Register Submit
    document.getElementById("register-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const empId = document.getElementById("register-emp-id").value;
        const name = document.getElementById("register-name").value;
        const role = document.getElementById("register-role").value;
        const dept = document.getElementById("register-dept").value;
        const password = document.getElementById("register-password").value;

        const employeeData = {
            id: empId,
            name: name,
            role: role,
            department: dept,
            password: password
        };

        fetch(`${API_BASE}/auth/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(employeeData)
        })
            .then(async res => {
                if (!res.ok) {
                    const errBody = await res.json().catch(() => ({}));
                    throw new Error(errBody.message || "Registration failed");
                }
                return res.json();
            })
            .then(savedUser => {
                document.getElementById("register-error").classList.add("hide");
                const successEl = document.getElementById("register-success");
                successEl.textContent = `Personnel ${savedUser.name} registered successfully! Switch to Sign In to login.`;
                successEl.classList.remove("hide");
                document.getElementById("register-form").reset();
            })
            .catch(err => {
                document.getElementById("register-success").classList.add("hide");
                const errEl = document.getElementById("register-error");
                errEl.textContent = err.message || "Registration failed";
                errEl.classList.remove("hide");
            });
    });

    // Logout
    document.getElementById("logout-btn").addEventListener("click", () => {
        localStorage.removeItem("mes_user");
        currentUser = null;
        showLogin();
    });

    // Nav Switchers
    document.querySelectorAll(".nav-item").forEach(item => {
        item.addEventListener("click", () => {
            document.querySelectorAll(".nav-item").forEach(i => i.classList.remove("active"));
            item.classList.add("active");
            switchTab(item.dataset.tab);
        });
    });

    // Sync button
    document.getElementById("refresh-dashboard").addEventListener("click", () => {
        fetchDashboardData();
    });

    // Forms Submissions
    setupFormHandlers();
}

function getRequestHeaders() {
    return {
        "Content-Type": "application/json",
        "X-Username": currentUser ? currentUser.name : "Unknown",
        "X-User-Role": currentUser ? currentUser.role : "Unknown"
    };
}

function switchTab(tabId) {
    document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.remove("active"));
    document.getElementById(tabId).classList.add("active");

    // Change Title header
    const tabName = document.querySelector(`.nav-item[data-tab="${tabId}"] span`).textContent;
    document.getElementById("current-tab-title").textContent = tabName;

    // Load data based on clicked tab
    if (tabId === "dashboard-tab") {
        fetchDashboardData();
    } else if (tabId === "products-tab") {
        fetchProducts();
    } else if (tabId === "materials-tab") {
        fetchMaterials();
    } else if (tabId === "machines-tab") {
        fetchMachines();
    } else if (tabId === "employees-tab") {
        fetchEmployees();
    } else if (tabId === "orders-tab") {
        fetchOrders();
    } else if (tabId === "tracking-tab") {
        fetchTrackingData();
    } else if (tabId === "qc-tab") {
        fetchQCData();
    } else if (tabId === "maintenance-tab") {
        fetchMaintenanceData();
    } else if (tabId === "reports-tab") {
        fetchReportCharts();
    } else if (tabId === "logs-tab") {
        fetchAuditLogs();
    }
}

// -------------------------------------------------------------
// CORE FETCH & RENDER LOGIC
// -------------------------------------------------------------

function fetchDashboardData() {
    fetch(`${API_BASE}/reports/dashboard`)
        .then(res => res.json())
        .then(data => {
            dashboardData = data;

            // Set Stat Cards
            document.getElementById("stat-machines-running").textContent = data.runningMachines;
            document.getElementById("stat-orders-pending").textContent = data.pendingOrders;
            document.getElementById("stat-today-production").textContent = data.todayProduction.toLocaleString();
            document.getElementById("stat-defects-count").textContent = data.defectiveProducts.toLocaleString();

            // Render stock list (progress bars)
            renderInventoryWidget(data.inventory);

            // Render finished products stock
            renderFinishedProductsWidget(data.products || []);

            // Render machine floor layout
            renderMachineFloor(data.machines);

            // Redraw charts
            renderDashboardCharts(data);

            // Low material alert check
            let lowMaterial = data.inventory.some(m => m.quantity < 1000);
            if (lowMaterial) {
                document.getElementById("low-material-flag").classList.remove("hide");
            } else {
                document.getElementById("low-material-flag").classList.add("hide");
            }
        })
        .catch(err => console.error("Error loading dashboard data", err));
}

function renderFinishedProductsWidget(products) {
    const container = document.getElementById("dashboard-products");
    if (!container) return;
    container.innerHTML = "";

    if (products.length === 0) {
        container.innerHTML = '<p class="text-muted" style="font-size:0.8rem; padding:10px 0;">No product stock defined.</p>';
        return;
    }

    products.forEach(p => {
        const pct = Math.min(((p.quantity || 0) / 10000) * 100, 100);
        let colorClass = "fill-blue";
        if (p.quantity >= 8000) colorClass = "fill-green";
        else if (p.quantity >= 2000) colorClass = "fill-blue";
        else colorClass = "fill-yellow";

        container.innerHTML += `
            <div class="inventory-progress-bar">
                <div class="inventory-label">
                    <span>${p.name}</span>
                    <span style="font-weight:600;">${(p.quantity || 0).toLocaleString()} / 10,000</span>
                </div>
                <div class="inventory-track">
                    <div class="inventory-fill ${colorClass}" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
    });
}

function renderInventoryWidget(materials) {
    const container = document.getElementById("dashboard-materials");
    container.innerHTML = "";

    // Sort materials
    materials.forEach(mat => {
        const pct = Math.min((mat.quantity / 25000) * 100, 100);
        let colorClass = "fill-green";
        if (mat.quantity < 1000) colorClass = "fill-red";
        else if (mat.quantity < 5000) colorClass = "fill-yellow";

        container.innerHTML += `
            <div class="inventory-progress-bar">
                <div class="inventory-label">
                    <span>${mat.name} (${mat.unit})</span>
                    <span style="font-weight:600;">${mat.quantity.toLocaleString()} / 25,000</span>
                </div>
                <div class="inventory-track">
                    <div class="inventory-fill ${colorClass}" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
    });
}

function renderMachineFloor(machines) {
    const container = document.getElementById("dashboard-machines");
    container.innerHTML = "";
    machines.forEach(m => {
        let statusClass = "indicator-idle";
        if ("Running".equalsIgnoreCase(m.status)) statusClass = "indicator-running";
        else if ("Maintenance".equalsIgnoreCase(m.status)) statusClass = "indicator-maintenance";

        container.innerHTML += `
            <div class="mach-status-card glass" onclick="showScanModal('${m.id}')" style="cursor:pointer;" title="Click to inspect info QR">
                <div class="mach-indicator ${statusClass}"></div>
                <div class="mach-info">
                    <h5>${m.id}: ${m.name}</h5>
                    <p>FLoor: ${m.location} | <strong>${m.status}</strong></p>
                </div>
            </div>
        `;
    });
}

// JS Helper to ignore case in status checks
String.prototype.equalsIgnoreCase = function (compareString) {
    return this.toLowerCase() === compareString.toLowerCase();
};

function renderDashboardCharts(data) {
    // 1. Order execution gauge/chart
    const ctxOrder = document.getElementById("dashboard-order-chart").getContext("2d");
    if (orderChart) orderChart.destroy();

    let active = data.orders.filter(o => o.status === "Running").length;
    let completed = data.orders.filter(o => o.status === "Completed").length;
    let pending = data.orders.filter(o => o.status === "Pending").length;
    let cancelled = data.orders.filter(o => o.status === "Cancelled").length;

    orderChart = new Chart(ctxOrder, {
        type: 'bar',
        data: {
            labels: ['Running', 'Completed', 'Pending', 'Cancelled'],
            datasets: [{
                label: 'Order count',
                data: [active, completed, pending, cancelled],
                backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'],
                borderWidth: 0,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8a99ad' } },
                x: { grid: { display: false }, ticks: { color: '#8a99ad' } }
            },
            plugins: { legend: { display: false } }
        }
    });

    // 2. Defect Analysis pie chart
    const ctxDefect = document.getElementById("dashboard-defect-pie").getContext("2d");
    if (defectChart) defectChart.destroy();

    const defectMap = data.defects || {};
    const labels = Object.keys(defectMap);
    const chartData = Object.values(defectMap);

    defectChart = new Chart(ctxDefect, {
        type: 'doughnut',
        data: {
            labels: labels.length > 0 ? labels : ['No Defects'],
            datasets: [{
                data: chartData.length > 0 ? chartData : [100],
                backgroundColor: labels.length > 0 ? ['#ef4444', '#f59e0b', '#3b82f6', '#8b5cf6'] : ['#10b981'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'right', labels: { color: '#8a99ad', boxWidth: 12 } }
            }
        }
    });
}

// -------------------------------------------------------------
// CRUD FETCH ACTIONS
// -------------------------------------------------------------

function fetchProducts() {
    fetch(`${API_BASE}/products`)
        .then(res => res.json())
        .then(products => {
            const body = document.getElementById("products-table-body");
            body.innerHTML = "";
            products.forEach(p => {
                body.innerHTML += `
                    <tr>
                        <td><strong>${p.id}</strong></td>
                        <td>${p.name}</td>
                        <td>${p.category}</td>
                        <td>$${p.price.toFixed(2)}</td>
                        <td><span class="badge role-badge" style="background:rgba(59,130,246,0.15); color:var(--primary);">${(p.quantity || 0).toLocaleString()} pcs</span></td>
                        <td>
                            <button class="btn btn-outline" onclick="deleteProduct('${p.id}')"><i class="fa-solid fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        });
}

function deleteProduct(id) {
    if (!confirm("Are you sure you want to delete product " + id + "?")) return;
    fetch(`${API_BASE}/products/${id}`, { method: "DELETE", headers: getRequestHeaders() })
        .then(() => fetchProducts());
}

function fetchMaterials() {
    fetch(`${API_BASE}/materials`)
        .then(res => res.json())
        .then(materials => {
            const body = document.getElementById("materials-table-body");
            body.innerHTML = "";
            materials.forEach(m => {
                let statusBadge = m.quantity < 1000 ? '<span class="badge status-cancelled">Low Stock</span>' : '<span class="badge status-completed">In Stock</span>';
                body.innerHTML += `
                    <tr>
                        <td><strong>${m.id}</strong></td>
                        <td>${m.name}</td>
                        <td>${m.quantity.toLocaleString()}</td>
                        <td>${m.unit}</td>
                        <td>${statusBadge}</td>
                        <td>
                            <button class="btn btn-outline" onclick="deleteMaterial('${m.id}')"><i class="fa-solid fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        });
}

function deleteMaterial(id) {
    if (!confirm("Are you sure you want to delete material " + id + "?")) return;
    fetch(`${API_BASE}/materials/${id}`, { method: "DELETE", headers: getRequestHeaders() })
        .then(() => fetchMaterials());
}

function fetchMachines() {
    fetch(`${API_BASE}/machines`)
        .then(res => res.json())
        .then(machines => {
            const body = document.getElementById("machines-table-body");
            body.innerHTML = "";
            machines.forEach(m => {
                let statusClass = "status-pending"; // Idle
                if (m.status === "Running") statusClass = "status-running";
                else if (m.status === "Maintenance") statusClass = "status-cancelled";

                body.innerHTML += `
                    <tr>
                        <td><strong>${m.id}</strong></td>
                        <td>${m.name}</td>
                        <td>${m.location}</td>
                        <td><span class="status-badge ${statusClass}">${m.status}</span></td>
                        <td>
                            <button class="btn btn-outline" onclick="deleteMachine('${m.id}')"><i class="fa-solid fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        });
}

function deleteMachine(id) {
    if (!confirm("Are you sure you want to delete machine " + id + "?")) return;
    fetch(`${API_BASE}/machines/${id}`, { method: "DELETE", headers: getRequestHeaders() })
        .then(() => fetchMachines());
}

function fetchEmployees() {
    fetch(`${API_BASE}/employees`)
        .then(res => res.json())
        .then(employees => {
            const body = document.getElementById("employees-table-body");
            body.innerHTML = "";
            employees.forEach(e => {
                body.innerHTML += `
                    <tr>
                        <td><strong>${e.id}</strong></td>
                        <td>${e.name}</td>
                        <td><span class="status-badge status-running">${e.role}</span></td>
                        <td>${e.department}</td>
                        <td>
                            <button class="btn btn-outline" onclick="deleteEmployee('${e.id}')"><i class="fa-solid fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            });
        });
}

function deleteEmployee(id) {
    if (!confirm("Are you sure you want to delete employee " + id + "?")) return;
    fetch(`${API_BASE}/employees/${id}`, { method: "DELETE", headers: getRequestHeaders() })
        .then(() => fetchEmployees());
}

// -------------------------------------------------------------
// ORDERS WORKFLOW ACTION
// -------------------------------------------------------------

function fetchOrders() {
    fetch(`${API_BASE}/orders`)
        .then(res => res.json())
        .then(orders => {
            const body = document.getElementById("orders-table-body");
            body.innerHTML = "";
            orders.forEach(o => {
                let allocateBtn = "";
                let cancelBtn = "";
                let completeBtn = "";

                const stateClass = `status-${o.status.toLowerCase()}`;

                if (o.status === "Pending") {
                    allocateBtn = `<button class="btn btn-green btn-xs" onclick="showStartOrderModal('${o.id}')"><i class="fa-solid fa-play"></i> Start Job</button>`;
                    cancelBtn = `<button class="btn btn-outline btn-xs" onclick="cancelOrder('${o.id}')"><i class="fa-solid fa-ban"></i> Cancel</button>`;
                } else if (o.status === "Running") {
                    completeBtn = `<button class="btn btn-primary btn-xs" onclick="completeOrder('${o.id}')"><i class="fa-solid fa-check-double"></i> Complete</button>`;
                    cancelBtn = `<button class="btn btn-outline btn-xs" onclick="cancelOrder('${o.id}')"><i class="fa-solid fa-ban"></i> Cancel</button>`;
                }

                const machineVal = o.machineId ? `<strong>${o.machineId}</strong>` : `<span style="color: var(--text-muted);">-</span>`;
                const operatorVal = o.operatorId ? `<strong>${o.operatorId}</strong>` : `<span style="color: var(--text-muted);">-</span>`;
                const shiftVal = o.shift ? `<strong>${o.shift}</strong>` : `<span style="color: var(--text-muted);">-</span>`;

                body.innerHTML += `
                    <tr>
                        <td><strong>${o.id}</strong></td>
                        <td>${o.productId}</td>
                        <td>${o.quantity.toLocaleString()}</td>
                        <td><span class="status-badge ${stateClass}">${o.status}</span></td>
                        <td>${machineVal}</td>
                        <td>${operatorVal}</td>
                        <td>${shiftVal}</td>
                        <td>
                            <div class="flex-actions" style="gap:5px;">
                                ${allocateBtn}
                                ${completeBtn}
                                ${cancelBtn}
                            </div>
                        </td>
                    </tr>
                `;
            });
        });
}

function cancelOrder(id) {
    if (!confirm("Cancel Production Order " + id + "?")) return;
    fetch(`${API_BASE}/orders/${id}/cancel`, { method: "POST", headers: getRequestHeaders() })
        .then(() => fetchOrders());
}

function completeOrder(id) {
    fetch(`${API_BASE}/orders/${id}/complete`, { method: "POST", headers: getRequestHeaders() })
        .then(() => fetchOrders());
}

// -------------------------------------------------------------
// PRODUCTION TRACKING
// -------------------------------------------------------------

function fetchTrackingData() {
    Promise.all([
        fetch(`${API_BASE}/orders`).then(res => res.json()),
        fetch(`${API_BASE}/tracking`).then(res => res.json())
    ])
        .then(([orders, trackings]) => {
            const container = document.getElementById("running-orders-container");
            container.innerHTML = "";

            let runningOrders = orders.filter(o => o.status === "Running");

            if (runningOrders.length === 0) {
                container.innerHTML = '<p class="text-muted" style="padding:20px;">No production orders are currently running on the floor.</p>';
                return;
            }

            runningOrders.forEach(o => {
                const track = trackings.find(t => t.orderId === o.id) || { completedQuantity: 0, defectiveQuantity: 0 };
                const completed = track.completedQuantity + track.defectiveQuantity;
                const pct = Math.min(((completed) / o.quantity) * 100, 100);

                container.innerHTML += `
                <div class="job-card">
                    <div class="job-header">
                        <h4>Order ${o.id}: ${o.productId}</h4>
                        <span class="badge role-badge">${o.shift} Shift</span>
                    </div>
                    <div class="job-meta-grid">
                        <span>Machine: <strong>${o.machineId}</strong></span>
                        <span>Operator: <strong>${o.operatorId}</strong></span>
                        <span>Target: <strong>${o.quantity.toLocaleString()}</strong></span>
                        <span>Processed: <strong>${completed.toLocaleString()}</strong></span>
                        <span>Remaining: <strong>${Math.max(0, o.quantity - completed).toLocaleString()}</strong></span>
                    </div>
                    <div class="job-progress-row">
                        <div class="job-progress-track">
                            <div class="job-progress-fill" style="width: ${pct}%"></div>
                        </div>
                        <span>${pct.toFixed(0)}%</span>
                    </div>
                    <div class="job-actions">
                        <button class="btn btn-primary" onclick="loadTrackingForm('${o.id}', ${track.completedQuantity})"><i class="fa-solid fa-pen-to-square"></i> Log Outputs</button>
                        <button class="btn btn-outline" onclick="showScanModal('${o.machineId}')"><i class="fa-solid fa-qrcode"></i> Scan Machine QR</button>
                    </div>
                </div>
            `;
            });
        });
}

function loadTrackingForm(orderId, completed) {
    document.getElementById("track-order-id").value = orderId;
    document.getElementById("track-display-order-id").value = orderId;
    document.getElementById("track-completed").value = completed;

    // Smooth scroll to update panel if mobile
    document.getElementById("tracking-update-panel").scrollIntoView({ behavior: 'smooth' });
}

// -------------------------------------------------------------
// QUALITY CONTROL
// -------------------------------------------------------------

function fetchQCData() {
    Promise.all([
        fetch(`${API_BASE}/orders`).then(res => res.json()),
        fetch(`${API_BASE}/tracking`).then(res => res.json())
    ])
        .then(([orders, trackings]) => {
            const body = document.getElementById("qc-table-body");
            body.innerHTML = "";

            let runningOrders = orders.filter(o => o.status === "Running");

            if (runningOrders.length === 0) {
                body.innerHTML = '<tr><td colspan="6" class="text-muted text-center">No active production jobs require QA inspections.</td></tr>';
                return;
            }

            runningOrders.forEach(o => {
                const track = trackings.find(t => t.orderId === o.id) || { completedQuantity: 0, defectiveQuantity: 0 };

                body.innerHTML += `
                <tr>
                    <td><strong>${o.id}</strong></td>
                    <td>${o.productId}</td>
                    <td>${o.quantity.toLocaleString()}</td>
                    <td><span class="status-badge status-completed">${track.completedQuantity.toLocaleString()}</span></td>
                    <td><span class="status-badge status-cancelled">${track.defectiveQuantity.toLocaleString()}</span></td>
                    <td>
                        <button class="btn btn-outline btn-xs" onclick="loadQCForm('${o.id}', ${track.completedQuantity}, ${track.defectiveQuantity})">
                            <i class="fa-solid fa-clipboard-check"></i> Inspect
                        </button>
                    </td>
                </tr>
            `;
            });
        });
}

function loadQCForm(orderId, completed, defective) {
    document.getElementById("qc-order-id").value = orderId;
    document.getElementById("qc-display-order-id").value = orderId;
    document.getElementById("qc-good").value = completed;
    document.getElementById("qc-defective").value = defective;

    // Clear sub defects
    document.getElementById("qc-crack").value = 0;
    document.getElementById("qc-color").value = 0;
    document.getElementById("qc-size").value = 0;
    document.getElementById("qc-damaged").value = 0;

    document.getElementById("qc-update-panel").scrollIntoView({ behavior: 'smooth' });
}

// -------------------------------------------------------------
// MAINTENANCE MODULE
// -------------------------------------------------------------

function fetchMaintenanceData() {
    fetch(`${API_BASE}/maintenance`)
        .then(res => res.json())
        .then(tickets => {
            const body = document.getElementById("maintenance-table-body");
            body.innerHTML = "";

            if (tickets.length === 0) {
                body.innerHTML = '<tr><td colspan="6" class="text-muted text-center">No maintenance logs filed. Status is normal.</td></tr>';
                return;
            }

            tickets.forEach(t => {
                let statusClass = "status-pending"; // Pending
                if (t.status === "In Progress") statusClass = "status-running";
                else if (t.status === "Resolved") statusClass = "status-completed";

                let actionBtn = "";
                if (t.status !== "Resolved" && (currentUser.role === "ADMIN" || currentUser.role === "MANAGER" || currentUser.role === "OPERATOR")) {
                    actionBtn = `<button class="btn btn-outline btn-xs" onclick="loadMaintenanceAction('${t.id}', '${t.status}')">
                                <i class="fa-solid fa-gears"></i> Manage
                             </button>`;
                }

                const formattedDate = new Date(t.date).toLocaleDateString() + " " + new Date(t.date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

                body.innerHTML += `
                <tr>
                    <td><span style="font-size:0.75rem;">${formattedDate}</span></td>
                    <td><strong>${t.machineId}</strong></td>
                    <td>${t.problem}</td>
                    <td><span class="status-badge ${statusClass}">${t.status}</span></td>
                    <td>${t.engineerName ? t.engineerName : "Unassigned"}</td>
                    <td>${actionBtn}</td>
                </tr>
            `;
            });
        });
}

function loadMaintenanceAction(ticketId, status) {
    document.getElementById("maint-ticket-id").value = ticketId;
    document.getElementById("maint-display-ticket-id").value = ticketId;
    document.getElementById("maint-status-select").value = status;
    document.getElementById("maint-engineer").value = "";

    document.getElementById("maintenance-action-card").style.display = "block";
    document.getElementById("maintenance-action-card").scrollIntoView({ behavior: 'smooth' });
}

// -------------------------------------------------------------
// REPORTS & GRAPHS
// -------------------------------------------------------------

function fetchReportCharts() {
    fetch(`${API_BASE}/reports/dashboard`)
        .then(res => res.json())
        .then(data => {
            // 1. Daily Summary metrics
            let completedOrders = data.orders ? data.orders.filter(o => o.status === "Completed").length : 0;
            document.getElementById("report-completed-orders").textContent = completedOrders;
            document.getElementById("report-manufactured-qty").textContent = data.todayProduction.toLocaleString();
            document.getElementById("report-defects-qty").textContent = data.defectiveProducts.toLocaleString();
            
            let totalProcessed = data.todayProduction + data.defectiveProducts;
            let efficiency = totalProcessed > 0 ? ((data.todayProduction / totalProcessed) * 100).toFixed(1) : "100.0";
            document.getElementById("report-quality-efficiency").textContent = efficiency + "%";

            // 2. Worker Performance table
            const body = document.getElementById("reports-worker-table-body");
            body.innerHTML = "";
            const workers = data.workerPerformance || [];
            if (workers.length === 0) {
                body.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No worker metrics calculated yet.</td></tr>';
            } else {
                workers.forEach(w => {
                    body.innerHTML += `
                        <tr>
                            <td><strong>${w.employeeId}</strong></td>
                            <td>${w.name}</td>
                            <td><span class="badge status-completed">${w.completed.toLocaleString()}</span></td>
                            <td><span class="badge status-cancelled">${w.defective.toLocaleString()}</span></td>
                            <td><strong>${w.efficiency}%</strong></td>
                        </tr>
                    `;
                });
            }

            // 3. Draw Report Charts
            drawReportCharts(data);
        })
        .catch(err => console.error("Error loading reports statistics", err));
}

function drawReportCharts(data) {
    // 1. Daily Production Line Chart
    const ctxDaily = document.getElementById("daily-output-chart").getContext("2d");
    if (dailyChart) dailyChart.destroy();

    // Compile mock dates or actual order dates
    let dates = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    let values = [12000, 18500, 15000, 22000, 18500, 6000, 3000]; // sample default layout

    if (data.todayProduction > 0) {
        values[4] = data.todayProduction; // shift current value
    }

    dailyChart = new Chart(ctxDaily, {
        type: 'line',
        data: {
            labels: dates,
            datasets: [{
                label: 'Products Manufactured',
                data: values,
                borderColor: '#10b981',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                borderWidth: 2,
                fill: true,
                tension: 0.3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8a99ad' } },
                x: { grid: { display: false }, ticks: { color: '#8a99ad' } }
            },
            plugins: { legend: { display: false } }
        }
    });

    // 2. Machine Utilization bar chart
    const ctxMUtil = document.getElementById("machine-util-chart").getContext("2d");
    if (machineChart) machineChart.destroy();

    let labels = [];
    let utils = [];
    data.machines.forEach(m => {
        labels.push(m.id);
        if (m.status === "Running") utils.push(95);
        else if (m.status === "Idle") utils.push(70);
        else utils.push(10); // Maintenance
    });

    machineChart = new Chart(ctxMUtil, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Utilization %',
                data: utils,
                backgroundColor: '#3b82f6',
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { max: 100, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8a99ad' } },
                x: { grid: { display: false }, ticks: { color: '#8a99ad' } }
            },
            plugins: { legend: { display: false } }
        }
    });
}

function downloadReport(type) {
    if (type === "excel") {
        window.location.href = `${API_BASE}/reports/export/excel`;
    } else if (type === "pdf") {
        window.location.href = `${API_BASE}/reports/export/pdf`;
    }
}

// -------------------------------------------------------------
// AUDIT LOGS DISPLAY
// -------------------------------------------------------------

function fetchAuditLogs() {
    fetch(`${API_BASE}/reports/dashboard`)
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById("audit-logs-container");
            container.innerHTML = "";

            const logs = data.auditLogs || [];
            if (logs.length === 0) {
                container.innerHTML = '<div class="text-center text-muted" style="padding:20px;">No logged events yet.</div>';
                return;
            }

            logs.forEach(log => {
                const formattedDate = new Date(log.timestamp).toLocaleDateString() + " " + new Date(log.timestamp).toLocaleTimeString();
                container.innerHTML += `
                <div class="log-item">
                    [<span class="log-time">${formattedDate}</span>] 
                    User <span class="log-user">${log.username}</span> 
                    performed <span class="log-action">${log.action}</span>: 
                    <span>${log.details}</span>
                </div>
            `;
            });
        });
}

// -------------------------------------------------------------
// BARCODE / QR SCANNING SIMULATOR
// -------------------------------------------------------------

function showScanModal(machineId) {
    document.getElementById("scan-modal").classList.remove("hide");
    const container = document.getElementById("scan-qr-content");
    container.innerHTML = `Scanning QR overlay for Machine <strong>${machineId}</strong>...`;

    // Simulate web connection delay
    setTimeout(() => {
        fetch(`${API_BASE}/machines/${machineId}`)
            .then(res => {
                if (!res.ok) throw new Error();
                return res.json();
            })
            .then(mach => {
                container.innerHTML = `
                <h4 style="color:var(--success); margin-bottom:8px;"><i class="fa-solid fa-circle-check"></i> SCAN SUCCESSFUL</h4>
                <strong>Machine ID:</strong> ${mach.id}<br>
                <strong>Machine Model:</strong> ${mach.name}<br>
                <strong>Status:</strong> ${mach.status}<br>
                <strong>Floor Location:</strong> ${mach.location}<br>
                <div style="margin-top:10px; font-size:0.75rem; color:var(--text-muted);">
                    Decoded QR payload: Factory://Floor-A/Machine/${mach.id}?auth=authenticated
                </div>
            `;
            })
            .catch(() => {
                container.innerHTML = `<span class="error-msg">Error. Defective QR code or machine does not exist in registry.</span>`;
            });
    }, 1200);
}

function closeScanModal() {
    document.getElementById("scan-modal").classList.add("hide");
}

// -------------------------------------------------------------
// DIALOG MODAL CONTROLLERS & FORM SETUP
// -------------------------------------------------------------

function showProductModal() { document.getElementById("product-modal").classList.remove("hide"); }
function closeProductModal() { document.getElementById("product-modal").classList.add("hide"); }

function showMaterialModal() { document.getElementById("material-modal").classList.remove("hide"); }
function closeMaterialModal() { document.getElementById("material-modal").classList.add("hide"); }

function showMachineModal() { document.getElementById("machine-modal").classList.remove("hide"); }
function closeMachineModal() { document.getElementById("machine-modal").classList.add("hide"); }

function showEmployeeModal() { document.getElementById("employee-modal").classList.remove("hide"); }
function closeEmployeeModal() { document.getElementById("employee-modal").classList.add("hide"); }

function showOrderModal() {
    // Fetch products list to populate the dropdown
    fetch(`${API_BASE}/products`)
        .then(res => res.json())
        .then(products => {
            const select = document.getElementById("order-product");
            select.innerHTML = "";
            products.forEach(p => {
                select.innerHTML += `<option value="${p.id}">${p.id} - ${p.name}</option>`;
            });
            document.getElementById("order-modal").classList.remove("hide");
        });
}
function closeOrderModal() { document.getElementById("order-modal").classList.add("hide"); }

function showStartOrderModal(orderId) {
    document.getElementById("start-order-id").value = orderId;
    document.getElementById("start-order-display-id").value = orderId;

    // Fetch idle machines and operators
    Promise.all([
        fetch(`${API_BASE}/machines`).then(res => res.json()),
        fetch(`${API_BASE}/employees/role/OPERATOR`).then(res => res.json())
    ])
        .then(([machines, operators]) => {
            const mSelect = document.getElementById("start-machine");
            mSelect.innerHTML = "";
            machines.filter(m => m.status === "Idle").forEach(m => {
                mSelect.innerHTML += `<option value="${m.id}">${m.id} - ${m.name}</option>`;
            });
            if (machines.filter(m => m.status === "Idle").length === 0) {
                mSelect.innerHTML = `<option value="">No Idle Machines Available</option>`;
            }

            const oSelect = document.getElementById("start-operator");
            oSelect.innerHTML = "";
            operators.forEach(op => {
                oSelect.innerHTML += `<option value="${op.id}">${op.name} (${op.department})</option>`;
            });
            if (operators.length === 0) {
                oSelect.innerHTML = `<option value="">No operators registered</option>`;
            }

            document.getElementById("start-order-modal").classList.remove("hide");
        });
}
function closeStartOrderModal() { document.getElementById("start-order-modal").classList.add("hide"); }

function showLogTicketModal() {
    fetch(`${API_BASE}/machines`)
        .then(res => res.json())
        .then(machines => {
            const select = document.getElementById("breakdown-machine");
            select.innerHTML = "";
            machines.forEach(m => {
                select.innerHTML += `<option value="${m.id}">${m.id} - ${m.name} (${m.status})</option>`;
            });
            document.getElementById("breakdown-modal").classList.remove("hide");
        });
}
function closeBreakdownModal() { document.getElementById("breakdown-modal").classList.add("hide"); }


// Form handlers bindings
function setupFormHandlers() {
    // 1. Product Form
    document.getElementById("product-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const prod = {
            id: document.getElementById("product-id").value,
            name: document.getElementById("product-name").value,
            category: document.getElementById("product-category").value,
            price: parseFloat(document.getElementById("product-price").value)
        };
        fetch(`${API_BASE}/products`, {
            method: "POST",
            headers: getRequestHeaders(),
            body: JSON.stringify(prod)
        })
            .then(() => {
                closeProductModal();
                fetchProducts();
                document.getElementById("product-form").reset();
            });
    });

    // 2. Material Form
    document.getElementById("material-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const mat = {
            id: document.getElementById("material-id").value,
            name: document.getElementById("material-name").value,
            quantity: parseFloat(document.getElementById("material-quantity").value),
            unit: document.getElementById("material-unit").value
        };
        fetch(`${API_BASE}/materials`, {
            method: "POST",
            headers: getRequestHeaders(),
            body: JSON.stringify(mat)
        })
            .then(() => {
                closeMaterialModal();
                fetchMaterials();
                document.getElementById("material-form").reset();
            });
    });

    // 3. Machine Form
    document.getElementById("machine-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const mach = {
            id: document.getElementById("machine-id").value,
            name: document.getElementById("machine-name").value,
            location: document.getElementById("machine-location").value,
            status: document.getElementById("machine-status").value
        };
        fetch(`${API_BASE}/machines`, {
            method: "POST",
            headers: getRequestHeaders(),
            body: JSON.stringify(mach)
        })
            .then(() => {
                closeMachineModal();
                fetchMachines();
                document.getElementById("machine-form").reset();
            });
    });

    // 4. Employee Form
    document.getElementById("employee-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const emp = {
            id: document.getElementById("employee-id").value,
            name: document.getElementById("employee-name").value,
            role: document.getElementById("employee-role").value,
            department: document.getElementById("employee-dept").value,
            password: document.getElementById("employee-password").value
        };
        fetch(`${API_BASE}/employees`, {
            method: "POST",
            headers: getRequestHeaders(),
            body: JSON.stringify(emp)
        })
            .then(() => {
                closeEmployeeModal();
                fetchEmployees();
                document.getElementById("employee-form").reset();
            });
    });

    // 5. Order Form
    document.getElementById("order-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const order = {
            id: document.getElementById("order-id").value,
            productId: document.getElementById("order-product").value,
            quantity: parseInt(document.getElementById("order-quantity").value)
        };
        fetch(`${API_BASE}/orders`, {
            method: "POST",
            headers: getRequestHeaders(),
            body: JSON.stringify(order)
        })
            .then(() => {
                closeOrderModal();
                fetchOrders();
                document.getElementById("order-form").reset();
            });
    });

    // 6. Start / Allocate Order Form
    document.getElementById("start-order-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const orderId = document.getElementById("start-order-id").value;
        const payload = {
            machineId: document.getElementById("start-machine").value,
            operatorId: document.getElementById("start-operator").value,
            shift: document.getElementById("start-shift").value
        };

        if (!payload.machineId || !payload.operatorId) {
            alert("Please make sure an active machine and operator are selected.");
            return;
        }

        fetch(`${API_BASE}/orders/${orderId}/start`, {
            method: "POST",
            headers: getRequestHeaders(),
            body: JSON.stringify(payload)
        })
            .then(res => {
                if (!res.ok) return res.json().then(d => { throw new Error(d.message); });
                return res.json();
            })
            .then(() => {
                closeStartOrderModal();
                fetchOrders();
                fetchDashboardData();
            })
            .catch(err => {
                alert("Error: " + err.message);
            });
    });

    // 7. Update tracking form (operator)
    document.getElementById("tracking-update-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const orderId = document.getElementById("track-order-id").value;
        const completed = parseInt(document.getElementById("track-completed").value);

        if (!orderId) return;

        fetch(`${API_BASE}/tracking/order/${orderId}`, {
            method: "PUT",
            headers: getRequestHeaders(),
            body: JSON.stringify({ completedQuantity: completed })
        })
            .then(() => {
                fetchTrackingData();
                fetchDashboardData();
                document.getElementById("tracking-update-form").reset();
            });
    });

    // 8. QA Inspection form
    document.getElementById("qc-update-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const orderId = document.getElementById("qc-order-id").value;
        const good = parseInt(document.getElementById("qc-good").value);
        const defective = parseInt(document.getElementById("qc-defective").value);

        const crack = parseInt(document.getElementById("qc-crack").value) || 0;
        const color = parseInt(document.getElementById("qc-color").value) || 0;
        const size = parseInt(document.getElementById("qc-size").value) || 0;
        const damaged = parseInt(document.getElementById("qc-damaged").value) || 0;

        const sumDefects = crack + color + size + damaged;
        const errEl = document.getElementById("qc-error");

        if (sumDefects !== defective) {
            errEl.textContent = `Sum of defects (${sumDefects}) must exactly match the Defective Goods Count (${defective}).`;
            errEl.classList.remove("hide");
            return;
        } else {
            errEl.classList.add("hide");
        }

        const defectReasons = {
            "Crack": crack,
            "Color mismatch": color,
            "Size issue": size,
            "Damaged": damaged
        };

        fetch(`${API_BASE}/tracking/order/${orderId}`, {
            method: "PUT",
            headers: getRequestHeaders(),
            body: JSON.stringify({
                completedQuantity: good,
                defectiveQuantity: defective,
                defectReasons: defectReasons
            })
        })
            .then(() => {
                fetchQCData();
                fetchDashboardData();
                document.getElementById("qc-update-form").reset();
            });
    });

    // 9. Log breakdown ticket (operator)
    document.getElementById("breakdown-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const payload = {
            machineId: document.getElementById("breakdown-machine").value,
            problem: document.getElementById("breakdown-problem").value
        };

        fetch(`${API_BASE}/maintenance`, {
            method: "POST",
            headers: getRequestHeaders(),
            body: JSON.stringify(payload)
        })
            .then(res => {
                if (!res.ok) throw new Error("Could not log ticket");
                return res.json();
            })
            .then(() => {
                closeBreakdownModal();
                fetchMaintenanceData();
                fetchDashboardData();
                document.getElementById("breakdown-form").reset();
            });
    });

    // 10. Maintenance Resolve Action Form
    document.getElementById("maintenance-resolve-form").addEventListener("submit", (e) => {
        e.preventDefault();
        const ticketId = document.getElementById("maint-ticket-id").value;
        const payload = {
            engineerName: document.getElementById("maint-engineer").value,
            status: document.getElementById("maint-status-select").value
        };

        fetch(`${API_BASE}/maintenance/${ticketId}`, {
            method: "PUT",
            headers: getRequestHeaders(),
            body: JSON.stringify(payload)
        })
            .then(() => {
                document.getElementById("maintenance-action-card").style.display = "none";
                fetchMaintenanceData();
                fetchDashboardData();
                document.getElementById("maintenance-resolve-form").reset();
            });
    });
}
