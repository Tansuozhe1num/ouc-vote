(function () {
  const state = {
    page: 1,
    size: 12,
    loading: false,
    done: false,
    snaps: [],
    sex: "",
    keyword: "",
    currentSnap: null,
    comments: [],
    replyTo: null,
    commentSending: false
  };

  const masonry = document.getElementById("masonry");
  const feedEmpty = document.getElementById("feedEmpty");
  const feedEnd = document.getElementById("feedEnd");
  const searchInput = document.getElementById("searchInput");
  const detailOverlay = document.getElementById("detailOverlay");
  const publishOverlay = document.getElementById("publishOverlay");
  const publishForm = document.getElementById("publishForm");
  const commentForm = document.getElementById("commentForm");
  const toastEl = document.getElementById("toast");
  const nickInput = document.getElementById("commentNick");
  const contentInput = document.getElementById("commentContent");
  const replyBar = document.getElementById("replyBar");
  const replyHint = document.getElementById("replyHint");
  const commentSubmit = document.getElementById("commentSubmit");

  nickInput.value = localStorage.getItem("campus-nick") || "";

  function toast(msg) {
    toastEl.textContent = msg;
    toastEl.classList.remove("hidden");
    clearTimeout(toastEl._t);
    toastEl._t = setTimeout(() => toastEl.classList.add("hidden"), 2200);
  }

  function escapeHtml(str) {
    return String(str || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function sexLabel(sex) {
    return String(sex) === "1" ? "校草" : "校花";
  }

  function firstChar(name) {
    return (name || "?").trim().charAt(0);
  }

  function sameId(a, b) {
    return String(a) === String(b);
  }

  function formatTime(ts) {
    if (!ts) return "";
    const d = typeof ts === "number" ? new Date(ts) : new Date(String(ts).replace(/-/g, "/"));
    if (Number.isNaN(d.getTime())) return "";
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    const h = String(d.getHours()).padStart(2, "0");
    const min = String(d.getMinutes()).padStart(2, "0");
    return `${m}-${day} ${h}:${min}`;
  }

  function visibleSnaps() {
    const kw = state.keyword.trim().toLowerCase();
    return state.snaps.filter((s) => {
      if (state.sex !== "" && String(s.sex) !== String(state.sex)) return false;
      if (!kw) return true;
      const blob = [s.name, s.major, s.desc].join(" ").toLowerCase();
      return blob.indexOf(kw) >= 0;
    });
  }

  function renderFeed() {
    const list = visibleSnaps();
    masonry.innerHTML = list.map((s) => {
      const desc = escapeHtml(s.desc || s.major || s.name || "");
      const oss = s.snapImage || "";
      const img = escapeHtml(OssImageCache.lookup(oss));
      const ossAttr = escapeHtml(oss);
      const name = escapeHtml(s.name || "");
      return `<article class="card" data-id="${escapeHtml(s.snapId)}">
        <img data-oss="${ossAttr}" src="${img}" alt="${name}" loading="lazy" />
        <div class="card-body">
          <p class="card-desc">${desc}</p>
          <div class="card-foot">
            <span class="dot">${escapeHtml(firstChar(s.name))}</span>
            <span>${name}</span>
          </div>
        </div>
      </article>`;
    }).join("");
    feedEmpty.classList.toggle("hidden", list.length > 0 || state.loading);
    OssImageCache.hydrate(masonry);
  }

  async function loadMore() {
    if (state.loading || state.done) return;
    state.loading = true;
    try {
      const data = await SnapApi.list(state.page, state.size);
      const batch = Array.isArray(data) ? data : [];
      if (batch.length === 0 || state.page >= 10) {
        state.done = true;
        feedEnd.classList.toggle("hidden", state.snaps.length === 0);
      } else {
        const ids = new Set(state.snaps.map((s) => String(s.snapId)));
        batch.forEach((item) => {
          if (!ids.has(String(item.snapId))) state.snaps.push(item);
        });
        OssImageCache.warmup(batch.map((item) => item.snapImage));
        state.page += 1;
        if (batch.length < state.size) {
          state.done = true;
          feedEnd.classList.remove("hidden");
        }
      }
      renderFeed();
    } catch (e) {
      toast(e.message || "列表加载失败");
    } finally {
      state.loading = false;
    }
  }

  function clearReplyMode() {
    state.replyTo = null;
    replyBar.classList.add("hidden");
    contentInput.placeholder = "说点好听的…";
    commentSubmit.textContent = "发送";
  }

  function setReplyMode(comment) {
    state.replyTo = comment;
    replyHint.textContent = "回复 @" + (comment.nickname || "同学");
    replyBar.classList.remove("hidden");
    contentInput.placeholder = "回复 @" + (comment.nickname || "同学");
    commentSubmit.textContent = "回复";
    contentInput.focus();
  }

  function countComments(list) {
    return list.reduce((sum, c) => sum + 1 + (c.children ? c.children.length : 0), 0);
  }

  function renderChild(child) {
    return `<li class="child-item">
      <strong>${escapeHtml(child.nickname || "同学")}</strong>
      <p>${escapeHtml(child.content || "")}</p>
      <div class="comment-meta">
        <time>${escapeHtml(formatTime(child.ctime))}</time>
      </div>
    </li>`;
  }

  function renderComments(list) {
    state.comments = list || [];
    const ul = document.getElementById("commentList");
    const empty = document.getElementById("commentEmpty");
    document.getElementById("commentCount").textContent = String(countComments(state.comments));
    ul.innerHTML = state.comments.map((c) => {
      const children = (c.children || []).map(renderChild).join("");
      return `<li class="comment-item" data-comment-id="${escapeHtml(c.commentId)}">
        <div class="comment-main">
          <strong>${escapeHtml(c.nickname || "同学")}</strong>
          <p>${escapeHtml(c.content || "")}</p>
          <div class="comment-meta">
            <time>${escapeHtml(formatTime(c.ctime))}</time>
            <button type="button" class="btn-reply" data-id="${escapeHtml(c.commentId)}">回复</button>
          </div>
        </div>
        ${children ? `<ul class="child-list">${children}</ul>` : ""}
      </li>`;
    }).join("");
    empty.classList.toggle("hidden", state.comments.length > 0);
  }

  async function refreshComments(snapId) {
    const list = await loadSnapComments(snapId);
    if (state.currentSnap && sameId(state.currentSnap.snapId, snapId)) {
      renderComments(list);
    }
    return list;
  }

  async function openDetail(snapId) {
    const id = String(snapId);
    let snap = state.snaps.find((s) => sameId(s.snapId, id));
    if (!snap) {
      try {
        snap = await SnapApi.detail(id);
      } catch (e) {
        toast(e.message || "内容不存在");
        return;
      }
    }
    state.currentSnap = snap;
    clearReplyMode();
    const nextHash = "#/snap/" + encodeURIComponent(id);
    if (location.hash !== nextHash) {
      location.hash = nextHash;
    }
    const detailImage = document.getElementById("detailImage");
    const ossUrl = snap.snapImage || "";
    detailImage.setAttribute("data-oss", ossUrl);
    detailImage.src = OssImageCache.lookup(ossUrl);
    detailImage.alt = snap.name || "";
    OssImageCache.store(ossUrl).then((local) => {
      if (local && state.currentSnap && sameId(state.currentSnap.snapId, id)) {
        detailImage.src = local;
      }
    });
    document.getElementById("detailName").textContent = snap.name || "";
    document.getElementById("detailAvatar").textContent = firstChar(snap.name);
    document.getElementById("detailBadge").textContent = sexLabel(snap.sex);
    const bits = [];
    if (snap.age) bits.push(snap.age + "岁");
    if (snap.major) bits.push(snap.major);
    document.getElementById("detailMeta").textContent = bits.join(" · ");
    document.getElementById("detailDesc").textContent = snap.desc || "这个人很神秘，还没有简介";
    const contact = [];
    if (snap.qq) contact.push("QQ " + snap.qq);
    if (snap.wx) contact.push("微信 " + snap.wx);
    document.getElementById("detailContact").textContent = contact.join("  |  ");
    detailOverlay.classList.remove("hidden");
    renderComments([]);
    try {
      await refreshComments(id);
    } catch (e) {
      toast(e.message || "评论加载失败");
    }
  }

  function closeDetail() {
    detailOverlay.classList.add("hidden");
    state.currentSnap = null;
    state.comments = [];
    clearReplyMode();
    if (location.hash.indexOf("#/snap/") === 0) {
      history.replaceState(null, "", "#/");
    }
  }

  function openPublish() {
    publishOverlay.classList.remove("hidden");
  }

  function closePublish() {
    publishOverlay.classList.add("hidden");
  }

  function route() {
    const hash = location.hash || "#/";
    const match = hash.match(/^#\/snap\/(.+)$/);
    if (match) {
      openDetail(decodeURIComponent(match[1]));
    } else {
      detailOverlay.classList.add("hidden");
    }
  }

  masonry.addEventListener("click", (e) => {
    const card = e.target.closest(".card");
    if (card) {
      location.hash = "#/snap/" + encodeURIComponent(card.getAttribute("data-id"));
    }
  });

  document.getElementById("commentList").addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-reply");
    if (!btn) return;
    const commentId = btn.getAttribute("data-id");
    const comment = state.comments.find((c) => sameId(c.commentId, commentId));
    if (comment) setReplyMode(comment);
  });

  document.getElementById("cancelReply").addEventListener("click", clearReplyMode);

  document.querySelectorAll(".tab").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab").forEach((b) => b.classList.remove("is-active"));
      btn.classList.add("is-active");
      state.sex = btn.getAttribute("data-sex");
      renderFeed();
    });
  });

  let searchTimer;
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      state.keyword = searchInput.value;
      renderFeed();
    }, 180);
  });

  document.getElementById("openPublish").addEventListener("click", openPublish);
  document.getElementById("fabPublish").addEventListener("click", openPublish);
  document.getElementById("closePublish").addEventListener("click", closePublish);
  document.getElementById("closeDetail").addEventListener("click", closeDetail);

  detailOverlay.addEventListener("click", (e) => {
    if (e.target === detailOverlay) closeDetail();
  });
  publishOverlay.addEventListener("click", (e) => {
    if (e.target === publishOverlay) closePublish();
  });

  document.getElementById("avatorInput").addEventListener("change", (e) => {
    const file = e.target.files && e.target.files[0];
    const preview = document.getElementById("coverPreview");
    const hint = document.getElementById("coverHint");
    if (!file) return;
    preview.src = URL.createObjectURL(file);
    preview.classList.remove("hidden");
    hint.classList.add("hidden");
  });

  publishForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const err = document.getElementById("publishError");
    err.classList.add("hidden");
    const fd = new FormData(publishForm);
    if (!fd.get("avator") || !fd.get("avator").size) {
      err.textContent = "请先选择照片";
      err.classList.remove("hidden");
      return;
    }
    const submitBtn = publishForm.querySelector(".btn-submit");
    submitBtn.disabled = true;
    submitBtn.textContent = "发布中…";
    try {
      await SnapApi.save(fd);
      toast("发布成功");
      publishForm.reset();
      document.getElementById("coverPreview").classList.add("hidden");
      document.getElementById("coverHint").classList.remove("hidden");
      closePublish();
      state.page = 1;
      state.done = false;
      state.snaps = [];
      masonry.innerHTML = "";
      feedEnd.classList.add("hidden");
      await loadMore();
    } catch (ex) {
      err.textContent = ex.message || "发布失败";
      err.classList.remove("hidden");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "发布到校园墙";
    }
  });

  commentForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!state.currentSnap || state.commentSending) return;
    const nickname = nickInput.value.trim();
    const content = contentInput.value.trim();
    if (!nickname) {
      toast("请填写昵称");
      return;
    }
    if (!content) {
      toast("请填写评论内容");
      return;
    }
    localStorage.setItem("campus-nick", nickname);
    const snapId = state.currentSnap.snapId;
    state.commentSending = true;
    commentSubmit.disabled = true;
    try {
      if (state.replyTo) {
        await ChildCommentApi.save(snapId, state.replyTo.commentId, nickname, content);
        toast("回复成功");
        clearReplyMode();
      } else {
        await CommentApi.save(snapId, nickname, content);
        toast("评论成功");
      }
      contentInput.value = "";
      await refreshComments(snapId);
    } catch (ex) {
      toast(ex.message || "评论失败");
    } finally {
      state.commentSending = false;
      commentSubmit.disabled = false;
    }
  });

  const observer = new IntersectionObserver((entries) => {
    if (entries.some((x) => x.isIntersecting)) loadMore();
  });
  observer.observe(document.getElementById("feedSentinel"));

  window.addEventListener("hashchange", route);
  loadMore().then(route);
})();
