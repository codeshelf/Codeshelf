package com.codeshelf.metrics;

import com.codeshelf.scheduler.CachedHealthCheckResults;

public class CachedHealthCheck extends CodeshelfHealthCheck {
	public CachedHealthCheck(String checkName) {
		super(checkName);
	}
	
	@Override
	protected Result check() throws Exception {
		Result result = CachedHealthCheckResults.getJobResult(getName());
		if (result == null) {
			return unhealthy("Did not find saved results for health check " + name + ".");
		}
		return result;
	}
	
	public static class CachedEdiHealthCheck extends CachedHealthCheck{
		public CachedEdiHealthCheck() {
			super(EdiHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedActiveSiteControllerHealthCheck extends CachedHealthCheck{
		public CachedActiveSiteControllerHealthCheck() {
			super(ActiveSiteControllerHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedDataQuantityHealthCheck extends CachedHealthCheck{
		public CachedDataQuantityHealthCheck() {
			super(DataQuantityHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedDatabaseConnectionHealthCheck extends CachedHealthCheck{
		public CachedDatabaseConnectionHealthCheck() {
			super(DatabaseConnectionHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedDropboxGatewayHealthCheck extends CachedHealthCheck{
		public CachedDropboxGatewayHealthCheck() {
			super(DropboxGatewayHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedIsProductionServerHealthCheck extends CachedHealthCheck{
		public CachedIsProductionServerHealthCheck() {
			super(IsProductionServerHealthCheck.class.getSimpleName());
		}
	}
	
	public static class CachedPicksActivityHealthCheck extends CachedHealthCheck{
		public CachedPicksActivityHealthCheck() {
			super(PicksActivityHealthCheck.class.getSimpleName());
		}
	}
}