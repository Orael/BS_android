package com.xy.fragment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.xy.adapter.Frag1ListAdapter;
import com.xy.mybs.R;
import com.xy.mybs.ui.MyImagePlayer;
import com.xy.mybs.ui.MyVidioPlayer;
import com.xy.utils.BitmapUtils;
import com.xy.utils.ListDataUtils;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class Fragment1 extends Fragment implements OnTouchListener, OnScrollListener, OnClickListener, OnItemClickListener {

	private String server_ip;
	private static final int OPENREQUESTCODE = 0;
	private static final int CAPUTREREQUESTCODE = 1;
	private static final int IMGREQUESTCODE = 2;
	private static final int GET_LIST_DATA_FINISHED = 3;
	private static final int FILE_UPLOAD_FINISH = 4;
	private static final int SERVER_LIST_DATA_CHANGE = 5;
	private static final int DOWNLOAD_FINISH = 6;
	private static final int DELETE_FINISH = 8;
	private static String SHARED_FILE_NAME = "BSFile";
	private View view;
	private File file;// Ҫ�ϴ����ļ�
	private File currentDownloadedFile;// ��ǰ���ص��ļ�
	private ImageButton btn_upload;
	private String userName;
	private SharedPreferences sharedPre;
	private ListView listView;
	private Frag1ListAdapter adapter;
	private ArrayList<HashMap<String[], Bitmap>> listDataForAdapter = new ArrayList<HashMap<String[], Bitmap>>();
	private ArrayList<HashMap<String[], Bitmap>> listDataFromServer;
	private ArrayList<HashMap<String[], String>> listDataFromServerString;
	public Handler handler;

	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment1, null);
		// ��ʼ��fragment
		init();
		return view;
	}

	/**
	 * ��ʼ��fragment1
	 */
	private void init() {

		// �������̵߳���Ϣ
		handler = new Handler(getActivity().getMainLooper()) {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case GET_LIST_DATA_FINISHED:
					// ����ɽ��յ����������������ϴ���ʷ�б����ϢlistData
					listDataForAdapter.clear();
					listDataForAdapter.addAll(listDataFromServer);
					// ˢ�¿ؼ���ʾ
					adapter.notifyDataSetChanged();
					break;
				case FILE_UPLOAD_FINISH:
					// �����ļ���Ϣ��������
					System.out.println("��ʼ�����ļ���Ϣ>>>>>>>");
					sendFileInfoToServer();
					break;
				case SERVER_LIST_DATA_CHANGE:
					// �ļ���Ϣ�������ʱ,���������ļ���Ϣ�Ѿ�����,�������¶�ȡlistData
					loadListViewData();
					break;
				case DOWNLOAD_FINISH:
					// �ļ��������ʱ,����AlertDialog��ʾ�û�
					String info = (String) msg.obj;
					showMessageDialog(info);
					// �����ص��ļ���Ϣд�뵽downloadListData��
					refreshDownloadListData();
					break;
				case DELETE_FINISH:
					// �ļ�ɾ�����ʱ,����AlertDialog��ʾ�û�
					String info2 = (String) msg.obj;
					showMessageDialog(info2);
					// �ļ�ɾ����ɺ�,��������listData�ļ���Ϣ�Ѿ�����,�������¶�ȡlistData
					loadListViewData();
					break;
				}
			}
		};
		// �õ�Manifest�ļ���meta-data�еķ�����IP��ַ
		try {
			ApplicationInfo applicationInfo = getActivity().getPackageManager()
					.getApplicationInfo(getActivity().getPackageName(), PackageManager.GET_META_DATA);
			server_ip = applicationInfo.metaData.getString("server_ip");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		// �õ��ϴ���ť
		btn_upload = (ImageButton) view.findViewById(R.id.btn_upload);
		// ���ü����¼�
		btn_upload.setOnClickListener(this);
		// ��ʾ�ϴ���ʷ��lsitView�ؼ�
		listView = (ListView) view.findViewById(R.id.frag1_list_view);
		// ����������
		adapter = new Frag1ListAdapter(getContext(), listDataForAdapter);
		listView.setAdapter(adapter);
		// ��listView���õ���¼�������
		listView.setOnItemClickListener(this);
		// ��listView���û���������
		listView.setOnScrollListener(this);
		// ��ʼ��sharedPreference
		sharedPre = getActivity().getSharedPreferences(SHARED_FILE_NAME, 0);
		// �õ���ǰ�û���
		userName = sharedPre.getString("lastUser", "");
		// ��ʼ��lsitView : �����ӷ������õ�����, ����ʾ��ҳ����
		initListView();
	}

	/**
	 * ����downloadListData,����ʾ"������ʷ"ʹ��
	 * �������ֻ��е�����Ϊ"downloadListData.obj"
	 */
	@SuppressWarnings("unchecked")
	protected void refreshDownloadListData() {
		// �õ������ļ���·��
		String absolutePath = getContext().getFilesDir().getAbsolutePath();
		File downloadListDataFile = new File(absolutePath + "/" + "downloadListData.obj");
		// �洢����String��ʽ�洢,����Bitmap�ᱨ��,ԭ��BitmapΪʵ��Serializable�ӿ�
		ArrayList<HashMap<String[], String>> downloadListData ;
		try {
			// ���ֻ������ж�ȡ�������ļ�,��ȡdownloadListData
			ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(downloadListDataFile));
			downloadListData = (ArrayList<HashMap<String[], String>>) objIn.readObject();
			objIn.close();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// ����ǵ�һ��ʹ��,�ֻ���û�и��ļ�,��FileNotFoundException,��ʱ�½�һ������
			downloadListData = new ArrayList<HashMap<String[],String>>();
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			downloadListData = new ArrayList<HashMap<String[],String>>();
		}
		// ��ȡ�����ص��ļ���Ϣ
		String fileName = currentDownloadedFile.getName();
		String filePath = currentDownloadedFile.getAbsolutePath();
		String fileSize = Formatter.formatFileSize(getContext(), currentDownloadedFile.length());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d hh:mm:ss", new Locale("zh_cn"));
		String downloadTime = sdf.format(new Date());

		// ��ȡ��ǰ�ϴ��ļ��ĸ�ʽ
		String[] path_arr = fileName.split("\\.");
		String format = path_arr[path_arr.length - 1];
		// ��������Ƶ�ļ��Ļ�,��ThumbnailUtils.createVideoThumbnail��ȡ��Ƶ������ͼ
		Bitmap thumbnail;
		if ("mp4".equalsIgnoreCase(format)) {
			thumbnail = ThumbnailUtils.createVideoThumbnail(filePath, Images.Thumbnails.MICRO_KIND);
		} else {
			// ����ʹ��ThumbnailUtils.extractThumbnail��ȡͼƬ������ͼ
			thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(filePath), 96, 96);
		}
		// ��װArrayList�е�HashMap������
		String[] info = new String[] { fileName, fileSize, downloadTime, userName };
		HashMap<String[], Bitmap> HMItem = new HashMap<String[], Bitmap>();
		HMItem.put(info, thumbnail);
		// ����Bitmap����ArrayList,�������
		ArrayList<HashMap<String[], Bitmap>> listData = new ArrayList<HashMap<String[], Bitmap>>();
		listData.add(HMItem);
		// Bitmap->String ����ת��,��ɾ���ظ���,��downloadLsitData���շ�������
		downloadListData = ListDataUtils.trimListData(BitmapUtils.fromBitmapToString(listData));
		// ��downlListData����д�뵽�ļ���
		try {
			ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(downloadListDataFile));
			objOut.writeObject(downloadListData);
			objOut.flush();
			objOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ���ListView��item��ʾ���������ݴ���
	 */
	private PopupWindow itemPopupWindow = null;

	/**
	 * listView ��item����¼�
	 * 
	 * @param parent
	 * @param view
	 * @param position
	 * @param id
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// ��ʾ֮ǰ�Ȱ�ԭ����dismiss��
		dismissPopupWindow();
		// ������popupWindow����
		View contentView = View.inflate(getContext(), R.layout.frag1_list_item_popup_window, null);
		// �õ���ǰitem��λ��
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
		// ���߲鿴 : �����ļ���,�ӷ������������ֻ�, Ȼ����ʾ���û�
		LinearLayout lookover = (LinearLayout) contentView.findViewById(R.id.lookover);
		// �������ֻ� : �����ļ���, �ӷ������������ֻ�, ��Ҫ���ļ���Ϣ����,
		LinearLayout download = (LinearLayout) contentView.findViewById(R.id.download);
		// �ӷ�����ɾ�� : �����ļ���, �����������ļ�ɾ��
		LinearLayout delete = (LinearLayout) contentView.findViewById(R.id.delete);

		// �� "�鿴" "����" "ɾ��" ���ü�����,��Ҫ�������ļ�������
		lookover.setOnClickListener(new MyItemClickListener(fileName));
		download.setOnClickListener(new MyItemClickListener(fileName));
		delete.setOnClickListener(new MyItemClickListener(fileName));
	}

	/**
	 * item popupWindow�����еĵĽ���Բ��ת����
	 * 
	 */
	private ProgressDialog progressDialog = null;

	class MyItemClickListener implements OnClickListener {
		/**
		 * ���û����item֮��,fileName��ᱻ��ֵ,Ϊ�����item��fileName
		 */
		private String fileName;

		private String absPath;

		public MyItemClickListener(String fileName) {
			this.fileName = fileName;
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.lookover:
				// ���߲鿴
				System.out.println(fileName + "�鿴");
				lookoverOption();
				break;
			case R.id.download:
				// ����������
				System.out.println(fileName + "����");
				downloadOption();
				break;
			case R.id.delete:
				// �ӷ�����ɾ������
				System.out.println(fileName + "ɾ��");
				deleteOption();
				break;
			}
		}

		// ���߲鿴����
		private void lookoverOption() {
			dismissPopupWindow();
			// 1.�ȵ���ProgressDialog,��ʾprogress, �ý�����ס
			progressDialog = initProgressDialog("������,���Ժ�...");
			progressDialog.show();
			// 2.�ȴӻ����ļ����²鿴�Ƿ��Ѿ�����:���Ѿ�������ֱ�ӽ�����һ��,��û����ȥ����������
			File cacheDir = getContext().getCacheDir();
			// ����ľ���·�� : absPath = /data/data/com.xy.mybs/cache
			absPath = cacheDir.getAbsolutePath();
			File f = new File(absPath + "/" + fileName);
			Log.e("isExist", "�Ƿ����" + f.exists());
			// �������,ֱ�Ӵ򿪸��ļ�
			if (f.exists()) {
				// 4.progress��ʧ
				progressDialog.dismiss();
				// 5.��ʾ���û�,�½�Activity ,��ʾý���ļ�
				showMediaFile(f);
			} else {
				// ���������,���ӷ���������
				new Thread() {
					public void run() {
						try {
							File file = getMediaFileFromServer(userName+"\\"+fileName, absPath + "/" + fileName);
							Log.e("FilePath", "�ļ�����·��>>>::" + file.getAbsolutePath());
							// 4.progress��ʧ
							progressDialog.dismiss();
							// 5.��ʾ���ص�ý���ļ�(.mp4  .jpg),�½�Activity��ʾ
							showMediaFile(file);
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
				}.start();
			}
		}

		// ���ز���
		private void downloadOption() {
			dismissPopupWindow();
			// 1.�ȵ���ProgressDialog, ��ʾprogress, �ý�����ס
			progressDialog = initProgressDialog("������,���Ժ�...");
			progressDialog.show();
			// 2.�ȴӷ������õ�����ļ�,���뱾��
			File publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			Log.e("publicDirectory.getAbsolutePath()>>>>>>>>>>>>>", publicDirectory.getAbsolutePath());
			absPath = publicDirectory.getAbsolutePath();
			new Thread() {
				public void run() {
					try {
						// �жϹ��������ļ�Ŀ¼�µ�/userName�Ƿ����,����������򴴽�
						File userDir = new File(absPath + "/" + userName);
						if (!userDir.exists()) {
							userDir.mkdir();
						}
						// 3.���ظ��ļ�
						File file = getMediaFileFromServer(userName+"\\"+fileName, absPath + "/" + userName + "/"+ fileName);
						currentDownloadedFile = file;
						Log.e("FilePath", "�ļ�����·��>>>::" + file.getAbsolutePath());
						// 4.progress��ʧ
						progressDialog.dismiss();
						// 6.����AlertDialog��ʾ�û�
						Message msg = new Message();
						msg.what = DOWNLOAD_FINISH;
						msg.obj = "���سɹ�\r\n" + absPath + "/" + userName + "/" + fileName;
						handler.sendMessage(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
			}.start();
		}

		// ɾ������
		private void deleteOption() {
			dismissPopupWindow();
			// 1.�ȵ���ProgressDialog, ��ʾprogress, �ý�����ס
			progressDialog = initProgressDialog("ɾ����,���Ժ�...");
			progressDialog.show();
			// 2.���ļ���������������, ��������ɾ�����ļ������ļ�
			new Thread(){
				public void run() {
					boolean isDelete = sendFileNameToServerForDelete(userName + "\\" +fileName);
					// 4.progress��ʧ
					progressDialog.dismiss();
					Message msg = new Message();
					msg.what = DELETE_FINISH;
					if (isDelete) {
						msg.obj = fileName + "\r\nɾ���ɹ�";
					} else {
						msg.obj = fileName + "\r\nɾ��ʧ��";
					}
					// 6.��ɾ��������͸�Handler,������AlertDialog��ʾ�û�,֪ͨHandler���»�ȡlistData
					handler.sendMessage(msg);
				};
			}.start();
		}
	}
	/**
	 * ���ļ�����������������,��������ɾ�����ļ������ļ�
	 */
	private boolean sendFileNameToServerForDelete(String userName_fileName) {
		try {
			String ipAddr = "http://" + server_ip + "/BSServer/servlet/DeleteFileServlet";
			// �����������û�������
			URL url = new URL(ipAddr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-type", "application/x-java-serialized-object");
			conn.connect();
			Log.e("DELETEFILE", "sendFileNameToServerForDelete�Ƿ����ӳɹ�");
			// ����װ�õ���Ϣд����������
			ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
			objOut.writeObject(userName_fileName);
			objOut.flush();
			objOut.close();

			// ����������˷���
			ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());
			Boolean feedBack = (Boolean) objIn.readObject();
			System.out.println(feedBack + ">>>>>>>>>>>>..DeleteFilFfeedback>>>>>>>>>>>>>>>>>");
			return feedBack.booleanValue();

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	/**
	 * ��ʾ��Ϣ��ʾ�Ի���
	 * @param message ��ʾ������ʾ������
	 */
	private void showMessageDialog(String message) {
		AlertDialog.Builder builder = new Builder(getContext());
		builder.setTitle("��ʾ");
		builder.setMessage(message);
		builder.setPositiveButton("ȷ��", null);
		builder.create();
		builder.show();
	}

	/**
	 * ���ݴ����message, ��ʼ��һ��ProgressDialog
	 * 
	 * @param message�������Ի������ʾ��Ϣ
	 * @return ��������������Ի���
	 */
	private ProgressDialog initProgressDialog(String message) {
		progressDialog = new ProgressDialog(getContext());
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
			Intent intent = new Intent(getActivity(), MyVidioPlayer.class);
			intent.setData(uri);
			startActivity(intent);
		} else {
			// ʹ��ͼƬ�������
			Intent intent = new Intent(getActivity(), MyImagePlayer.class);
			intent.setData(uri);
			startActivity(intent);
		}
	}

	/**
	 * �ӷ������������ļ�
	 * 
	 * @param fileName
	 *            : Ҫ������ļ���
	 * @param fileAbsPath
	 *            : �õ��ļ�Ҫ�����·�� :���� "/data/download/example.mp4"
	 * @return ���õ�������ļ���File��ʽ���ظ�������
	 * @throws Exception
	 */
	private File getMediaFileFromServer(String userName_fileName, String fileAbsPath) throws Exception {
		String str = "http://" + server_ip + "/BSServer/servlet/MediaFileDownloadServlet";
		// �õ����������������ݵ�URL
		URL url = new URL(str);
		// �õ�HttpURLConnection���Ӷ���
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// �����������
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setConnectTimeout(4000);
		connection.setReadTimeout(4000);
		connection.setRequestProperty("Content-type", "application/x-java-serialized-object");
		connection.setRequestMethod("POST");
		connection.connect();

		// �������ߴ����"�û���\\�ļ���"����������������
		ObjectOutputStream osw = new ObjectOutputStream(connection.getOutputStream());
		osw.writeObject(userName_fileName);
		osw.flush();

		// ���շ������˷�����MediaFile����
		BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
		File file = new File(fileAbsPath);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
		byte[] buf = new byte[1024];
		int len = 0;
		while ((len = bis.read(buf)) != -1) {
			bos.write(buf, 0, len);
		}
		bos.close();
		bis.close();
		osw.close();
		// ���õ����ļ�����
		return file;
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		// ��ջ���Ŀ¼�ļ�
		File cacheDir = getContext().getCacheDir();
		File[] files = cacheDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			System.gc();
			files[i].delete();
			Log.e("CacheFile", files[i].getAbsolutePath());
		}
	}
	/**
	 * ��listView��ʼ��:�ӷ�������������û����ϴ���ʷ�ļ���Ϣ,����ʾ�ڿؼ���
	 * ��˵�����½�һ�����߳�,��ȡ�������˵�listView
	 */
	private void initListView() {
		// �½��̴߳ӷ�������ȡlistData
		new Thread() {
			@Override
			public void run() {
				try {
					// �ӷ�����������data
					loadListViewData();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * �ӷ������˼���������ʾ��listView�е�����,��ͨ��handlerˢ��listView����
	 * listDataFromServer : �����صĽ����ֵ��ȫ�ֱ���listDataFromServer
	 * 
	 */
	private void loadListViewData() {
		new Thread() {
			@Override
			public void run() {
				try {
					// �ӷ�����������data
					String str = "http://" + server_ip + "/BSServer/servlet/GetListViewDataServlet";
					// �õ����������������ݵ�URL
					URL url = new URL(str);
					// �õ�HttpURLConnection���Ӷ���
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					// �����������
					connection.setDoOutput(true);
					connection.setDoInput(true);
					connection.setUseCaches(false);
					connection.setConnectTimeout(4000);
					connection.setReadTimeout(4000);
					connection.setRequestProperty("Content-type", "application/x-java-serialized-object");
					connection.setRequestMethod("POST");
					connection.connect();

					// �õ���ǰ�û����û���,������ǰ�û������͸�������
					ObjectOutputStream osw = new ObjectOutputStream(connection.getOutputStream());
					osw.writeObject(userName);
					osw.flush();
					osw.close();
					// ���շ������˷�����listData����
					ObjectInputStream objIn = new ObjectInputStream(connection.getInputStream());
					listDataFromServerString = (ArrayList<HashMap<String[], String>>) objIn.readObject();
					objIn.close();
					// ��Stringת����ΪBitmap
					listDataFromServer = BitmapUtils.fromStringToBitmap(listDataFromServerString);
					// ����message�����͸�handler���������߳�view
					Message msg = new Message();
					msg.what = GET_LIST_DATA_FINISHED;
					handler.sendMessage(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * �ϴ���ť�����¼�, �������ϴ����Ͱ�ť�ļ����¼�
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_upload:// �ϴ���ť���
			btnUploadPressed();
			break;
		case R.id.btn_tupian_upload:// ͼƬ�ϴ�
			btnTuPianUploadPressed();
			break;
		case R.id.btn_bendi_upload:// ������Ƶ�ϴ�
			btnPaiSheUploadPressed();
			break;
		case R.id.btn_paishe_upload:// ������Ƶ�ϴ�
			btnBenDiUploadPressed();
			break;
		}
	}

	/**
	 * ҳ����Ͳ���ϴ���ť�����, ������PopupWindow
	 */
	private PopupWindow btnPopupWindow;

	/**
	 * ���ҳ��ײ����ϴ���ť������ѡ���ϴ����͵�popupWindow
	 */
	@SuppressLint("InflateParams")
	private void btnUploadPressed() {
		dismissPopupWindow();
		// �õ�popupWindow����ʾ��ͼ
		View view = LayoutInflater.from(getContext()).inflate(R.layout.popupwindow, null);
		// �õ������ϴ���ʽ�Ŀؼ�
		ImageButton btn_tupian_upload = (ImageButton) view.findViewById(R.id.btn_tupian_upload);
		ImageButton btn_bendi_upload = (ImageButton) view.findViewById(R.id.btn_bendi_upload);
		ImageButton btn_paishe_upload = (ImageButton) view.findViewById(R.id.btn_paishe_upload);
		// �������ϴ��ؼ���Ӽ����¼�
		btn_tupian_upload.setOnClickListener(this);
		btn_bendi_upload.setOnClickListener(this);
		btn_paishe_upload.setOnClickListener(this);
		// ��ʼ��popupWindow,�������ñ���
		btnPopupWindow = new PopupWindow(view, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
		btnPopupWindow.setTouchable(true);// Ĭ��Ϊtrue
		// ʹ��Animation����������view����,���ܸ�popupWindow����
		// view.setAnimation(animation);
		btnPopupWindow.setAnimationStyle(R.style.PopupAnimation);
		btnPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		// ����ⲿ����ʧ
		// BtnpPpupWindow.setOutsideTouchable(false); //����ʹ
		// ���õ�����λ��
		btnPopupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
	}

	/**
	 * ������Ƶ�ļ��ϴ���ť����¼�����
	 */
	private void btnBenDiUploadPressed() {
		btnPopupWindow.dismiss();
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(intent, CAPUTREREQUESTCODE);
	}

	/**
	 * ������Ƶ�ļ��ϴ���ť����¼�����
	 */
	private void btnPaiSheUploadPressed() {
		btnPopupWindow.dismiss();
		Intent intentOpen = new Intent(Intent.ACTION_GET_CONTENT);
		intentOpen.setType("video/*");
		startActivityForResult(intentOpen, OPENREQUESTCODE);
	}

	/**
	 * ͼƬ�ļ��ϴ���ť����¼�����
	 */
	private void btnTuPianUploadPressed() {
		btnPopupWindow.dismiss();
		Intent intentImg = new Intent(Intent.ACTION_GET_CONTENT);
		intentImg.setType("image/*");
		startActivityForResult(intentImg, IMGREQUESTCODE);
	}

	/**
	 * mediaFilePath ; ��ǰ�ϴ��ļ���·�� 
	 * mediaFilePathForInfo : ������Ϣ��������ͼ�͵õ��ļ���ʹ�õ�·��
	 */
	private String mediaFilePath;
	private String mediaFilePathForInfo;

	/**
	 * �򿪻�������ķ���Activity onActivityResult����
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// ����Ƶ��ѡ����Ƶ��Ϊ��ʱ
		if (data == null) {
			if (mediaFilePath == null) {
				// ��֪�û�δѡ���ļ�
				Toast.makeText(getContext(), R.string.no_select_video, Toast.LENGTH_SHORT).show();
			}
			return;
		} else {
			Uri uri = data.getData();
			// ������Ƶ
			if (requestCode == CAPUTREREQUESTCODE) {
				if (resultCode == Activity.RESULT_OK) {
					mediaFilePath = getVideoPath(uri);
					mediaFilePathForInfo = mediaFilePath;
					// �����ݷ�����������
					sendToServer();
				} else if (resultCode == Activity.RESULT_CANCELED) {
					Toast.makeText(getContext(), R.string.no_capture_video, Toast.LENGTH_SHORT).show();
				}
			} else if (requestCode == OPENREQUESTCODE) {
				if (resultCode == Activity.RESULT_OK) {
					mediaFilePath = uri.getPath();
					if (uri.getScheme().startsWith("content")) {
						mediaFilePath = getVideoPath(uri);
					}
					mediaFilePathForInfo = mediaFilePath;
					// �����ݷ�����������
					sendToServer();
				}
			} else if (requestCode == IMGREQUESTCODE) {
				if (resultCode == Activity.RESULT_OK) {
					mediaFilePath = uri.getPath();
					if (uri.getScheme().startsWith("content")) {
						mediaFilePath = getVideoPath(uri);
					}
					mediaFilePathForInfo = mediaFilePath;
					// �����ݷ�����������
					sendToServer();
				}
			}
		}
	}

	/**
	 * ת��uri
	 * 
	 * @param uri
	 * @return
	 */
	protected String getVideoPath(Uri uri) {
		Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
		if (cursor.moveToFirst()) {
			mediaFilePath = cursor.getString(cursor.getColumnIndex("_data"));
		}
		return mediaFilePath;
	}

	/**
	 * �����ݷ��͵�������
	 */
	private void sendToServer() {
		MyAsyncTask myAsyncTask = new MyAsyncTask();
		myAsyncTask.execute("http://" + server_ip + "/BSServer/servlet/ReceiveFileData");
	}

	/**
	 * �첽�����߳�
	 * 
	 * @author Administrator
	 *
	 */
	public class MyAsyncTask extends AsyncTask<String, Integer, String> {

		String message = null;
		ProgressDialog mProgressDialog = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mProgressDialog = new ProgressDialog(getContext());
			mProgressDialog.setCancelable(true);
			mProgressDialog.setMessage("�ļ��ϴ���...");
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setProgress(0);
			mProgressDialog.setMax(100);
			mProgressDialog.show();
		}

		/**
		 * �����ϴ�������
		 * 
		 * @param values
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if (values[0] < 100) {
				mProgressDialog.setProgress(values[0]);
			}
		}

		/**
		 * ��̨�������ʱִ�д˷���
		 * 
		 * @param s
		 */
		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);
			mProgressDialog.dismiss();
			// ����ϴ��ɹ�,���ÿ�mediaFilePath,���´��ϴ��ļ�ʹ��
			if (s.startsWith("�ϴ��ɹ�")) {
				mediaFilePath = null;
			}
			// �����Ի�����ʾ�û�:�ϴ��ɹ�����ʧ��
			new AlertDialog.Builder(getContext()).setMessage(s).setTitle(R.string.resulttip)
					.setPositiveButton(R.string.alert_dialog_btn_text, null).setCancelable(false).show();
		}

		/**
		 * ��̨��Ҫ����
		 * 
		 * @param params
		 * @return
		 */
		@Override
		protected String doInBackground(String... params) {
			try {
				file = new File(mediaFilePath);
				// ����Ҫ���������
				String userName_fileName = userName + "\\" + file.getName();
				Log.e("FilePathToUpdate", mediaFilePath + "<<<<<<");
				if (file.exists()) {
					// ��ȡý���ļ�
					FileInputStream fis = new FileInputStream(file);
					// �������Ӳ����ñ�Ҫ����
					URL url = new URL(params[0]);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setDoOutput(true);
					connection.setDoInput(true);
					connection.setConnectTimeout(3000);
					connection.setReadTimeout(3000);
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-type", "application/x-java-serialized-object");

					OutputStream out = connection.getOutputStream();
					BufferedOutputStream bufo = new BufferedOutputStream(out);
					// ��"�û���\\�ļ���"д�뵽������
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufo);
					objectOutputStream.writeObject(userName_fileName);
					objectOutputStream.flush();
					// ˢ��֮����������ļ�����
					int total = fis.available();
					byte[] buf = new byte[1024];
					int count = 0;
					int len;
					while ((len = fis.read(buf)) != -1) {
						bufo.write(buf, 0, len);
						count += len;
						publishProgress((int) ((count / (float) total) * 100));
					}
					bufo.flush();
					fis.close();
					// bufo.close();
					// ��ȡ������������Ϣ
					InputStream in = connection.getInputStream();
					BufferedReader bufr = new BufferedReader(new InputStreamReader(in));
					while ((message = bufr.readLine()) != null) {
						if (message.startsWith("OK")) {
							// �ļ����ͳɹ�,������Ϣ��handler,���䷢��fileInfo
							Message msg = new Message();
							msg.what = FILE_UPLOAD_FINISH;
							handler.sendMessage(msg);
							return "�ϴ��ɹ���";
						} else if (message.endsWith("NO")) {
							return "�ϴ�ʧ�ܣ������ԣ�";
						}
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return "�����ˣ������ԣ�";
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return "�����ˣ������ԣ�";
			} catch (ProtocolException e) {
				e.printStackTrace();
				return "�����ˣ������ԣ�";
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				return "�������ӳ�ʱ";
			} catch (IOException e) {
				e.printStackTrace();
				return "�����ˣ������ԣ�";
			}
			return "δ֪���������ԣ�";
		}
	}

	/**
	 * ���ļ�����Ϣ������������ ,����:fileName fileSize uploadTime
	 */
	public void sendFileInfoToServer() {
		new Thread() {
			public void run() {
				String[] pathArr = mediaFilePathForInfo.split("/");
				String fileName = pathArr[pathArr.length - 1];
				Log.e("FILEPATH", fileName);
				String fileSize = Formatter.formatFileSize(getContext(), file.length());
				Log.e("FILEPATH", fileSize);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d hh:mm:ss", new Locale("zh_cn"));
				String uploadTime = sdf.format(new Date());
				Log.e("FILEPATH", uploadTime);
				// filePath��ʾ��Ƶ�ļ�·��
				// kind��ʾ���ͣ�����������ѡ�
				// �ֱ���Images.Thumbnails.MICRO_KIND��Images.Thumbnails.MINI_KIND��
				// ���У�MINI_KIND: 512 x 384��MICRO_KIND: 96 x 96,
				// ��Ȼ���˴�����ᷢ�֣���Ҳ���Դ��������int�����֣�
				// ֻ�ǾͲ���Ի�ȡ��bitmap������ص����ã�
				// ���ǿ����Լ�ʹ��extractThumbnail( Bitmap source, int width, int
				// height)
				// �����Է��ص�bitmap����������á�

				// ��ȡ��ǰ�ϴ��ļ��ĸ�ʽ
				String[] path_arr = fileName.split("\\.");
				String format = path_arr[path_arr.length - 1];
				// ��������Ƶ�ļ��Ļ�,��ThumbnailUtils.createVideoThumbnail��ȡ��Ƶ������ͼ
				Bitmap thumbnail;
				if ("mp4".equalsIgnoreCase(format)) {
					thumbnail = ThumbnailUtils.createVideoThumbnail(mediaFilePathForInfo, Images.Thumbnails.MICRO_KIND);
				} else {
					// ����ʹ��ThumbnailUtils.extractThumbnail��ȡͼƬ������ͼ
					thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(mediaFilePathForInfo), 96, 96);
				}
				// ������ʹ�õ��ļ�·���ÿ�,���´�ʹ��,��������ͼ��Ҫʹ�ø�·��
				mediaFilePathForInfo = null;
				// ��װ����
				String[] info = new String[] { fileName, fileSize, uploadTime, userName };
				HashMap<String[], Bitmap> fileInfo = new HashMap<String[], Bitmap>();
				// �õ���װ����fileInfo
				fileInfo.put(info, thumbnail);
				ArrayList<HashMap<String[], Bitmap>> listData = new ArrayList<HashMap<String[], Bitmap>>();
				listData.add(fileInfo);
				// ��Bitmapת����String
				ArrayList<HashMap<String[], String>> listData_Stirng = BitmapUtils.fromBitmapToString(listData);
				// ����ϴ���ַhttp://localhost:8080/BSServer/servlet/ReceiveFileInfoServlet
				String ipAddr = "http://" + server_ip + "/BSServer/servlet/ReceiveFileInfoServlet";
				try {
					// �����������û�������
					URL url = new URL(ipAddr);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setDoOutput(true);
					conn.setDoInput(true);
					conn.setUseCaches(false);
					conn.setConnectTimeout(3000);
					conn.setReadTimeout(3000);
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-type", "application/x-java-serialized-object");
					conn.connect();
					Log.e("FILEPATH", "�Ƿ����ӳɹ�");
					// ����װ�õ���Ϣд����������
					ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
					objOut.writeObject(listData_Stirng);
					objOut.flush();
					objOut.close();
					Log.e("FILEPATH", "�Ƿ��ͳɹ�");

					// ����������˷���
					ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());
					String feedBack = (String) objIn.readObject();
					System.out.println(feedBack + ">>>>>>>>>>>>..feedback>>>>>>>>>>>>>>>>>");
					Message msg = new Message();
					msg.what = SERVER_LIST_DATA_CHANGE;
					handler.sendMessage(msg);

				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		dismissPopupWindow();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		dismissPopupWindow();
		return true;
	}
}
