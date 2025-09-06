function setUpMap() {
    const map = new L.Map('map').setView([0, 0], 2);  // Center on world, zoom level 2
    const attribution = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
    const tiles = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', 
                                  {attribution: attribution})
                                 .addTo(map);

    const popup = new L.Popup()
    function onMapClick(e) {
     popup.setLatLng([e.latlng.lat, e.latlng.lng])
          .setContent(`You clicked ${e.latlng}`)
          .openOn(map);
    }
    map.on('click', onMapClick);

}

window.galt = {
  copyInputToClipboard: function(input) {
    input.select();
    navigator.clipboard.writeText(input.value);
    input.blur();
  },
  setUpMap: setUpMap


}
