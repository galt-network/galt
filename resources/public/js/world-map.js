// World map page: auto-hiding top navigation bar.
// Shown at load, slides up after a fixed delay or on first scroll.
// The visible handle strip can be clicked or dragged down to reveal the menu.
// After a manual reveal the menu stays open until the user interacts with
// the map (globe canvas) or the HUD - then it hides again. The initial
// 3s auto-hide timer is never re-armed after a manual reveal.
(function () {
  var wrap = document.getElementById('world-map-navbar-wrap');
  var handle = document.getElementById('world-map-handle');
  if (!wrap || !handle) return;

  var HIDE_DELAY_MS = 3000;
  var DRAG_THRESHOLD = 24; // px of downward drag to reveal
  var CLICK_MOVE_MS = 400; // pointerup within this time is a click

  var hidden = false;
  var revealed = false; // user intentionally revealed the menu
  var scrolled = false;
  // One-shot timer for the initial auto-hide; never re-armed after a reveal.
  var timer = setTimeout(hide, HIDE_DELAY_MS);

  function hide() {
    if (hidden) return;
    hidden = true;
    revealed = false;
    wrap.classList.add('hidden');
    removeMapHudListeners();
  }

  function reveal() {
    if (!hidden) return;
    hidden = false;
    revealed = true;
    clearTimeout(timer); // cancel the initial auto-hide, no re-arm
    wrap.classList.remove('hidden');
    addMapHudListeners();
  }

  function onScroll() {
    if (scrolled || revealed) return;
    scrolled = true;
    clearTimeout(timer);
    if (window.scrollY > 0) hide();
  }

  var dragging = false;
  var startY = 0;
  var maxDelta = 0;
  var downAt = 0;

  function onPointerDown(e) {
    dragging = true;
    startY = e.clientY;
    maxDelta = 0;
    downAt = Date.now();
  }

  function onPointerMove(e) {
    if (!dragging) return;
    var dy = e.clientY - startY;
    if (dy > maxDelta) maxDelta = dy;
    if (hidden && maxDelta > DRAG_THRESHOLD) reveal();
  }

  function onPointerUp(e) {
    if (!dragging) return;
    dragging = false;
    var dy = e.clientY - startY;
    var isClick = Math.abs(dy) < 8 && Date.now() - downAt < CLICK_MOVE_MS;
    if (isClick) {
      if (hidden) {
        reveal();
      } else {
        hidden = true;
        revealed = false;
        wrap.classList.add('hidden');
        removeMapHudListeners();
      }
    }
  }

  // Interactions with the map (globe canvas inside #app) or the HUD hide the
  // navbar again after a manual reveal. Capture-phase document-level listeners
  // because #hud is rendered by the globo UI after this script runs.
  function isMapOrHudTarget(e) {
    var t = e.target;
    return t && t.closest && t.closest('#app, #hud');
  }

  function onMapHudPointerDown(e) {
    if (revealed && isMapOrHudTarget(e)) hide();
  }

  function onMapHudWheel(e) {
    if (revealed && isMapOrHudTarget(e)) hide();
  }

  function onMapHudTouchStart(e) {
    if (revealed && isMapOrHudTarget(e)) hide();
  }

  function addMapHudListeners() {
    document.addEventListener('pointerdown', onMapHudPointerDown, true);
    document.addEventListener('wheel', onMapHudWheel, true);
    document.addEventListener('touchstart', onMapHudTouchStart, true);
  }

  function removeMapHudListeners() {
    document.removeEventListener('pointerdown', onMapHudPointerDown, true);
    document.removeEventListener('wheel', onMapHudWheel, true);
    document.removeEventListener('touchstart', onMapHudTouchStart, true);
  }

  handle.addEventListener('pointerdown', onPointerDown);
  window.addEventListener('pointermove', onPointerMove);
  window.addEventListener('pointerup', onPointerUp);
  window.addEventListener('scroll', onScroll, { passive: true });
})();
