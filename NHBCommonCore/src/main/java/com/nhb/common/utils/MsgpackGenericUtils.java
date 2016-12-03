package com.nhb.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;
import org.msgpack.value.impl.ImmutableBinaryValueImpl;

import com.nhb.common.exception.UnsupportedTypeException;

public class MsgpackGenericUtils {

	public static void main(String[] args) throws IOException {
		Map<String, Object> profile = new HashMap<>();
		profile.put("age", 28);
		profile.put("male", true);
		profile.put("fullName", "Nguyễn Hoàng Bách");
		profile.put("height", 1.75f);

		Collection<Object> languages = Arrays.asList("Tiếng Việt", "English", 1000l);

		Map<String, Object> contact = new HashMap<>();
		contact.put("languages", languages);
		contact.put("profile", profile);

		byte[] bytes = pack(contact);
		System.out.println("Packed data (length=" + bytes.length + "): " + new String(bytes));
		System.out.println("Unpacked obj: " + unpack(bytes));
	}

	public static byte[] pack(Object value) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			pack(value, out);
			return out.toByteArray();
		}
	}

	public static void pack(Object value, OutputStream out) throws IOException {
		if (value == null || out == null) {
			throw new NullPointerException("Input map and output stream must both be not-null");
		}
		try (MessagePacker packer = MessagePack.newDefaultPacker(out)) {
			pack(value, packer);
			packer.flush();
		}
	}

	public static <T> T unpack(byte[] bytes) throws IOException {
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			return unpack(in);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T unpack(InputStream in) throws IOException {
		Object result = null;
		try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(in)) {
			result = unpack(unpacker);
		}
		return (T) result;
	}

	public static Object unpack(MessageUnpacker unpacker) throws IOException {
		Object value = null;
		MessageFormat format = unpacker.getNextFormat();
		if (format.getValueType() == ValueType.BINARY) {
			value = unpacker.unpackValue().asBinaryValue().asByteArray();
		} else if (format.getValueType() == ValueType.MAP) {
			Map<String, Object> map = new HashMap<>();
			unpackMap(unpacker, map);
			value = map;
		} else if (format.getValueType() == ValueType.ARRAY) {
			Collection<Object> array = new ArrayList<>();
			unpackArray(unpacker, array);
			value = array;
		} else {
			value = unpackPrimitive(unpacker, format);
		}
		return value;
	}

	public static void unpackMap(MessageUnpacker unpacker, Map<String, Object> mapHolder) throws IOException {
		if (unpacker == null || mapHolder == null) {
			throw new NullPointerException("Both unpacker and map holder must be not-null");
		}
		int size = unpacker.unpackMapHeader();
		for (int i = 0; i < size; i++) {
			String key = unpacker.unpackString();
			mapHolder.put(key, unpack(unpacker));
		}
	}

	public static void unpackArray(MessageUnpacker unpacker, Collection<Object> arrayHolder) throws IOException {
		if (unpacker == null || arrayHolder == null) {
			throw new NullPointerException("Both unpacker and array holder must be not-null");
		}
		int size = unpacker.unpackArrayHeader();
		for (int i = 0; i < size; i++) {
			arrayHolder.add(unpack(unpacker));
		}
	}

	public static Object unpackPrimitive(MessageUnpacker unpacker, MessageFormat format) throws IOException {
		if (unpacker == null || format == null) {
			throw new NullPointerException("Both unpacker and format must be not-null");
		}

		Value value = unpacker.unpackValue();
		switch (format) {
		case NIL:
		case NEVER_USED:
			return null;
		case BOOLEAN:
			return value.asBooleanValue().getBoolean();
		case FLOAT32:
			return value.asNumberValue().toFloat();
		case FLOAT64:
			return value.asNumberValue().toDouble();
		case STR8:
		case STR16:
		case STR32:
		case FIXSTR:
			return value.asStringValue().toString();
		case UINT8:
		case INT16:
		case NEGFIXINT:
			return value.asNumberValue().toShort();
		case INT32:
		case UINT16:
			return value.asNumberValue().toInt();
		case INT64:
		case UINT32:
		case UINT64:
			return value.asNumberValue().toLong();
		case INT8:
		case POSFIXINT:
			return value.asNumberValue().toByte();
		default:
			throw new UnsupportedTypeException("Expected primitive value");
		}
	}

	@SuppressWarnings("unchecked")
	public static void pack(Object value, MessagePacker packer) throws IOException {
		Class<?> valueClass = value.getClass();
		if (value instanceof byte[]) {
			packer.packValue(new ImmutableBinaryValueImpl((byte[]) value));
		} else if (PrimitiveTypeUtils.isPrimitiveOrWrapperType(valueClass)) {
			packPrimitive(value, packer);
		} else if (ArrayUtils.isArrayOrCollection(valueClass)) {
			packArray(value, packer);
		} else if (Map.class.isAssignableFrom(valueClass)) {
			packMap((Map<String, Object>) value, packer);
		} else {
			throw new IllegalArgumentException("Support only primitive type, array/collection and map");
		}
	}

	public static void packPrimitive(Object primitiveObject, MessagePacker packer) throws IOException {
		Class<?> objClass = primitiveObject.getClass();
		if (PrimitiveTypeUtils.isPrimitiveOrWrapperType(objClass)) {
			if (objClass == Boolean.class || objClass == Boolean.TYPE) {
				packer.packBoolean((boolean) primitiveObject);
			} else if (objClass == Byte.class || objClass == Byte.TYPE) {
				packer.packByte((byte) primitiveObject);
			} else if (objClass == Short.class || objClass == Short.TYPE) {
				packer.packShort((short) primitiveObject);
			} else if (objClass == Integer.class || objClass == Integer.TYPE) {
				packer.packInt((int) primitiveObject);
			} else if (objClass == Long.class || objClass == Long.TYPE) {
				packer.packLong((long) primitiveObject);
			} else if (objClass == Float.class || objClass == Float.TYPE) {
				packer.packFloat((float) primitiveObject);
			} else if (objClass == Double.class || objClass == Double.TYPE) {
				packer.packDouble((double) primitiveObject);
			} else if (objClass == String.class) {
				packer.packString((String) primitiveObject);
			}
		} else {
			throw new UnsupportedTypeException("Exepcted primitive type object, got " + objClass);
		}
	}

	public static void packArray(Object arrayOrCollection, MessagePacker packer) throws IOException {
		packer.packArrayHeader(ArrayUtils.length(arrayOrCollection));
		Iterator<Object> it = ArrayUtils.iterator(arrayOrCollection);
		while (it.hasNext()) {
			Object element = it.next();
			pack(element, packer);
		}
	}

	public static void packMap(Map<String, Object> map, MessagePacker packer) throws IOException {
		packer.packMapHeader(map.size());
		for (Entry<String, Object> entry : map.entrySet()) {
			packer.packString(entry.getKey());
			pack(entry.getValue(), packer);
		}
	}

}
