#import <Flutter/Flutter.h>

@interface FlutterMyNativePlugin : NSObject<FlutterPlugin>
+ (instancetype) sharedInstance;
- (void)onPush:( NSDictionary * __nullable)userInfo;
@end
