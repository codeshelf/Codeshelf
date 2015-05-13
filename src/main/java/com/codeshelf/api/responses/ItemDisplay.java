package com.codeshelf.api.responses;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.lang.ObjectUtils;

@EqualsAndHashCode
public class ItemDisplay implements Comparable<ItemDisplay> {
	
	public static final FieldComparator<Map<Object, Object>>	ItemComparator = new FieldComparator<Map<Object, Object>>() {

		@Getter
		private String[] sortedBy = {"itemId", "location", "uom"};
		
		@Override
		public int compare(Map<Object, Object> record1, Map<Object, Object> record2) {
			int value = 0;
			
			for (String sortField : sortedBy) {
				Comparable<?> item1 = (Comparable<?>) record1.get(sortField);
				Comparable<?> item2 = (Comparable<?>) record2.get(sortField);
				value =  ObjectUtils.compare(item1, item2);
				if (value != 0) {
					break;
				}
			}
			return value;
		}
		
	};

	@Getter
	private String itemId;
	
	@Getter
	private String uom;
	
	@Getter
	private String description;


	@Getter
	private String location;

	public ItemDisplay(EventDisplay eventDisplay) {
		this.itemId = eventDisplay.getItemId();
		this.uom = eventDisplay.getItemUom();
		this.description = eventDisplay.getItemDescription();
		this.location = eventDisplay.getItemLocation();
	}

	@Override
	public int compareTo(ItemDisplay item) {
		Comparable<?> thisValue = this.getItemId();
		Comparable<?> value = item.getItemId();
		// TODO Auto-generated method stub
		return ObjectUtils.compare(thisValue, value);
	}
}
