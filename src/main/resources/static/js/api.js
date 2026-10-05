/**
 * Snap:
 * GET  /api/snap/list?page=&size=
 * GET  /api/snap/detail?snapId=
 * POST /api/snap/save  multipart
 *
 * Comment:
 * GET  /api/comment/list?snapId=&page=&size=
 * POST /api/comment/save  form: snapId, nickname|userId, content|text
 * POST /api/comment/update|delete
 *
 * ChildComment:
 * GET  /api/childComment/list?parentId=&page=&size=
 * GET  /api/childComment/listBySnap?snapId=&page=&size=
 * POST /api/childComment/save  form: snapId, parentId, nickname|userId, content|text
 * POST /api/childComment/update|delete
 */
const API_BASE = "/api";

async function request(path, options = {}) {
  const res = await fetch(API_BASE + path, options);
  const json = await res.json().catch(() => null);
  if (!res.ok || !json || json.status !== "success" || json.code !== 200) {
    const err = new Error((json && json.info) || "请求失败");
    err.status = res.status;
    err.body = json;
    throw err;
  }
  return json.data;
}

function postForm(path, fields) {
  const body = new URLSearchParams();
  Object.keys(fields).forEach((key) => {
    const value = fields[key];
    if (value !== undefined && value !== null) {
      body.append(key, String(value));
    }
  });
  return request(path, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body
  });
}

function normalizeComment(c) {
  if (!c) return c;
  return {
    commentId: c.commentId,
    snapId: c.snapId,
    nickname: c.userId || c.nickname || "同学",
    content: c.text || c.content || "",
    userId: c.userId || c.nickname,
    text: c.text || c.content,
    ctime: c.ctime,
    status: c.status,
    children: []
  };
}

function normalizeChildComment(c) {
  if (!c) return c;
  return {
    cId: c.cId,
    snapId: c.snapId,
    parentId: c.parentId,
    nickname: c.userId || c.nickname || "同学",
    content: c.text || c.content || "",
    userId: c.userId || c.nickname,
    text: c.text || c.content,
    ctime: c.ctime,
    status: c.status
  };
}

const SnapApi = {
  list(page, size) {
    return request(`/snap/list?page=${page}&size=${size}`);
  },
  detail(snapId) {
    return request(`/snap/detail?snapId=${encodeURIComponent(snapId)}`);
  },
  save(formData) {
    return request("/snap/save", { method: "POST", body: formData });
  }
};

const CommentApi = {
  async list(snapId) {
    const data = await request(
      `/comment/list?snapId=${encodeURIComponent(snapId)}&page=1&size=50`
    );
    return (Array.isArray(data) ? data : []).map(normalizeComment);
  },
  save(snapId, nickname, content) {
    return postForm("/comment/save", { snapId, nickname, content }).then(normalizeComment);
  },
  update(commentId, content) {
    return postForm("/comment/update", { commentId, content }).then(normalizeComment);
  },
  remove(commentId) {
    return postForm("/comment/delete", { commentId });
  }
};

const ChildCommentApi = {
  async listBySnap(snapId) {
    const data = await request(
      `/childComment/listBySnap?snapId=${encodeURIComponent(snapId)}&page=1&size=50`
    );
    return (Array.isArray(data) ? data : []).map(normalizeChildComment);
  },
  async listByParent(parentId) {
    const data = await request(
      `/childComment/list?parentId=${encodeURIComponent(parentId)}&page=1&size=50`
    );
    return (Array.isArray(data) ? data : []).map(normalizeChildComment);
  },
  save(snapId, parentId, nickname, content) {
    return postForm("/childComment/save", {
      snapId,
      parentId,
      nickname,
      content
    }).then(normalizeChildComment);
  },
  remove(cId) {
    return postForm("/childComment/delete", { cId });
  }
};

/**
 * 拉取一级评论 + 子评论，并按 parentId 挂到 children
 */
async function loadSnapComments(snapId) {
  const [parents, children] = await Promise.all([
    CommentApi.list(snapId),
    ChildCommentApi.listBySnap(snapId).catch(() => [])
  ]);
  const map = {};
  parents.forEach((p) => {
    p.children = [];
    map[String(p.commentId)] = p;
  });
  children.forEach((child) => {
    const parent = map[String(child.parentId)];
    if (parent) parent.children.push(child);
  });
  return parents;
}
