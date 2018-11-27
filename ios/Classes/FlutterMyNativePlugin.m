#import "FlutterMyNativePlugin.h"

@implementation FlutterMyNativePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"cn.cnganen.flutter_my_native"
            binaryMessenger:[registrar messenger]];
  FlutterMyNativePlugin* instance = [[FlutterMyNativePlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"goToHome" isEqualToString:call.method]) {
    [self goToHome:call result:result];
  } else {
    result(FlutterMethodNotImplemented);
  }
}

- (void) goToHome:(FlutterMethodCall*)call result:(FlutterResult)result
{
    result([@"success"]]);
}

@end
