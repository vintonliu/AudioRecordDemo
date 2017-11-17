/**
 * Copyright (c) 2017 The UcPaas project authors. All Rights Reserved.
 */
package cn.freedom.audiorecorddemo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vinton
 * @date 2017-01-06
 * Utility to request and check System permissions for apps targeting Android M (API >= 23).
 */
public class EasyPermissions {
	
	private static final String TAG = "EasyPermissions";
	
	public static final int RC_CAMERA_PERM = 123;
	public static final int RC_RECORD_AUDIO = 124;
	public static final int RC_WRITE_EXTERNAL_STORAGE = 125;
    
    public interface PermissionCallbacks extends
	    	ActivityCompat.OnRequestPermissionsResultCallback {
	
		void onPermissionsGranted(int requestCode, List<String> perms);
		
		void onPermissionsDenied(int requestCode, List<String> perms);
	
	}
    
	/**
	 * 
	 */
	public EasyPermissions() {
		// TODO Auto-generated constructor stub
	}

	/**
     * Check if the calling context has a set of permissions.
     *
     * @param context
     *         the calling context.
     * @param perms
     *         one ore more permissions, such as {@code android.Manifest.permission.CAMERA}.
     * @return true if all permissions are already granted, false if at least one permission is not yet granted.
     */
    public static boolean hasPermissions(@NonNull Context context, @NonNull String... perms) {
        // Always return true for SDK < M, let the system deal with the permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "hasPermissions: API version < M, returning true by default");
            return true;
        }

        for (String perm : perms) {
            boolean hasPerm = (ContextCompat.checkSelfPermission(context, perm) ==
                    PackageManager.PERMISSION_GRANTED);
            if (!hasPerm) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Request a set of permissions, showing rationale if the system requests it.
     *
     * @param object
     *         Activity or Fragment requesting permissions. Should implement
     *         {@link android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback}
     *         or {@code android.support.v13.app.FragmentCompat.OnRequestPermissionsResultCallback}
     * @param rationale
     *         a message explaining why the application needs this set of permissions, will be displayed if the user rejects the request the first
     *         time.
     * @param requestCode
     *         request code to track this request, must be < 256.
     * @param perms
     *         a set of permissions to be requested.
     */
    public static void requestPermissions(@NonNull final Object object,
                                          final int requestCode, 
                                          @NonNull final String... perms) {
        
        ActivityCompat.requestPermissions((Activity) object, perms, requestCode);
    }
    
    /**
     * Handle the result of a permission request, should be called from the calling Activity's {@link android.support.v4.app.ActivityCompat
     * .OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])} method.
     * <p>
     * If any permissions were granted or denied, the {@code object} will receive the appropriate callbacks through {@link PermissionCallbacks} and
     * methods annotated with {@link AfterPermissionGranted} will be run if appropriate.
     *
     * @param requestCode
     *         requestCode argument to permission result callback.
     * @param permissions
     *         permissions argument to permission result callback.
     * @param grantResults
     *         grantResults argument to permission result callback.
     * @param receivers
     *         an array of objects that have a method annotated with {@link AfterPermissionGranted} or implement {@link PermissionCallbacks}.
     */
    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                  @NonNull int[] grantResults,
                                                  @NonNull Object... receivers) {

        // Make a collection of granted and denied permissions from the request.
        ArrayList<String> granted = new ArrayList<String>();
        ArrayList<String> denied = new ArrayList<String>();
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm);
            } else {
                denied.add(perm);
            }
        }

        // iterate through all receivers
        for (Object object : receivers) {
            // Report granted permissions, if any.
            if (!granted.isEmpty()) {
                if (object instanceof PermissionCallbacks) {
                    ((PermissionCallbacks) object).onPermissionsGranted(requestCode, granted);
                }
            }

            // Report denied permissions, if any.
            if (!denied.isEmpty()) {
                if (object instanceof PermissionCallbacks) {
                    ((PermissionCallbacks) object).onPermissionsDenied(requestCode, denied);
                }
            }
        }
    }
}
