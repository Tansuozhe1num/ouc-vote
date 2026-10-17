(function () {
  "use strict";

  const API = "/admin/api";
  const TYPE_NAMES = ["学习", "旅游", "恋爱", "其他", "游戏"];
  const state = {
    key: localStorage.getItem("campus-admin-key") || "",
    section: "overview",
    pages: { users: 1, snaps: 1, comments: 1 },
    editing: null,
    loading: false
  };

  const $ = (id) => document.getElementById(id);
  const esc = (value) => String(value == null ? "" : value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]));
  const initials = (name) => (String(name || "同").trim().charAt(0) || "同");
  const typeName = (type) => TYPE_NAMES[Number(type)] || TYPE_NAMES[3];

  function toast(message) {
    const node = $("toast"); node.textContent = message; node.classList.remove("hidden");
    clearTimeout(node._timer); node._timer = setTimeout(() => node.classList.add("hidden"), 2600);
  }

  async function request(path, options) {
    const config = options || {};
    const headers = Object.assign({}, config.headers || {});
    if (state.key) headers["X-Admin-Key"] = state.key;
    const response = await fetch(API + path, Object.assign({}, config, { credentials: "same-origin", headers }));
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || body.status !== "success" || body.code !== 200) {
      const error = new Error((body && body.info) || "请求失败"); error.status = response.status; throw error;
    }
    return body.data;
  }

  function query(params) {
    const search = new URLSearchParams();
    Object.keys(params).forEach((key) => { if (params[key] !== undefined && params[key] !== null && params[key] !== "") search.set(key, params[key]); });
    const value = search.toString(); return value ? "?" + value : "";
  }

  function formBody(form) {
    const body = new URLSearchParams();
    Array.from(new FormData(form).entries()).forEach(([name, value]) => {
      if (value !== null && value !== undefined) body.append(name, String(value));
    });
    return body;
  }

  function post(path, form) {
    return request(path, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: formBody(form) });
  }

  function formatDate(value, seconds) {
    if (!value) return "—";
    let date;
    if (value instanceof Date) date = value;
    else if (typeof value === "number" || /^\d+$/.test(String(value))) {
      const number = Number(value); date = new Date(seconds ? number * 1000 : (number < 10000000000 ? number * 1000 : number));
    } else date = new Date(String(value).replace(" ", "T"));
    if (Number.isNaN(date.getTime())) return esc(value);
    return date.toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
  }

  function statusTag(status, kind) {
    const value = Number(status);
    let label = "正常", modifier = "";
    if (kind === "user") { label = value === 1 ? "封禁" : value === 2 ? "已注销" : "正常"; modifier = value === 1 ? "is-warm" : value === 2 ? "is-danger" : ""; }
    if (kind === "snap") { label = value === 1 ? "已下线" : value === 2 ? "仅自己可见" : "公开"; modifier = value === 1 ? "is-danger" : value === 2 ? "is-warm" : ""; }
    if (kind === "comment") { label = value === 1 ? "已删除" : "正常"; modifier = value === 1 ? "is-danger" : ""; }
    return `<span class="status-tag ${modifier}">${label}</span>`;
  }

  function renderPagination(targetId, result, section) {
    const target = $(targetId); const total = Number(result.total || 0); const size = Number(result.size || 20); const current = Number(result.page || 1);
    const totalPages = Math.max(1, Math.ceil(total / size));
    if (!total) { target.innerHTML = ""; return; }
    const buttons = [];
    buttons.push(`<span class="page-summary">共 ${total} 条</span>`);
    buttons.push(`<button class="page-button" data-page-section="${section}" data-page="${current - 1}" ${current <= 1 ? "disabled" : ""}>‹</button>`);
    const start = Math.max(1, Math.min(current - 2, totalPages - 4));
    const end = Math.min(totalPages, start + 4);
    for (let page = start; page <= end; page++) buttons.push(`<button class="page-button ${page === current ? "is-active" : ""}" data-page-section="${section}" data-page="${page}">${page}</button>`);
    buttons.push(`<button class="page-button" data-page-section="${section}" data-page="${current + 1}" ${current >= totalPages ? "disabled" : ""}>›</button>`);
    target.innerHTML = buttons.join("");
  }

  function renderUsers(result) {
    const body = $("usersBody"), items = result.items || [];
    $("usersEmpty").classList.toggle("hidden", items.length > 0);
    body.innerHTML = items.map((user) => `<tr>
      <td><div class="user-cell"><span class="avatar">${esc(initials(user.name))}</span><span><strong>${esc(user.name || "未命名")}</strong><small>ID ${esc(user.userId)}</small></span></div></td>
      <td>${esc(user.loginName || "—")}</td><td>${esc(user.major || "未填写")}</td><td>${statusTag(user.status, "user")}</td>
      <td class="muted-cell">${formatDate(user.ctime, true)}</td><td class="action-col"><div class="row-actions"><button class="row-action" data-edit="users" data-id="${esc(user.userId)}">编辑</button><button class="row-action danger" data-delete="users" data-id="${esc(user.userId)}">删除</button></div></td>
    </tr>`).join("");
    renderPagination("usersPagination", result, "users");
    state.users = items;
  }

  function renderSnaps(result) {
    const body = $("snapsBody"), items = result.items || [];
    $("snapsEmpty").classList.toggle("hidden", items.length > 0);
    body.innerHTML = items.map((snap) => `<tr>
      <td class="id-col">#${esc(snap.snapId)}</td><td class="desc-cell" title="${esc(snap.desc)}">${esc(snap.desc || "—")}</td>
      <td><div class="user-cell"><span class="avatar">${esc(initials(snap.userName))}</span><span><strong>${esc(snap.userName || "未知用户")}</strong><small>ID ${esc(snap.userId)}</small></span></div></td>
      <td><span class="type-tag">${esc(typeName(snap.type))}</span></td><td>${statusTag(snap.status, "snap")}</td><td>${esc(snap.score == null ? 100 : snap.score)}</td><td class="muted-cell">${formatDate(snap.ctime)}</td>
      <td class="action-col"><div class="row-actions"><button class="row-action" data-edit="snaps" data-id="${esc(snap.snapId)}">编辑</button><button class="row-action danger" data-delete="snaps" data-id="${esc(snap.snapId)}">删除</button></div></td>
    </tr>`).join("");
    renderPagination("snapsPagination", result, "snaps"); state.snaps = items;
  }

  function renderComments(result) {
    const body = $("commentsBody"), items = result.items || [];
    $("commentsEmpty").classList.toggle("hidden", items.length > 0);
    body.innerHTML = items.map((comment) => `<tr>
      <td class="id-col">#${esc(comment.commentId)}</td><td class="desc-cell" title="${esc(comment.text)}">${esc(comment.text || "—")}</td><td>${esc(comment.userId || "—")}</td><td>#${esc(comment.snapId)}</td><td>${statusTag(comment.status, "comment")}</td><td class="muted-cell">${formatDate(comment.ctime)}</td>
      <td class="action-col"><div class="row-actions"><button class="row-action" data-edit="comments" data-id="${esc(comment.commentId)}">编辑</button><button class="row-action danger" data-delete="comments" data-id="${esc(comment.commentId)}">删除</button></div></td>
    </tr>`).join("");
    renderPagination("commentsPagination", result, "comments"); state.comments = items;
  }

  async function loadOverview() {
    const data = await request("/overview");
    $("metricUsers").textContent = data.users == null ? "—" : data.users;
    $("metricActiveUsers").textContent = `${data.activeUsers == null ? "—" : data.activeUsers} 位正常`;
    $("metricSnaps").textContent = data.snaps == null ? "—" : data.snaps;
    $("metricVisibleSnaps").textContent = `${data.visibleSnaps == null ? "—" : data.visibleSnaps} 条公开`;
    $("metricComments").textContent = data.comments == null ? "—" : data.comments;
    $("metricPendingSnaps").textContent = data.pendingSnaps == null ? "—" : data.pendingSnaps;
  }

  async function loadUsers() {
    const result = await request("/users" + query({ page: state.pages.users, size: 20, keyword: $("userKeyword").value.trim(), status: $("userStatus").value })); renderUsers(result);
  }
  async function loadSnaps() {
    const result = await request("/snaps" + query({ page: state.pages.snaps, size: 20, keyword: $("snapKeyword").value.trim(), type: $("snapType").value, status: $("snapStatus").value })); renderSnaps(result);
  }
  async function loadComments() {
    const result = await request("/comments" + query({ page: state.pages.comments, size: 20, keyword: $("commentKeyword").value.trim(), snapId: $("commentSnapId").value, status: $("commentStatus").value })); renderComments(result);
  }

  async function loadCurrent() {
    if (state.loading) return; state.loading = true;
    try {
      if (state.section === "overview") await loadOverview();
      if (state.section === "users") await loadUsers();
      if (state.section === "snaps") await loadSnaps();
      if (state.section === "comments") await loadComments();
    } catch (error) {
      if (error.status === 403) showGate("管理员密钥无效或已失效，请重新验证。"); else toast(error.message || "数据加载失败");
    } finally { state.loading = false; }
  }

  function setSection(section) {
    if (!["overview", "users", "snaps", "comments"].includes(section)) section = "overview";
    state.section = section; state.pages[section] = state.pages[section] || 1;
    document.querySelectorAll("[data-section]").forEach((node) => node.classList.toggle("is-active", node.dataset.section === section));
    document.querySelectorAll("[data-panel]").forEach((node) => node.classList.toggle("hidden", node.dataset.panel !== section));
    const titles = { overview: "概览", users: "用户管理", snaps: "笔记管理", comments: "评论管理" }; $("pageTitle").textContent = titles[section];
    history.replaceState(null, "", `#${section}`); $("sidebar").classList.remove("is-open"); loadCurrent();
  }

  function showGate(message) {
    $("accessGate").classList.remove("hidden"); $("adminApp").classList.add("is-locked");
    if (message) { $("authError").textContent = message; $("authError").classList.remove("hidden"); }
  }
  function hideGate() { $("accessGate").classList.add("hidden"); $("adminApp").classList.remove("is-locked"); $("authError").classList.add("hidden"); }

  function fieldsFor(entity, item) {
    if (entity === "users") return `<div class="field-grid">
      ${item ? `<input type="hidden" name="userId" value="${esc(item.userId)}" />` : ""}
      <label>登录账号<input name="loginName" value="${esc(item && item.loginName)}" maxlength="64" required placeholder="手机号或自定义账号" /></label>
      <label>登录密码<input name="password" type="password" minlength="6" ${item ? "" : "required"} placeholder="${item ? "留空则不修改" : "至少 6 位"}" /></label>
      <label>昵称<input name="name" value="${esc(item && item.name)}" maxlength="64" required placeholder="用户昵称" /></label>
      <label>专业<input name="major" value="${esc(item && item.major)}" maxlength="128" placeholder="例如：计算机科学与技术" /></label>
      <label>性别<select name="sex"><option value="-1" ${!item || item.sex == null ? "selected" : ""}>未设置</option><option value="0" ${item && Number(item.sex) === 0 ? "selected" : ""}>男</option><option value="1" ${item && Number(item.sex) === 1 ? "selected" : ""}>女</option></select></label>
      <label>状态<select name="status"><option value="0" ${!item || Number(item.status) === 0 ? "selected" : ""}>正常</option><option value="1" ${item && Number(item.status) === 1 ? "selected" : ""}>封禁</option><option value="2" ${item && Number(item.status) === 2 ? "selected" : ""}>已注销</option></select></label>
      <label class="field-full">个人简介<textarea name="description" maxlength="500" placeholder="用户的公开介绍">${esc(item && item.description)}</textarea></label>
    </div>`;
    if (entity === "snaps") return `<div class="field-grid">
      ${item ? `<input type="hidden" name="snapId" value="${esc(item.snapId)}" />` : ""}
      <label>作者用户 ID<input name="userId" value="${esc(item && item.userId)}" required placeholder="输入用户 ID" /></label>
      <label>类型<select name="type"><option value="0" ${!item || Number(item.type) === 0 ? "selected" : ""}>学习</option><option value="1" ${item && Number(item.type) === 1 ? "selected" : ""}>旅游</option><option value="2" ${item && Number(item.type) === 2 ? "selected" : ""}>恋爱</option><option value="3" ${item && Number(item.type) === 3 ? "selected" : ""}>其他</option><option value="4" ${item && Number(item.type) === 4 ? "selected" : ""}>游戏</option></select></label>
      <label>状态<select name="status"><option value="0" ${!item || Number(item.status) === 0 ? "selected" : ""}>公开</option><option value="1" ${item && Number(item.status) === 1 ? "selected" : ""}>已下线</option><option value="2" ${item && Number(item.status) === 2 ? "selected" : ""}>仅自己可见</option></select></label>
      <label>热度分<input name="score" type="number" step="1" value="${esc(item ? item.score : 100)}" required /></label>
      <label>图片地址<input name="snapImage" value="${esc(item && item.snapImage)}" placeholder="可选，填写图片 URL" /></label>
      <label class="field-full">笔记内容<textarea name="desc" maxlength="500" required placeholder="写下这条笔记的内容">${esc(item && item.desc)}</textarea><small class="field-hint">最多 500 个字符</small></label>
    </div>`;
    return `<div class="field-grid">
      ${item ? `<input type="hidden" name="commentId" value="${esc(item.commentId)}" />` : ""}
      <label>笔记 ID<input name="snapId" value="${esc(item && item.snapId)}" required placeholder="输入笔记 ID" /></label>
      <label>评论者<input name="userId" value="${esc(item && item.userId)}" required placeholder="昵称或用户标识" /></label>
      <label>状态<select name="status"><option value="0" ${!item || Number(item.status) === 0 ? "selected" : ""}>正常</option><option value="1" ${item && Number(item.status) === 1 ? "selected" : ""}>已删除</option></select></label>
      <label class="field-full">评论内容<textarea name="text" maxlength="1000" required placeholder="输入评论内容">${esc(item && item.text)}</textarea></label>
    </div>`;
  }

  function openModal(entity, item) {
    state.editing = { entity, item: item || null };
    const isEdit = !!item; const labels = { users: ["USER RECORD", isEdit ? "编辑用户" : "新增用户", isEdit ? "更新用户资料与账号状态。" : "创建一个可登录的用户账号。"], snaps: ["CONTENT RECORD", isEdit ? "编辑笔记" : "新增笔记", isEdit ? "更新笔记内容与公开状态。" : "创建一条新的找搭子笔记。"], comments: ["COMMENT RECORD", isEdit ? "编辑评论" : "新增评论", isEdit ? "更新评论文本与状态。" : "为指定笔记添加一条评论。"] }[entity];
    $("modalEyebrow").textContent = labels[0]; $("modalTitle").textContent = labels[1]; $("modalIntro").textContent = labels[2]; $("recordFields").innerHTML = fieldsFor(entity, item); $("recordError").classList.add("hidden"); $("modalBackdrop").classList.remove("hidden");
    setTimeout(() => { const first = $("recordFields").querySelector("input:not([type=hidden]), textarea, select"); if (first) first.focus(); }, 20);
  }
  function closeModal() { $("modalBackdrop").classList.add("hidden"); state.editing = null; }

  async function submitModal(event) {
    event.preventDefault(); if (!state.editing) return;
    const { entity } = state.editing; const button = $("modalSubmit"); button.disabled = true; $("recordError").classList.add("hidden");
    try { await post(`/${entity}/save`, $("recordForm")); closeModal(); toast("保存成功"); await loadCurrent(); if (entity !== "overview") await loadOverview(); }
    catch (error) { $("recordError").textContent = error.message || "保存失败"; $("recordError").classList.remove("hidden"); }
    finally { button.disabled = false; }
  }

  async function removeRecord(entity, id) {
    const labels = { users: "用户", snaps: "笔记", comments: "评论" };
    if (!window.confirm(`确认删除这条${labels[entity]}吗？删除后前台将不再展示。`)) return;
    try { await request(`/${entity}?${entity === "users" ? "userId" : entity === "snaps" ? "snapId" : "commentId"}=${encodeURIComponent(id)}`, { method: "DELETE" }); toast("已删除"); await loadCurrent(); await loadOverview(); }
    catch (error) { toast(error.message || "删除失败"); }
  }

  async function checkSession() {
    try {
      const data = await request("/session");
      if (!data.authenticated) { if (!data.keyConfigured) $("gateDescription").textContent = "当前服务尚未配置管理员密钥，请先设置 CAMPUS_ADMIN_API_KEY。"; showGate(); return; }
      hideGate(); await loadCurrent();
    } catch (error) { showGate(error.message); }
  }

  document.querySelectorAll("[data-section]").forEach((button) => button.addEventListener("click", () => setSection(button.dataset.section)));
  document.querySelectorAll("[data-go]").forEach((button) => button.addEventListener("click", () => setSection(button.dataset.go)));
  $("menuToggle").addEventListener("click", () => $("sidebar").classList.toggle("is-open"));
  $("refreshButton").addEventListener("click", () => loadCurrent());
  $("userSearchButton").addEventListener("click", () => { state.pages.users = 1; loadUsers(); });
  $("snapSearchButton").addEventListener("click", () => { state.pages.snaps = 1; loadSnaps(); });
  $("commentSearchButton").addEventListener("click", () => { state.pages.comments = 1; loadComments(); });
  ["userKeyword", "snapKeyword", "commentKeyword", "commentSnapId"].forEach((id) => $(id).addEventListener("keydown", (event) => { if (event.key !== "Enter") return; event.preventDefault(); const section = id.startsWith("user") ? "users" : id.startsWith("snap") ? "snaps" : "comments"; state.pages[section] = 1; loadCurrent(); }));
  $("addUserButton").addEventListener("click", () => openModal("users"));
  $("addSnapButton").addEventListener("click", () => openModal("snaps"));
  $("addCommentButton").addEventListener("click", () => openModal("comments"));
  $("modalClose").addEventListener("click", closeModal); $("modalCancel").addEventListener("click", closeModal); $("recordForm").addEventListener("submit", submitModal);
  $("modalBackdrop").addEventListener("click", (event) => { if (event.target === $("modalBackdrop")) closeModal(); });
  $("logoutButton").addEventListener("click", () => { localStorage.removeItem("campus-admin-key"); state.key = ""; showGate("已退出管理台"); });
  $("authForm").addEventListener("submit", async (event) => { event.preventDefault(); const key = $("adminKey").value.trim(); if (!key) return; const button = event.target.querySelector("button"); button.disabled = true; try { state.key = key; await request("/auth", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: new URLSearchParams({ apiKey: key }) }); localStorage.setItem("campus-admin-key", key); hideGate(); toast("验证成功"); await loadCurrent(); } catch (error) { state.key = ""; $("authError").textContent = error.message || "密钥无效"; $("authError").classList.remove("hidden"); } finally { button.disabled = false; } });
  document.addEventListener("click", (event) => {
    const page = event.target.closest("[data-page-section]"); if (page && !page.disabled) { state.pages[page.dataset.pageSection] = Number(page.dataset.page); loadCurrent(); return; }
    const edit = event.target.closest("[data-edit]"); if (edit) { const entity = edit.dataset.edit; const idField = { users: "userId", snaps: "snapId", comments: "commentId" }[entity]; const list = state[entity] || []; const item = list.find((record) => String(record[idField]) === String(edit.dataset.id)); if (item) openModal(entity, item); return; }
    const remove = event.target.closest("[data-delete]"); if (remove) removeRecord(remove.dataset.delete, remove.dataset.id);
  });
  $("headerDate").textContent = new Date().toLocaleDateString("zh-CN", { year: "numeric", month: "long", day: "numeric" });
  const hashSection = (location.hash || "").slice(1); if (hashSection) state.section = hashSection;
  setSection(state.section); checkSession();
})();
