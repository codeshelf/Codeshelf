select order_header.domainid as OrderId, item_master.ddc_id, item_master.domainid as ItemMaster from order_header
join order_detail on order_detail.parent_persistentid = order_header.persistentid
join item_master on order_detail.item_master_persistentid = item_master.persistentid
where order_header.active = true and item_master.ddc_id like '%XS%'

select order_header.domainid, work_instruction.*, item_master.ddc_id from work_instruction
join order_detail on order_detail.persistentid = work_instruction.parent_persistentid
join order_header on order_header.persistentid = order_detail.parent_persistentid
join item_master on item_master.persistentid = order_detail.item_master_persistentid
order by work_instruction.pos_along_path, order_header.domainid

select * from item where item.stored_location_persistentid = '5a1484a0-ad71-11e2-839c-b8f6b111cbaf'

select * FROM container_use WHERE container_use.order_header_persistentid IN (SELECT persistentid FROM order_header WHERE order_header.active = false)


delete from container_use;
delete from container;
delete from work_instruction;
delete from order_detail;
delete from order_header;
delete from order_group;
delete from item;
delete from item_master;

update order_header set status_enum = 'CREATED';
update order_detail set status_enum = 'CREATED';
delete from work_instruction;

drop schema codeshelf CASCADE