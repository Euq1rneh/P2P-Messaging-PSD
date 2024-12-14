package common;

import java.util.Arrays;
import java.util.Base64;

public class ByteArray {
	private byte[] arr;

	public ByteArray(byte[] array) {
		arr = array;
	}
	
	public byte[] getArr() {
		return arr;
	}
	
	@Override
    public boolean equals(Object obj) {
        return obj instanceof ByteArray && Arrays.equals(arr, ((ByteArray)obj).getArr());
    }
	
	@Override
	/**
	 * Returns a base64 encoded ByteArray
	 */
	public String toString() {
		return Base64.getEncoder().encodeToString(arr);
	}
	
    @Override
    public int hashCode() {
        return Arrays.hashCode(arr);
    }
}
