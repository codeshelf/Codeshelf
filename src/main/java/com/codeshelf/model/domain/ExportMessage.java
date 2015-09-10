package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.edi.ExportMessageFuture;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "export_message")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ExportMessage extends DomainObjectTreeABC<Facility>{
	public static class ExportMessageDao extends GenericDaoABC<ExportMessage> implements ITypedDao<ExportMessage> {
		public final Class<ExportMessage> getDaoClass() {
			return ExportMessage.class;
		}
	}
	
	//private static final Logger		LOGGER				= LoggerFactory.getLogger(ExportMessage.class);
	
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
	public final ITypedDao<ExportMessage> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<ExportMessage> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ExportMessage.class);
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "ExMsg";
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}
	
	public ExportMessage() {
		super();
	}
	
	public ExportMessage(Facility facility, ExportMessageFuture message) {
		setParent(facility);
		setContents(message.getContents());
		setActive(true);
		setDomainId(getDefaultDomainIdPrefix() + "_" + System.currentTimeMillis());
	}

}
