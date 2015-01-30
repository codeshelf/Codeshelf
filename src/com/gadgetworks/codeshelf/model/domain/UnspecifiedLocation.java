package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Entity
@DiscriminatorValue("UNSPECIFIED")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class UnspecifiedLocation extends Location {

	@Inject
	public static ITypedDao<UnspecifiedLocation>	DAO;

	@Singleton
	public static class UnspecifiedLocationDao extends GenericDaoABC<UnspecifiedLocation> implements ITypedDao<UnspecifiedLocation> {
		@Inject
		public UnspecifiedLocationDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<UnspecifiedLocation> getDaoClass() {
			return UnspecifiedLocation.class;
		}
	}

	@SuppressWarnings("unused") //used by hibernate framework 
	private UnspecifiedLocation() {};
	
	public UnspecifiedLocation(String domainId) {
		super(domainId, Point.getZeroPoint());
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "X";
	}

	@Override
	public ITypedDao<UnspecifiedLocation> getDao() {
		// TODO Auto-generated method stub
		return DAO;
	}

}