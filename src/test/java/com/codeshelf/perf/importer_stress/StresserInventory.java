package com.codeshelf.perf.importer_stress;
public class StresserInventory extends StresserABS{
	public static void main(String[] args) throws Exception{
		run(ImporterType.Inventory, 2);
	}
}
