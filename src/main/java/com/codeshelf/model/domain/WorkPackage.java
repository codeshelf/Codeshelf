package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Getter;
import lombok.Setter;

public class WorkPackage {

	/**
	 * SingleWorkItem represents a single good work instruction, short work instruction, or order detail for which a work instruction could not be made.
	 * Later enhancement will likely need two detail fields. One for not on my current path, but in my work area. And the other for unknown path or path in other area.
	 */
	public static class SingleWorkItem {
		private static final Logger	LOGGER	= LoggerFactory.getLogger(SingleWorkItem.class);

		@Getter
		private WorkInstruction		instruction;

		@Getter
		private OrderDetail			detail;

		public void addInstruction(WorkInstruction inInstruction) {
			instruction = inInstruction;
			if (getDetail() != null)
				LOGGER.error("misuse of SingleWorkItem");
		}

		public void addDetail(OrderDetail inDetail) {
			detail = inDetail;
			if (getInstruction() != null)
				LOGGER.error("misuse of SingleWorkItem");
		}
	}

	/**
	 * Later enhancement will like needly two detail lists. One for not on my current path, but in my work area. And the other for unknown path or path in other area.
	 */
	public static class WorkList {
		@Getter
		@Setter
		private List<WorkInstruction>	instructions	= new ArrayList<WorkInstruction>();

		@Getter
		@Setter
		private List<OrderDetail>		details			= new ArrayList<OrderDetail>();
	}
}
