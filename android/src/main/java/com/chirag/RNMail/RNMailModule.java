package com.chirag.RNMail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.URLUtil;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * NativeModule that allows JS to open emails sending apps chooser.
 */
public class RNMailModule extends ReactContextBaseJavaModule {

  ReactApplicationContext reactContext;

  public RNMailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNMail";
  }

  /**
   * Converts a ReadableArray to a String array
   *
   * @param r the ReadableArray instance to convert
   * @return array of strings
   */
  private String[] readableArrayToStringArray(ReadableArray r) {
    int length = r.size();
    String[] strArray = new String[length];

    for (int keyIndex = 0; keyIndex < length; keyIndex++) {
      strArray[keyIndex] = r.getString(keyIndex);
    }

    return strArray;
  }

  @ReactMethod
  public void mail(ReadableMap options, Callback callback) {
    ArrayList<Uri> fileAttachmentUriList = getFileAttachmentUriList(options);
    Intent intent;
    if (fileAttachmentUriList.size() > 0) {
      intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
      intent.setType("plain/text");
      intent.putExtra(Intent.EXTRA_STREAM, fileAttachmentUriList);
    } else if (options.hasKey("attachment") && !options.isNull("attachment")) {
      intent = new Intent(Intent.ACTION_SEND);
      intent.setType("vnd.android.cursor.dir/email");
    } else {
      intent = new Intent(Intent.ACTION_SENDTO);
      intent.setData(Uri.parse("mailto:"));
    }

    if (options.hasKey("subject") && !options.isNull("subject")) {
      intent.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
    }

    if (options.hasKey("body") && !options.isNull("body")) {
      intent.putExtra(Intent.EXTRA_TEXT, options.getString("body"));
    }

    if (options.hasKey("recipients") && !options.isNull("recipients")) {
      ReadableArray recipients = options.getArray("recipients");
      intent.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
    }

    if (options.hasKey("ccRecipients") && !options.isNull("ccRecipients")) {
      ReadableArray ccRecipients = options.getArray("ccRecipients");
      intent.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
    }

    if (options.hasKey("bccRecipients") && !options.isNull("bccRecipients")) {
      ReadableArray bccRecipients = options.getArray("bccRecipients");
      intent.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
    }

    if (options.hasKey("attachment") && !options.isNull("attachment")) {
      ReadableMap attachment = options.getMap("attachment");
      if (attachment.hasKey("path") && !attachment.isNull("path")) {
        String path = attachment.getString("path");
        Uri p;
        // Check for valid URI
        if (URLUtil.isValidUrl(path)) {
          p = Uri.parse(path);
        }
        // Else this is an absolute file path
        else {
          File file = new File(path);
          p = Uri.fromFile(file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, p);
      }
    }

    PackageManager manager = reactContext.getPackageManager();
    List<ResolveInfo> list = manager.queryIntentActivities(intent, 0);

    if (list == null || list.size() == 0) {
      callback.invoke("not_available");
      return;
    }

    if (list.size() == 1) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        reactContext.startActivity(intent);
      } catch (Exception ex) {
        callback.invoke("error");
      }
    } else {
      String chooserLabel;
      if (options.hasKey("chooserLabel") && !options.isNull("chooserLabel")) {
        chooserLabel = options.getString("chooserLabel");
      } else {
        chooserLabel = "Send Mail";
      }
      Intent chooser = Intent.createChooser(intent, chooserLabel);
      chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      try {
        reactContext.startActivity(chooser);
      } catch (Exception ex) {
        callback.invoke("error");
      }
    }
  }

  private ArrayList<Uri> getFileAttachmentUriList(ReadableMap options) {
    ArrayList<Uri> fileAttachmentUriList = new ArrayList<Uri>();
    if (hasKeyAndDefined(options, "attachmentList")) {
      ReadableArray attachmentList = options.getArray("attachmentList");
      int length = attachmentList.size();

      for (int i = 0; i < length; ++i) {
        ReadableMap attachmentItem = attachmentList.getMap(i);
        if (hasKeyAndDefined(attachmentItem, "path")) {
          String path = attachmentItem.getString("path");
          Uri p;
          // Check for valid URI
          if (URLUtil.isValidUrl(path)) {
            p = Uri.parse(path);
            fileAttachmentUriList.add(p);
          }
          // Else this is an absolute file path
          else {
            File file = new File(path);
            boolean fileExists = file.exists();
            if (fileExists) {
              p = Uri.fromFile(file);
              fileAttachmentUriList.add(p);
            }
          }
        }
      }
    }
    return fileAttachmentUriList;
  }

  private Boolean hasKeyAndDefined(ReadableMap readableMap, String key) {
    return readableMap.hasKey(key) && !readableMap.isNull(key);
  }
}