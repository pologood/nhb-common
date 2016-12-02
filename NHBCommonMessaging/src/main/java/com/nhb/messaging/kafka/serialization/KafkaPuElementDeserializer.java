package com.nhb.messaging.kafka.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;

import com.nhb.common.data.PuElement;
import com.nhb.common.data.msgpkg.PuMsgpackHelper;

public class KafkaPuElementDeserializer extends MsgpackCodec implements Deserializer<PuElement> {

	@Override
	public void close() {
	}

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {

	}

	@Override
	public PuElement deserialize(String topic, byte[] data) {
		try {
			return PuMsgpackHelper.unpack(new ByteArrayInputStream(data));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
