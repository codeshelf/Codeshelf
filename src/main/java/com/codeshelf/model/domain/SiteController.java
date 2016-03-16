package com.codeshelf.model.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.ws.protocol.message.SiteControllerOperationMessage;
import com.codeshelf.ws.protocol.message.SiteControllerOperationMessage.SiteControllerTask;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "site_controller",uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class SiteController extends WirelessDeviceABC {

	public final static String defaultLocationDescription = "Unknown";
	
	public static class SiteControllerDao extends GenericDaoABC<SiteController> implements ITypedDao<SiteController> {
		public final Class<SiteController> getDaoClass() {
			return SiteController.class;
		}
	}

	private static final Logger		LOGGER	= LoggerFactory.getLogger(SiteController.class);

	// Monitor/alert enabled for this equipment's online status (for equipment that is expected to be left on)
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				monitor;

	// Text describing how to find the hardware installed in the warehouse (should be required non-blank when registering)
	@Column(nullable = false,name="describe_location")
	@Getter
	@JsonProperty
	private String				location;
	
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private SiteControllerRole	role;
	
	
	public enum SiteControllerRole {NETWORK_PRIMARY, STANDBY};


	public SiteController () {
		this.monitor = true; // maybe this should be false, once there is a UI to set it, as it can cause bogus alerts when setting up sites
		this.location = defaultLocationDescription;
		this.role = SiteControllerRole.NETWORK_PRIMARY;
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "SC";
	}

	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<SiteController> getDao() {
		return SiteController.staticGetDao();
	}
	
	public static ITypedDao<SiteController> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(SiteController.class);
	}

	@Override
	public String toString() {
		return this.getDomainId();
	}
	
	public String getNetworkDomainId(){
		return getParent().getDomainId();
	}

	public String getChannelUi(){
		return getParent().getChannel().toString();
	}

	public void setLocation(String location) {
		this.location = location;
		setDescription("Site Controller for " + location);
	}
	
	public User getUser(){
		User user = TenantManagerService.getInstance().getUser(getDomainId());
		if (user == null) {
			LOGGER.warn("Couldn't find user for site controller " + getDomainId());
		}
		return user;
	}

	public boolean getUserExists(){
		return getUser() != null;
	}
	
	public void shutdown(){
		Set<User> users = new HashSet<>();
		User user = getUser();
		if (user != null) {
			users.add(user);
		}
		SiteControllerOperationMessage shutdownMessage = new SiteControllerOperationMessage(SiteControllerTask.SHUTDOWN);
		WebSocketManagerService.getInstance().sendMessage(users, shutdownMessage);
	}
}
