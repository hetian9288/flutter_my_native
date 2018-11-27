package cn.cnganen.fluttermynative;

import android.content.Intent;

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

  FlutterMyNativePlugin(Registrar registrar) {
    this.registrar = registrar;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("goToHome")) {
      goToHome();
      result.success("success");
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
}
