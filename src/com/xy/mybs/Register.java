package com.xy.mybs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.xy.utils.MD5Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
import android.widget.Toast;

public class Register extends Activity implements OnClickListener, TextWatcher {

	private String server_ip;
	protected static final int REGISTE_SUCCESS = 1;
	protected static final int REGISTE_FALSE = 0;
	private static final int ILLEGEL_CHARACTER = 2;
	protected static final int REGISTE_ERROR = 3;
	private EditText register_account;
	private EditText register_password_check;
	private EditText register_password;
	private Button btn_register;
	private Toast toast;
	private String acount;
	private String pass;
	private String pass_check;
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REGISTE_FALSE:
				toast = Toast.makeText(getApplicationContext(), "���û����ѱ�ʹ��", Toast.LENGTH_SHORT);
				toast.show();
				break;
			case REGISTE_ERROR:
				toast = Toast.makeText(getApplicationContext(), "ϵͳ����������", Toast.LENGTH_SHORT);
				toast.show();
				break;
			case REGISTE_SUCCESS:
				toast = Toast.makeText(getApplicationContext(), "ע��ɹ�", Toast.LENGTH_SHORT);
				toast.show();
				Intent intent = new Intent(getApplication(), Login.class);
				startActivity(intent);
				finish();
				break;
			case ILLEGEL_CHARACTER:
				toast = Toast.makeText(getApplicationContext(), "������Ϸ��ַ�", Toast.LENGTH_SHORT);
				toast.show();
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_act);

		// �õ�Manifest�ļ���meta-data�еķ�����IP��ַ
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(),
					PackageManager.GET_META_DATA);
			server_ip = applicationInfo.metaData.getString("server_ip");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// �õ���������ؼ�
		register_account = (EditText) findViewById(R.id.register_account);
		register_password = (EditText) findViewById(R.id.register_password);
		register_password_check = (EditText) findViewById(R.id.register_password_check);

		register_account.addTextChangedListener(this);
		// �õ�ע�ᰴť�ؼ�
		btn_register = (Button) findViewById(R.id.btn_register);
		// ���õ��������
		btn_register.setOnClickListener(this);

	}

	/**
	 * �ؼ����������
	 * 
	 * @param v
	 */

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_register:
			btnRegisterPressed();
		}
	}

	/**
	 * ע�ᰴť�ĵ���¼�
	 */
	private void btnRegisterPressed() {
		// �õ��ؼ��е�����
		acount = register_account.getText().toString().trim();
		pass = register_password.getText().toString().trim();
		pass_check = register_password_check.getText().toString().trim();
		// ���ж��Ƿ�ȫ������
		if ("".equals(acount) || "".equals(pass) || "".equals(pass_check)) {
			// δ����������Toast��ʾ
			toast = Toast.makeText(this, "����������", Toast.LENGTH_SHORT);
			toast.show();
		} else {
			// �ж���������������Ƿ�һ��
			if (pass.equals(pass_check)) {
				// ���һ�£��򴴽����̣߳����ӷ�����������ע�����
				new Thread() {
					@Override
					public void run() {
						Message msg = new Message();
						try {
							// ���û�ע����Ϣ�������������˽�����֤
							boolean isSuccess = registe(acount, pass);
							// ע��ɹ�
							if (isSuccess) {
								msg.what = REGISTE_SUCCESS;
							} else {
								msg.what = REGISTE_FALSE;
							}

						} catch (ClassNotFoundException e) {
							e.printStackTrace();
							msg.what = REGISTE_ERROR;
						} catch (IOException e) {
							e.printStackTrace();
							msg.what = REGISTE_ERROR;
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
							msg.what = REGISTE_ERROR;
						}
						handler.sendMessage(msg);
					};
				}.start();
			} else {
				// ��һ����Toast��ʾ
				toast = Toast.makeText(this, "�����������벻һ��", Toast.LENGTH_SHORT);
				toast.show();
			}
		}

	}

	/**
	 * ����ע�����������һ��boolean���͵�ֵ����־�Ƿ�ע��ɹ� true ����ɹ� false �����û����Ѿ�ʹ��
	 * 
	 * @param acount
	 * @param pass
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException 
	 */
	private boolean registe(String acount, String pass) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		// ��װ���ݣ�ע����������
		String[] data = new String[] { acount, MD5Utils.md5Digest(pass) };
		// ע��URL
		String address = "http://" + server_ip + "/BSServer/servlet/RegisterServlet";
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
		// �����˻��������Ƿ���ȷ
		ObjectInputStream objIn = new ObjectInputStream(connection.getInputStream());
		Boolean result = (Boolean) objIn.readObject();
		objIn.close();
		objOut.close();
		return result.booleanValue();

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		String editable = register_account.getText().toString();  
        String str = stringFilter(editable.toString());
        if(!editable.equals(str)){
        	register_account.setText(str);
        	Message msg = new Message();
        	msg.what = ILLEGEL_CHARACTER;
        	handler.dispatchMessage(msg);
            //�����µĹ������λ��  
        	register_account.setSelection(str.length());
        }		
	}
	public static String stringFilter(String str)throws PatternSyntaxException{     
	      // ֻ������ĸ�����ֺͺ���      
	      String   regEx  =  "[^a-zA-Z0-9\u4E00-\u9FA5]";                     
	      Pattern   p   =   Pattern.compile(regEx);     
	      Matcher   m   =   p.matcher(str);     
	      return   m.replaceAll("").trim();     
	  }

}
