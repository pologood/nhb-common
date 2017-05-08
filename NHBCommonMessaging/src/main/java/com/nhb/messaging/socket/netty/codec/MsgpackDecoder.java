package com.nhb.messaging.socket.netty.codec;

import java.util.List;

import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack.UnpackerConfig;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.buffer.InputStreamBufferInput;

import com.nhb.common.Loggable;
import com.nhb.common.data.PuElement;
import com.nhb.common.data.msgpkg.PuMsgpackHelper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MsgpackDecoder extends ByteToMessageDecoder implements Loggable {

	public static final MsgpackDecoder newInstance() {
		return new MsgpackDecoder();
	}

	private UnpackerConfig unpackerConfig = new UnpackerConfig();
	{
		unpackerConfig.withBufferSize(1024);
	}

	private MessageUnpacker unpacker = null;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		int oldReaderIndex = in.readerIndex();

		ByteBufInputStream inputStream = new ByteBufInputStream(in);
		if (this.unpacker == null) {
			this.unpacker = this.unpackerConfig.newUnpacker(inputStream);
		} else {
			this.unpacker.reset(new InputStreamBufferInput(inputStream));
		}

		try {
			PuElement decodedObj = PuMsgpackHelper.unpack(unpacker);
			if (decodedObj != null) {
				out.add(decodedObj);
				in.readerIndex(oldReaderIndex + Long.valueOf(unpacker.getTotalReadBytes()).intValue());
			} else {
				in.readerIndex(oldReaderIndex);
				getLogger().warn(
						"This may an error implicit, decoded object == null but MessageInsufficientBufferException had not thrown",
						new RuntimeException());
			}
		} catch (MessageInsufficientBufferException e) {
			in.readerIndex(oldReaderIndex);
		} catch (Exception e) {
			getLogger().error("Exception while decoding input stream", e);
			throw e;
		}
	}
}
