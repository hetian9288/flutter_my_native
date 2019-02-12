// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility that Flutter provides. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_my_native_example/main.dart';

void main() {
  test('Verify Platform version', () async {
    final a = Uri.parse("pinduoduo://com.xunmeng.pinduoduo/goods.html?goods_id=6051&direct_switch_from_wechat=1&_p_utm=other_");
    print(a.host);
  });
}
