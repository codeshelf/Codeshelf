package com.codeshelf.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a two-key map - look up an item with either key.
 * 
 * Removing or replacing a key or value causes any secondary key or other value to also be removed/replaced.
 * 
 * Backed by 3 HashMaps. Keys and values both must properly implement hashCode and equals or this will not work!
 * 
 * Does not work on Hibernate proxy objects.
 * 
 * @author ivan
 *
 */
public class TwoKeyMap<KT1, KT2, VT> {
	public class KeyPair<KPT1, KPT2> {
		final public KPT1	key1;
		final public KPT2	key2;

		public KeyPair(KPT1 x1, KPT2 y1) {
			key1 = x1;
			key2 = y1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((key1 == null) ? 0 : key1.hashCode());
			result = prime * result + ((key2 == null) ? 0 : key2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			KeyPair<KPT1, KPT2> other = (KeyPair<KPT1, KPT2>) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (key1 == null) {
				if (other.key1 != null)
					return false;
			} else if (!key1.equals(other.key1))
				return false;
			if (key2 == null) {
				if (other.key2 != null)
					return false;
			} else if (!key2.equals(other.key2))
				return false;
			return true;
		}

		private TwoKeyMap<KT1, KT2, VT> getOuterType() {
			return TwoKeyMap.this;
		}
	}

	private Map<KT1, VT>			map1;
	private Map<KT2, VT>			map2;
	private Map<VT, KeyPair<KT1, KT2>>	values;

	public TwoKeyMap() {
		map1 = new HashMap<KT1, VT>();
		map2 = new HashMap<KT2, VT>();
		values = new HashMap<VT, KeyPair<KT1, KT2>>();
	}

	public TwoKeyMap(int s) {
		map1 = new HashMap<KT1, VT>(s);
		map2 = new HashMap<KT2, VT>(s);
		values = new HashMap<VT, KeyPair<KT1, KT2>>(s);
	}

	public void put(KT1 key1, KT2 key2, VT value) {
		if(value==null) {
			// throw new NullPointerException("TwoKeyMap.put() called with value null");
			// let's just ignore this.
			return;
		}
		boolean doMap = false;
		KeyPair<KT1, KT2> keys = values.get(value);
		if (keys != null) {
			// this value is mapped
			if (!keys.key1.equals(key1) || !keys.key2.equals(key2)) {
				// ...but keys are not the same. remove all mappings for old keys.
				map1.remove(keys.key1);
				map2.remove(keys.key2);
				doMap = true;
			}
			// else value is already mapped this way, do nothing
		} else {
			// value is not in map
			doMap = true;
		}
		if (doMap) {
			values.put(value, new KeyPair<KT1, KT2>(key1, key2));
			map1.put(key1, value);
			map2.put(key2, value);
		}
	}
	
	@SuppressWarnings("unchecked")
	private VT removeValue(Object value) {
		if (value == null)
			return null;

		KeyPair<KT1, KT2> keys = values.get(value);
		if (keys != null) {
			map1.remove(keys.key1);
			map2.remove(keys.key2);
			return (VT)value;
		}
		return null;

	}

	@SuppressWarnings("unchecked")
	public VT remove(Object valueOrKey) {
		if(removeValue(valueOrKey) != null) {
			return (VT)valueOrKey; // it's a value
		} // else
		
		VT value = map1.get(valueOrKey);
		if(removeValue(value) != null) {
			return value;
		} // else

		value = map2.get(valueOrKey);
		if(removeValue(value) != null) {
			return value;
		} // else
		return null;
	}

	public VT get(Object key) {
		VT value=map1.get(key);
		if(value != null) {
			return value;
		} //else
		value=map2.get(key);
		if(value != null) {
			return value;
		} //else
		return null;
	}
	
	public KeyPair<KT1,KT2> getKeys(VT value) {
		return values.get(value);
	}
	
	public boolean containsValue(VT value) {
		return (values.get(value) != null);
	}

	public Collection<VT> values() {
		return map1.values();
	}

	public Collection<KT1> keys1() {
		return map1.keySet();
	}

	public Collection<KT2> keys2() {
		return map2.keySet();
	}
}
