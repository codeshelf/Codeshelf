package com.codeshelf.perf.importer_stress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class StresserABS {
	public enum ImporterType {Aisle, Location, Inventory, Orders}

	private static Integer threadCounter = 0;
	private static Integer errorCode = 0;
	
	public static Integer run(ImporterType type, int numThreads) throws Exception{
		threadCounter = 0;
		errorCode = 0;
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		Importer.init();
		for (int i = 0; i < numThreads; i++) {
			executorService.execute(new StresserThread(type));
		}
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);
		System.out.println(type + " Done");
		return errorCode;
	}
	
	static class StresserThread implements Runnable{
		
		private int threadId = 0;
		private ImporterType type;
		
		public StresserThread(ImporterType type) {
			this.type = type;
			synchronized (threadCounter) {
				threadId = ++threadCounter;
			}
		}
		
		@Override
		public void run() {
			System.out.println("Run " + type + " " + threadId);
			int response = 0;
			try {
				Importer importer = new Importer();
				if (type == ImporterType.Aisle) {
					response = importer.postAisles();
				} else if (type == ImporterType.Location) {
					response = importer.postLocations();
				} else if (type == ImporterType.Inventory) {
					response = importer.postInventory();
				} else if (type == ImporterType.Orders) {
					response = importer.postOrders();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			synchronized (errorCode) {
				if (response < 200 || response >= 400) {
					errorCode = response;
				}
			}
			System.out.println("Done " + threadId);
		}
	}

}
