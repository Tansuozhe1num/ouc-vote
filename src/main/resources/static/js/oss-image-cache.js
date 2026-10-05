/**
 * 将 OSS 图片缓存在浏览器 Cache Storage / 内存中，
 * 列表重绘、详情打开、再次访问时优先走本地，减少 OSS GET。
 */
const OssImageCache = (function () {
  const CACHE_NAME = "vote-oss-images-v1";
  const MAX_MEMORY = 120;
  const memory = new Map();
  const inflight = new Map();

  function isOssUrl(url) {
    return typeof url === "string" && /aliyuncs\.com/i.test(url);
  }

  function remember(url, blobUrl) {
    if (memory.size >= MAX_MEMORY) {
      const oldest = memory.keys().next().value;
      const prev = memory.get(oldest);
      if (prev && String(prev).indexOf("blob:") === 0) {
        URL.revokeObjectURL(prev);
      }
      memory.delete(oldest);
    }
    memory.set(url, blobUrl);
  }

  async function openCache() {
    if (!window.caches) return null;
    return caches.open(CACHE_NAME);
  }

  async function blobUrlFromResponse(url, res) {
    if (!res || !res.ok) return "";
    const blob = await res.blob();
    if (!blob || !blob.size) return "";
    const obj = URL.createObjectURL(blob);
    remember(url, obj);
    return obj;
  }

  async function readStored(url) {
    if (memory.has(url)) return memory.get(url);
    const cache = await openCache();
    if (!cache) return "";
    const res = await cache.match(url, { ignoreSearch: true });
    try {
      return await blobUrlFromResponse(url, res);
    } catch (e) {
      return "";
    }
  }

  async function store(url) {
    if (!url) return "";
    if (!isOssUrl(url)) return url;
    if (memory.has(url)) return memory.get(url);
    if (inflight.has(url)) return inflight.get(url);

    const task = (async () => {
      try {
        const hit = await readStored(url);
        if (hit) return hit;
        const res = await fetch(url, { mode: "cors", credentials: "omit" });
        if (!res.ok) return url;
        const cache = await openCache();
        if (cache) {
          await cache.put(url, res.clone());
        }
        const local = await blobUrlFromResponse(url, res);
        return local || url;
      } catch (e) {
        return url;
      } finally {
        inflight.delete(url);
      }
    })();

    inflight.set(url, task);
    return task;
  }

  function lookup(url) {
    if (!url) return "";
    return memory.get(url) || "";
  }

  function warmup(urls) {
    (urls || []).forEach((url) => {
      if (url) store(url);
    });
  }

  async function hydrate(root) {
    const scope = root || document;
    const imgs = scope.querySelectorAll("img[data-oss]");
    await Promise.all(
      Array.from(imgs).map(async (img) => {
        const url = img.getAttribute("data-oss");
        if (!url) return;
        const local = await store(url);
        if (local && img.getAttribute("src") !== local) {
          img.src = local;
        }
      })
    );
  }

  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("./sw.js").catch(function () {});
  }

  return { store, lookup, warmup, hydrate };
})();
