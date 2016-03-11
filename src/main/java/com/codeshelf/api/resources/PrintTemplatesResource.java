package com.codeshelf.api.resources;

import com.codeshelf.api.CRUDResource;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.PrintTemplate;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class PrintTemplatesResource extends CRUDResource<PrintTemplate, Facility> {

	public PrintTemplatesResource() {
		super(PrintTemplate.class, "printtemplate", Optional.of(ImmutableSet.of("active", "template")));
	}

	@Override
	protected String getNewDomainIdExistsMessage(String inDomainId) {
		return "Another template with name " + inDomainId + " already exists";
	}

}
