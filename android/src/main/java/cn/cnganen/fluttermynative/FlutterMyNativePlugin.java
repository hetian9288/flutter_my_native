package cn.cnganen.fluttermynative;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;


import com.alipay.sdk.app.AuthTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import io.flutter.plugin.common.ErrorLogResult;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/** FlutterMyNativePlugin */
public class FlutterMyNativePlugin implements MethodCallHandler, EventChannel.StreamHandler, PluginRegistry.NewIntentListener {
  @VisibleForTesting
  static final int FLUTTER_REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION = 2344;
  private final PermissionManager permissionManager;
//  private QiniuUpload qiniuUpload;
  private BroadcastReceiver receiver;
  private String DeeplinkFilter = "deeplinkFilter";
  static public HashMap<String, String> initialLink;
  static FlutterMyNativePlugin myNativePlugin;
  static QiniuUpload qiniuUpload;
  ScriptEngine engine;
  Registrar registrar;
  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    qiniuUpload = new QiniuUpload(registrar);
    myNativePlugin = new FlutterMyNativePlugin(registrar);
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "cn.cnganen.flutter_my_native");
    channel.setMethodCallHandler(myNativePlugin);

    EventChannel eventChannel = new EventChannel(registrar.messenger(), "cn.cnganen.flutter_my_native.qiniu_upload_event");
    eventChannel.setStreamHandler(qiniuUpload);

    EventChannel linkChannel = new EventChannel(registrar.messenger(), "cn.cnganen.flutter_my_native.deeplink");
    linkChannel.setStreamHandler(myNativePlugin);

    registrar.addNewIntentListener(myNativePlugin);
  }

  @Override
  public void onListen(Object o, final EventChannel.EventSink eventSink) {
    receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String uri = intent.getStringExtra("uri");
        String fragment = intent.getStringExtra("fragment");
        HashMap<String, Object> constants = new HashMap<String, Object>();
        constants.put("uri", uri);
        constants.put("fragment", fragment);
        eventSink.success(constants);
      }
    };
    registrar.context().registerReceiver(receiver, new IntentFilter(DeeplinkFilter));
  }

  @Override
  public void onCancel(Object o) {
    registrar.context().unregisterReceiver(receiver);
  }

  interface PermissionManager {
    boolean isPermissionGranted(String permissionName);

    void askForPermission(String permissionName, int requestCode);
  }

  QiniuUpload getQiniuUpload() {
    return qiniuUpload;
  }

  FlutterMyNativePlugin(final Registrar registrar) {
    this.registrar = registrar;
    permissionManager = new PermissionManager() {
      @Override
      public boolean isPermissionGranted(String permissionName) {
        return ActivityCompat.checkSelfPermission(registrar.activity(), permissionName)
                == PackageManager.PERMISSION_GRANTED;
      }

      @Override
      public void askForPermission(String permissionName, int requestCode) {
        ActivityCompat.requestPermissions(registrar.activity(), new String[] {permissionName}, requestCode);
      }
    };
    deeplink(registrar.activity().getIntent());
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {
      case "goToHome":
        goToHome();
        result.success("success");
        break;
      case "openlink":
        openNativeLink(call, result);
        break;
      case "hasOpen":
        hasOpen(call, result);
        break;
      case "saveFile":
        try {
          saveImageToGallery(call, result);
        } catch (IOException e) {
          result.error("110", "save error", "");
        }
        break;
      case "upload":
        qiniuUpload.upload(call, result);
        break;
      case "cancelUpload":
        qiniuUpload.cancelUpload(result);
        break;
      case "alipayAuto":
        alipayAuto(call, result);
        break;
      case "getInitialLink":
        getInitialLink(call, result);
        break;
      case "runJavaScript":
        runJavaScript(call, result);
        break;
      default:
        result.notImplemented();
    }
  }

  void goToHome() {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_HOME);
    registrar.activity().startActivity(intent);
  }

  void openNativeLink(MethodCall call, Result result) {
    String content = call.argument("uri");
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(content));
    registrar.activity().startActivity(intent);
  }

  void hasOpen(MethodCall call, Result result) {
    final boolean isIntall = Utils.checkHasInstalledApp(registrar.context(), (String)call.argument("pkgname"));
    if (isIntall) {
      result.success("success");
    }else {
      result.error("110", "not install", "");
    }
  }

  // 运行
  void runJavaScript(MethodCall call, final Result result) {
    final String jsCode = call.argument("jsCode");
    ScriptEngineManager sem = new ScriptEngineManager();
    if (engine == null) {
      engine=sem.getEngineByName("javascript");
    }
    try {
      result.success(engine.eval(jsCode).toString());
    } catch (ScriptException e) {
      result.success("");
    }
  }

  // 支付宝授权
  void alipayAuto(MethodCall call, final Result result) {
    final String authInfo = call.argument("authInfo");
    if (authInfo.isEmpty()) {
      result.error("error", "authInfo 参数必传", null);
      return;
    }
    Runnable authRunnable = new Runnable() {

      @Override
      public void run() {
        // 构造AuthTask 对象
        AuthTask authTask = new AuthTask(registrar.activity());
        // 调用授权接口，获取授权结果
        Map<String, String> ret = authTask.authV2(authInfo, true);

        result.success(ret);
      }
    };

    // 必须异步调用
    Thread authThread = new Thread(authRunnable);
    authThread.start();
  }

  /**
   * 保存图片
   * @param methodCall
   * @param result
   */
  public void saveImageToGallery(MethodCall methodCall, final Result result) throws IOException {

    if (!permissionManager.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      permissionManager.askForPermission(
              Manifest.permission.WRITE_EXTERNAL_STORAGE, FLUTTER_REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION);
      return;
    }
    final byte[] fileData = methodCall.argument("fileData");
    final String filePath = methodCall.argument("filePath"); // 图片路径方式
    final String title = methodCall.argument("title");
    final String desc = methodCall.argument("desc");

    // Bitmap bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.length);
    new Thread(new Runnable() {
      @Override
      public void run() {
        String newPath = null;
        try {
          byte[] data = new byte[1024];
          // 优先使用路径方式
          if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);
            if (!file.exists()) {
              result.error("fail", "文件不存在", null);
              return;
            }
            FileInputStream inputStream = new FileInputStream(file);
            data = readStream(inputStream);
          } else if (fileData != null) {
            data = fileData;
          }
          newPath = CapturePhotoUtils.insertImage(registrar.activity().getContentResolver(), data, title, desc);
        } catch (IOException e) {
          result.error("fail", e.getLocalizedMessage(), null);
          return;
        } catch (Exception e) {
          e.printStackTrace();
        }
        result.success(newPath);
      }
    }).run();
  }

  public byte[] readStream(InputStream inStream) throws Exception {
    byte[] buffer = new byte[1024];
    int len = -1;
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    while ((len = inStream.read(buffer)) != -1) {
      outStream.write(buffer, 0, len);
    }
    byte[] data = outStream.toByteArray();
    outStream.close();
    inStream.close();
    return data;
  }


  private void getInitialLink(MethodCall methodCall, Result result) {
    result.success(initialLink);
  }


  @Override
  public boolean onNewIntent(Intent intent) {
    deeplink(intent);
    return false;
  }

  static public void deeplink(Intent intent) {
    Log.i("deeplink", "deeplink: " + "---->init");
    checkForLinkEvent(intent);
    checkForPushEvent(intent);
  }

  static void checkForLinkEvent(Intent intent) {
    Log.i("deeplink", "deeplink: " + "---->checkForLinkEvent");
    if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null && intent.getData().getPath() != null) {
      new ErrorLogResult("deeplink").success("getIntent2");
      openFlutterRoute(intent.getData().getPath() + "?" + intent.getData().getQuery(), "deeplink");
    }
  }

  static void checkForPushEvent(Intent intent) {
    Log.i("deeplink", "deeplink: " + "---->checkForPushEvent");
    Bundle bun = intent.getExtras();
    if (bun != null) {
      if (bun.getString("action") != null && bun.getString("action").equals("route") && bun.getString("path") != null) {
        openFlutterRoute(bun.getString("path") + "?" + bun.getString("query"), "push");
      }
    }
  }

  static void openFlutterRoute(final String url, final String f) {
    Log.i("deeplink", "deeplink: " + "---->openFlutterRoute" + url);
    initialLink = new HashMap<String, String>();
    initialLink.put("uri", url);
    initialLink.put("fragment", f);
    final Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (myNativePlugin != null) {
          Intent i = new Intent();
          i.setAction(myNativePlugin.DeeplinkFilter);
          i.putExtra("uri", url);
          i.putExtra("fragment", f);
          if (myNativePlugin.receiver != null) myNativePlugin.registrar.context().sendBroadcast(i);
          timer.cancel();
        }
      }
    }, 1000);
  }
}
