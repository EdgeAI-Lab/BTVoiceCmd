package com.fan.myvoice;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static String TAG = MainActivity.class.getSimpleName();
	
	private Toast mToast;
	// 语音听写对象
	private SpeechRecognizer mIat;
	
	// 语音合成对象
	private SpeechSynthesizer mTts;
	
	private static boolean isConnectBT = false;
	
	private TextView tShow = null;
	
	// 用HashMap存储听写结果
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
	
	//用于存储命令词(此命令次为本项目自定义)
	byte[] c = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
	
	
	
	
	//add for BT
	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备
	BluetoothDevice _device = null;     //蓝牙设备
    static BluetoothSocket _socket = null;      //蓝牙通信socket
    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄
	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号
	boolean bRun = true;
	
	
	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		tShow = (TextView) findViewById(R.id.tShow);
		
		
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		
		SpeechUtility.createUtility(MainActivity.this, "appid=" + getString(R.string.app_id));
		
		// 使用SpeechRecognizer对象，可根据回调消息自定义界面；
		mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);
		// 初始化合成对象
		mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, mTtsInitListener);
		// 设置语音识别参数
		setRecognizerParam();
		// 设置语音合成参数
		setTtsParam();
		
		//如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (_bluetooth == null){
        	Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // 设置设备可以被搜索  
       new Thread(){
    	   public void run(){
    		   if(_bluetooth.isEnabled()==false){
        		_bluetooth.enable();
    		   }
    	   }   	   
       }.start();   
	}

	
	/**
	 * 初始化语音识别监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败，错误码：" + code);
			}
		}
	};

	
	/**
	 * 初始化语音合成监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	} else {
				// 初始化成功，之后可以调用startSpeaking方法
        		// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
        		// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}		
		}
	};
	
	
	
	
	/**
	 * 语音识别参数设置
	 * 
	 * @param param
	 * @return
	 */
	public void setRecognizerParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);

		// 设置听写引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

	
		mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		// 设置语言区域
		mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
		
		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
		
		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
		
		// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
		mIat.setParameter(SpeechConstant.ASR_PTT, "0");
		
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
		
		// 设置听写结果是否结果动态修正，为“1”则在听写过程中动态递增地返回结果，否则只在听写结束之后返回最终结果
		// 注：该参数暂时只对在线听写有效
		mIat.setParameter(SpeechConstant.ASR_DWA, "0");
	}
	
	
	/**
	 * 语音合成参数设置
	 * @param param
	 * @return 
	 */
	private void setTtsParam(){
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 根据合成引擎设置相应参数
		
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// 设置在线合成发音人
		mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoqi");
		
		//设置合成语速
		mTts.setParameter(SpeechConstant.SPEED, "50");
		//设置合成音调
		mTts.setParameter(SpeechConstant.PITCH, "60");
		//设置合成音量
		mTts.setParameter(SpeechConstant.VOLUME, "50");
		
		//设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
		
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
	}
	
	
	
	//接收活动结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode){
    	case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
    		// 响应返回结果
            if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                // MAC地址，由DeviceListActivity设置返回
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // 得到蓝牙设备句柄      
                _device = _bluetooth.getRemoteDevice(address);
 
                // 用服务号得到socket
                try{
                	_socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                }catch(IOException e){
                	Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                }
                //连接socket
            	Button btn = (Button) findViewById(R.id.bConnect);
                try{
                	_socket.connect();
                	isConnectBT = true;
//                	Toast.makeText(this, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                	mTts.startSpeaking("蓝牙连接成功", mTtsListener);
                	btn.setText("断开连接");
                }catch(IOException e){
                	try{
                		isConnectBT = false;
//                		Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                		mTts.startSpeaking("蓝牙连接失败", mTtsListener);
                		_socket.close();
                		_socket = null;
                	}catch(IOException ee){
                		isConnectBT = false;
//                		Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                		mTts.startSpeaking("蓝牙连接失败", mTtsListener);
                	}
                	
                	return;
                }
                
            }
    		break;
    	default:break;
    	}
    }
    
 
    
    /*
     * 帮助文档
     * 
     */
    public void getHelp(View v){
    	Intent intent = new Intent(MainActivity.this, Help.class);
    	startActivity(intent);
    }
    
    
    
    /*
     * 语音识别按键相应程序
     * 
     */
    int ret = 0; // 函数调用返回值
    public void startSpeech(View v){
    	
    	if (isConnectBT) {
    		ret = mIat.startListening(mRecognizerListener);
        	if (ret != ErrorCode.SUCCESS) {
    			showTip("听写失败,错误码：" + ret);
    		} else {
    			showTip(getString(R.string.text_begin));
    		}
		}else {
//			showTip("蓝牙未连接！");
			mTts.startSpeaking("请连接蓝牙后重试", mTtsListener);
		}
    	
    }
    
    
    
    /**
	 * 听写监听器。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			// Tips：
			// 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
			// 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//			showTip("结束说话");
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			Log.d(TAG, results.getResultString());
			
			String text = JsonParser.parseIatResult(results.getResultString());

			String sn = null;
			// 读取json结果中的sn字段
			try {
				JSONObject resultJson = new JSONObject(results.getResultString());
				sn = resultJson.optString("sn");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			mIatResults.put(sn, text);

			
			
			

			if (isLast) {
				
				StringBuffer resultBuffer = new StringBuffer();
				for (String key : mIatResults.keySet()) {
					resultBuffer.append(mIatResults.get(key));
				}
				printResult(resultBuffer);
			}
		}

		@Override
		public void onVolumeChanged(int volume, byte[] data) {
			showTip("当前正在说话，音量大小：" + volume);
			Log.d(TAG, "返回音频数据："+data.length);
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
	};

	
	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {
		
		@Override
		public void onSpeakBegin() {
//			showTip("开始播放");
		}

		@Override
		public void onSpeakPaused() {
//			showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
//			showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
				String info) {
			// 合成进度
			
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
//				showTip("播放完成");
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
	};

	
	
	private void printResult(StringBuffer results) {
		
		
		final String cmd = results.toString();
		
		tShow.setText(cmd);
		
		/*
		 * 判断是否为特定命令词
		 * add by Frank
		 */
		//前进,后退,向左转,向右转,向前滑行,向后滑行,向左转头,向右转头,摇头,抬起左臂,放下左臂,抬起右臂,放下右臂,向内侧摆动左手腕,
		//向外侧摆动左手腕,摆动左手腕,向内侧摆动右手腕,向外侧摆动右手腕,摆动右手腕,摆动腰部,停止,noCmd;
		switch (Cmd.getCmd(cmd)) {
		case 前进:
			SendByBT(1);
			showTip("Forward");
			break;
			
		case 后退:
			SendByBT(2);
			showTip("Backward");
			break;
			
		case 向左转:
		case 左转弯:
			SendByBT(3);
			showTip("TurnLeft");
			break;
		case 向右转:
		case 右转弯:
			SendByBT(4);
			showTip("TurnRight");
			break;
			
		case 向前滑行:
			SendByBT(5);
			showTip("SlideForward");
			break;
			
		case 向后滑行:
			SendByBT(6);
			showTip("SlideBackward");
			break;
			
		case 向左转头:
			SendByBT(7);
			showTip("HeadTurnLeft");
			break;
		case 向右转头:
			SendByBT(8);
			showTip("HeadTurnRight");
			break;
			
		case 摇头:
			SendByBT(9);
			showTip("HeadSwing");
			break;
			
		case 抬起左臂:
			SendByBT(10);
			showTip("LeftArmUp");
			break;
			
		case 放下左臂:
			SendByBT(11);
			showTip("LeftArmDown");
			break;
		case 抬起右臂:
			SendByBT(12);
			showTip("RightArmUp");
			break;
			
		case 放下右臂:
			SendByBT(13);
			showTip("RightArmDown");
			break;
			
		case 向左摆动左手腕:
		case 左手腕向左摆:
			SendByBT(14);
			showTip("LeftWristInside");
			break;
			
		case 向右摆动左手腕:
		case 左手腕向右摆:
			SendByBT(15);
			showTip("LeftWristInside");
			break;
		case 摆动左手腕:
			SendByBT(16);
			showTip("LeftWristSwing");
			break;
			
		case 向左摆动右手腕:
		case 右手腕向左摆:
			SendByBT(17);
			showTip("RightWristInside");
			break;
			
		case 向右摆动右手腕:
		case 右手腕向右摆:
			SendByBT(18);
			showTip("RightWristOutside");
			break;
			
		case 摆动右手腕:
			SendByBT(19);
			showTip("RightWristSwing");
			break;
		case 摆动腰部:
			SendByBT(20);
			showTip("WaistSwing");
			break;
			
		case 加速:
			SendByBT(21);
			showTip("Stop");
			break;
			
		case 减速:
			SendByBT(22);
			showTip("Stop");
			break;
			
		case 停止:
			SendByBT(23);
			showTip("Stop");
			break;

		default:
			int code = mTts.startSpeaking("指令不正确！", mTtsListener);
			if (code != ErrorCode.SUCCESS) {
				
				showTip("语音合成失败,错误码: " + code);	
				
			}
			showTip("指令不正确！");
			break;
		}

	}

	/*
	 * 将命令通过蓝牙发送出去
	 * add by Frank
	 */
	public void SendByBT(int theCmd)
	{
		OutputStream os = null;
		try {
			os = MainActivity._socket.getOutputStream();
		} catch (IOException e) {} 
		
		//蓝牙连接输出流
		switch(theCmd)
		{
		case 1:
			try {
				os.write(c[0]);
			} catch (IOException e) {} 
			break;
			
		case 2:
			try {
				os.write(c[1]);
			} catch (IOException e) {}
			break;
		case 3:
			try {
				os.write(c[2]);
			} catch (IOException e) {}
			break;
		case 4:
			try {
				os.write(c[3]);
			} catch (IOException e) {}
			break;
		case 5:
			try {
				os.write(c[4]);
			} catch (IOException e) {}
			break;
			
		case 6:
			try {
				os.write(c[5]);
			} catch (IOException e) {} 
			break;
			
		case 7:
			try {
				os.write(c[6]);
			} catch (IOException e) {}
			break;
		case 8:
			try {
				os.write(c[7]);
			} catch (IOException e) {}
			break;
		case 9:
			try {
				os.write(c[8]);
			} catch (IOException e) {}
			break;
		case 10:
			try {
				os.write(c[9]);
			} catch (IOException e) {}
			break;
			
		case 11:
			try {
				os.write(c[10]);
			} catch (IOException e) {} 
			break;
			
		case 12:
			try {
				os.write(c[11]);
			} catch (IOException e) {}
			break;
		case 13:
			try {
				os.write(c[12]);
			} catch (IOException e) {}
			break;
		case 14:
			try {
				os.write(c[13]);
			} catch (IOException e) {}
			break;
		case 15:
			try {
				os.write(c[14]);
			} catch (IOException e) {}
			break;
			
		case 16:
			try {
				os.write(c[15]);
			} catch (IOException e) {} 
			break;
			
		case 17:
			try {
				os.write(c[16]);
			} catch (IOException e) {}
			break;
		case 18:
			try {
				os.write(c[17]);
			} catch (IOException e) {}
			break;
		case 19:
			try {
				os.write(c[18]);
			} catch (IOException e) {}
			break;
		case 20:
			try {
				os.write(c[19]);
			} catch (IOException e) {}
			break;
		case 21:
			try {
				os.write(c[20]);
			} catch (IOException e) {}
			break;
			
		case 22:
			try {
				os.write(c[21]);
			} catch (IOException e) {}
			break;
			
		case 23:
			try {
				os.write(c[22]);
			} catch (IOException e) {}
			break;
		}
	}
    
    
    
  //连接按键响应函数
    public void onConnectButtonClicked(View v){ 
    	if(_bluetooth.isEnabled()==false){  //如果蓝牙服务不可用则提示
    		Toast.makeText(this, " 打开蓝牙中...", Toast.LENGTH_LONG).show();
    		return;
    	}
    	
    	
        //如未连接设备则打开DeviceListActivity进行设备搜索
    	Button btn = (Button) findViewById(R.id.bConnect);
    	if(_socket==null){
    		Intent serverIntent = new Intent(this, DeviceListActivity.class); //跳转程序设置
    		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
    	}
    	else{
    		 //关闭连接socket
    	    try{
    	    	_socket.close();
    	    	_socket = null;
//    	    	_bluetooth.disable(); //断开连接时，不想关闭蓝牙，所以注释掉
    	    	bRun = false;
    	    	btn.setText("连接蓝牙");
    	    	isConnectBT = false;
    	    	mTts.startSpeaking("蓝牙已断开", mTtsListener);
    	    }catch(IOException e){}   
    	}
    	return;
    }

    //关闭程序掉用处理部分
    public void onDestroy(){
    	super.onDestroy();
    	if(_socket!=null)  //关闭连接socket
    	try{
    		_socket.close();
    	}catch(IOException e){}
    	_bluetooth.disable(); //退出程序时时，不想关闭蓝牙，所以注释掉
    	
    	// 退出时释放连接
		mIat.cancel();
		mIat.destroy();
		
		mTts.stopSpeaking();
		// 退出时释放连接
		mTts.destroy();
    }
	

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}


	
}
