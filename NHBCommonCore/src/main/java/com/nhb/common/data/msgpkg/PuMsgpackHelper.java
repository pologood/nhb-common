package com.nhb.common.data.msgpkg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map.Entry;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;
import org.msgpack.value.impl.ImmutableBinaryValueImpl;

import com.nhb.common.data.PuArray;
import com.nhb.common.data.PuArrayList;
import com.nhb.common.data.PuElement;
import com.nhb.common.data.PuObject;
import com.nhb.common.data.PuObjectRO;
import com.nhb.common.data.PuObjectRW;
import com.nhb.common.data.PuValue;
import com.nhb.common.exception.UnsupportedTypeException;
import com.nhb.common.utils.ArrayUtils;

public class PuMsgpackHelper {

	public static void pack(PuElement value, MessagePacker packer) throws IOException {
		if (value == null || packer == null) {
			throw new NullPointerException("Both value and packer must be not-null");
		}
		if (value instanceof PuValue) {
			packPrimitive((PuValue) value, packer);
		} else if (value instanceof PuArray) {
			packArray((PuArray) value, packer);
		} else if (value instanceof PuObjectRO) {
			packObject((PuObjectRO) value, packer);
		} else {
			throw new IllegalArgumentException("Support only primitive type, array/collection and map");
		}
	}

	public static void packPrimitive(PuValue obj, MessagePacker packer) throws IOException {
		switch (obj.getType()) {
		case BOOLEAN:
			packer.packBoolean(obj.getBoolean());
			break;
		case BYTE:
			packer.packByte(obj.getByte());
			break;
		case CHARACTER:
			packer.packInt(obj.getCharacter());
			break;
		case DOUBLE:
			packer.packDouble(obj.getDouble());
			break;
		case FLOAT:
			packer.packFloat(obj.getFloat());
			break;
		case INTEGER:
			packer.packInt(obj.getInteger());
			break;
		case LONG:
			packer.packLong(obj.getLong());
			break;
		case NULL:
			packer.packNil();
			break;
		case PUARRAY:
			packArray(obj.getPuArray(), packer);
			break;
		case PUOBJECT:
			packObject(obj.getPuObject(), packer);
			break;
		case RAW:
			packer.packValue(new ImmutableBinaryValueImpl(obj.getRaw()));
			break;
		case SHORT:
			packer.packShort(obj.getShort());
			break;
		case STRING:
			packer.packString(obj.getString());
			break;
		}
	}

	public static void packArray(PuArray array, MessagePacker packer) throws IOException {
		packer.packArrayHeader(ArrayUtils.length(array));
		Iterator<PuValue> it = ArrayUtils.iterator(array);
		while (it.hasNext()) {
			PuValue element = it.next();
			pack(element, packer);
		}
	}

	public static void packObject(PuObjectRO map, MessagePacker packer) throws IOException {
		packer.packMapHeader(map.size());
		for (Entry<String, PuValue> entry : map) {
			packer.packString(entry.getKey());
			pack(entry.getValue(), packer);
		}
	}

	public static byte[] pack(PuElement value) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			pack(value, out);
			return out.toByteArray();
		}
	}

	public static void pack(PuElement value, OutputStream out) throws IOException {
		if (value == null || out == null) {
			throw new NullPointerException("Input map and output stream must both be not-null");
		}
		try (MessagePacker packer = MessagePack.newDefaultPacker(out)) {
			pack(value, packer);
			packer.flush();
		}
	}

	public static <T extends PuElement> T unpack(byte[] bytes) throws IOException {
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			return unpack(in);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends PuElement> T unpack(InputStream in) throws IOException {
		PuElement result = null;
		try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(in)) {
			result = unpack(unpacker);
		}
		return (T) result;
	}

	public static PuElement unpack(MessageUnpacker unpacker) throws IOException {
		PuElement value = null;
		MessageFormat format = unpacker.getNextFormat();
		if (format.getValueType() == ValueType.BINARY) {
			value = new PuValue(unpacker.unpackValue().asBinaryValue().asByteArray());
		} else if (format.getValueType() == ValueType.MAP) {
			PuObject map = new PuObject();
			unpackObject(unpacker, map);
			value = map;
		} else if (format.getValueType() == ValueType.ARRAY) {
			PuArray array = new PuArrayList();
			unpackArray(unpacker, array);
			value = array;
		} else {
			value = unpackPrimitive(unpacker, format);
		}
		return value;
	}

	public static void unpackObject(InputStream inputStream, PuObjectRW mapHolder) throws IOException {
		if (inputStream == null) {
			throw new NullPointerException("inputStream cannot be null");
		}
		unpackObject(MessagePack.newDefaultUnpacker(inputStream), mapHolder);
	}

	public static void unpackObject(MessageUnpacker unpacker, PuObjectRW mapHolder) throws IOException {
		if (unpacker == null || mapHolder == null) {
			throw new NullPointerException("Both unpacker and map holder must be not-null");
		}
		int size = unpacker.unpackMapHeader();
		for (int i = 0; i < size; i++) {
			String key = unpacker.unpackString();
			mapHolder.set(key, unpack(unpacker));
		}
	}

	public static void unpackArray(InputStream inputStream, PuArray arrayHolder) throws IOException {
		if (inputStream == null) {
			throw new NullPointerException("Input stream cannot be null");
		}
		unpackArray(MessagePack.newDefaultUnpacker(inputStream), arrayHolder);
	}

	public static void unpackArray(MessageUnpacker unpacker, PuArray arrayHolder) throws IOException {
		if (unpacker == null || arrayHolder == null) {
			throw new NullPointerException("Both unpacker and array holder must be not-null");
		}
		int size = unpacker.unpackArrayHeader();
		for (int i = 0; i < size; i++) {
			arrayHolder.addFrom(unpack(unpacker));
		}
	}

	public static PuValue unpackPrimitive(MessageUnpacker unpacker, MessageFormat format) throws IOException {
		if (unpacker == null || format == null) {
			throw new NullPointerException("Both unpacker and format must be not-null");
		}

		Value value = unpacker.unpackValue();
		switch (format) {
		case NIL:
		case NEVER_USED:
			return null;
		case BOOLEAN:
			return new PuValue(value.asBooleanValue().getBoolean());
		case FLOAT32:
			return new PuValue(value.asNumberValue().toFloat());
		case FLOAT64:
			return new PuValue(value.asNumberValue().toDouble());
		case STR8:
		case STR16:
		case STR32:
		case FIXSTR:
			return new PuValue(value.asStringValue().toString());
		case UINT8:
		case INT16:
		case NEGFIXINT:
			return new PuValue(value.asNumberValue().toShort());
		case INT32:
		case UINT16:
			return new PuValue(value.asNumberValue().toInt());
		case INT64:
		case UINT32:
		case UINT64:
			return new PuValue(value.asNumberValue().toLong());
		case INT8:
		case POSFIXINT:
			return new PuValue(value.asNumberValue().toByte());
		default:
			throw new UnsupportedTypeException("Expected primitive value, got " + format);
		}
	}
}
