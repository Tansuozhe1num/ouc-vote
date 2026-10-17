(function () {
  const types = ["学习搭子", "旅游搭子", "恋爱搭子", "其他活动", "游戏搭子"];
  const state = { page: 1, size: 16, loading: false, done: false, snaps: [], rankSnaps: [], rankLoading: false, type: "", keyword: "", user: null, currentSnap: null, drawResult: null, authMode: "login", captchaKey: null, chatPeer: null, chatSnapId: null, viewedUser: null, publishing: false, publishRequestId: null, commenting: false, sendingMessage: false, feedRequestId: 0, feedAbortController: null, detailRequestId: 0, chatRequestId: 0 };
  const $ = (id) => document.getElementById(id);
  const masonry = $("masonry"), feedEmpty = $("feedEmpty"), feedEnd = $("feedEnd"), searchInput = $("searchInput");
  const viewButtons = Array.from(document.querySelectorAll("[data-view]"));
  const typeButtons = Array.from(document.querySelectorAll("[data-type]"));
  const authButtons = Array.from(document.querySelectorAll("[data-auth]"));
  const postTypeButtons = Array.from(document.querySelectorAll("[data-post-type]"));
  const overlayIds = ["detailOverlay", "publishOverlay", "profileEditOverlay", "authOverlay", "inboxOverlay", "chatOverlay", "listOverlay"];

  function toast(message) { const el = $("toast"); el.textContent = message; el.classList.remove("hidden"); clearTimeout(el._timer); el._timer = setTimeout(() => el.classList.add("hidden"), 2400); }
  function escapeHtml(value) { return String(value == null ? "" : value).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
  function firstChar(name) { return String(name || "同").trim().charAt(0) || "同"; }
  function setButtonSelection(buttons, predicate, activeClass) {
    buttons.forEach((button) => {
      const selected = predicate(button);
      button.classList.toggle(activeClass, selected);
      button.setAttribute("aria-pressed", selected ? "true" : "false");
    });
  }
  function newRequestId() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") return window.crypto.randomUUID();
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
  function typeName(type) { return types[Number(type)] || types[3]; }
  const feedIntroCopy = {
    "": { title: "在校园里，遇见刚好同频的人。", copy: "自习、出发、运动，下一段日常可以一起。" },
    "0": { title: "校园里，找到一起学习的伙伴。", copy: "找一位学习伙伴，分享学习进度，查漏补缺，取长补短。" },
    "1": { title: "说走就走。", copy: "叫上几位伙伴，来一场说走就走的旅程。" },
    "2": { title: "心动不如行动。", copy: "发布动态或主动出击，找到心仪的 ta。" },
    "4": { title: "游戏，多人玩才爽", copy: "四缺一，速度上号" }
  };
  function updateFeedIntro(type) {
    const copy = feedIntroCopy[type] || feedIntroCopy[""];
    $("feedView").dataset.activeType = type;
    $("feedIntroTitle").textContent = copy.title;
    $("feedIntroCopy").textContent = copy.copy;
  }
  function snapId(snap) { return snap.snapId || snap.id; }
  function currentUserId(user) { return user && (user.userId || user.user_id); }
  function genderLabel(sex) {
    if (String(sex) === "0") return "男";
    if (String(sex) === "1") return "女";
    return "";
  }
  function visibleSnaps(source) {
    const needle = state.keyword.trim().toLocaleLowerCase();
    return source.filter((snap) => {
      const authorName = snap.userName || snap.name || "";
      const major = snap.major || "";
      return !needle || [snap.desc, authorName, major].join(" ").toLocaleLowerCase().includes(needle);
    });
  }
  function renderCards(list, target) {
    target.innerHTML = list.map((snap, index) => {
      const id = snapId(snap), name = snap.userName || snap.name || "同學";
      const imageUrl = snap.snapImage ? OssImageCache.lookup(snap.snapImage) : "";
      const coverClass = imageUrl ? "snap-cover" : "snap-cover no-image";
      return `<article class="snap-card" data-id="${escapeHtml(id)}" style="animation-delay:${Math.min(index * 35, 280)}ms"><div class="${coverClass}">${imageUrl ? `<img data-oss="${escapeHtml(snap.snapImage)}" src="${escapeHtml(imageUrl)}" alt="" loading="lazy" />` : ""}<span class="snap-type">${escapeHtml(typeName(snap.type))}</span></div><div class="snap-body"><p class="snap-desc">${escapeHtml(snap.desc || "正在寻找同频的你")}</p><div class="snap-author"><span class="avatar">${escapeHtml(firstChar(name))}</span><span><strong>${escapeHtml(name)}</strong><small class="snap-major">${escapeHtml(snap.major || "校园同学")}</small></span></div></div></article>`;
    }).join("");
    OssImageCache.hydrate(target);
  }
  function renderFeed() {
    const filtered = visibleSnaps(state.snaps.filter((snap) => state.type === "" || String(snap.type) === state.type));
    renderCards(filtered, masonry);
    feedEmpty.classList.toggle("hidden", filtered.length > 0 || state.loading);
  }
  function renderRank() {
    const visible = state.rankSnaps.map((snap, index) => ({ snap, position: index + 1 }))
      .filter((entry) => visibleSnaps([entry.snap]).length > 0);
    const maxScore = Math.max(1, ...visible.map(({ snap }) => Number(snap.score) || 0));
    $("rankList").innerHTML = visible.map(({ snap, position }) => {
      const id = snapId(snap), name = snap.userName || snap.name || "同学";
      const score = Number(snap.score) || 0;
      const imageUrl = snap.snapImage ? OssImageCache.lookup(snap.snapImage) : "";
      const cover = imageUrl
        ? `<img data-oss="${escapeHtml(snap.snapImage)}" src="${escapeHtml(imageUrl)}" alt="" loading="lazy" />`
        : `<span aria-hidden="true">✦</span>`;
      const width = Math.max(7, Math.min(100, score / maxScore * 100));
      const medal = position === 1 ? "✦" : position === 2 ? "✧" : position === 3 ? "✳" : "";
      return `<article class="rank-entry rank-entry-${Math.min(position, 3)}" data-id="${escapeHtml(id)}" style="animation-delay:${Math.min((position - 1) * 45, 360)}ms">
        <div class="rank-position"><span>${String(position).padStart(2, "0")}</span>${medal ? `<i>${medal}</i>` : ""}</div>
        <div class="rank-cover ${imageUrl ? "" : "rank-cover-empty"}">${cover}</div>
        <div class="rank-entry-copy"><div class="rank-entry-meta"><span class="rank-type">${escapeHtml(typeName(snap.type))}</span><span class="rank-community">校园人气</span></div><h3>${escapeHtml(snap.desc || "正在寻找同频的你")}</h3><div class="rank-author"><span class="avatar">${escapeHtml(firstChar(name))}</span><span><strong>${escapeHtml(name)}</strong><small>${escapeHtml(snap.major || "校园同学")}</small></span></div></div>
        <div class="rank-score"><strong>${score.toLocaleString("zh-CN")}</strong><small>热度分</small><span class="rank-meter"><i style="width:${width}%"></i></span></div>
        <span class="rank-entry-arrow" aria-hidden="true">↗</span>
      </article>`;
    }).join("");
    OssImageCache.hydrate($("rankList"));
    $("rankEmpty").classList.toggle("hidden", visible.length > 0 || state.rankLoading);
  }
  async function loadRank() {
    if (state.rankLoading) return;
    state.rankLoading = true;
    $("rankLoading").classList.remove("hidden");
    $("rankEmpty").classList.add("hidden");
    try {
      const data = await SnapApi.ranks();
      state.rankSnaps = Array.isArray(data) ? data : [];
      OssImageCache.warmup(state.rankSnaps.map((snap) => snap.snapImage).filter(Boolean));
      renderRank();
    } catch (e) {
      toast(e.message || "热榜加载失败");
      $("rankEmpty").classList.toggle("hidden", state.rankSnaps.length > 0);
    } finally {
      state.rankLoading = false;
      $("rankLoading").classList.add("hidden");
      renderRank();
    }
  }
  function resetFeed() {
    state.feedRequestId += 1;
    if (state.feedAbortController) state.feedAbortController.abort();
    state.feedAbortController = null;
    state.loading = false;
    state.page = 1; state.done = false; state.snaps = [];
    masonry.innerHTML = ""; feedEnd.classList.add("hidden"); loadMore();
  }
  async function loadMore() {
    if (state.loading || state.done || $("feedView").classList.contains("hidden")) return;
    const requestId = state.feedRequestId;
    const controller = new AbortController();
    state.feedAbortController = controller;
    state.loading = true;
    try {
      const data = await SnapApi.list(state.page, state.size, state.type, { signal: controller.signal });
      if (requestId !== state.feedRequestId) return;
      const batch = Array.isArray(data) ? data : [];
      const known = new Set(state.snaps.map((snap) => String(snapId(snap))));
      batch.forEach((snap) => { if (!known.has(String(snapId(snap)))) state.snaps.push(snap); });
      if (batch.length) OssImageCache.warmup(batch.map((snap) => snap.snapImage).filter(Boolean));
      state.page++;
      if (batch.length < state.size || batch.length === 0) { state.done = true; feedEnd.classList.toggle("hidden", state.snaps.length === 0); }
      renderFeed();
    } catch (e) {
      if (e.name !== "AbortError" && requestId === state.feedRequestId) toast(e.message || "动态加载失败");
    } finally {
      if (requestId === state.feedRequestId) {
        state.loading = false;
        state.feedAbortController = null;
      }
    }
  }

  function setView(view) {
    ["feed", "draw", "rank", "profile"].forEach((key) => $(key + "View").classList.toggle("hidden", key !== view));
    setButtonSelection(viewButtons, (button) => button.dataset.view === view, "is-active");
    if (view === "feed") { location.hash = "#/feed"; }
    if (view === "draw") location.hash = "#/draw";
    if (view === "rank") { location.hash = "#/rank"; loadRank(); }
    if (view === "profile") { location.hash = "#/profile"; loadProfile(); }
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function showOverlay(id) { $(id).classList.remove("hidden"); document.body.style.overflow = "hidden"; }
  function hideOverlay(id) { $(id).classList.add("hidden"); if (!overlayIds.some((key) => !$(key).classList.contains("hidden"))) document.body.style.overflow = ""; }
  function openAuth(mode) { setAuthMode(mode || "login"); showOverlay("authOverlay"); refreshCaptcha(); }
  function setAuthMode(mode) {
    state.authMode = mode;
    setButtonSelection(authButtons, (button) => button.dataset.auth === mode, "is-active");
    $("registerFields").classList.toggle("hidden", mode !== "register");
    $("authTitle").textContent = mode === "register" ? "创建你的账号" : "欢迎回来";
    $("authSubmit").innerHTML = `${mode === "register" ? "注册并加入" : "登录"} <span>→</span>`;
  }
  async function refreshCaptcha() {
    $("captchaPlaceholder").textContent = "加载中";
    try {
      const captcha = await AuthApi.captcha(); state.captchaKey = captcha.key;
      $("captchaImage").src = captcha.image.startsWith("data:") ? captcha.image : "data:image/png;base64," + captcha.image;
      $("captchaImage").classList.remove("hidden"); $("captchaPlaceholder").classList.add("hidden");
      $("authForm").elements.captchaCode.value = "";
    } catch (e) { $("captchaPlaceholder").textContent = "点击重试"; toast(e.message || "验证码加载失败"); }
  }
  function applyUser(user) {
    state.user = user;
    $("loginButton").classList.toggle("hidden", !!user);
    $("userChip").classList.toggle("hidden", !user);
    $("commentAuthor").classList.toggle("hidden", !user);
    $("commentForm").classList.toggle("has-comment-author", !!user);
    if (user) {
      const name = user.name || "同学"; $("userNameLabel").textContent = name; $("userAvatar").textContent = firstChar(name);
      $("commentAuthor").textContent = name;
    }
  }
  async function checkSession() {
    try { applyUser(await AuthApi.me()); }
    catch (_) { applyUser(null); }
  }
  async function loadProfile() {
    if (!state.user) { openAuth("login"); setView("feed"); return; }
    const user = state.user, name = user.name || "同学";
    $("profileName").innerHTML = `${escapeHtml(name)}${genderLabel(user.sex) ? `<span class="profile-gender">${genderLabel(user.sex)}</span>` : ""}`;
    $("profileAvatar").textContent = firstChar(name);
    $("profileMeta").textContent = user.major || "校园同学";
    $("profileDescription").textContent = user.description || "还没有自我介绍";
    try {
      const [following, followers, data] = await Promise.all([
        UserApi.following().catch(() => []), UserApi.followers().catch(() => []), SnapApi.mine(1, 30)
      ]);
      $("followingCount").textContent = String(following.length); $("followerCount").textContent = String(followers.length);
      const snaps = Array.isArray(data) ? data.filter((snap) => String(snap.userId) === String(currentUserId(user))) : [];
      renderCards(snaps, $("mySnaps"));
    } catch (e) { toast(e.message || "主页加载失败"); }
  }
  async function openDetail(id) {
    const requestId = ++state.detailRequestId;
    let snap = state.snaps.find((item) => String(snapId(item)) === String(id));
    if (!snap && state.drawResult && String(snapId(state.drawResult)) === String(id)) snap = state.drawResult;
    if (!snap) { try { snap = await SnapApi.detail(id); } catch (e) { if (requestId === state.detailRequestId) toast(e.message || "动态不存在"); return; } }
    if (requestId !== state.detailRequestId) return;
    state.currentSnap = snap;
    const authorId = snap.userId, authorName = snap.userName || snap.name || "同学";
    $("detailName").textContent = authorName; $("detailAvatar").textContent = firstChar(authorName);
    $("detailMeta").textContent = [snap.major, snap.sex === 1 ? "女生" : "同学"].filter(Boolean).join(" · ");
    $("detailType").textContent = typeName(snap.type); $("detailDesc").textContent = snap.desc || "正在寻找同频的你";
    const photo = $("detailPhoto"), image = $("detailImage");
    if (snap.snapImage) { photo.classList.remove("empty-image"); image.classList.remove("hidden"); image.dataset.oss = snap.snapImage; image.src = OssImageCache.lookup(snap.snapImage); OssImageCache.store(snap.snapImage).then((local) => { if (local && state.currentSnap === snap) image.src = local; }); }
    else { photo.classList.add("empty-image"); image.classList.add("hidden"); }
    const own = state.user && String(currentUserId(state.user)) === String(authorId);
    $("followButton").classList.toggle("hidden", own || !state.user);
    $("detailAuthor").dataset.userId = authorId;
    $("followButton").dataset.userId = authorId;
    $("followButton").classList.remove("is-following"); $("followButton").textContent = "＋ 关注";
    if (state.user && !own) {
      UserApi.isFollowing(authorId).then((following) => {
        if (state.currentSnap === snap) { $("followButton").classList.toggle("is-following", following); $("followButton").textContent = following ? "✓ 已关注" : "＋ 关注"; }
      }).catch(() => {});
    }

    // Show the snap immediately; a slow comments request must not block the detail view.
    $("commentList").innerHTML = "";
    $("commentEmpty").textContent = "评论加载中…";
    $("commentEmpty").classList.remove("hidden");
    showOverlay("detailOverlay");
    try {
      const comments = await loadSnapComments(id);
      if (requestId === state.detailRequestId) renderComments(comments);
    } catch (_) {
      if (requestId === state.detailRequestId) renderComments([]);
    }
  }
  function renderComments(comments) {
    $("commentList").innerHTML = comments.map((comment) => {
      const name = comment.nickname || "同学";
      const authorId = String(comment.authorId || "");
      const canChat = /^\d+$/.test(authorId);
      return `<li class="comment-item"><button class="comment-avatar-button" type="button" data-user-id="${escapeHtml(authorId)}" data-user-name="${escapeHtml(name)}" aria-label="和 ${escapeHtml(name)} 聊天" title="和 ${escapeHtml(name)} 聊天"${canChat ? "" : " disabled"}><span class="avatar">${escapeHtml(firstChar(name))}</span></button><div class="comment-copy"><strong>${escapeHtml(name)}</strong><p>${escapeHtml(comment.content || "")}</p></div></li>`;
    }).join("");
    $("commentEmpty").textContent = "来做第一个打招呼的人吧";
    $("commentEmpty").classList.toggle("hidden", comments.length > 0);
  }
  async function runDraw() {
    if (!state.user) { openAuth("login"); return; }
    const button = $("drawButton"), card = $("drawCard"); state.drawResult = null; button.disabled = true; $("drawChat").disabled = true; $("drawButtonLabel").textContent = "缘分正在靠近"; card.classList.add("is-drawing"); card.classList.remove("is-ready"); card.removeAttribute("title");
    try {
      const result = await SnapApi.draw(); state.drawResult = result;
      $("drawType").textContent = typeName(result.type); $("drawTitle").textContent = result.desc || "正在寻找同频的你";
      $("drawDesc").textContent = "TA 正在校园里，等待一个友好的招呼。";
      const name = result.userName || "同学"; $("drawName").textContent = name; $("drawMajor").textContent = result.major || "校园同学"; $("drawAvatar").textContent = firstChar(name);
      $("drawCardArt").classList.add("ready"); $("drawCardArt").innerHTML = "✦"; $("drawChat").disabled = false;
      if (result.snapImage) { $("drawCardArt").innerHTML = `<img src="${escapeHtml(OssImageCache.lookup(result.snapImage))}" data-oss="${escapeHtml(result.snapImage)}" alt="" style="width:100%;height:100%;object-fit:cover" />`; OssImageCache.hydrate($("drawCardArt")); }
      card.classList.remove("is-drawing"); card.classList.add("is-ready"); card.title = "查看这条动态的内容和评论";
    } catch (e) { card.classList.remove("is-drawing"); toast(e.message || "抽取失敗"); }
    finally { button.disabled = false; $("drawButtonLabel").textContent = "再抽一位搭子"; }
  }
  async function openChat(userId, name, snapIdValue) {
    if (!state.user) { openAuth("login"); return; }
    const peerId = String(userId == null ? "" : userId);
    if (!peerId || String(currentUserId(state.user)) === peerId) { toast("不能和自己聊天"); return; }
    state.chatPeer = { userId: peerId, name }; state.chatSnapId = snapIdValue || null;
    const requestId = ++state.chatRequestId;
    $("chatName").textContent = name || "同学"; $("chatAvatar").textContent = firstChar(name); $("chatMessages").innerHTML = "";
    $("chatAvatar").dataset.userId = peerId;
    showOverlay("chatOverlay"); await loadConversation(requestId); if (requestId === state.chatRequestId) $("chatInput").focus();
  }
  async function loadConversation(requestId) {
    const activeRequestId = requestId || ++state.chatRequestId;
    const peerId = state.chatPeer && state.chatPeer.userId;
    if (!peerId) return;
    try {
      const messages = await MessageApi.conversation(peerId);
      if (activeRequestId !== state.chatRequestId || !state.chatPeer || state.chatPeer.userId !== peerId) return;
      $("chatMessages").innerHTML = messages.map((message) => `<div class="message-bubble ${String(message.senderId) === String(currentUserId(state.user)) ? "mine" : ""}">${escapeHtml(message.content)}</div>`).join("");
      $("chatMessages").scrollTop = $("chatMessages").scrollHeight;
    } catch (e) { if (activeRequestId === state.chatRequestId) toast(e.message || "聊天记录加载失败"); }
  }
  async function showUserList(kind) {
    if (!state.user) { openAuth("login"); return; }
    const users = await (kind === "following" ? UserApi.following() : UserApi.followers());
    $("listTitle").textContent = kind === "following" ? "我的关注" : "我的粉丝";
    $("userList").innerHTML = users.length ? users.map((user) => `<div class="user-list-item"><span class="avatar">${escapeHtml(firstChar(user.name))}</span><span><strong>${escapeHtml(user.name || "同学")}</strong><small>${escapeHtml(user.major || "校园同学")}</small></span></div>`).join("") : `<p class="empty-mini">还没有${kind === "following" ? "关注的人" : "粉丝"}</p>`;
    showOverlay("listOverlay");
  }

  masonry.addEventListener("click", (event) => { const card = event.target.closest(".snap-card"); if (card) openDetail(card.dataset.id); });
  $("mySnaps").addEventListener("click", (event) => { const card = event.target.closest(".snap-card"); if (card) openDetail(card.dataset.id); });
  $("rankList").addEventListener("click", (event) => { const entry = event.target.closest(".rank-entry"); if (entry) openDetail(entry.dataset.id); });
  $("refreshRank").addEventListener("click", loadRank);
  $("commentList").addEventListener("click", (event) => {
    const button = event.target.closest(".comment-avatar-button");
    if (!button || button.disabled) return;
    const userId = String(button.dataset.userId || "");
    if (!state.user) { openAuth("login"); return; }
    if (userId && userId === String(currentUserId(state.user))) {
      hideOverlay("detailOverlay");
      setView("profile");
      return;
    }
    openChat(userId, button.dataset.userName, null);
  });
  $("drawCard").addEventListener("click", (event) => {
    if (event.target.closest("#drawChat") || !state.drawResult || $("drawCard").classList.contains("is-drawing")) return;
    openDetail(snapId(state.drawResult));
  });
  viewButtons.forEach((button) => button.addEventListener("click", () => setView(button.dataset.view)));
  typeButtons.forEach((button) => button.addEventListener("click", () => {
    setButtonSelection(typeButtons, (item) => item === button, "is-active");
    state.type = button.dataset.type; updateFeedIntro(state.type); resetFeed();
  }));
  let searchTimer;
  searchInput.addEventListener("input", () => { clearTimeout(searchTimer); searchTimer = setTimeout(() => { state.keyword = searchInput.value; renderFeed(); renderRank(); }, 160); });
  $("loginButton").addEventListener("click", () => openAuth("login")); $("userChip").addEventListener("click", () => setView("profile"));
  $("openPublish").addEventListener("click", () => state.user ? showOverlay("publishOverlay") : openAuth("login"));
  $("mobilePublish").addEventListener("click", () => state.user ? showOverlay("publishOverlay") : openAuth("login"));
  $("mobileProfile").addEventListener("click", () => setView("profile"));
  $("drawButton").addEventListener("click", runDraw); $("drawChat").addEventListener("click", () => state.drawResult && openChat(state.drawResult.userId, state.drawResult.userName, snapId(state.drawResult)));
  $("detailAuthor").addEventListener("click", () => {
    const uid = $("detailAuthor").dataset.userId;
    if (state.user && String(uid) === String(currentUserId(state.user))) { hideOverlay("detailOverlay"); setView("profile"); return; }
    if (state.currentSnap) openChat(uid, $("detailName").textContent, snapId(state.currentSnap));
  });
  $("followButton").addEventListener("click", async () => {
    if (!state.user) { openAuth("login"); return; }
    try { const followed = await UserApi.toggleFollow($("followButton").dataset.userId); $("followButton").classList.toggle("is-following", followed); $("followButton").textContent = followed ? "✓ 已关注" : "＋ 关注"; }
    catch (e) { toast(e.message || "关注失败"); }
  });
  $("showFollowing").addEventListener("click", () => showUserList("following")); $("showFollowers").addEventListener("click", () => showUserList("followers"));
  $("editProfile").addEventListener("click", () => {
    if (!state.user) return openAuth("login");
    const form = $("profileEditForm");
    form.elements.name.value = state.user.name || "";
    form.elements.description.value = state.user.description || "";
    form.elements.major.value = state.user.major || "";
    form.elements.sex.value = state.user.sex === 0 || state.user.sex === 1 ? String(state.user.sex) : "-1";
    $("profileDescriptionCount").textContent = String(form.elements.description.value.length);
    $("profileEditError").classList.add("hidden");
    showOverlay("profileEditOverlay");
    form.elements.name.focus();
  });
  $("closeProfileEdit").addEventListener("click", () => hideOverlay("profileEditOverlay"));
  $("profileEditOverlay").addEventListener("click", (event) => { if (event.target === $("profileEditOverlay")) hideOverlay("profileEditOverlay"); });
  $("profileEditForm").elements.description.addEventListener("input", (event) => { $("profileDescriptionCount").textContent = String(event.target.value.length); });
  $("profileEditForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget, error = $("profileEditError"), data = Object.fromEntries(new FormData(form).entries());
    data.sex = data.sex === "-1" ? "-1" : data.sex;
    error.classList.add("hidden");
    try {
      const updated = await UserApi.update(data);
      applyUser(updated);
      hideOverlay("profileEditOverlay");
      await loadProfile();
      toast("资料已更新");
    } catch (e) {
      error.textContent = e.message || "资料更新失败";
      error.classList.remove("hidden");
    }
  });
  $("logoutButton").addEventListener("click", async () => { await AuthApi.logout().catch(() => {}); applyUser(null); setView("feed"); toast("已退出登录"); });
  async function openInbox() {
    if (!state.user) { openAuth("login"); return; }
    try {
      const conversations = await MessageApi.inbox();
      const list = Array.isArray(conversations) ? conversations : [];
      $("inboxList").innerHTML = list.map((conversation) => {
        const peerId = String(conversation.peerId == null ? "" : conversation.peerId);
        const name = conversation.peerName || "同学";
        return `<button class="inbox-item" type="button" data-peer-id="${escapeHtml(peerId)}" data-peer-name="${escapeHtml(name)}"><span class="avatar inbox-avatar">${escapeHtml(firstChar(name))}</span><span class="inbox-item-copy"><strong>${escapeHtml(name)}</strong><small>${escapeHtml(conversation.lastContent || "")}</small></span><span class="inbox-item-meta">${conversation.lastSenderId && String(conversation.lastSenderId) === String(currentUserId(state.user)) ? "我" : ""}</span></button>`;
      }).join("");
      $("inboxEmpty").classList.toggle("hidden", list.length > 0);
      showOverlay("inboxOverlay");
    } catch (e) { toast(e.message || "消息加载失败"); }
  }
  $("inboxButton").addEventListener("click", openInbox);
  $("inboxList").addEventListener("click", (event) => {
    const item = event.target.closest(".inbox-item");
    if (!item) return;
    hideOverlay("inboxOverlay");
    // A conversation can contain messages tied to different snaps.  The snap
    // id is only needed for the first message opened from a snap detail.
    openChat(item.dataset.peerId, item.dataset.peerName, null);
  });
  $("chatAvatar").addEventListener("click", () => {
    const userId = $("chatAvatar").dataset.userId;
    if (userId) toast("已在和 TA 聊天");
  });
  authButtons.forEach((button) => button.addEventListener("click", () => setAuthMode(button.dataset.auth)));
  $("closeAuth").addEventListener("click", () => hideOverlay("authOverlay")); $("captchaRefresh").addEventListener("click", refreshCaptcha);
  $("authForm").addEventListener("submit", async (event) => {
    event.preventDefault(); const form = event.currentTarget, error = $("authError"); error.classList.add("hidden");
    const data = Object.fromEntries(new FormData(form).entries()); data.captchaKey = state.captchaKey;
    try { const user = state.authMode === "register" ? await AuthApi.register(data) : await AuthApi.login(data); applyUser(user); hideOverlay("authOverlay"); toast(state.authMode === "register" ? "欢迎加入同频" : "登录成功"); }
    catch (e) { error.textContent = e.message || "登录失败"; error.classList.remove("hidden"); refreshCaptcha(); }
  });
  $("wechatLogin").addEventListener("click", async () => {
    const captchaCode = $("authForm").elements.captchaCode.value.trim();
    if (!state.captchaKey || !captchaCode) { $("authError").textContent = "请先填写图形验证码"; $("authError").classList.remove("hidden"); return; }
    location.href = `/auth/wechat/start?captchaKey=${encodeURIComponent(state.captchaKey)}&captchaCode=${encodeURIComponent(captchaCode)}`;
  });
  $("closePublish").addEventListener("click", () => hideOverlay("publishOverlay")); $("closeDetail").addEventListener("click", () => hideOverlay("detailOverlay"));
  $("closeInbox").addEventListener("click", () => hideOverlay("inboxOverlay")); $("closeChat").addEventListener("click", () => hideOverlay("chatOverlay")); $("closeList").addEventListener("click", () => hideOverlay("listOverlay"));
  ["detailOverlay", "publishOverlay", "profileEditOverlay", "authOverlay", "inboxOverlay", "chatOverlay", "listOverlay"].forEach((id) => $(id).addEventListener("click", (event) => { if (event.target === $(id)) hideOverlay(id); }));
  $("avatarInput").addEventListener("change", (event) => { const file = event.target.files && event.target.files[0]; if (!file) return; const preview = $("coverPreview"); preview.src = URL.createObjectURL(file); preview.classList.remove("hidden"); $("coverHint").classList.add("hidden"); });
  $("publishForm").elements.desc.addEventListener("input", (event) => { $("descCount").textContent = String(event.target.value.length); });
  postTypeButtons.forEach((button) => button.addEventListener("click", () => {
    setButtonSelection(postTypeButtons, (option) => option === button, "is-selected");
    $("postType").value = button.dataset.postType;
  }));
  $("publishForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (state.publishing) return;
    state.publishing = true;
    const form = event.currentTarget, button = form.querySelector(".form-submit"), data = new FormData(form), error = $("publishError");
    state.publishRequestId = state.publishRequestId || newRequestId();
    error.classList.add("hidden");
    button.disabled = true;
    button.dataset.originalText = button.innerHTML;
    button.innerHTML = "发布中…";
    try {
      await SnapApi.save(data, state.publishRequestId);
      state.publishRequestId = newRequestId();
      form.reset(); setButtonSelection(postTypeButtons, (option) => option.dataset.postType === "0", "is-selected"); $("postType").value = "0"; $("coverPreview").classList.add("hidden"); $("coverHint").classList.remove("hidden"); $("descCount").textContent = "0"; hideOverlay("publishOverlay"); state.type = ""; updateFeedIntro(state.type); setButtonSelection(typeButtons, (button) => button.dataset.type === "", "is-active"); resetFeed(); toast("动态发布成功");
    } catch (e) { error.textContent = e.message || "发布失败"; error.classList.remove("hidden"); }
    finally { state.publishing = false; button.disabled = false; button.innerHTML = button.dataset.originalText || "发布到广场 <span>→</span>"; }
  });
  $("commentForm").addEventListener("submit", async (event) => {
    event.preventDefault(); if (!state.currentSnap) return;
    if (!state.user) { toast("请先登录后留言"); openAuth("login"); return; }
    if (state.commenting) return;
    const snap = state.currentSnap, id = snapId(snap), button = event.currentTarget.querySelector("button");
    const content = $("commentContent").value.trim(); if (!content) return;
    state.commenting = true; button.disabled = true;
    try {
      await CommentApi.save(id, content);
      if (state.currentSnap !== snap) return;
      $("commentContent").value = "";
      const comments = await loadSnapComments(id);
      if (state.currentSnap === snap) renderComments(comments);
    } catch (e) { toast(e.message || "评论失败"); }
    finally { state.commenting = false; button.disabled = false; }
  });
  $("chatForm").addEventListener("submit", async (event) => {
    event.preventDefault(); if (!state.chatPeer) return; const input = $("chatInput"), content = input.value.trim(); if (!content) return;
    if (state.sendingMessage) return;
    const peer = state.chatPeer, snapIdValue = state.chatSnapId, button = event.currentTarget.querySelector("button");
    state.sendingMessage = true; button.disabled = true;
    try {
      await MessageApi.send(peer.userId, content, snapIdValue);
      if (state.chatPeer !== peer) return;
      state.chatSnapId = null; input.value = ""; await loadConversation();
    } catch (e) { toast(e.message || "消息发送失败"); }
    finally { state.sendingMessage = false; button.disabled = false; }
  });
  const observer = new IntersectionObserver((entries) => { if (entries.some((entry) => entry.isIntersecting)) loadMore(); }, { rootMargin: "500px" }); observer.observe($("feedSentinel"));
  window.addEventListener("hashchange", () => { const hash = location.hash || "#/feed"; if (hash === "#/draw") setView("draw"); else if (hash === "#/rank") setView("rank"); else if (hash === "#/profile") setView("profile"); else setView("feed"); });
  const initialRoute = (location.hash || "#/feed").slice(2);
  if (["feed", "draw", "rank", "profile"].includes(initialRoute)) setView(initialRoute);
  checkSession(); loadMore();
})();
