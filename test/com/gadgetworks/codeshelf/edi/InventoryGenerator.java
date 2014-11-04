package com.gadgetworks.codeshelf.edi;

import java.sql.Timestamp;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.google.common.collect.Lists;

public class InventoryGenerator {

	private InventoryCsvImporter itemImporter;
	
	public InventoryGenerator(InventoryCsvImporter itemImporter) {
		this.itemImporter = itemImporter;
	}
	
	public void setupVirtuallySlottedInventory(Aisle aisle, int itemsPerTier) {
		List<InventorySlottedCsvBean> inventoryRecords = Lists.newArrayList();
		List<Tier> tiers = aisle.getActiveChildrenAtLevel(Tier.class);
		for (Tier tier : tiers) {
			Double maxWidth = tier.getLocationWidthMeters();
			for (int i = 0; i < itemsPerTier; i++) {
				int spacedPosition = (int) (maxWidth*100/(double)itemsPerTier * (double) i);
				InventorySlottedCsvBean  record = generateInventoryRecord(tier, spacedPosition);
				inventoryRecords.add(record);
			}
		}
		itemImporter.importSlottedInventory(inventoryRecords, aisle.getParent(), new Timestamp(System.currentTimeMillis()));
	}

	
	private InventorySlottedCsvBean generateInventoryRecord(ILocation<?> location, int cmFromLeft) {
		InventorySlottedCsvBean bean = new InventorySlottedCsvBean();
		bean.setItemId(generateItemId());
		bean.setDescription(generateString());
		bean.setUom(generateUom());
		bean.setQuantity(String.valueOf(generateQuantity()));
		bean.setCmFromLeft(String.valueOf(cmFromLeft));
		bean.setLocationId(location.getNominalLocationId());
		bean.setInventoryDate(generatePastDate().toGMTString());
		return bean;
	}
	
	private String generateItemId() {		
		return generateString();
	}

	private String generateUom() {
		return generateString();
	}
	
	private int generateQuantity() {
		return generateInt();
	}

	
	private String generateString() {
		return UUID.randomUUID().toString();
	}
	
	private int generateInt() {
		return new Random(System.currentTimeMillis()).nextInt(99);
	}
	
	private Timestamp generatePastDate() {
		return new Timestamp(System.currentTimeMillis() - generateInt());
	}
	
}