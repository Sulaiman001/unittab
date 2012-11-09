select * from unit where shortName="VND";


select categoryId, count(id) as c from unit 
group by categoryid
having c<2
;



select * from category where id = 11;


update "category" set "enabled"="0" where "id"="11";



select * from unit 
order by lower(name) asc;


728 1208 1210

;
select * from enumValue where id = 723;
select * from unit where id = 1208;



select * from category where enabled = 0;




SELECT conversion.* FROM conversion JOIN unit ON conversion.base = unit.id WHERE unit.categoryId = ?





;


dssdsd




;


-- Describe CONVERSION
CREATE TABLE "corresponding" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "enumId1" INTEGER NOT NULL,
    "enumId2" INTEGER NOT NULL,
    FOREIGN KEY("enumId1") REFERENCES "enumvalue"(id),
    FOREIGN KEY("enumId2") REFERENCES "enumvalue"(id)
);

8888



-- Clothing Size - Women
insert into unit(id, categoryId, name) values (1207, 7, "Autralia");
insert into unit(id, categoryId, name) values (1208, 7, "France");
insert into unit(id, categoryId, name) values (1209, 7, "Germany")
insert into unit(id, categoryId, name) values (1210, 7, "Italy");
insert into unit(id, categoryId, name) values (1211, 7, "Japan");
insert into unit(id, categoryId, name) values (1212, 7, "U.K")
insert into unit(id, categoryId, name) values (1213, 7, "U.S");
insert into unit(id, categoryId, name) values (1214, 7, "U.S.x");

-- Clothing Size - Women / Autralia
insert into enumvalue(id, unitid, "value") values (1, 1207, "6");
insert into enumvalue(id, unitid, "value") values (2, 1207, "8");
insert into enumvalue(id, unitid, "value") values (3, 1207, "10");
insert into enumvalue(id, unitid, "value") values (4, 1207, "12");
insert into enumvalue(id, unitid, "value") values (5, 1207, "14");
insert into enumvalue(id, unitid, "value") values (6, 1207, "16");
insert into enumvalue(id, unitid, "value") values (7, 1207, "18");
insert into enumvalue(id, unitid, "value") values (8, 1207, "20");
insert into enumvalue(id, unitid, "value") values (9, 1207, "22");
insert into enumvalue(id, unitid, "value") values (10, 1207, "24");
insert into enumvalue(id, unitid, "value") values (11, 1207, "26");
insert into enumvalue(id, unitid, "value") values (12, 1207, "28");

-- Clothing Size - Women / France
insert into enumvalue(id, unitid, "value") values (13, 1208, "32");
insert into enumvalue(id, unitid, "value") values (14, 1208, "34");
insert into enumvalue(id, unitid, "value") values (15, 1208, "36");
insert into enumvalue(id, unitid, "value") values (16, 1208, "38");
insert into enumvalue(id, unitid, "value") values (16, 1208, "40");
insert into enumvalue(id, unitid, "value") values (17, 1208, "42");
insert into enumvalue(id, unitid, "value") values (18, 1208, "44");
insert into enumvalue(id, unitid, "value") values (19, 1208, "46");
insert into enumvalue(id, unitid, "value") values (20, 1208, "48");
insert into enumvalue(id, unitid, "value") values (21, 1208, "50");
insert into enumvalue(id, unitid, "value") values (22, 1208, "52");
insert into enumvalue(id, unitid, "value") values (23, 1208, "54");
insert into enumvalue(id, unitid, "value") values (24, 1208, "56");

-- Clothing Size - Women / Germany


;
sdsdsdsdsdq
;
-- Describe CONVERSION
CREATE TABLE "enumValue" (
	"id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
	"unitId" INTEGER NOT NULL,
	"value" TEXT,
	FOREIGN KEY("unitId") REFERENCES "unit"(id)
);



1206
;

select * from unit where categoryId in
(
	select categoryId from
	(
		select count(u.id) as ucnt, c.name, c.id as categoryId 
		from unit u join category c on u.categoryId=c.id
		group by c.id
	)
	where ucnt <= 1
);


select * from
	(
		select count(u.id) as ucnt, c.name, c.id as categoryId 
		from unit u join category c on u.categoryId=c.id
		group by c.id
	)
	where ucnt <= 1;




update category set enabled = 0 
where id in 
(
	select categoryId from
	(
		select count(u.id) as ucnt, c.name, c.id as categoryId 
		from unit u join category c on u.categoryId=c.id
		group by c.id
	)
	where ucnt <= 1
);


select * from category where id=50;

select * from unit where id=685;

select c.id, ub.name, ub.id, c.fx, ut.id, ut.name from conversion c
left join unit ub on c.base = ub.id
join unit ut on c.target = ut.id
where ub.categoryId = 47 
and
c.id=604
;


select c.id, ub.name, ub.id, c.formula, ut.id, ut.name from conversion c
left join unit ub on c.base = ub.id
join unit ut on c.target = ut.id
where c.formula is not null;

select * from category where name like "%Fuel consum%";

select * from unit where categoryId=32;

select * from conversion where formula like "%e%" or reversedFormula like "%e%";