import java.io.IOException;
import java.util.Arrays;

import com.nhb.common.data.PuArray;
import com.nhb.common.data.PuArrayList;
import com.nhb.common.data.PuObject;
import com.nhb.common.data.msgpkg.PuMsgpackHelper;

public class TestPuObject {

	public static void main(String[] args) throws IOException {
		PuObject profile = new PuObject();
		profile.set("fullName", "Nguyễn Hoàng Bách");
		profile.set("age", 28);
		profile.set("height", 1.75f);

		PuObject value = new PuObject();
		value.set("key", new byte[] { 1, 2, 3, 4, 5 });
		PuArray languages = PuArrayList.fromObject(Arrays.asList("Tiếng Việt", "English", 1000l, value));

		PuObject contact = new PuObject();
		contact.set("profile", profile);
		contact.set("languages", languages);

		byte[] bytes = PuMsgpackHelper.pack(contact);
		System.out.println("PuElement packed data: " + new String(bytes));
		System.out.println("PuElement unpacked obj: " + PuMsgpackHelper.unpack(bytes));

	}
}
