const API_BASE = "";

async function request(path, options = {}) {
  const res = await fetch(API_BASE + path, { credentials: "same-origin", ...options });
  const json = await res.json().catch(() => null);
  if (!res.ok || !json || json.status !== "success" || json.code !== 200) {
    const err = new Error((json && json.info) || "请求失败"); err.status = res.status; err.body = json; throw err;
  }
  return json.data;
}

function postForm(path, fields) {
  const body = new URLSearchParams();
  Object.keys(fields).forEach((key) => { const value = fields[key]; if (value !== undefined && value !== null) body.append(key, String(value)); });
  return request(path, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body });
}

const AuthApi = {
  captcha: () => request("/auth/captcha"),
  login: (data) => postForm("/auth/login", data),
  register: (data) => postForm("/auth/register", data),
  wechatLogin: (data) => postForm("/auth/wechat/login", data),
  me: () => request("/auth/me"),
  logout: () => postForm("/auth/logout", {})
};

const SnapApi = {
  list(page, size, type) { const filter = type === "" || type == null ? "" : `&type=${encodeURIComponent(type)}`; return request(`/snap/list?page=${page}&size=${size}${filter}`); },
  detail(snapId) { return request(`/snap/detail?snapId=${encodeURIComponent(snapId)}`); },
  mine(page, size) { return request(`/snap/mine?page=${page}&size=${size}`); },
  save(formData, idempotencyKey) {
    return request("/snap/save", { method: "POST", headers: { "Idempotency-Key": idempotencyKey }, body: formData });
  },
  draw() { return request("/snap/draw"); },
  status(snapId, status) { return postForm("/snap/status", { snapId, status }); }
};

const UserApi = {
  profile(userId) { return request(`/user/profile?userId=${encodeURIComponent(userId)}`); },
  update(data) { return postForm("/user/profile", data); },
  toggleFollow(userId) { return postForm("/follow/toggle", { userId }); },
  isFollowing(userId) { return request(`/follow/status?userId=${encodeURIComponent(userId)}`); },
  following() { return request("/follow/following"); },
  followers() { return request("/follow/followers"); }
};

const MessageApi = {
  send(receiverId, content, snapId) { return postForm("/message/send", { receiverId, content, snapId }); },
  conversation(peerId) { return request(`/message/conversation?peerId=${encodeURIComponent(peerId)}`); },
  inbox() { return request("/message/inbox"); }
};

const CommentApi = {
  async list(snapId) {
    const data = await request(`/comment/list?snapId=${encodeURIComponent(snapId)}&page=1&size=50`);
    return (Array.isArray(data) ? data : []).map((c) => ({ commentId: c.commentId, nickname: c.userId || c.nickname || "同学", content: c.text || c.content || "", ctime: c.ctime }));
  },
  save(snapId, content) { return postForm("/comment/save", { snapId, content }); }
};

async function loadSnapComments(snapId) { return CommentApi.list(snapId); }
