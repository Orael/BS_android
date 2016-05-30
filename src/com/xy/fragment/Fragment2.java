package com.xy.fragment;

import java.util.ArrayList;

import com.xy.adapter.Frag2ListAdapter;
import com.xy.mybs.ChangePassword;
import com.xy.mybs.DownLoadHistory;
import com.xy.mybs.Login;
import com.xy.mybs.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class Fragment2 extends Fragment implements OnItemClickListener, OnClickListener {
	
	private static int CHANGE_PASSWORD_REQUEST_CODE = 1;
	private static String SHARED_FILE_NAME = "BSFile";
	private ListView lv;
	private View view;
	private Editor editor;
	private String nickName;
	private Button btn_logoff;
	private String lastusername;
	private SharedPreferences sharedPre;
	private Frag2ListAdapter listAdapter;
	private TextView frag2_username_text;
	private ArrayList<String> frag2ListData;

	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment2, null);
		init();
		return view;
	}

	/**
	 * ��ʼ������
	 */
	private void init() {
		// ��ʼ��ҳ��ײ��"�˳���¼"��ť
		btn_logoff = (Button) view.findViewById(R.id.btn_logoff);
		btn_logoff.setOnClickListener(this);
		// �洢����ʹ��
		sharedPre = getActivity().getSharedPreferences(SHARED_FILE_NAME, 0);
		editor = sharedPre.edit();
		// ��ʼ����ʾ��ǰ�û��ؼ�����ʾ��������
		frag2_username_text = (TextView) view.findViewById(R.id.frag2_username_text);
		lastusername = sharedPre.getString("lastUser", "user");
		nickName = sharedPre.getString(lastusername+"nickName", lastusername);
		frag2_username_text.setText("�ǳ�:" + nickName +"\r\n["+lastusername+"]");
		LinearLayout face = (LinearLayout) view.findViewById(R.id.frag2_face);
		face.getBackground().setAlpha(100);

		lv = (ListView) view.findViewById(R.id.frag2_list_view);
		frag2ListData = new ArrayList<String>();
		frag2ListData.add("������ʷ");
		frag2ListData.add("�����ǳ�");
		frag2ListData.add("�޸�����");
		listAdapter = new Frag2ListAdapter(getActivity(), frag2ListData);
		// ��listView����������
		lv.setAdapter(listAdapter);
		// ��listView����item���������
		lv.setOnItemClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_logoff:
			// ҳ����Ͳ� �˳���ǰ��¼�İ�ť
			editor.putBoolean("Login", false).commit();
			Intent intent = new Intent(getActivity().getApplicationContext(), Login.class);
			getActivity().startActivity(intent);
			getActivity().finish();
			break;
		}
	}

	/**
	 * listView item ����¼�����
	 * @param parent : listView
	 * @param view : itemView
	 * @param position : ���λ��
	 * @param id : ���item��ID
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch(position){
		case 0:
			// ������ʷѡ��
			showDownloadHistory();
			break;
		case 1:
			// �����ǳ�ѡ��
			setNickName();
			break;
		case 2:
			// �޸�����
			changePassword();
			break;
		}
	}

	/**
	 * ��ʾ������ʷ : ��һ���µ�Activity
	 * ����DownloadĿ¼�еĵ��ļ���,���ļ�����listData
	 * ����ͬ��ѡ�����,��ʾ��ҳ����.
	 */
	private void showDownloadHistory() {
		// ���µ�Activity,��ʾ�Ѿ����ص��ļ����б���Ϣ
		Intent intent = new Intent(getContext(), DownLoadHistory.class);
		startActivity(intent);
	}

	/**
	 * �޸��������
	 */
	private void changePassword() {
		Intent intent = new Intent(getContext(), ChangePassword.class);
		startActivityForResult(intent, CHANGE_PASSWORD_REQUEST_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CHANGE_PASSWORD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			btn_logoff.performClick();
		}
	}
	/**
	 * �����ǳƹ���
	 */
	private EditText editText;
	private void setNickName() {
		// 1.��������Ի���
		editText = new EditText(getContext());
		editText.setBackground(null);
		editText.setHint("�������ǳ�");
		editText.setTextSize(25f);
		Builder builder = new AlertDialog.Builder(getContext());
		builder.setView(editText);
		builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// �õ��û�������ı�
				nickName = editText.getText().toString().trim();
				// д�뵽sharedPreference
				editor.putString(lastusername+"nickName", nickName);
				editor.commit();
				// ����ҳ����ʾ���ǳ�
				frag2_username_text.setText("��ǰ�û�:" + nickName +"\r\n["+lastusername+"]");
			}
		});
		builder.setNegativeButton("ȡ��", null);
		builder.create();
		builder.show();
	}

}
