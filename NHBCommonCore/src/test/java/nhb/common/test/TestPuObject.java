package nhb.common.test;

import java.util.Base64;

import com.nhb.common.data.PuObject;

public class TestPuObject {

	public static void main(String[] args) {
		PuObject puo = new PuObject();
		puo.set("name", "Nguyễn Hoàng Bách");
		
		PuObject puo1 = new PuObject();
		puo1.set("name", "Nguyễn Hoàng Bách".getBytes());

		System.out.println(Base64.getEncoder().encodeToString(puo.toBytes()));
		System.out.println(Base64.getEncoder().encodeToString(puo1.toBytes()));
	}

}
