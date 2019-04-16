package cn.cnganen.fluttermynative;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;


import java.io.IOException;
import java.util.HashMap;
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

/** FlutterMyNativePlugin */
public class FlutterMyNativePlugin implements MethodCallHandler, EventChannel.StreamHandler, PluginRegistry.NewIntentListener {
  @VisibleForTesting
  static final int FLUTTER_REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION = 2344;
  private final PermissionManager permissionManager;
  private final QiniuUpload qiniuUpload;
  private BroadcastReceiver receiver;
  private String DeeplinkFilter = "deeplinkFilter";
  private HashMap<String, String> initialLink;
  Registrar registrar;
  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final FlutterMyNativePlugin plugin = new FlutterMyNativePlugin(registrar, new QiniuUpload(registrar));
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "cn.cnganen.flutter_my_native");
    channel.setMethodCallHandler(plugin);

    EventChannel eventChannel = new EventChannel(registrar.messenger(), "cn.cnganen.flutter_my_native.qiniu_upload_event");
    eventChannel.setStreamHandler(plugin.qiniuUpload);

    EventChannel linkChannel = new EventChannel(registrar.messenger(), "cn.cnganen.flutter_my_native.deeplink");
    linkChannel.setStreamHandler(plugin);

    registrar.addNewIntentListener(plugin);
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


  FlutterMyNativePlugin(final Registrar registrar, QiniuUpload qiniuUpload) {
    this.registrar = registrar;
    this.qiniuUpload = qiniuUpload;
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
      case "getInitialLink":
        getInitialLink(call, result);
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

  /**
   * 保存图片
   * @param methodCall
   * @param result
   */
  public void saveImageToGallery(MethodCall methodCall, Result result) throws IOException {

    if (!permissionManager.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      permissionManager.askForPermission(
              Manifest.permission.WRITE_EXTERNAL_STORAGE, FLUTTER_REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION);
      return;
    }
    byte[] fileData = methodCall.argument("fileData");
    String title = methodCall.argument("title");
    String desc = methodCall.argument("desc");

    //Bitmap bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.length);

    String filePath = CapturePhotoUtils.insertImage(registrar.activity().getContentResolver(), fileData, title, desc);
    result.success(filePath);
  }


  private void getInitialLink(MethodCall methodCall, Result result) {
    result.success(initialLink);
  }


  @Override
  public boolean onNewIntent(Intent intent) {
    deeplink(intent);
    return false;
  }

  private void deeplink(Intent intent) {
    Log.i("deeplink", "deeplink: " + "---->init");
    checkForLinkEvent(intent);
    checkForPushEvent(intent);
  }

  private void checkForLinkEvent(Intent intent) {
    Log.i("deeplink", "deeplink: " + "---->checkForLinkEvent");
    if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null && intent.getData().getPath() != null) {
      new ErrorLogResult("deeplink").success("getIntent2");
      openFlutterRoute(intent.getData().getPath() + "?" + intent.getData().getQuery(), "deeplink");
    }
  }

  private void checkForPushEvent(Intent intent) {
    Log.i("deeplink", "deeplink: " + "---->checkForPushEvent");
    Bundle bun = intent.getExtras();
    if (bun != null) {
      if (bun.getString("action") != null && bun.getString("action").equals("route") && bun.getString("path") != null) {
        openFlutterRoute(bun.getString("path") + "?" + bun.getString("query"), "push");
      }
    }
  }

  private void openFlutterRoute(final String url, final String f) {
    Log.i("deeplink", "deeplink: " + "---->openFlutterRoute" + url);
    initialLink = new HashMap<String, String>();
    initialLink.put("uri", url);
    initialLink.put("fragment", f);
    final Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        Intent i = new Intent();
        i.setAction(DeeplinkFilter);
        i.putExtra("uri", url);
        i.putExtra("fragment", f);
        if (receiver != null) registrar.context().sendBroadcast(i);
        timer.cancel();
      }
    }, 1000);
  }
}
