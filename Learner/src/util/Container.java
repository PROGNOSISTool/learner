package util;

public class Container<T> {
	public T value;
	
	public Container() {
		this.value = null;
	}
	
	public Container(T value) {
		this.value = value;
	}
}
