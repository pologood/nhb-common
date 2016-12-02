package com.nhb.common.data;

import com.nhb.common.utils.ArrayUtils;
import com.nhb.common.utils.PrimitiveTypeUtils;

public enum PuDataType {

	NULL(0),
	RAW(1),
	BOOLEAN(2, Boolean.class),
	BYTE(3, Byte.class),
	SHORT(4, Short.class),
	INTEGER(5, Integer.class),
	LONG(6, Long.class),
	FLOAT(7, Float.class),
	DOUBLE(8, Double.class),
	CHARACTER(9, Character.class),
	STRING(10, String.class),
	PUOBJECT(11),
	PUARRAY(12);

	private byte typeId;
	private Class<?> dataClass;

	private PuDataType(int typeId) {
		this.typeId = (byte) typeId;
	}

	private PuDataType(int typeId, Class<?> dataClass) {
		this(typeId);
		this.dataClass = dataClass;
	}

	public byte getTypeId() {
		return this.typeId;
	}

	public String getName() {
		return this.name().toLowerCase();
	}

	public static PuDataType fromId(byte id) {
		if (id >= 0) {
			for (PuDataType dt : values()) {
				if (dt.getTypeId() == id) {
					return dt;
				}
			}
		}
		return null;
	}

	public static PuDataType fromObject(Object obj) {
		if (obj != null) {
			if (obj instanceof byte[]) {
				return RAW;
			} else if (PrimitiveTypeUtils.isPrimitiveOrWrapperType(obj.getClass())) {
				if (obj.getClass() == Byte.class || obj.getClass() == Byte.TYPE) {
					return PuDataType.BYTE;
				} else if (obj.getClass() == Short.class || obj.getClass() == Short.TYPE) {
					return PuDataType.SHORT;
				} else if (obj.getClass() == Integer.class || obj.getClass() == Integer.TYPE) {
					return PuDataType.INTEGER;
				} else if (obj.getClass() == Long.class || obj.getClass() == Long.TYPE) {
					return PuDataType.LONG;
				} else if (obj.getClass() == Float.class || obj.getClass() == Float.TYPE) {
					return PuDataType.FLOAT;
				} else if (obj.getClass() == Double.class || obj.getClass() == Double.TYPE) {
					return PuDataType.DOUBLE;
				} else if (obj.getClass() == String.class) {
					return PuDataType.STRING;
				} else if (obj.getClass() == Character.class || obj.getClass() == Character.TYPE) {
					return PuDataType.CHARACTER;
				} else if (obj.getClass() == Boolean.class || obj.getClass() == Boolean.TYPE) {
					return PuDataType.BOOLEAN;
				}
			} else if (ArrayUtils.isArrayOrCollection(obj.getClass())) {
				return PUARRAY;
			} else if (obj instanceof PuObject) {
				return PUOBJECT;
			}
			throw new RuntimeException("Object type not supported: " + obj.getClass());
		}
		return PuDataType.NULL;
	}

	public static PuDataType fromName(String name) {
		if (name != null) {
			for (PuDataType dt : values()) {
				if (dt.name().equalsIgnoreCase(name)) {
					return dt;
				}
			}
		}
		return null;
	}

	public Class<?> getDataClass() {
		return dataClass;
	}

}