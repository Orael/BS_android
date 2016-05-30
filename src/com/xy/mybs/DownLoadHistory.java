package com.xy.mybs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;

import com.xy.adapter.DownloadHistoryListAdapter;
import com.xy.mybs.ui.MyImagePlayer;
import com.xy.mybs.ui.MyVidioPlayer;
import com.xy.utils.BitmapUtils;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class DownLoadHistory extends Activity implements OnItemClickListener {

	private String userName;
	private SharedPreferences sharedPre;
	private DownloadHistoryListAdapter adapter;
	private static String SHARED_FILE_NAME = "BSFile";
	private ArrayList<HashMap<String[], Bitmap>> listDataBitmap;
	private ArrayList<HashMap<String[], Bitmap>> listDataForAdapter = new ArrayList<HashMap<String[], Bitmap>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_history);
		// ��ʼ��SharedPreferences
		sharedPre = getSharedPreferences(SHARED_FILE_NAME, 0);
		// �õ���ǰ�û���
		userName = sharedPre.getString("lastUser", "");
		// �õ�ListView
		ListView dh_listView = (ListView) findViewById(R.id.dh_list_view);
		// �õ������ļ���·��
		String absolutePath = getFilesDir().getAbsolutePath();
		downloadListDataFile = new File(absolutePath + "/" + "downloadListData.obj");
		try {
			// �õ�DownloadListData ArrayList����
			ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(downloadListDataFile));
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String[], String>> listDataString = (ArrayList<HashMap<String[], String>>) objIn
					.readObject();
			// �ر�������
			objIn.close();
			listDataBitmap = BitmapUtils.fromStringToBitmap(listDataString);
			// ��ʼ��listData���� �� ������
			listDataForAdapter.clear();
			listDataForAdapter.addAll(listDataBitmap);
			adapter = new DownloadHistoryListAdapter(this, listDataForAdapter);
			// ����������
			dh_listView.setAdapter(adapter);
			// ����item�ĵ���¼�
			dh_listView.setOnItemClickListener(this);
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ���ListView��item��ʾ���������ݴ���
	 */
	private PopupWindow itemPopupWindow = null;

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// ��ʾ֮ǰ�Ȱ�ԭ����dismiss��
		dismissPopupWindow();
		// ������popupWindow����
		View contentView = View.inflate(getApplicationContext(), R.layout.dh_list_item_popup_window, null);
		// �õ���ǰitem��λ�� , ����Ļ�ϵ�λ�� ,x�����y����
		int[] location = new int[2];
		view.getLocationInWindow(location);
		// ��ʼ������
		itemPopupWindow = new PopupWindow(contentView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		// ���ñ��� : ͸��ɫ
		itemPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		// ��ʾ���ݵ�λ��
		itemPopupWindow.showAtLocation(parent, Gravity.END | Gravity.TOP, 0, location[1] + 10);
		// �õ���ǰitem���ļ���,
		String fileName = ((TextView) view.findViewById(R.id.filename)).getText().toString().trim();
		// �õ�������Ŀؼ�
		// �򿪰�ť : �����ļ���,���ļ�, Ȼ����ʾ���û�
		LinearLayout dh_open = (LinearLayout) contentView.findViewById(R.id.dh_open);
		// ɾ����ť : �����ļ���, ���ļ����ֻ���ɾ��
		LinearLayout dh_delete = (LinearLayout) contentView.findViewById(R.id.dh_delete);

		// �� "��" "ɾ��" ���ü�����,��Ҫ�������ļ�������
		dh_open.setOnClickListener(new MyItemClickListener(fileName));
		dh_delete.setOnClickListener(new MyItemClickListener(fileName));
	}

	/**
	 * item popupWindow�����еĵĽ���Բ��ת����
	 * 
	 */
	private ProgressDialog progressDialog = null;
	private File downloadListDataFile;

	class MyItemClickListener implements OnClickListener {

		private String absPath;

		private String fileDir;

		private File operatFile;

		public MyItemClickListener(String fileName) {
			// 2.�õ�ϵͳĿ¼
			File publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			absPath = publicDirectory.getAbsolutePath();
			// 3.�õ��ļ�Ŀ¼
			fileDir = absPath + "/" + userName;
			// �õ�Ҫ�򿪻���ɾ�����ļ�
			operatFile = new File(fileDir + "/" + fileName);
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.dh_open:
				// ��
				lookoverOption();
				break;
			case R.id.dh_delete:
				// ɾ��
				deleteOption();
				break;
			}
		}

		// ���߲鿴����
		private void lookoverOption() {
			dismissPopupWindow();
			// 1.�ȵ���ProgressDialog, ��ʾprogress, �ý�����ס
			progressDialog = initProgressDialog("���ڴ�,���Ժ�...");
			progressDialog.show();
			// �������,ֱ�Ӵ򿪸��ļ�
			if (operatFile.exists()) {
				// 4.progress��ʧ
				progressDialog.dismiss();
				// 5.��ʾ���û�,�½�Activity ,��ʾý���ļ�
				showMediaFile(operatFile);
			} else {
				// 4.progress��ʧ
				progressDialog.dismiss();
				// ��ʧ��,������Ϣ�Ի�����ʾ�û�
				showMessageDialog("��ʧ��");
			}
		}

		// ɾ������
		private void deleteOption() {
			dismissPopupWindow();
			// 1.�ȵ���ProgressDialog, ��ʾprogress, �ý�����ס
			progressDialog = initProgressDialog("ɾ����,���Ժ�...");
			progressDialog.show();
			// 5.�������,��ɾ��,
			boolean isDelete = false;
			if (operatFile.exists()) {
				System.gc();
				isDelete = operatFile.delete();
			}
			// ���ɾ���ɹ�,�޸�downloadLsitData,
			if (isDelete) {
				for (int i = 0; i < listDataBitmap.size(); i++) {
					HashMap<String[], Bitmap> hashMap = listDataBitmap.get(i);
					String[] strings = hashMap.keySet().iterator().next();
					String temp_name = strings[0];
					if (temp_name.equals(operatFile.getName())) {
						listDataBitmap.remove(i);
						break;
					}
				}
			}
			// ���µ�listDataBitmapд�ص��ֻ�
			try {
				ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(downloadListDataFile));
				objOut.writeObject(listDataBitmap);
				objOut.flush();
				objOut.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// ������������listData
			listDataForAdapter.clear();
			listDataForAdapter.addAll(listDataBitmap);
			// ˢ��ҳ����ʾ����
			adapter.notifyDataSetChanged();
			// �������Ի�����ʧ
			progressDialog.dismiss();
			// ������Ϣ��ʾ��
			showMessageDialog(isDelete ? "ɾ���ɹ�" : "ɾ��ʧ��");
		}
	}

	/**
	 * ����listView�������ʾ��PopupWindow
	 */
	public void dismissPopupWindow() {
		if (itemPopupWindow != null) {
			itemPopupWindow.dismiss();
		}
		itemPopupWindow = null;
	}

	/**
	 * ���ݴ����message, ��ʼ��һ��ProgressDialog
	 * 
	 * @param message�������Ի������ʾ��Ϣ
	 * @return ��������������Ի���
	 */
	private ProgressDialog initProgressDialog(String message) {
		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(message);
		return progressDialog;
	}

	/**
	 * ���ݴ����ý���ļ�·��,���ļ���ʾ���� ��Ƶ�ļ�ʹ��MyVideoPlayer Activity���� ͼƬ�ļ�ʹ��MyImagePlayer
	 * Activity��ʾ
	 * 
	 * @param file : Ҫ��ʾ���ļ�
	 */
	private void showMediaFile(File file) {
		// �õ�����·��
		Uri uri = Uri.fromFile(file);
		// �õ��ļ�����չ��, ���ļ����з����
		String name = file.getName();
		Log.e("showMediaFile>>>fileName::::", name);
		Log.e("showMediaFile>>>uri.getEncodiPath::::", uri.getEncodedPath());
		String[] name_split = name.split("\\.");
		String schema = name_split[name_split.length - 1];
		Log.e("showMediaFile>>>fileSchema::::", schema);
		// �����mp4��ʽ���ļ�,����ϵͳ����Ƶ��������
		if ("mp4".equalsIgnoreCase(schema)) {
			Intent intent = new Intent(this, MyVidioPlayer.class);
			intent.setData(uri);
			startActivity(intent);
			dismissPopupWindow();
		} else {
			// ʹ��ͼƬ�������
			Intent intent = new Intent(this, MyImagePlayer.class);
			intent.setData(uri);
			startActivity(intent);
			dismissPopupWindow();
		}
	}

	/**
	 * ��ʾ��Ϣ��ʾ�Ի���
	 * 
	 * @param message
	 *            ��ʾ������ʾ������
	 */
	private void showMessageDialog(String message) {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle("��ʾ");
		builder.setMessage(message);
		builder.setPositiveButton("ȷ��", null);
		builder.create();
		builder.show();
	}
}
