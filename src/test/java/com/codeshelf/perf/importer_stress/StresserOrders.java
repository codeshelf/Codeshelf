package com.codeshelf.perf.importer_stress;
public class StresserOrders extends StresserABS{
	public static void main(String[] args) throws Exception{
		run(ImporterType.Orders, 2);
	}
}