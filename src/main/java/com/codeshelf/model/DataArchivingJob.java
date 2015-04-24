package com.codeshelf.model;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public class DataArchivingJob implements Job {

	private static final Logger LOGGER	= LoggerFactory.getLogger(DataArchivingJob.class);
	
	public DataArchivingJob() {
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOGGER.info("Starting data archiving job");
		for (Tenant tenant : TenantManagerService.getInstance().getTenants()) {
			archiveTenantData(tenant);
		}
		LOGGER.info("Data archiving job finished");
	}

	private void archiveTenantData(Tenant tenant) {
		boolean completed = false;
		long startTime = System.currentTimeMillis();
		try {
			UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
			CodeshelfSecurityManager.setContext(systemUser, tenant);
			TenantPersistenceService.getInstance().beginTransaction();
			List<Facility> facilities = Facility.staticGetDao().getAll();
			for (Facility fac : facilities) {
				LOGGER.info("Archiving unused objects for tenant "+tenant.getName()+" in facility "+fac.getDomainId());
				archiveUnusedContainers(fac);
				archiveUnusedOrderGroups(fac);
			}
			TenantPersistenceService.getInstance().commitTransaction();
			completed = true;
		} catch (RuntimeException e) {
			LOGGER.error("Unable to archive data for tenant "+tenant.getId(), e);
		} finally {
			long endTime = System.currentTimeMillis();
			CodeshelfSecurityManager.removeContext();			
			if (completed) {
				LOGGER.info("Data archiving process for tenant {} completed in {}s",tenant.getName(),(endTime-startTime)/1000);
			}
			else {
				TenantPersistenceService.getInstance().rollbackTransaction();
				LOGGER.warn("Data archiving process did not complete successfully for tenant {}",tenant.getName());
			}
		}
	}
	
	// deactivate containers in one facility that don't have any active uses
	// TODO: replace with single SQL query to perform work directly in the database
	public void archiveUnusedContainers(final Facility inFacility) {
		LOGGER.debug("Archive unused containers");
		// Iterate all of the containers to see if they're still active.
		int numArchived = 0;
		for (Container container : Container.staticGetDao().findByParent(inFacility)) {
			boolean shouldInactivateContainer = true;
			for (ContainerUse containerUse : container.getUses()) {
				if (containerUse.getActive()) {
					shouldInactivateContainer = false;
					break;
				}
			}
			if (shouldInactivateContainer) {
				container.setActive(false);
				Container.staticGetDao().store(container);
				numArchived++;
			}
		}
		if (numArchived==0) {
			LOGGER.info("No unused containers found");			
		}
		else {
			LOGGER.info(numArchived+" unused containers archived");
		}
	}	

	// deactivate order groups in one facility that don't have any active orders
	// TODO: replace with single SQL query to perform work directly in the database
	@SuppressWarnings("unchecked")
	public void archiveUnusedOrderGroups(final Facility inFacility) {
		LOGGER.debug("Archive unused order groups");
		int numArchived = 0;
		for (OrderGroup group : OrderGroup.staticGetDao().findByParent(inFacility)) {
			Criteria crit = OrderHeader.staticGetDao().createCriteria();
			crit.add(Restrictions.eq("orderGroup", group))
				.add(Restrictions.eq("active", true));
			crit.setMaxResults(1);
			List<OrderHeader> orders = crit.list();
			if (orders==null || orders.size()==0) {
				group.setActive(false);
				OrderGroup.staticGetDao().store(group);				
				numArchived++;			
			}
		}
		if (numArchived==0) {
			LOGGER.info("No unused order groups found");			
		}
		else {
			LOGGER.info(numArchived+" unused order groups archived");
		}
	}

	// deactivate item masters in one facility that don't have any active item
	// TODO: replace with single SQL query to perform work directly in the database
	@SuppressWarnings("unchecked")
	public void archiveUnusedItemMasters(final Facility inFacility) {
		LOGGER.debug("Archive unused item masters");
		int numArchived = 0;
		for (ItemMaster im : ItemMaster.staticGetDao().findByParent(inFacility)) {
			Criteria crit = Item.staticGetDao().createCriteria();
			crit.add(Restrictions.eq("parent", im))
				.add(Restrictions.eq("active", true));
			crit.setMaxResults(1);
			List<OrderHeader> orders = crit.list();
			if (orders==null || orders.size()==0) {
				im.setActive(false);
				OrderGroup.staticGetDao().store(im);
				numArchived++;			
			}
		}
		if (numArchived==0) {
			LOGGER.info("No unused item masters found");	
		}
		else {
			LOGGER.info(numArchived+" unused item masters archived");
		}
	}
}
