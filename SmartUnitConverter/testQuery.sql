select * from unit where categoryId=47;

select ub.name, ub.id, c.fx, ut.id, ut.name from conversion c
left join unit ub on c.base = ub.id
join unit ut on c.target = ut.id
where ub.categoryId = 47;
