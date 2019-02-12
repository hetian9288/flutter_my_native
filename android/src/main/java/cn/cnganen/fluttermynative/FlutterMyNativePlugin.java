package cn.cnganen.fluttermynative;

import android.content.Intent;
import android.net.Uri;


import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FlutterMyNativePlugin */
public class FlutterMyNativePlugin implements MethodCallHandler {

  Registrar registrar;
  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "cn.cnganen.flutter_my_native");
    channel.setMethodCallHandler(new FlutterMyNativePlugin(registrar));
  }


  FlutterMyNativePlugin(final Registrar registrar) {
    this.registrar = registrar;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("goToHome")) {
      goToHome();
      result.success("success");
    } else if (call.method.equals("openlink")) {
      openNativeLink(call, result);
    }  else if (call.method.equals("hasOpen")) {
      hasOpen(call, result);
    } else {
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
}
