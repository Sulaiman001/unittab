


select categoryId from
(
	select count(u.id) as ucnt, c.name, c.id as categoryId 
	from unit u join category c on u.categoryId=c.id
	group by c.id
)
where ucnt <= 1;



select * from category where id in 
(
	select categoryId from
	(
		select count(u.id) as ucnt, c.name, c.id as categoryId 
		from unit u join category c on u.categoryId=c.id
		group by c.id
	)
	where ucnt <= 1
);




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
