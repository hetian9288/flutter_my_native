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
  } if ([@"hasOpen" isEqualToString:call.method]) {
      [self hasOpen:call result:result];
  } if ([@"openNativeLink" isEqualToString:call.method]) {
      [self openNativeLink:call result:result];
  } else {
    result(FlutterMethodNotImplemented);
  }
}

// IOS 端不需要该功能
- (void) goToHome:(FlutterMethodCall*)call result:(FlutterResult)result
{
    result(@"success");
}

- (void) hasOpen:(FlutterMethodCall*)call result:(FlutterResult)result
{
    NSString *url = call.arguments[@"uri"];
    NSURL *uri = [NSURL URLWithString:url];
    if ([[UIApplication sharedApplication]
         canOpenURL:uri]) {
        result(@"success");
    }else {
        result([FlutterError errorWithCode:@"110" message:@"not install" details:@""]);
    }
}

- (void) openNativeLink:(FlutterMethodCall*)call result:(FlutterResult)result
{
    NSString *url = call.arguments[@"uri"];
    NSURL *uri = [NSURL URLWithString:url];
    if ([[UIApplication sharedApplication]
         canOpenURL:uri]) {
        [[UIApplication sharedApplication] openURL:uri];
        result(@"success");
    }else {
        result([FlutterError errorWithCode:@"110" message:@"not install" details:@""]);
    }
    
}

@end
