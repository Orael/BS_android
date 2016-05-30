package com.xy.mybs;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.xy.utils.MD5Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Login extends Activity implements OnClickListener, TextWatcher {
	
	private String server_ip ;
	private static final int ILLEGEL_CHARACTER = 12;
	private EditText accountET, passwordET;
	private Button login_btn;
	private SharedPreferences sharedPre;
	private Editor editor;
	private String name;
	private String password;
	private Toast toast;
	private static String SHARED_FILE_NAME = "BSFile";

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// �������벻��ȷʱ���ܾ���¼
			case 0:
				Toast.makeText(getApplicationContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();
				break;

			// ����������ȷʱ����¼���޸����һ�ε�¼���û���
			case 1:
				editor.putBoolean("Login", true).commit();
				editor.putString("lastUser", name).commit();
				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(intent);
				finish();
				break;
			case ILLEGEL_CHARACTER:
				toast = Toast.makeText(getApplicationContext(), "������Ϸ��ַ�", Toast.LENGTH_SHORT);
				toast.show();
				break;
			
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_act);
		
		// �õ�Manifest�ļ���meta-data�еķ�����IP��ַ
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
			server_ip = applicationInfo.metaData.getString("server_ip");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		sharedPre = getApplicationContext().getSharedPreferences(SHARED_FILE_NAME, 0);
		editor = sharedPre.edit();
		boolean isLogin = sharedPre.getBoolean("Login", false);
		// ����ǵ�¼״̬���� �ϴ�δע����¼��ֱ�ӽ���
		if (isLogin) {
			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			startActivity(intent);
			finish();
		}
		// �ҵ���������ؼ�
		accountET = (EditText) findViewById(R.id.account);
		passwordET = (EditText) findViewById(R.id.password);
		login_btn = (Button) findViewById(R.id.btn_login);

		login_btn.setOnClickListener(this);
		accountET.addTextChangedListener(this);

		// �������û���������תActivity
		TextView new_user = (TextView) findViewById(R.id.new_user);
		new_user.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:
			// ��¼��ť�ĵ������,�õ��û�������û���������
			name = accountET.getText().toString().trim();
			password = passwordET.getText().toString().trim();
			// �ж������Ƿ��пյ����,�� ����ʾ�û���������
			if ("".equals(name) || "".equals(password)) {
				Toast.makeText(this, R.string.input_complete, Toast.LENGTH_SHORT).show();
			} else {
				// ����,�������߳����ӷ�����
				new Thread() {
					@Override
					public void run() {
						try {
							// ���ӷ�������֤����
							loginOption(name, password);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
			break;
			// ���û�ע�� �ı� �ĵ���¼�,��ת�����û�ע��Activity
		case R.id.new_user:
			Intent intent = new Intent();
			intent.setClass(this, Register.class);
			startActivity(intent);
			break;
		}
	}

	/**
	 * ��¼����
	 * @param name
	 * @param password
	 * @throws Exception
	 */
	private void loginOption(String name, String password) throws Exception {
		// ��װ���ݣ�������֮���password���͸���������
		String[] data = new String[] { name, MD5Utils.md5Digest(password) };
		String address = "http://" + server_ip + "/BSServer/servlet/LoginServlet";
		URL url = new URL(address);
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
		// ���û����������װ�Ķ���д��������
		connection.connect();
		ObjectOutputStream objOut = new ObjectOutputStream(connection.getOutputStream());
		objOut.writeObject(data);
		objOut.flush();
		objOut.close();
		// �����˻��������Ƿ���ȷ
		ObjectInputStream objIn = new ObjectInputStream(connection.getInputStream());
		Boolean result = (Boolean) objIn.readObject();
		objIn.close();
		// �����ؽ�����͸�Handler
		if (result) {
			handler.sendEmptyMessage(1);
		} else {
			handler.sendEmptyMessage(0);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}

	/**
	 * �����û��������:ֻ����ʻ����������/��Сд��ĸ/����,��ֹ�����ַ�����
	 */
	@Override
	public void afterTextChanged(Editable s) {
		String editable = accountET.getText().toString();
		// ����������ݹ���
		String str = stringFilter(editable.toString());
		// ������˺��������ԭ���ݲ���ͬ,��˵�����������ַ�.������ʾ���������ַ�ɾ��
		if (!editable.equals(str)) {
			accountET.setText(str);
			Message msg = new Message();
			msg.what = ILLEGEL_CHARACTER;
			handler.dispatchMessage(msg);
			// �����µĹ������λ��
			accountET.setSelection(str.length());
		}
	}

	/**
	 * �����ַ�������
	 * @param str
	 * @return
	 * @throws PatternSyntaxException
	 */
	public static String stringFilter(String str) throws PatternSyntaxException {
		// ֻ������ĸ�����ֺͺ���
		String regEx = "[^a-zA-Z0-9\u4E00-\u9FA5]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		return m.replaceAll("").trim();
	}
}
