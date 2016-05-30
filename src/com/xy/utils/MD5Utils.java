package com.xy.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * md5���ܷ�ʽ
 * @author Administrator
 *
 */
public class MD5Utils {
	public static String md5Digest(String password) throws NoSuchAlgorithmException {
		StringBuilder sb = new StringBuilder();
		// 1. ��ȡժҪ��
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		// 2.�õ������ժҪ Ϊbyte����
		byte[] digest = messageDigest.digest(password.getBytes());
		// 3.��������
		for (int i = 0; i < digest.length; i++) {
			// 4.MD5����
			int result = digest[i] & 0xff;
			// ת��Ϊ16����
			String hexString = Integer.toHexString(result) + 1;
			if(hexString.length() < 2){
				sb.append("0");
			}
			sb.append(hexString);
		}
		
		return sb.toString();
	}
}
