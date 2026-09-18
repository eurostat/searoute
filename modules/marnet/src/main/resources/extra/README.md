# Extra maritime lines

Every `*.gpkg` in this folder is loaded by `MarnetBuilding` and merged with `marnet.gpkg`
before the networks are generalised. Use it to add fairways the ORNL network lacks without
editing the original dataset. Lines must be `LineString`s in WGS84 (EPSG:4326) and should
end exactly on an existing line of `marnet.gpkg` (or cross one), otherwise the connected-
components filter drops them as a disconnected fragment.

## hagland_ais_tracks.gpkg

Fairway lines along the Norwegian coast and inside its fjords, traced from the AIS/ScanReach
positions of the Hagland Shipping fleet (8 vessels, ~350 000 positions, March–September 2026).
Contributed by Hagland Shipping AS under the repository licence (EUPL-1.2).

How the lines were derived:

1. Positions under way (speed ≥ 1.5 kn) were joined into tracks per vessel, split at gaps of
   more than 20 minutes or jumps of more than 6 km, and simplified (Douglas–Peucker, 50 m).
2. Only stretches more than 5 km from the existing 5 km network were kept, plus stretches
   3–5 km away where at least two voyages repeat them. This keeps the open-sea lanes the
   network already has out of the file: of ~300 000 km of tracks, ~19 000 km survived.
3. Each stretch's ends were snapped onto the nearest line of `marnet.gpkg` (distance computed
   in kilometres, not degrees) so that `planifyLines` connects them.

Effect, measured against 2 559 voyage legs with distances recorded by the crews: legs the
service could not route fell from 58 to 33, legs more than 30 % off from 145 to 84, and pairs
such as Eidsnes–Utne, Halden–Sarpsborg or Sunndalsøra–Surnadal now come within a few percent
of the recorded distance (at the 5 km and 10 km resolutions; the 20 km generalisation merges
edges shorter than ~10 km and straightens fjord bends).

## Building

`MarnetBuilding` needs the JTS OverlayNG engine, otherwise the union in `planifyLines` fails
with "found non-noded intersection" on mixed-source input:

```
java -Djts.overlay=ng -Xss16m -Xmx4g -cp ... eu.europa.ec.eurostat.searoute.MarnetBuilding [20km,10km,5km]
```

The optional argument limits the resolutions built.
