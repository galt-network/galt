## UI project for Nomiatim

https://github.com/osm-search/nominatim-ui/tree/master

## Reverse geocoding

UI: https://nominatim.openstreetmap.org/ui/reverse.html?lat=58.653350636402536&lon=25.97851395606995&zoom=18
API: https://github.com/osm-search/Nominatim/blob/master/docs/api/Reverse.md

Query:
```
https://nominatim.openstreetmap.org/reverse?lat=13.735528768357648&lon=-89.37638640403748&zoom=18&format=jsonv2
```

Response:
```json
{
    "place_id": 285113370,
    "licence": "Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
    "osm_type": "way",
    "osm_id": 398197879,
    "lat": "13.7355497",
    "lon": "-89.3763865",
    "category": "shop",
    "type": "hardware",
    "place_rank": 30,
    "importance": 4.5001144426596745e-05,
    "addresstype": "shop",
    "name": "Ferretería La Provincia",
    "display_name": "Ferretería La Provincia, Boulevard Belén, Lourdes, La Libertad, 0515, El Salvador",
    "address": {
        "shop": "Ferretería La Provincia",
        "road": "Boulevard Belén",
        "city": "Lourdes",
        "state": "La Libertad",
        "ISO3166-2-lvl4": "SV-LI",
        "postcode": "0515",
        "country": "El Salvador",
        "country_code": "sv"
    },
    "boundingbox": [
        "13.7355062",
        "13.7355946",
        "-89.3764359",
        "-89.3763378"
    ]
}
```

## Geocoding

UI: https://nominatim.openstreetmap.org/ui/search.html?q=P%C3%B5ltsamaa
API: https://github.com/osm-search/Nominatim/blob/master/docs/api/Search.md

Query:

```
https://nominatim.openstreetmap.org/search?q=P%C3%B5ltsamaa&format=jsonv2
```

Response:
```json
[
    {
        "place_id": 156009018,
        "licence": "Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
        "osm_type": "relation",
        "osm_id": 350813,
        "lat": "58.6548999",
        "lon": "25.9791000",
        "category": "boundary",
        "type": "administrative",
        "place_rank": 18,
        "importance": 0.46774517084930844,
        "addresstype": "town",
        "name": "Põltsamaa linn",
        "display_name": "Põltsamaa linn, Põltsamaa vald, Jõgeva County, Estonia",
        "boundingbox": [
            "58.6383092",
            "58.6689923",
            "25.9544209",
            "25.9993752"
        ]
    }
]
```
