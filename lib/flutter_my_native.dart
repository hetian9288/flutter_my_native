import 'dart:async';

import 'package:flutter/services.dart';
import 'dart:io';

class FlutterMyNative {
  static const MethodChannel _channel =
      const MethodChannel('cn.cnganen.flutter_my_native');

  static Future<String> goToHome() async {
    assert(Platform.isAndroid, "回到桌面只支持android");
    final String version = await _channel.invokeMethod('goToHome');
    return version;
  }
}
