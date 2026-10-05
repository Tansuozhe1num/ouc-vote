const OSS_CACHE = "vote-oss-sw-v1";
const OSS_HOST = "aliyuncs.com";
const MAX_ENTRIES = 150;

self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

function isOssRequest(request) {
  return request.method === "GET" && request.url.indexOf(OSS_HOST) !== -1;
}

async function trimCache(cache) {
  const keys = await cache.keys();
  if (keys.length <= MAX_ENTRIES) return;
  const extra = keys.length - MAX_ENTRIES;
  for (let i = 0; i < extra; i += 1) {
    await cache.delete(keys[i]);
  }
}

self.addEventListener("fetch", (event) => {
  if (!isOssRequest(event.request)) return;
  event.respondWith(
    (async () => {
      const cache = await caches.open(OSS_CACHE);
      const hit = await cache.match(event.request, { ignoreSearch: true });
      if (hit) return hit;
      const res = await fetch(event.request, {
        mode: "no-cors",
        credentials: "omit"
      });
      cache.put(event.request, res.clone()).then(() => trimCache(cache));
      return res;
    })()
  );
});
