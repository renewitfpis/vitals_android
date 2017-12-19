package sg.lifecare.medicare.ble;

public class BleException extends Exception {

	private static final long serialVersionUID = -2524970097568078476L;

	public BleException(String message) {
		super(message);
	}

	public BleException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
