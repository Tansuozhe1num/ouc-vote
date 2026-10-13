self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(Promise.all([
    self.clients.claim(),
    caches.delete("vote-oss-sw-v1"),
    caches.delete("vote-oss-sw-v2")
  ]));
});
