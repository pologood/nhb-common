package com.nhb.messaging.socket.netty.codec;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

import com.nhb.common.Loggable;
import com.nhb.common.data.PuArray;
import com.nhb.common.data.PuArrayList;
import com.nhb.common.data.PuElement;
import com.nhb.common.data.PuObject;
import com.nhb.common.data.PuValue;
import com.nhb.common.data.msgpkg.PuMsgpackHelper;
import com.nhb.common.exception.UnsupportedTypeException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MsgpackDecoder extends ByteToMessageDecoder implements Loggable {

	public static final MsgpackDecoder newInstance() {
		return new MsgpackDecoder();
	}

	// @Override
	// protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object>
	// out) throws Exception {
	// in.markReaderIndex();
	// ByteBufInputStream inputStream = new ByteBufInputStream(in);
	// try {
	// out.add(PuMsgpackHelper.unpack(inputStream));
	// } catch (MessageInsufficientBufferException ex) {
	// in.resetReaderIndex();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	private List<Object> buffer = new ArrayList<>();

	private ByteBuf prevRemainingBytes = null;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf originalIn, List<Object> out) throws Exception {

		ByteBuf in = null;
		if (prevRemainingBytes != null) {
			in = wrappedBuffer(prevRemainingBytes, in);
			prevRemainingBytes = null;
		} else {
			in = wrappedBuffer(originalIn);
		}

		originalIn.readerIndex(originalIn.writerIndex());

		try (ByteBufInputStream inputStream = new ByteBufInputStream(in)) {
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(inputStream);
			PuElement unpackedObject = this.continueUnpack(unpacker, in);
			while (unpackedObject != null) {
				// System.out.println("Decoded message: " + unpackedObject);
				out.add(unpackedObject);
				unpackedObject = this.continueUnpack(unpacker, in);
			}
			if (in.readableBytes() > 0) {
				prevRemainingBytes = wrappedBuffer(in);
			} else {
				prevRemainingBytes = null;
				// System.out.println("Remaining none...");
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	@SuppressWarnings("unchecked")
	private List<Object> getTempBuffer() {
		List<Object> result = this.buffer;
		while (result.size() > 0) {
			Object last = result.get(result.size() - 1);
			if (!(last instanceof PuArray) && last instanceof List<?>) {
				result = (List<Object>) last;
			} else {
				break;
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<Object> getParentBuffer(List<Object> tmpBuffer) {
		List<Object> parent = this.buffer;
		while (parent.size() > 0) {
			Object lastElement = parent.get(parent.size() - 1);
			if (!(lastElement instanceof PuArray) && (lastElement instanceof List<?>)) {
				if (lastElement == tmpBuffer) {
					return parent;
				} else {
					parent = (List<Object>) lastElement;
				}
			} else {
				break;
			}
		}
		return null;
	}

	private boolean unpackSize(ByteBuf in, MessageUnpacker unpacker, ValueType valueType, List<Object> tmpBuffer)
			throws IOException {
		final int totalReadBytes = Long.valueOf(unpacker.getTotalReadBytes()).intValue();
		try {
			if (valueType == ValueType.MAP) {
				tmpBuffer.add(unpacker.unpackMapHeader());
			} else if (valueType == ValueType.ARRAY) {
				tmpBuffer.add(unpacker.unpackArrayHeader());
			}
			return true;
		} catch (MessageInsufficientBufferException e) {
			in.readerIndex(totalReadBytes);
		}
		return false;
	}

	private MessageFormat readNextFormat(MessageUnpacker unpacker, ByteBuf in) throws IOException {
		// unpack format
		final int totalReadBytes = Long.valueOf(unpacker.getTotalReadBytes()).intValue();
		try {
			return unpacker.getNextFormat();
		} catch (MessageInsufficientBufferException e) {
			// if the buffer not enough data, reset input bytebuf reader index,
			// then return null;
			in.readerIndex(totalReadBytes);
			return null;
		}
	}

	private Object unpackPrimitive(MessageUnpacker unpacker, MessageFormat nextFormat, ByteBuf in) throws IOException {
		final int totalReadBytes = Long.valueOf(unpacker.getTotalReadBytes()).intValue();
		Value value = null;
		try {
			value = unpacker.unpackValue();
		} catch (MessageInsufficientBufferException e) {
			// System.out.println("Reset reader index: " + totalReadBytes);
			in.readerIndex(totalReadBytes);
			throw e;
		}

		switch (nextFormat) {
		case NIL:
		case NEVER_USED:
			return null;
		case BOOLEAN:
			return value.asBooleanValue().getBoolean();
		case FLOAT32:
			return value.asFloatValue().toFloat();
		case FLOAT64:
			return value.asFloatValue().toDouble();
		case STR8:
		case STR16:
		case STR32:
		case FIXSTR:
			return value.asStringValue().toString();
		case UINT8:
		case INT16:
			return value.asNumberValue().toShort();
		case INT32:
		case UINT16:
		case POSFIXINT:
		case NEGFIXINT:
			return value.asNumberValue().toInt();
		case INT64:
		case UINT32:
		case UINT64:
			return value.asNumberValue().toLong();
		case INT8:
			return value.asNumberValue().toByte();
		default:
			throw new UnsupportedTypeException("Expected primitive format, got " + nextFormat);
		}
	}

	private PuElement continueUnpack(MessageUnpacker unpacker, ByteBuf in) throws IOException {
		// read next format
		MessageFormat nextFormat = readNextFormat(unpacker, in);
		if (nextFormat == null) {
			return null;
		}

		// System.out.println(
		// "(1) total read bytes: " + unpacker.getTotalReadBytes() + ", reader
		// index: " + in.readerIndex());

		ValueType valueType = nextFormat.getValueType();
		List<Object> tmpBuffer = getTempBuffer();
		if (nextFormat.getValueType() == ValueType.MAP || valueType == ValueType.ARRAY) {
			if (tmpBuffer.size() > 0) {
				// add new buffer
				List<Object> newBuffer = new ArrayList<>();
				tmpBuffer.add(newBuffer);
				tmpBuffer = newBuffer;
			}
			tmpBuffer.add(valueType);
			if (!unpackSize(in, unpacker, valueType, tmpBuffer)) {
				// System.out.println("(2) total read bytes: " +
				// unpacker.getTotalReadBytes() + ", reader index: "
				// + in.readerIndex());
				return null;
			} else {
				// System.out.println("(3) total read bytes: " +
				// unpacker.getTotalReadBytes() + ", reader index: "
				// + in.readerIndex());
				int size = (int) tmpBuffer.get(1);
				if (size == 0) {
					valueType = (ValueType) tmpBuffer.get(0);
					if (valueType == ValueType.MAP) {
						return new PuObject();
					} else if (valueType == ValueType.ARRAY) {
						return new PuArrayList();
					} else {
						throw new RuntimeException("Illegal buffer type, expect MAP or ARRAY, got " + valueType);
					}
				}
			}
			return continueUnpack(unpacker, in);
		} else {
			if (tmpBuffer.size() == 0) {
				// truong hop suy bien 1, goi tin gui den o dang primitive type
				PuValue result = new PuValue(unpackPrimitive(unpacker, nextFormat, in));
				System.out.println("(4) total read bytes: " + unpacker.getTotalReadBytes() + ", reader index: "
						+ in.readerIndex());
				return result;
			} else if (tmpBuffer.size() == 1) {
				if (!unpackSize(in, unpacker, valueType, tmpBuffer)) {
					// System.out.println("(5) total read bytes: " +
					// unpacker.getTotalReadBytes() + ", reader index: "
					// + in.readerIndex());
					return null;
				} else {
					// System.out.println("(6) total read bytes: " +
					// unpacker.getTotalReadBytes() + ", reader index: "
					// + in.readerIndex());
					int size = (int) tmpBuffer.get(1);
					if (size == 0) {
						valueType = (ValueType) tmpBuffer.get(0);
						if (valueType == ValueType.MAP) {
							return new PuObject();
						} else if (valueType == ValueType.ARRAY) {
							return new PuArrayList();
						} else {
							throw new RuntimeException("Illegal buffer type, expect MAP or ARRAY, got " + valueType);
						}
					} else {
						return continueUnpack(unpacker, in);
					}
				}
			} else {
				try {
					Object data = this.unpackPrimitive(unpacker, nextFormat, in);
					// System.out.println("(7) total read bytes: " +
					// unpacker.getTotalReadBytes() + ", reader index: "
					// + in.readerIndex());

					tmpBuffer.add(data);
					// System.out.println("add " + data + " to tmp buffer type "
					// + tmpBuffer.get(0) + ", data format : "
					// + nextFormat);
					PuElement result = checkAndWrapEverything(tmpBuffer);
					if (result != null) {
						return result;
					}

					return continueUnpack(unpacker, in);
				} catch (MessageInsufficientBufferException e) {
					return null;
				}
			}
		}
	}

	private PuElement checkAndWrapEverything(List<Object> tmpBuffer) {
		if (tmpBuffer.size() >= 2) {
			ValueType valueType = (ValueType) tmpBuffer.get(0);
			int size = (int) tmpBuffer.get(1);
			if (size == 0) {
				throw new RuntimeException("Expected size cannot be zero");
			} else {
				PuElement data = null;
				if (valueType == ValueType.ARRAY && tmpBuffer.size() == size + 2) {
					PuArray arr = new PuArrayList();
					for (int i = 2; i < tmpBuffer.size(); i++) {
						arr.addFrom(tmpBuffer.get(i));
					}
					data = arr;
				} else if (valueType == ValueType.MAP && tmpBuffer.size() == size * 2 + 2) {
					PuObject puo = new PuObject();
					try {
						for (int i = 2; i < tmpBuffer.size() - 1; i += 2) {
							puo.set((String) tmpBuffer.get(i), tmpBuffer.get(i + 1));
						}
					} catch (Exception e) {
						throw new RuntimeException("Error while wrapping tmp buffer: " + tmpBuffer, e);
					}
					data = puo;
				} else {
					return null;
				}

				List<Object> parentBuffer = getParentBuffer(tmpBuffer);
				if (parentBuffer == null) {
					this.buffer.clear();
					return data;
				} else {
					if (tmpBuffer != parentBuffer.remove(parentBuffer.size() - 1)) {
						throw new RuntimeException(
								"Something was wrong, the last element in parent buffer is not the current tmp buffer");
					} else {
						parentBuffer.add(data);
						return checkAndWrapEverything(parentBuffer);
					}
				}
			}
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		PuObject profile = new PuObject();
		profile.set("age", 28);
		profile.set("male", true);
		profile.set("fullName", "Nguyễn Hoàng Bách");
		profile.set("height", 1.75f);

		PuArray languages = PuArrayList.fromObject(Arrays.asList("Tiếng Việt", "English", 1000l));

		PuObject contact = new PuObject();
		contact.set("languages", languages);
		contact.set("profile", profile);

		byte[] bytes = PuMsgpackHelper.pack(contact);
		System.out.println("Packed data: " + new String(bytes));

		int count = 100;
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length * count);
		for (int i = 0; i < count; i++) {
			out.write(bytes);
		}

		MsgpackDecoder decoder = new MsgpackDecoder();
		ByteBuf in = new EmptyByteBuf(new UnpooledByteBufAllocator(true));
		try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(out.toByteArray())) {
			count = 0;
			while (unpacker.hasNext()) {
				PuElement unpacked = decoder.continueUnpack(unpacker, in);
				System.out.println("Unpacked data (" + (count++) + "): " + unpacked);
			}
		}
		System.out.println(decoder.buffer);
	}
}
