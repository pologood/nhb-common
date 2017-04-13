package com.nhb.messaging.socket.netty.codec;

import java.util.List;

import org.msgpack.core.MessageInsufficientBufferException;

import com.nhb.common.Loggable;
import com.nhb.common.data.msgpkg.PuMsgpackHelper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MsgpackDecoder extends ByteToMessageDecoder implements Loggable {

	public static final MsgpackDecoder newInstance() {
		return new MsgpackDecoder();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		in.markReaderIndex();
		ByteBufInputStream inputStream = new ByteBufInputStream(in);
		try {
			out.add(PuMsgpackHelper.unpack(inputStream));
		} catch (MessageInsufficientBufferException ex) {
			in.resetReaderIndex();
		} catch (Exception e) {
			throw e;
		}
	}

}
