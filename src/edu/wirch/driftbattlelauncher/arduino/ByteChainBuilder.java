package edu.wirch.driftbattlelauncher.arduino;

public class ByteChainBuilder {
	private byte[] bytes = new byte[0];

	public ByteChainBuilder add(byte[] moreBytes) {
		byte[] tmp = new byte[bytes.length + moreBytes.length];
		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(moreBytes, 0, tmp, bytes.length, moreBytes.length);
		bytes = tmp;
		return this;
	}

	public byte[] build() {
		return bytes;
	}
}
