package cn.cnganen.fluttermynative;

import android.content.Context;
import android.content.pm.PackageManager;

public class Utils {

    // 检查是否安装应用
    public static boolean checkHasInstalledApp(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(pkgName, PackageManager.GET_GIDS);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        } catch (RuntimeException e) {
            app_installed = false;
        }
        return app_installed;
    }

}
