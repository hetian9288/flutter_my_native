package cn.cnganen.fluttermynative;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;


import java.io.IOException;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FlutterMyNativePlugin */
public class FlutterMyNativePlugin implements MethodCallHandler {
  @VisibleForTesting
  static final int FLUTTER_REQUEST_EXTERNAL_IMAGE_STORAGE_PERMISSION = 2344;
  private final PermissionManager permissionManager;
  private final QiniuUpload qiniuUpload;
  Registrar registrar;
  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final FlutterMyNativePlugin plugin = new FlutterMyNativePlugin(registrar, new QiniuUpload(registrar));
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "cn.cnganen.flutter_my_native");
    channel.setMethodCallHandler(plugin);

    EventChannel eventChannel = new EventChannel(registrar.messenger(), "cn.cnganen.flutter_my_native.qiniu_upload_event");
    eventChannel.setStreamHandler(plugin.qiniuUpload);
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
}
