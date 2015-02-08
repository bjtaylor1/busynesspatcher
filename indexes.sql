--drop them first cos they could be on old tables that imposm has renamed

drop index if exists osm_mainroads_ref;
drop index if exists osm_minorroads_ref;
drop index if exists osm_motorways_ref;
drop index if exists busyness_ref;
drop index if exists osm_mainroads_osm_id;
drop index if exists osm_minorroads_osm_id;
drop index if exists osm_motorways_osm_id;
drop index if exists busyness_geometry;
drop index if exists busyness_category;


create index osm_mainroads_ref on osm_mainroads(ref);
create index osm_minorroads_ref on osm_minorroads(ref);
create index osm_motorways_ref on osm_motorways(ref);

create index osm_mainroads_osm_id on osm_mainroads(osm_id);
create index osm_minorroads_osm_id on osm_minorroads(osm_id);
create index osm_motorways_osm_id on osm_motorways(osm_id);

create index busyness_geometry on busyness using gist(geometry);

create index busyness_ref on busyness(ref);
create index busyness_category on busyness(category);

cluster busyness using busyness_geometry;

vacuum;

