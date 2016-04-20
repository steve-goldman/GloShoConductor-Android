package constantbeta.com.gloshoconductor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;

public class CameraPermissions
{
    private static final String TAG = "dialog";
    private static final String[] PERMISSIONS = new String[] { Manifest.permission.CAMERA };
    public static final int REQUEST_CODE = 1;

    private final Fragment fragment;

    // package scope
    CameraPermissions(Fragment fragment)
    {
        this.fragment = fragment;
    }

    // package scope
    boolean has()
    {
        return ContextCompat.checkSelfPermission(fragment.getActivity(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    // package scope
    void request()
    {
        if (FragmentCompat.shouldShowRequestPermissionRationale(fragment, Manifest.permission.CAMERA))
        {
            new ConfirmationDialog().show(fragment.getChildFragmentManager(), TAG);
        }
        else
        {
            FragmentCompat.requestPermissions(fragment, PERMISSIONS, REQUEST_CODE);
        }
    }

    void onRequestPermissionsResult(@NonNull int[] grantResults)
    {
        if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
        {
            ErrorDialog.newInstance("Camera permission required")
                           .show(fragment.getChildFragmentManager(), TAG);
        }
    }

    public static class ConfirmationDialog extends DialogFragment
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("Camera permission required")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which)
                        {
                            FragmentCompat.requestPermissions(parent, PERMISSIONS, REQUEST_CODE);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which)
                        {
                            final Activity activity = parent.getActivity();
                            if (activity != null)
                            {
                                activity.finish();
                            }
                        }
                    })
                    .create();
        }
    }

    public static class ErrorDialog extends DialogFragment
    {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                activity.finish();
                            }
                        })
                    .create();
        }
    }
}
