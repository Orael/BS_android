package com.xy.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class ListDataUtils {
	/**
	 * ɾ���ظ���¼
	 * 
	 */
	public static ArrayList<HashMap<String[], String>> trimListData(ArrayList<HashMap<String[], String>> listData) {
		System.out.println("IndexUtils>trimArray>listData.size():" + listData.size());
		for (int i = 0; i < listData.size(); i++) {
			// �õ���ǰ��HashMap
			HashMap<String[], String> temp = listData.get(i);
			String[] info = temp.keySet().iterator().next();
			// �õ��ļ������û���
			String fileName = info[0];
			String user = info[3];
			// �ӵ�ǰλ�õ���һ��λ�ÿ�ʼ����
			for (int j = i + 1; j < listData.size(); j++) {
				// �õ���ǰ��HashMap
				HashMap<String[], String> temp2 = listData.get(j);
				String[] info2 = temp2.keySet().iterator().next();
				// �õ��ļ������û���
				String fileName2 = info2[0];
				String user2 = info2[3];
				// �����ж�,�����ͬ,��ɾ��ǰ���,�������µ�
				if (fileName.equals(fileName2) && user.equals(user2)) {
					listData.remove(i);
					i--;
					break;
				}
			}
		}
		return listData;
	}
}
