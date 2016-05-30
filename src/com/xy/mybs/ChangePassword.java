package com.xy.mybs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import com.xy.utils.MD5Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChangePassword extends Activity implements OnClickListener{
	
	private String server_ip;
	private EditText old_pass_et;
	private EditText new_pass_et;
	private EditText new_pass_check_et;
	private String userName;
	private SharedPreferences sharedPre;
	private static String SHARED_FILE_NAME = "BSFile";
	private static final int CHANGE_SUCCESS = 1;
	private static final int CHANGE_FAIL = 2;
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CHANGE_SUCCESS:
				setResult(RESULT_OK);
				showDialog("�޸ĳɹ�");
				break;
			case CHANGE_FAIL:
				setResult(RESULT_CANCELED);
				showDialog("�޸�ʧ��");
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.change_password);
		// �õ�Manifest�ļ���meta-data�еķ�����IP��ַ
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
			server_ip = applicationInfo.metaData.getString("server_ip");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		// ��ʼ��ȷ���޸İ�ť, �����õ���¼�������
		Button btn_change_password = (Button) findViewById(R.id.btn_change_password);
		btn_change_password.setOnClickListener(this);
		old_pass_et = (EditText) findViewById(R.id.change_password_old_pass);
		new_pass_et = (EditText) findViewById(R.id.change_password_new_pass);
		new_pass_check_et = (EditText) findViewById(R.id.change_password_new_pass_checked);
		// ��ʼ��sharedPreference
		sharedPre = getSharedPreferences(SHARED_FILE_NAME, 0);
		// �õ���ǰ�û���
		userName = sharedPre.getString("lastUser", "");
	}

	private String old_pass ;
	private String new_pass ;
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_change_password:
			// 1. �õ�����������,������
			old_pass = old_pass_et.getText().toString();
			new_pass = new_pass_et.getText().toString();
			String new_pass_check = new_pass_check_et.getText().toString();
			// 2. ����Ƿ��пյ����
			if (TextUtils.isEmpty(old_pass) || TextUtils.isEmpty(new_pass) || TextUtils.isEmpty(new_pass_check)) {
				Toast.makeText(this, "����������", Toast.LENGTH_SHORT).show();
			} else {
				// 3. �ж�ȷ��������������Ƿ���ͬ,����ͬ,�����ӷ�������֤
				if (new_pass.equals(new_pass_check)) {
					new Thread(){
						public void run() {
							boolean isSuccess = btnChangePassOption();					
							Message msg = new Message();
							// ����޸ĳɹ�
							if (isSuccess) {
								msg.what = CHANGE_SUCCESS;
							} else {
								// �޸�ʧ��
								msg.what = CHANGE_FAIL;
							}
							handler.sendMessage(msg);
						};
					}.start();
				} else {
					Toast.makeText(this, "��������ȷ�����벻ͬ", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		}
	}

	private boolean btnChangePassOption() {
		try {
			// ��װ���������
			ArrayList<String> data = new ArrayList<String>();
			data.add(userName);
			data.add(MD5Utils.md5Digest(old_pass));
			data.add(MD5Utils.md5Digest(new_pass));
			// 2. ���ӷ�������֤�������Ƿ���ȷ,��ȷ���޸�
			// �ӷ�����������data
			String str = "http://" + server_ip + "/BSServer/servlet/ChangePasswordServlet";
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
			osw.writeObject(data);
			osw.flush();
			osw.close();

			// ���շ������˷�����listData����
			ObjectInputStream objIn = new ObjectInputStream(connection.getInputStream());
			Boolean isChange = (Boolean) objIn.readObject();
			objIn.close();
			return isChange.booleanValue();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	private void showDialog(final String message){
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if ("�޸ĳɹ�".equals(message)) {
					finish();
				}
			}
		}).show();
	}
}
