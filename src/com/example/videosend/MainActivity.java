package com.example.videosend;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
	String md5;
	File big;
	String upload_token;
	String upload_server_uri;
	String code;
	String access_token;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		WebView web = (WebView) findViewById(R.id.webview);
		String url = "https://openapi.youku.com/v2/oauth2/authorize?client_id=08bd74bd69c8365f&response_type=code&redirect_uri=http://www.uvicsoft.com";
		web.getSettings().setJavaScriptEnabled(true);
		web.requestFocus();
		web.loadUrl(url);
		web.setWebViewClient(new WebViewClient(){
			public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {};
			@Override 
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				code = getcode(url);
				Log.i("code", url);
				Log.i("code", code); 
				
				if(!"".equals(code)){
					Toast.makeText(MainActivity.this, code, Toast.LENGTH_LONG).show();
					uploadnew();
				}
				
			} 
		});
		
	}
	private void uploadnew() {
		new Thread(){
			public void run() {
				get_access_token();
				get_upload_info();
				
				try {
					upload();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				newSlice();
				uploadSlice();
				
				//getUploadState();
				
				//commit();
				
			}
		}.start();
	}
	
	private void upload() throws Exception {
		
		 FileInputStream fis;
	     fis = new FileInputStream(big);
	     int fileLen = fis.available(); 
	     
	     Log.i("file length= ", fileLen+"  ");
	     
	     Log.i("file len ", big.length()+" ");
	 
			String url =  "http://"	+upload_server_ip+"/gupload/create_file";
			Log.i("create url ", url);
			 HttpClient httpClient = new DefaultHttpClient();
		        HttpPost httpPost = new HttpPost(url);
		        
		        HttpParams httpParams = httpClient.getParams();
		         
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("upload_token",upload_token));
				params.add(new BasicNameValuePair("file_size", ""+fileLen));
				params.add(new BasicNameValuePair("ext","3gp"));
				params.add(new BasicNameValuePair("slice_length", "2048"));
				httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				httpPost.setParams(httpParams);
				HttpResponse httpResponse;
				try {
					httpResponse = httpClient.execute(httpPost);
					HttpEntity entity = httpResponse.getEntity();
					String strResult = EntityUtils.toString(entity, "utf-8");
					Log.i("upload create file", strResult);
				} catch (Exception e) {
					e.printStackTrace();
				}
				 
	}
	
	private void newSlice(){
		
		try {
			String url = "http://"+ upload_server_ip+"/gupload/new_slice";
			url = url+"?upload_token="+upload_token;
			HttpClient httpClient = new DefaultHttpClient();
			Log.i("new slice url ", url);
			HttpGet httpGet = new HttpGet(url);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			String strResult = EntityUtils.toString(entity, "utf-8");
			Log.i("new slice info ", strResult);
			
			JSONObject jObj = new JSONObject(strResult);
			taskId = jObj.getString("slice_task_id");
			offset = jObj.getInt("offset");
			length = jObj.getInt("length");
			Log.i("new slice  id  ", taskId);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
	}
	
	String taskId;
	int offset;
	int length;
	
	String boundary = "*****";
	
	private void uploadSlice(){
		
		try {
			String url = "http://" +upload_server_ip+"/gupload/upload_slice";
			url = url+"?upload_token="+upload_token+"&slice_task_id="
				+taskId+"&offset="+offset+"&length="+length;
			Log.i("upload slice ", url);
			URL u =  new URL(url);
			HttpURLConnection con =  (HttpURLConnection) u.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Connection", "Keep-Alive");
			
		    con.setRequestProperty("Content-Type", "multipart/form-data");
			con.setFixedLengthStreamingMode(length);
			con.setRequestProperty("Content-Length", "" +length);
			con.setDoInput(true);
			con.setDoOutput(true);
			
			con.connect();
			
			OutputStream out =  con.getOutputStream() ;
	 
			FileInputStream is =new FileInputStream(big);
			
			int rn;  
			byte[] buf = new byte[length];    
			while ((rn = is.read(buf, offset, length)) > 0) { 
				out.write(buf, offset, rn);  
			}  
			out.flush();  
			out.close();  
			is.close(); 
			
			InputStream sis=con.getInputStream();
			DataInputStream dis=new DataInputStream(sis);
			byte d[]=new byte[dis.available()];
			dis.read(d);
		    String data=new String(d);
		    con.disconnect();
			Log.i("upload slice ", data);
			
			JSONObject jObj = new JSONObject(data);
			boolean isFinished = jObj.getBoolean("finished");
			taskId = jObj.getString("slice_task_id");
			offset = jObj.getInt("offset");
			length = jObj.getInt("length");
			
			getUploadState();
		 
			
/*			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);
			MultipartEntity reqEntity = new MultipartEntity();
			FileBody fB = new FileBody(big);
            reqEntity.addPart("data", fB);
	        httpPost.setEntity(reqEntity);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity entity = httpResponse.getEntity();
			String strResult = EntityUtils.toString(entity, "utf-8");
			Log.i("upload slice info ", strResult);*/
		} catch (Exception e) {
			Log.e("upload slice  ", e.getLocalizedMessage()+" "+e.getStackTrace().toString());
		}
		
	}
	
	 String upload_server_id;
	
	private void getUploadState(){
		HttpClient httpC = new DefaultHttpClient();
		String url = "http://"+upload_server_ip+"/gupload/check";
		url = url+"?upload_token="+upload_token;
		HttpGet httpGet = new HttpGet(url);
		try {
			HttpResponse response = httpC.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String strResult = EntityUtils.toString(entity, "utf-8");
			Log.i("Upload state ", strResult);
			JSONObject jObj = new JSONObject(strResult);
			Log.i("upload state   ", strResult);
			upload_server_id = jObj.getString("upload_server_ip");
			boolean isFinished = jObj.getBoolean("finished");
			if(isFinished){
				commit();
			}
			else{
				uploadSlice();
			}
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void get_upload_format(){
		HttpClient httpC = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("https://openapi.youku.com/v2/schemas/upload/spec.json");
		try {
			HttpResponse response = httpC.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String strResult = EntityUtils.toString(entity, "utf-8");
			Log.i("upload_format   ", strResult);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void commit() {
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("https://openapi.youku.com/v2/uploads/commit.json");
			HttpParams httpParams = httpClient.getParams();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("access_token",access_token));
			params.add(new BasicNameValuePair("client_id","08bd74bd69c8365f"));
			params.add(new BasicNameValuePair("upload_token", upload_token));
			params.add(new BasicNameValuePair("upload_server_ip",upload_server_id));
			httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			httpPost.setParams(httpParams);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity entity = httpResponse.getEntity();
			String strResult = EntityUtils.toString(entity, "utf-8");
			Log.i("commit info ", strResult);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	};
	
	String strFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+File.separator+"Banban"+File.separator+"201309"+File.separator+"450.3gp";
	public void get_upload_info() {
		
		//the local video file
		
		big = new File(strFile);
		try {
			md5 = MD5FileUtil.getFileMD5String(big);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		
		try {
			
			 FileInputStream fis;
		     fis = new FileInputStream(big);
		     int fileLen = fis.available(); 
			 Log.i("file length", ""+fileLen);
			
			String url  = "https://openapi.youku.com/v2/uploads/create.json?"
						+ "client_id=08bd74bd69c8365f&"
						+ "access_token="	+	access_token	+	"&"
						+ "title=demo_video&"
						+ "tags=hello&"
						+ "public_type=all&"
						+ "copyright_type=original&"
						+ "watch_password=''&"
						+ "description=''&"
						+ "file_md5=" + md5 + "&"
						+ "file_name=hell.3gp&"
						+ "file_size="+fileLen+"&"
						+ "latitude=0&"
						+ "longitude=0";
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			String strResult = EntityUtils.toString(entity, "utf-8");
			
			Log.i("get upload info  ",  strResult);
			JSONObject json = new JSONObject(strResult);
			upload_token = json.getString("upload_token");
			upload_server_uri = json.getString("upload_server_uri");
			Log.i("upload_server_url ", upload_server_uri);
			
			InetAddress giriAddress =   InetAddress.getByName(upload_server_uri);
			upload_server_ip = giriAddress.getHostAddress();
			Log.i("upload_server_ip ", upload_server_ip);
			
			InetAddress add = InetAddress.getByName(upload_server_uri.trim());
			String ip = add.getHostAddress();
			Log.i("ip ",ip);
			
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	String upload_server_ip;
	
	public void get_access_token() {
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("https://openapi.youku.com/v2/oauth2/token");
			HttpParams httpParams = httpClient.getParams();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("client_id","08bd74bd69c8365f"));
			params.add(new BasicNameValuePair("client_secret","c0a3b9cbd584ee23fca76297b04d0e06"));
			params.add(new BasicNameValuePair("grant_type", "authorization_code"));
			params.add(new BasicNameValuePair("code",code));
			params.add(new BasicNameValuePair("redirect_uri", "http://www.uvicsoft.com"));
			httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			httpPost.setParams(httpParams);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity entity = httpResponse.getEntity();
			String strResult = EntityUtils.toString(entity, "utf-8");
			JSONObject json = new JSONObject(strResult);
			access_token = json.getString("access_token");
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	private String getcode(String url) {
		if(url.contains("code=") && url.contains("&state=")){
			String code = url.substring(url.indexOf("code=")+5,url.indexOf("&state="));
			return code;
		}
		return "";
	}
 

	public Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 200:
				Bundle bundle = msg.getData();
				String temp = bundle.getString("result");
				break;
			default:
				Toast.makeText(MainActivity.this, "��������ʧ�ܣ�������룺" + msg.what,
						100).show();
				break;
			}
		};
	};
}
