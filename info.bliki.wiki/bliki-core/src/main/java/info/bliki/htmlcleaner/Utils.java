/*  Copyright (c) 2006-2007, Vladimir Nikic
 All rights reserved.
 
 Redistribution and use of this software in source and binary forms, 
 with or without modification, are permitted provided that the following 
 conditions are met:
 
 * Redistributions of source code must retain the above
 copyright notice, this list of conditions and the
 following disclaimer.
 
 * Redistributions in binary form must reproduce the above
 copyright notice, this list of conditions and the
 following disclaimer in the documentation and/or other
 materials provided with the distribution.
 
 * The name of HtmlCleaner may not be used to endorse or promote 
 products derived from this software without specific prior
 written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 POSSIBILITY OF SUCH DAMAGE.
 
 You can contact Vladimir Nikic by sending e-mail to
 nikic_vladimir@yahoo.com. Please include the word "HtmlCleaner" in the
 subject line.
 */

package info.bliki.htmlcleaner;

import info.bliki.api.Connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * <p>
 * Common utilities.
 * </p>
 * 
 * Created by: Vladimir Nikic<br/> Date: November, 2006.
 */
public class Utils {

	/**
	 * Trims specified string from left.
	 * 
	 * @param s
	 */
	public static String ltrim(String s) {
		if (s == null) {
			return null;
		}

		int index = 0;
		int len = s.length();

		while (index < len && Character.isWhitespace(s.charAt(index))) {
			index++;
		}

		return (index >= len) ? "" : s.substring(index);
	}

	/**
	 * Trims specified string from right.
	 * 
	 * @param s
	 */
	public static String rtrim(String s) {
		if (s == null) {
			return null;
		}

		int len = s.length();
		int index = len;

		while (index > 0 && Character.isWhitespace(s.charAt(index - 1))) {
			index--;
		}

		return (index <= 0) ? "" : s.substring(0, index);
	}

	/**
	 * Trims specified string from left and stop at <code>\n</code> character
	 * 
	 * @param s
	 */
	public static String ltrimNewline(String s) {
		if (s == null) {
			return null;
		}

		int index = 0;
		int len = s.length();

		while (index < len && Character.isWhitespace(s.charAt(index))) {
			if (s.charAt(index) == '\n') {
				break;
			}
			index++;
		}

		return (index >= len) ? "" : s.substring(index);
	}

	/**
	 * Trims specified string from left and right and stops at <code>\n</code> character
	 * 
	 * @param s
	 */
	public static String trimNewlineRight(String s) {
		if (s == null) {
			return null;
		}
		int leftIndex = 0;
		int len = s.length(); 

		while (leftIndex < len && Character.isWhitespace(s.charAt(leftIndex))) {
//			if (s.charAt(leftIndex) == '\n') {
//				break;
//			}
			leftIndex++;
		}
    if (leftIndex>=len) {
    	return "";
    }
//		return (leftIndex >= len) ? "" : s.substring(leftIndex);

		int rightIndex = len;

		while (rightIndex > 0 && Character.isWhitespace(s.charAt(rightIndex - 1))) {
			rightIndex--;
			if (s.charAt(rightIndex) == '\n') {
				rightIndex++;
				break;
			}
		}
		if (rightIndex <= 0) {
    	return "";
    }
		return s.substring(leftIndex, rightIndex);
		
	}
	/**
	 * Reads content from the specified URL with specified charset into string
	 * 
	 * @param url
	 * @param charset
	 * @throws IOException
	 */
	public static StringBuffer readUrl(URL url, String charset) throws IOException {
		StringBuffer buffer = new StringBuffer(1024);

		Object content = url.getContent();
		if (content instanceof InputStream) {
			InputStreamReader reader = new InputStreamReader((InputStream) content, charset);
			char[] charArray = new char[1024];
			try {
				int charsRead = 0;
				do {
					charsRead = reader.read(charArray);
					if (charsRead >= 0) {
						buffer.append(charArray, 0, charsRead);
					}
				} while (charsRead > 0);
			} finally {
				reader.close();
			}
		}

		return buffer;
	}

	public static boolean isHexadecimalDigit(char ch) {
		return Character.isDigit(ch) || ch == 'A' || ch == 'a' || ch == 'B' || ch == 'b' || ch == 'C' || ch == 'c' || ch == 'D'
				|| ch == 'd' || ch == 'E' || ch == 'e' || ch == 'F' || ch == 'f';
	}

