package com.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
		return UnspecifiedLocation.DAO;
	}

}
