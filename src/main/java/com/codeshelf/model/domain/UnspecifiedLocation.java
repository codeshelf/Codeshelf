package com.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@Entity
@DiscriminatorValue("UNSPECIFIED")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class UnspecifiedLocation extends Location {

	public static class UnspecifiedLocationDao extends GenericDaoABC<UnspecifiedLocation> implements ITypedDao<UnspecifiedLocation> {
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

	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<UnspecifiedLocation> getDao() {
		return UnspecifiedLocation.staticGetDao();
	}

	public static ITypedDao<UnspecifiedLocation> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(UnspecifiedLocation.class);
	}

}
