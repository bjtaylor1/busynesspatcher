
create table busyness(
	ref character varying(255) not null
,	category character (2) not null
,	Score1 float not null
,	Score2 float not null
,	Score3 float not null
, ScoreForCatUnweighted float not null
);

select AddGeometryColumn('busyness', 'geometry', 900913, 'polygon', 2);

alter table busyness alter column "geometry" set not null;


