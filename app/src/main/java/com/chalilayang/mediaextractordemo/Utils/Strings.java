package com.chalilayang.mediaextractordemo.Utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Strings {

    /**
     * The digits for every supported radix.
     */
    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static final char[] UPPER_CASE_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    public static final boolean isEmpty(String string){
        return string == null || string.length() == 0;
    }

    /**
     * Converts the byte array to a string using the given charset.
     *
     * <p>The behavior when the bytes cannot be decoded by the given charset
     * is to replace malformed input and unmappable characters with the charset's default
     * replacement string. Use {@link java.nio.charset.CharsetDecoder} for more control.
     *
     * @throws IndexOutOfBoundsException
     *             if {@code byteCount < 0 || offset < 0 || offset + byteCount > data.length}
     * @throws NullPointerException
     *             if {@code data == null}
     */
    public static final String construct(byte[] data, int offset, int byteCount, Charset charset) {
        try {
            return new String(data, offset, byteCount, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns a new byte array containing the characters of this string encoded using the
     * given charset.
     *
     * <p>The behavior when this string cannot be represented in the given charset
     * is to replace malformed input and unmappable characters with the charset's default
     * replacement byte array. Use {@link java.nio.charset.CharsetEncoder} for more control.
     */
    public static byte[] getBytes(String string, Charset charset) {
        try {
            return string.getBytes(charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String bytesToHexString(byte[] bytes, boolean upperCase) {
        char[] digits = upperCase ? UPPER_CASE_DIGITS : DIGITS;
        char[] buf = new char[bytes.length * 2];
        int c = 0;
        for (byte b : bytes) {
            buf[c++] = digits[(b >> 4) & 0xf];
            buf[c++] = digits[b & 0xf];
        }
        return new String(buf);
    }
    
    /**
     * 替换HTML字符.
     */
    public static String htmlDecoder(String src) throws Exception {
        if (src == null || src.equals("")) {
            return "";
        }

        String dst = src;
        dst = replaceAll(dst, "&lt;", "<");
        dst = replaceAll(dst, "&rt;", ">");
        dst = replaceAll(dst, "&gt;", ">");
        dst = replaceAll(dst, "&quot;", "\"");
        dst = replaceAll(dst, "&039;", "'");
        dst = replaceAll(dst, "&nbsp;", " ");
        dst = replaceAll(dst, "&nbsp", " ");
        dst = replaceAll(dst, "<br>", "\n");
        dst = replaceAll(dst, "<BR>", "\n");
        dst = replaceAll(dst, "\r\n", "\n");
        dst = replaceAll(dst, "&#8826;", "•");
        dst = replaceAll(dst, "&#8226;", "•");
        dst = replaceAll(dst, "&#9642;", "•");
        dst = replaceAll(dst, "&amp;", "&");
        dst = replaceAll(dst, "<br/>", "");
        dst = replaceAll(dst, "<BR/>", "");
        return dst;
    }
    
    /**
     * 字符替换
     * 
     * @param src
     * @param fnd
     * @param rep
     * @return String
     * @throws Exception
     */
    public static String replaceAll(String src, String fnd, String rep)
            throws Exception {
        if (src == null || src.equals("")) {
            return "";
        }

        String dst = src;

        int idx = dst.indexOf(fnd);

        while (idx >= 0) {
            dst = dst.substring(0, idx) + rep
                    + dst.substring(idx + fnd.length(), dst.length());
            idx = dst.indexOf(fnd, idx + rep.length());
        }

        return dst;
    }
    
	public static String FilterCNSpace(String src) throws Exception{
	    String dst = src;
	    dst = replaceAll(dst,"　","");
	    return dst;
	}
	
	/**
	 * @param s 源字符串
	 * @param srcString 需要替换的字符串
	 * @param dstString 替换后的字符串
	 * @return
	 */
	public static String replace(String s, String srcString, String dstString) {
		if (s == null || s.equals("")) {
			return "";
		}

		String d = s;
		try {
			d = replaceAll(d, srcString, dstString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
		return d;
	}
	
	private static String replaceAll4Body(String body,String regEx,String replaceStr){
        
        if (body == null || body.equals("")) {
            return "";
        }
        String dst = body;
        Pattern p=Pattern.compile(regEx);
        Matcher m=p.matcher(dst);
        while(m.find()){
           String findStr = m.group();
           dst = dst.replace(findStr, replaceStr);
        }
        return dst;   
    }
	
    public static String formatBody(String body){
        
        if (body == null || body.equals("")) {
            return "";
        }
        
        
        try {
            String dst = body;
            
            dst = replaceAll4Body(dst,"<!--IMG.[0-9]{1,3}-->","");
            dst = replaceAll4Body(dst,"<!--VIDEO.[0-9]{1,3}-->","");
            dst = replaceAll4Body(dst,"<!--link[0-9]{1,3}-->","");
            dst = replaceAll(dst, "&gt;", ">");
            dst = replaceAll(dst,"　","");
            dst = replaceAll(dst,"<p>","");
            dst = replaceAll(dst,"</p>","");
            dst = replaceAll(dst, "<br>", "");
            dst = replaceAll(dst, "<BR>", "");
            dst = replaceAll(dst, "<br/>", "");
            dst = replaceAll(dst, "<BR/>", "");
            
            return dst;
        } catch (Exception e) {
            return "";
        }
        
    }
	
	
}