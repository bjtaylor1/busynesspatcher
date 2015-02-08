drop table if exists weightings;


create table weightings(
category character(2) unique not null primary key
, weighting float not null);

insert into weightings(category, weighting) values
 ('PR', -0.5) --A-road, rural
,('PU', 0)    --A-road, urban
,('TR', 0.5)  --Trunk road, rural
,('TU', 0.5)  --Trunk road, urban
,('BR', -1.0) --B-road, rural
,('BU', 0)    --B-road, urban
,('CR', -0.8) --C-road, rural
,('CU', 0)    --C-road, urban
,('UR', -0.7) --U-road, rural
,('UU', 0)    --U-road, urban
;

drop index if exists weightings_category;
create index weightings_category on weightings(category);
cluster weightings using weightings_category;
vacuum;

