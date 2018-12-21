package naneos.analyze;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SplashScreenActivity extends AppCompatActivity {

    Context thisContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_splash_screen);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NaneosSplashScreen", "onResume invoked!");
        thisContext = getApplicationContext();;
        permissionsForAndroidM();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("NaneosOnRequestPermiss", "PermissionResult received!");

        Intent intent = new Intent(getApplicationContext(),
                MainActivity.class);
        startActivity(intent);
        finish();

    }

    private void permissionsForAndroidM(){
        if (Build.VERSION.SDK_INT > 22) {
            String[] allPermissionNeeded = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.RECORD_AUDIO};

            List<String> permissionNeeded = new ArrayList<>();
            for (String permission : allPermissionNeeded)
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                    permissionNeeded.add(permission);
            if (permissionNeeded.size() > 0) {
                requestPermissions(permissionNeeded.toArray(new String[0]), 0);
            }
        }
    }
}
