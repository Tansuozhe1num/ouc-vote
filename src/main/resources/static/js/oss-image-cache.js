/**
 * OSS 图片交给浏览器的 img 请求加载。
 * 跨域资源不能通过 fetch 转成 blob，浏览器自己的图片缓存已经足够。
 */
const OssImageCache = (function () {
  // 继续注册轻量 Service Worker，用没有 fetch 拦截器的新版本替换旧版本。
  // 旧版本会用 fetch 代理 OSS 图片，触发 OSS CORS 限制。
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("./sw.js?v=20261013-2").catch(function () {});
  }

  async function store(url) {
    return url || "";
  }

  function lookup(url) {
    return url || "";
  }

  function warmup() {}

  async function hydrate(root) {
    const scope = root || document;
    scope.querySelectorAll("img[data-oss]").forEach((img) => {
      const url = img.getAttribute("data-oss");
      if (url && img.getAttribute("src") !== url) img.src = url;
    });
  }

  return { store, lookup, warmup, hydrate };
})();
