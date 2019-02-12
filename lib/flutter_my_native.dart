import 'dart:async';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart';
import 'dart:io';

// 根据app 链接判断是否安装、打开
class MyNativeUri {
  final Uri uri;
  MyNativeUri({@required this.uri});
  
  Future<bool> hasOpen() async {
    if (Platform.isAndroid) {
      return await FlutterMyNative.hasOpenNativeLinkAndroid(pkgname: uri.host);
    }else if (Platform.isIOS) {
      return await FlutterMyNative.hasOpenNativeLinkIos(url: uri.toString());
    }
    return false;
  }

  Future<bool> open() async {
    return await FlutterMyNative.openNativeLink(url: uri.toString());
  }
}

class FlutterMyNative {
  static const MethodChannel _channel =
      const MethodChannel('cn.cnganen.flutter_my_native');

  static Future<String> goToHome() async {
    assert(Platform.isAndroid, "回到桌面只支持android");
    final String version = await _channel.invokeMethod('goToHome');
    return version;
  }

  static Future<bool> hasOpenNativeLinkIos({@required String url}) async {
    try {
      final ret = await _channel.invokeMethod("hasOpen", {"uri": url});
      if (ret == "success") {
        return true;
      }
      return false;
    } catch(e) {
      return false;
    }
  }

  static Future<bool> hasOpenNativeLinkAndroid({@required String pkgname}) async {
    try {
      final ret = await _channel.invokeMethod("hasOpen", {"pkgname": pkgname});
      if (ret == "success") {
        return true;
      }
      return false;
    } catch(e) {
      return false;
    }
  }

  static Future<bool> openNativeLink({@required url}) async {
    try {
      final ret = await _channel.invokeMethod("openlink", {"uri": url});
      if (ret == "success") {
        return true;
      }
      return false;
    } catch(e) {
      return false;
    }
  }
}
