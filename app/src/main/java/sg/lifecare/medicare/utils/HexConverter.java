/*
 * Copyright 2010-15 Fraunhofer ISE
 *
 * This file is part of jMBus.
 * For more information visit http://www.openmuc.org
 *
 * jMBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jMBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jMBus.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package sg.lifecare.medicare.utils;

public class HexConverter {

	public static String getShortHexStringFromByte(int b) {
		StringBuilder builder = new StringBuilder();
		appendShortHexStringFromByte(b, builder);
		return builder.toString();
	}

	public static void appendShortHexStringFromByte(int b, StringBuilder builder) {
		String hexString = Integer.toHexString(b & 0xff);
		if (hexString.length() == 1) {
			builder.append("0");
		}
		builder.append(hexString);
	}

	public static String getHexStringFromByte(int b) {
		StringBuilder builder = new StringBuilder();
		appendHexStringFromByte(b, builder);
		return builder.toString();
	}

	public static void appendHexStringFromByte(int b, StringBuilder builder) {
		builder.append("0x");
		appendShortHexStringFromByte(b, builder);
	}

	public static String getHexStringFromByteArray(byte[] byteArray) {
		return getHexStringFromByteArray(byteArray, 0, byteArray.length);
	}

	public static String getHexStringFromByteArray(byte[] byteArray, int offset, int length) {
		StringBuilder builder = new StringBuilder();
		appendHexStringFromByteArray(builder, byteArray, offset, length);
		return builder.toString();
	}

	public static void appendHexStringFromByteArray(StringBuilder builder, byte[] byteArray, int offset, int length) {
		int l = 1;
		for (int i = offset; i < (offset + length); i++) {
			if ((l != 1) && ((l - 1) % 8 == 0)) {
				builder.append(' ');
			}
			if ((l != 1) && ((l - 1) % 16 == 0)) {
				builder.append('\n');
			}
			l++;
			appendHexStringFromByte(byteArray[i], builder);
			if (i != offset + length - 1) {
				builder.append(" ");
			}
		}
	}

	public static String getShortHexStringFromByteArray(byte[] byteArray) {
		StringBuilder builder = new StringBuilder();
		appendShortHexStringFromByteArray(builder, byteArray, 0, byteArray.length);
		return builder.toString();
	}

	public static String getShortHexStringFromByteArray(byte[] byteArray, int offset, int length) {
		StringBuilder builder = new StringBuilder();
		appendShortHexStringFromByteArray(builder, byteArray, offset, length);
		return builder.toString();
	}

	public static void appendShortHexStringFromByteArray(StringBuilder builder, byte[] byteArray, int offset, int length) {
		for (int i = offset; i < (offset + length); i++) {
			appendShortHexStringFromByte(byteArray[i], builder);
		}
	}

	public static byte[] getByteArrayFromShortHexString(String s) throws NumberFormatException {

		if (s == null) {
			throw new IllegalArgumentException("string s may not be null");
		}

		int length = s.length();

		if ((length == 0) || ((length % 2) != 0)) {
			throw new NumberFormatException("string is not a legal hex string.");
		}

		byte[] data = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			int firstCharacter = Character.digit(s.charAt(i), 16);
			int secondCharacter = Character.digit(s.charAt(i + 1), 16);

			if (firstCharacter == -1 || secondCharacter == -1) {
				throw new NumberFormatException("string is not a legal hex string.");
			}

			data[i / 2] = (byte) ((firstCharacter << 4) + secondCharacter);
		}
		return data;
	}
}
