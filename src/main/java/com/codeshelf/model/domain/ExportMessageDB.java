package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.edi.ExportMessage;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "export_message_db")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ExportMessageDB extends DomainObjectTreeABC<Facility>{
	public static class ExportMessageDBDao extends GenericDaoABC<ExportMessageDB> implements ITypedDao<ExportMessageDB> {
		public final Class<ExportMessageDB> getDaoClass() {
			return ExportMessageDB.class;
		}
	}
	
	//private static final Logger		LOGGER				= LoggerFactory.getLogger(ExportMessageDB.class);
	
	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@Setter
	protected Facility			parent;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				active;
	
	@Column(nullable = false, columnDefinition = "TEXT")
	@Getter @Setter
	@JsonProperty
	String contents;


	@SuppressWarnings("unchecked")
	public final ITypedDao<ExportMessageDB> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<ExportMessageDB> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ExportMessageDB.class);
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "ExMsg";
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}
	
	public ExportMessageDB() {
		super();
	}
	
	public ExportMessageDB(Facility facility, ExportMessage message) {
		setParent(facility);
		setContents(message.getContents());
		setActive(true);
		setDomainId(getDefaultDomainIdPrefix() + "_" + System.currentTimeMillis());
	}

}
