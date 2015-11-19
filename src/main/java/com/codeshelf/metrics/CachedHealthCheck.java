package com.codeshelf.metrics;

import com.codeshelf.scheduler.CachedHealthCheckResults;

public class CachedHealthCheck extends CodeshelfHealthCheck {
	private String storageName;
	
	public CachedHealthCheck(String checkName, String storageName) {
		super(checkName);
		this.storageName = storageName;
	}
	
	@Override
	protected Result check() throws Exception {
		Result result = CachedHealthCheckResults.getJobResult(storageName);
		if (result == null) {
			return unhealthy("Did not find saved results for health check " + storageName + ".");
		}
		return result;
	}
	
	public static class CachedEdiHealthCheck extends CachedHealthCheck{
		public CachedEdiHealthCheck() {
			super("EDI", EdiHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedActiveSiteControllerHealthCheck extends CachedHealthCheck{
		public CachedActiveSiteControllerHealthCheck() {
			super("Active Site Controllers", ActiveSiteControllerHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedDataQuantityHealthCheck extends CachedHealthCheck{
		public CachedDataQuantityHealthCheck() {
			super("DataQuantity", DataQuantityHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedDatabaseConnectionHealthCheck extends CachedHealthCheck{
		public CachedDatabaseConnectionHealthCheck() {
			super("Database Connection", DatabaseConnectionHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedDropboxGatewayHealthCheck extends CachedHealthCheck{
		public CachedDropboxGatewayHealthCheck() {
			super("Dropbox service", DropboxGatewayHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedIsProductionServerHealthCheck extends CachedHealthCheck{
		public CachedIsProductionServerHealthCheck() {
			super("IsProduction", IsProductionServerHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedPicksActivityHealthCheck extends CachedHealthCheck{
		public CachedPicksActivityHealthCheck() {
			super("PicksActivity", PicksActivityHealthCheck.class.getSimpleName());
		}
	}
}