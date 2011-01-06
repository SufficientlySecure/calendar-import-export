package at.aichbauer.tools.activity;

public class Mapping {
	private String key;
	private Object value;

	public Mapping(String key, Object value) {
		super();
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}
}
