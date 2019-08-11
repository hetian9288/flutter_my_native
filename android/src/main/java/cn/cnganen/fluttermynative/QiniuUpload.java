package cn.cnganen.fluttermynative;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class QiniuUpload implements EventChannel.StreamHandler {
    private static final String TAG = ",,,,,,,,,";
    private static final String UploadProgressFilter = "UploadProgressFilter";

    private boolean isCancelled = false;
    private Registrar registrar;
    private BroadcastReceiver receiver;

    QiniuUpload(final Registrar registrar) {
        this.registrar = registrar;
    }

    public void upload(final MethodCall call, final Result result){
        this.isCancelled = false;
        final String key = call.argument("key");
        final String token = call.argument("token");
        final String uploadType = call.argument("type");

        Configuration config = new Configuration.Builder()
                .chunkSize(512 * 1024)        // 分片上传时，每片的大小。 默认256K
                .putThreshhold(1024 * 1024)   // 启用分片上传阀值。默认512K
                .connectTimeout(10)           // 链接超时。默认10秒
                .useHttps(true)               // 是否使用https上传域名
                .responseTimeout(60)          // 服务器响应超时。默认60秒
                .zone(AutoZone.autoZone)        // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
                .build();
        // 重用uploadManager。一般地，只需要创建一个uploadManager对象
        UploadManager uploadManager = new UploadManager(config);

        UpCompletionHandler upCompletionHandler = new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject res) {
                //res包含hash、key等信息，具体字段取决于上传策略的设置
                if(info.isOK()) {
                    Log.i("qiniu", "Upload Success");
                } else {
                    Log.i("qiniu", "Upload Fail: "+info.error);
                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                }
                result.success(info.isOK());
                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
            }
        };


        UploadOptions options = new UploadOptions(null, null, false, new UpProgressHandler(){
            public void progress(String key, double percent){
                Log.i("qiniu", key + ": " + percent);
                Intent i = new Intent();
                i.setAction(UploadProgressFilter);
                i.putExtra("key", key);
                i.putExtra("percent",percent);
                registrar.context().sendBroadcast(i);
            }
        }, new UpCancellationSignal(){
            public boolean isCancelled(){
                return isCancelled;
            }
        });

        if (uploadType.equals("path")) {
            uploadManager.put((String)call.argument("filepath"),key,token,upCompletionHandler,options);
        } else if (uploadType.equals("bytes")) {
            uploadManager.put((byte[]) call.argument("bytes"),key,token,upCompletionHandler,options);
        }else {
            Log.e(TAG, "upload: 支持：path、bytes 上传类型");
        }

    }

    public void cancelUpload(final Result result){
        this.isCancelled = true;
        result.success(null);
    }

    @Override
    public void onListen(Object o, final EventChannel.EventSink eventSink) {
        Log.e(TAG,"onListen");
        this.isCancelled = false;

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG,"rec");
                String key = intent.getStringExtra("key");
                double percent = intent.getDoubleExtra("percent",0);
                HashMap<String, Object> constants = new HashMap<String, Object>();
                constants.put("key", key);
                constants.put("percent", percent);
                eventSink.success(constants);
            }
        };
        registrar.context().registerReceiver(receiver, new IntentFilter(UploadProgressFilter));
    }

    @Override
    public void onCancel(Object o) {
        Log.e(TAG,"onCancel");
        this.isCancelled = true;
        registrar.context().unregisterReceiver(receiver);
    }
}