	/**
	 * Escapes XML string.
	 */
	public static String escapeXml(String s, boolean advanced, boolean recognizeUnicodeChars, boolean translateSpecialEntities) {
		if (s != null) {
			int len = s.length();
			StringBuilder result = new StringBuilder(len + len / 10);

			for (int i = 0; i < len; i++) {
				char ch = s.charAt(i);

				if (ch == '&') {
					if (recognizeUnicodeChars && (i < len - 1) && (s.charAt(i + 1) == '#')) {
						int charIndex = i + 2;
						String unicode = "";
						while (charIndex < len
								&& (isHexadecimalDigit(s.charAt(charIndex)) || s.charAt(charIndex) == 'x' || s.charAt(charIndex) == 'X')) {
							unicode += s.charAt(charIndex);
							charIndex++;
						}
						if (charIndex == len || !"".equals(unicode)) {
							try {
								char unicodeChar = unicode.toLowerCase().startsWith("x") ? (char) Integer.parseInt(unicode.substring(1), 16)
										: (char) Integer.parseInt(unicode);
								if ("&<>\'\"".indexOf(unicodeChar) < 0) {
									int replaceChunkSize = (charIndex < len && s.charAt(charIndex) == ';') ? unicode.length() + 1 : unicode.length();
									result.append(String.valueOf(unicodeChar));
									i += replaceChunkSize + 1;
								} else {
									i = charIndex;
									result.append("&#" + unicode + ";");
								}
							} catch (NumberFormatException e) {
								i = charIndex;
								result.append("&amp;#" + unicode + ";");
							}
						} else {
							result.append("&amp;");
						}
					} else {
						if (translateSpecialEntities) {
							// get following sequence of most 10 characters
							String seq = s.substring(i, i + Math.min(10, len - i));
							int semiIndex = seq.indexOf(';');
							if (semiIndex > 0) {
								String entity = seq.substring(1, semiIndex);
								Integer code = (Integer) SpecialEntities.entities.get(entity);
								if (code != null) {
									int entityLen = entity.length();
									result.append("&#");
									// result.append((char) code.intValue());
									result.append(code.intValue());
									result.append(";");
									i += entityLen + 1;
									continue;
								}
							}
						}

						if (advanced) {
							String sub = s.substring(i);
							if (sub.startsWith("&amp;")) {
								// result.append("&amp;");
								result.append("&#38;");
								i += 4;
							} else if (sub.startsWith("&apos;")) {
								// result.append("&apos;");
								result.append("&#39;");
								i += 5;
							} else if (sub.startsWith("&gt;")) {
								// result.append("&gt;");
								result.append("&#62;");
								i += 3;
							} else if (sub.startsWith("&lt;")) {
								// result.append("&lt;");
								result.append("&#60;");
								i += 3;
							} else if (sub.startsWith("&quot;")) {
								// result.append("&quot;");
								result.append("&#34;");
								i += 5;
							} else {
								// result.append("&amp;");
								result.append("&#38;");
							}

							continue;
						}

						result.append("&amp;");
					}
				} else if (ch == '\'') {
					// result.append("&apos;");
					result.append("&#39;");
				} else if (ch == '>') {
					// result.append("&gt;");
					result.append("&#62;");
				} else if (ch == '<') {
					// result.append("&lt;");
					result.append("&#60;");
				} else if (ch == '\"') {
					// result.append("&quot;");
					result.append("&#34;");
				} else {
					result.append(ch);
				}
			}

			return result.toString();
		}

		return null;
	}

	public static String escapeXmlChars(String s) {
		if (s != null) {
			int len = s.length();
			StringBuilder result = new StringBuilder(len + len / 10);

			for (int i = 0; i < len; i++) {
				char ch = s.charAt(i);
				if (ch == '\'') {
					result.append("&apos;");
				} else if (ch == '>') {
					result.append("&gt;");
				} else if (ch == '<') {
					result.append("&lt;");
				} else if (ch == '\"') {
					result.append("&quot;");
				} else {
					result.append(ch);
				}
			}

			return result.toString();
		}

		return null;
	}

	public static void appendAmpersandEscapedAttribute(StringBuilder writer, String attributeName, Map<String, String> tagAtttributes) {
		String attributeValue = (String) tagAtttributes.get(attributeName);
		if (attributeValue != null) {
			if (writer.length() > 0) {
				writer.append("&amp;");
			}
			writer.append(attributeName);
			writer.append("=");
			try {
				writer.append(URLEncoder.encode(attributeValue, Connector.UTF8_CHARSET));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	public static void appendEscapedAttribute(Appendable writer, String attributeName, Map<String, String> tagAtttributes)
			throws IOException {
		String attributeValue = (String) tagAtttributes.get(attributeName);
		if (attributeValue != null) {
			writer.append(Utils.escapeXml(attributeValue, false, false, false));
		}
	}

}