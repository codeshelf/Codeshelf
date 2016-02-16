package com.codeshelf.api.responses;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.lang.ObjectUtils;

import com.codeshelf.behavior.NotificationBehavior.ItemEventTypeGroup;
import com.google.common.base.Strings;

@EqualsAndHashCode(of={"itemId", "uom", "location"})
public class ItemDisplay implements Comparable<ItemDisplay> {

	public static final FieldComparator<Map<Object, Object>>	ItemComparator = new FieldComparator<Map<Object, Object>>() {
		@Getter
		private String[] sortedBy = {"count", "itemId", "uom", "location"};

		
	};

	@Getter
	private String itemId;

	@Getter
	private String gtin;

	@Getter
	private String uom;
	
	@Getter
	private String description;


	@Getter
	private String location;

	public ItemDisplay(EventDisplay eventDisplay) {
		this.itemId = Strings.nullToEmpty(eventDisplay.getItemId());
		this.location = Strings.nullToEmpty(eventDisplay.getItemLocation());
		this.gtin = Strings.nullToEmpty(eventDisplay.getItemGtin());
		this.uom = Strings.nullToEmpty(eventDisplay.getItemUom());
		this.description = Strings.nullToEmpty(eventDisplay.getItemDescription());
	}

	public ItemDisplay(ItemEventTypeGroup groupedEvent) {
		this.itemId = Strings.nullToEmpty(groupedEvent.getItemId());
		this.location = Strings.nullToEmpty(groupedEvent.getLocation());
		this.gtin = Strings.nullToEmpty(groupedEvent.getItemGtin());
		this.uom = Strings.nullToEmpty(groupedEvent.getItemUom());
		this.description = Strings.nullToEmpty(groupedEvent.getItemDescription());
	}

	@Override
	public int compareTo(ItemDisplay item) {
		Comparable<?> thisValue = this.getItemId();
		Comparable<?> value = item.getItemId();
		// TODO Auto-generated method stub
		return ObjectUtils.compare(thisValue, value);
	}
}
