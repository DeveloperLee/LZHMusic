package com.lzh.activity;



import com.lzh.app.ApplicationManager;
import com.lzh.lzhmusic.R;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class DownloadManageActivity extends ActivityGroup {
	
	private ImageButton back_btn;
	private ImageButton manage_btn;
	private TextView manage_text;
	private FrameLayout mContent;
	
	private static final String HOME_MANAGE_ID = "manage";
	private static final String HOME_DELETE_ID = "delete"; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ApplicationManager.getInstance().addActivity(this);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.download_manage_layout);
		initView();
		showMyView();
	}

	private void initView() {
		mContent = (FrameLayout) findViewById(R.id.content);
		back_btn = (ImageButton) findViewById(R.id.download_back);
		back_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				ApplicationManager.getInstance().finishActivity();
				Intent intent = new Intent(DownloadManageActivity.this,MainActivity.class);
				startActivity(intent);
			}
		});
		manage_text = (TextView) findViewById(R.id.manage_text);
		manage_text.setText("����");
		manage_btn = (ImageButton) findViewById(R.id.download_manage_btn);
		manage_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String text = (String) manage_text.getText();
				if(text.equals("����")){
				    manage_text.setText("ɾ��");
				    showMarkView();
				}else{
					manage_text.setText("����");
					showMyView();
				}
			}
		});
		
	}
	
	class ActivityHandler extends Handler{
		
		public ActivityHandler(Looper looper){
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			
			}
		}
	}
	
	public void addView(String id, Class<?> clazz) {
		Intent intent = new Intent(this, clazz);
		//�Ƴ�������������е����
		mContent.removeAllViews();
		mContent.addView(getLocalActivityManager().startActivity(id, intent).getDecorView());
	}	

	/**
	 * ��ʾ�ҵ�����ҳ��
	 */
	private void showMyView(){
		addView(HOME_MANAGE_ID, ManageActivity.class);
	}
	
	/**
	 * ��ʾ�ҵı�ǩҳ��
	 */
	private void showMarkView(){
		addView(HOME_DELETE_ID, DeleteActivity.class);
	}
	
	@Override
	public boolean onKeyDown(int keyCode,KeyEvent event){
		
		if(keyCode == KeyEvent.KEYCODE_BACK){
			Intent intent = new Intent(DownloadManageActivity.this,MainActivity.class);
			startActivity(intent);
			ApplicationManager.getInstance().finishActivity();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}

}
