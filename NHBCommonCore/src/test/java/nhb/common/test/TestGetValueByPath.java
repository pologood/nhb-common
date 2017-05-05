package nhb.common.test;

import com.nhb.common.utils.ObjectUtils;

import lombok.Data;

public class TestGetValueByPath {

	@Data
	public static class Foo {
		private String name;
	}

	@Data
	public static class Bar {
		private Foo foo;
	}

	@Data
	public static class VO {
		private Bar bar;
	}

	public static void main(String[] args) {
		Foo foo = new Foo();
		foo.setName("Mario");

		Bar bar = new Bar();
		bar.setFoo(foo);

		VO vo = new VO();
		vo.setBar(bar);

		Object val = ObjectUtils.getValueByPath(vo, "bar");
		System.out.println("Value: " + val);
	}

}
