select order_header.domainid as OrderId, item_master.ddc_id, item_master.domainid as ItemMaster from order_header
join order_detail on order_detail.parent_persistentid = order_header.persistentid
join item_master on order_detail.item_master_persistentid = item_master.persistentid
where order_header.active = true and item_master.ddc_id like '%XS%'

select order_header.domainid, work_instruction.*, item_master.ddc_id from work_instruction
join order_detail on order_detail.persistentid = work_instruction.parent_persistentid
join order_header on order_header.persistentid = order_detail.parent_persistentid
join item_master on item_master.persistentid = order_detail.item_master_persistentid
order by work_instruction.pos_along_path, order_header.domainid

select * FROM container_use WHERE container_use.order_header_persistentid IN (SELECT persistentid FROM order_header WHERE order_header.active = false)

# Delete all of the production pick data, but leave the facility setup.
delete from container_use;
delete from container;
delete from work_instruction;
delete from order_location;
delete from order_detail;
delete from order_header;
delete from order_group;
delete from item;
delete from item_master;

# reset all of the WIs and orders to rerun a pick operation.
update order_header set status_enum = 'CREATED';
update order_detail set status_enum = 'CREATED';
delete from work_instruction;

# nuke the schema and start over from scratch when the app starts.
drop schema codeshelf CASCADE

# cleanup all locations except the facility
delete from vertex where vertex.pos_type_enum = 'METERS_PARENT';
delete from location where location.dtype = 'SLOT';
delete from location where location.dtype = 'TIER';
delete from location where location.dtype = 'BAY';
delete from location where location.dtype = 'AISLE';
delete from path_segment;
delete from path;

# Select all of the slots in LED position order to get a sense of the electronics wiring relative to path distance.
select
aisle.domainid, bay.domainid, tier.domainid, slot.domainid, slot.first_led_num_along_path, slot.last_led_num_along_path, slot.pos_along_path, led_controller.domainid
from location as aisle
join location as bay on aisle.persistentid = bay.parent_persistentid
join location as tier on bay.persistentid = tier.parent_persistentid
join location as slot on tier.persistentid = slot.parent_persistentid
join led_controller on led_controller.persistentid = slot.led_controller_persistentid
where aisle.domainid like 'A%'
order by bay.first_led_num_along_path, tier.first_led_num_along_path, slot.first_led_num_along_path;

# Select work instructions in "group sort code" order and show the location names (to help understand if the order is correct)
select aisle.domainid, bay.domainid, tier.domainid, slot.domainid from work_instruction
join location as slot on slot.persistentid = work_instruction.location_persistentid
join location as tier on tier.persistentid = slot.parent_persistentid
join location as bay on bay.persistentid = tier.parent_persistentid
join location as aisle on aisle.persistentid = bay.parent_persistentid
order by work_instruction.group_and_sort_code;

# See a cross-batch orders location by the item id
select order_header.domainid, order_header.active, item_master.description, order_location.domainid, location_alias.domainid from order_header
join order_detail on order_detail.parent_persistentid = order_header.persistentid
join item_master on item_master.persistentid = order_detail.item_master_persistentid
join order_location on order_location.parent_persistentid = order_header.persistentid
join location on location.persistentid = order_location.location_persistentid
join location_alias on location_alias.mapped_location_persistentid = location.persistentid
where item_master.domainid = '500e08946c106c0300000012';
