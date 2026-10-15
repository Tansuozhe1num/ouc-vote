(function () {
  const types = ["学习搭子", "旅游搭子", "恋爱搭子", "其他活动"];
  const state = { page: 1, size: 16, loading: false, done: false, snaps: [], type: "", keyword: "", user: null, currentSnap: null, drawResult: null, authMode: "login", captchaKey: null, chatPeer: null, chatSnapId: null, viewedUser: null, publishing: false, publishRequestId: null };
  const $ = (id) => document.getElementById(id);
  const masonry = $("masonry"), feedEmpty = $("feedEmpty"), feedEnd = $("feedEnd"), searchInput = $("searchInput");

  function toast(message) { const el = $("toast"); el.textContent = message; el.classList.remove("hidden"); clearTimeout(el._timer); el._timer = setTimeout(() => el.classList.add("hidden"), 2400); }
  function escapeHtml(value) { return String(value == null ? "" : value).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
  function firstChar(name) { return String(name || "同").trim().charAt(0) || "同"; }
  function newRequestId() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") return window.crypto.randomUUID();
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
  function typeName(type) { return types[Number(type)] || types[3]; }
  const feedIntroCopy = {
    "": { title: "在校园里，遇见刚好同频的人。", copy: "自习、出发、运动，下一段日常可以一起。" },
    "0": { title: "校园里，找到一起学习的伙伴。", copy: "找一位学习伙伴，分享学习进度，查漏补缺，取长补短。" },
    "1": { title: "说走就走。", copy: "叫上几位伙伴，来一场说走就走的旅程。" },
    "2": { title: "心动不如行动。", copy: "发布动态或主动出击，找到心仪的 ta。" }
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
  function resetFeed() { state.page = 1; state.done = false; state.snaps = []; masonry.innerHTML = ""; feedEnd.classList.add("hidden"); loadMore(); }
  async function loadMore() {
    if (state.loading || state.done || $("feedView").classList.contains("hidden")) return;
    state.loading = true;
    try {
      const data = await SnapApi.list(state.page, state.size, state.type);
      const batch = Array.isArray(data) ? data : [];
      const known = new Set(state.snaps.map((snap) => String(snapId(snap))));
      batch.forEach((snap) => { if (!known.has(String(snapId(snap)))) state.snaps.push(snap); });
      if (batch.length) OssImageCache.warmup(batch.map((snap) => snap.snapImage).filter(Boolean));
      state.page++;
      if (batch.length < state.size || batch.length === 0) { state.done = true; feedEnd.classList.toggle("hidden", state.snaps.length === 0); }
      renderFeed();
    } catch (e) { toast(e.message || "动态加载失败"); }
    finally { state.loading = false; }
  }

  function setView(view) {
    ["feed", "draw", "profile"].forEach((key) => $(key + "View").classList.toggle("hidden", key !== view));
    document.querySelectorAll("[data-view]").forEach((button) => button.classList.toggle("is-active", button.dataset.view === view));
    if (view === "feed") { location.hash = "#/feed"; }
    if (view === "draw") location.hash = "#/draw";
    if (view === "profile") { location.hash = "#/profile"; loadProfile(); }
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function showOverlay(id) { $(id).classList.remove("hidden"); document.body.style.overflow = "hidden"; }
  function hideOverlay(id) { $(id).classList.add("hidden"); if (!["detailOverlay", "publishOverlay", "profileEditOverlay", "authOverlay", "inboxOverlay", "chatOverlay", "listOverlay"].some((key) => !$(key).classList.contains("hidden"))) document.body.style.overflow = ""; }
  function openAuth(mode) { setAuthMode(mode || "login"); showOverlay("authOverlay"); refreshCaptcha(); }
  function setAuthMode(mode) {
    state.authMode = mode;
    document.querySelectorAll("[data-auth]").forEach((button) => button.classList.toggle("is-active", button.dataset.auth === mode));
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
    let snap = state.snaps.find((item) => String(snapId(item)) === String(id));
    if (!snap && state.drawResult && String(snapId(state.drawResult)) === String(id)) snap = state.drawResult;
    if (!snap) { try { snap = await SnapApi.detail(id); } catch (e) { toast(e.message || "动态不存在"); return; } }
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
    $("detailChat").disabled = own;
    $("detailChat").classList.toggle("hidden", own);
    $("detailAuthor").dataset.userId = authorId;
    $("followButton").dataset.userId = authorId;
    $("followButton").classList.remove("is-following"); $("followButton").textContent = "＋ 关注";
    if (state.user && !own) {
      UserApi.isFollowing(authorId).then((following) => {
        if (state.currentSnap === snap) { $("followButton").classList.toggle("is-following", following); $("followButton").textContent = following ? "✓ 已关注" : "＋ 关注"; }
      }).catch(() => {});
    }
    try { const comments = await loadSnapComments(id); renderComments(comments); } catch (_) { renderComments([]); }
    showOverlay("detailOverlay");
  }
  function renderComments(comments) {
    $("commentList").innerHTML = comments.map((comment) => `<li class="comment-item"><strong>${escapeHtml(comment.nickname || "同学")}</strong><p>${escapeHtml(comment.content || "")}</p></li>`).join("");
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
    if (!peerId || String(currentUserId(state.user)) === peerId) { toast("这是你自己的动态"); return; }
    state.chatPeer = { userId: peerId, name }; state.chatSnapId = snapIdValue || null;
    $("chatName").textContent = name || "同学"; $("chatAvatar").textContent = firstChar(name); $("chatMessages").innerHTML = "";
    $("chatAvatar").dataset.userId = peerId;
    showOverlay("chatOverlay"); await loadConversation(); $("chatInput").focus();
  }
  async function loadConversation() {
    try {
      const messages = await MessageApi.conversation(state.chatPeer.userId);
      $("chatMessages").innerHTML = messages.map((message) => `<div class="message-bubble ${String(message.senderId) === String(currentUserId(state.user)) ? "mine" : ""}">${escapeHtml(message.content)}</div>`).join("");
      $("chatMessages").scrollTop = $("chatMessages").scrollHeight;
    } catch (e) { toast(e.message || "聊天记录加载失败"); }
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
  $("drawCard").addEventListener("click", (event) => {
    if (event.target.closest("#drawChat") || !state.drawResult || $("drawCard").classList.contains("is-drawing")) return;
    openDetail(snapId(state.drawResult));
  });
  document.querySelectorAll("[data-view]").forEach((button) => button.addEventListener("click", () => setView(button.dataset.view)));
  document.querySelectorAll("[data-type]").forEach((button) => button.addEventListener("click", () => {
    document.querySelectorAll("[data-type]").forEach((item) => {
      const selected = item === button;
      item.classList.toggle("is-active", selected);
      item.setAttribute("aria-pressed", selected ? "true" : "false");
    });
    state.type = button.dataset.type; updateFeedIntro(state.type); resetFeed();
  }));
  let searchTimer;
  searchInput.addEventListener("input", () => { clearTimeout(searchTimer); searchTimer = setTimeout(() => { state.keyword = searchInput.value; renderFeed(); }, 160); });
  $("loginButton").addEventListener("click", () => openAuth("login")); $("userChip").addEventListener("click", () => setView("profile"));
  $("openPublish").addEventListener("click", () => state.user ? showOverlay("publishOverlay") : openAuth("login"));
  $("mobilePublish").addEventListener("click", () => state.user ? showOverlay("publishOverlay") : openAuth("login"));
  $("mobileProfile").addEventListener("click", () => setView("profile"));
  $("drawButton").addEventListener("click", runDraw); $("drawChat").addEventListener("click", () => state.drawResult && openChat(state.drawResult.userId, state.drawResult.userName, snapId(state.drawResult)));
  $("detailChat").addEventListener("click", () => state.currentSnap && openChat(state.currentSnap.userId, state.currentSnap.userName || state.currentSnap.name, snapId(state.currentSnap)));
  $("detailAuthor").addEventListener("click", () => { const uid = $("detailAuthor").dataset.userId; if (state.user && String(uid) === String(currentUserId(state.user))) { hideOverlay("detailOverlay"); setView("profile"); } else toast("打开同学主页"); });
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
  document.querySelectorAll("[data-auth]").forEach((button) => button.addEventListener("click", () => setAuthMode(button.dataset.auth)));
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
  document.querySelectorAll("[data-post-type]").forEach((button) => button.addEventListener("click", () => {
    document.querySelectorAll("[data-post-type]").forEach((option) => {
      const selected = option === button;
      option.classList.toggle("is-selected", selected);
      option.setAttribute("aria-pressed", selected ? "true" : "false");
    });
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
      form.reset(); document.querySelectorAll("[data-post-type]").forEach((option) => { const selected = option.dataset.postType === "0"; option.classList.toggle("is-selected", selected); option.setAttribute("aria-pressed", selected ? "true" : "false"); }); $("postType").value = "0"; $("coverPreview").classList.add("hidden"); $("coverHint").classList.remove("hidden"); $("descCount").textContent = "0"; hideOverlay("publishOverlay"); state.type = ""; updateFeedIntro(state.type); document.querySelectorAll("[data-type]").forEach((button) => { const selected = button.dataset.type === ""; button.classList.toggle("is-active", selected); button.setAttribute("aria-pressed", selected ? "true" : "false"); }); resetFeed(); toast("动态发布成功");
    } catch (e) { error.textContent = e.message || "发布失败"; error.classList.remove("hidden"); }
    finally { state.publishing = false; button.disabled = false; button.innerHTML = button.dataset.originalText || "发布到广场 <span>→</span>"; }
  });
  $("commentForm").addEventListener("submit", async (event) => {
    event.preventDefault(); if (!state.currentSnap) return;
    if (!state.user) { toast("请先登录后留言"); openAuth("login"); return; }
    try { await CommentApi.save(snapId(state.currentSnap), $("commentContent").value.trim()); $("commentContent").value = ""; renderComments(await loadSnapComments(snapId(state.currentSnap))); }
    catch (e) { toast(e.message || "评论失败"); }
  });
  $("chatForm").addEventListener("submit", async (event) => {
    event.preventDefault(); if (!state.chatPeer) return; const input = $("chatInput"), content = input.value.trim(); if (!content) return;
    try { await MessageApi.send(state.chatPeer.userId, content, state.chatSnapId); state.chatSnapId = null; input.value = ""; await loadConversation(); }
    catch (e) { toast(e.message || "消息发送失败"); }
  });
  const observer = new IntersectionObserver((entries) => { if (entries.some((entry) => entry.isIntersecting)) loadMore(); }, { rootMargin: "500px" }); observer.observe($("feedSentinel"));
  window.addEventListener("hashchange", () => { const hash = location.hash || "#/feed"; if (hash === "#/draw") setView("draw"); else if (hash === "#/profile") setView("profile"); else setView("feed"); });
  checkSession(); loadMore();
})();
