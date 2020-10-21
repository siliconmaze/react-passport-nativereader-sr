/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tradle.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
//import org.jmrtd.lds.COMFile;
//import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.CardSecurityFile;
import org.jmrtd.lds.SecurityInfo;
//import org.jmrtd.lds.DG1File;
//import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
//LDS	 = The ICAO logical data structure version 1.7 from MMRTD 5.5
//import org.jmrtd.lds.LDS;
//import org.jmrtd.lds.MRZInfo;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SODFile;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE;
import static org.jmrtd.PassportService.NORMAL_MAX_TRANCEIVE_LENGTH;

public class RNPassportReaderModule extends ReactContextBaseJavaModule implements LifecycleEventListener, ActivityEventListener {

  private static final int SCAN_REQUEST_CODE = 8735738;
  private static final String E_NOT_SUPPORTED = "E_NOT_SUPPORTED";
  private static final String E_NOT_ENABLED = "E_NOT_ENABLED";
  private static final String E_SCAN_CANCELED = "E_SCAN_CANCELED";
  private static final String E_SCAN_FAILED = "E_SCAN_FAILED";
  private static final String E_MRZINFO_INVALID = "E_MRZINFO_INVALID";
  private static final String E_SCAN_FAILED_DISCONNECT = "E_SCAN_FAILED_DISCONNECT";
  private static final String E_ONE_REQ_AT_A_TIME = "E_ONE_REQ_AT_A_TIME";
//  private static final String E_MISSING_REQUIRED_PARAM = "E_MISSING_REQUIRED_PARAM";
  private static final String KEY_IS_SUPPORTED = "isSupported";
  private static final String KEY_FIRST_NAME = "firstName";
  private static final String KEY_LAST_NAME = "lastName";
  private static final String KEY_GENDER = "gender";
  private static final String KEY_ISSUER = "issuer";
  private static final String KEY_NATIONALITY = "nationality";
  private static final String KEY_PHOTO = "photo";
  private static final String PARAM_DOC_NUM = "documentNumber";
  private static final String PARAM_DOB = "dateOfBirth";
  private static final String PARAM_DOE = "dateOfExpiry";
  private static final String TAG = "passportreader";
  private static final String JPEG_DATA_URI_PREFIX = "data:image/jpeg;base64,";

  

  private final ReactApplicationContext reactContext;
  private Promise scanPromise;
  private ReadableMap opts;
  public RNPassportReaderModule(ReactApplicationContext reactContext) {
    super(reactContext);
    Security.insertProviderAt(new BouncyCastleProvider(), 1);

    reactContext.addLifecycleEventListener(this);
    reactContext.addActivityEventListener(this);

    this.reactContext = reactContext;
  }

  private boolean isOptsValid(ReadableMap opts) {

    Log.i(TAG,"PARAM_DOC_NUM="+opts.getString(PARAM_DOC_NUM));
    Log.i(TAG,"PARAM_DOB="+opts.getString(PARAM_DOB));
    Log.i(TAG,"PARAM_DOE="+opts.getString(PARAM_DOE));

    return opts.getString(PARAM_DOC_NUM) != null && opts.getString(PARAM_DOC_NUM).length() >= 8 &&
    opts.getString(PARAM_DOB) != null && opts.getString(PARAM_DOB).length() == 6 &&
    opts.getString(PARAM_DOE) != null && opts.getString(PARAM_DOE).length() == 6;
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
  }


  @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {
                clearViews();
                if (passportNumber != null && !passportNumber.isEmpty()
                        && expirationDate != null && !expirationDate.isEmpty()
                        && birthDate != null && !birthDate.isEmpty()) {
                    BACKeySpec bacKey = new BACKey(passportNumber, birthDate, expirationDate);
                    new ReadTask(IsoDep.get(tag), bacKey).execute();
                    mainLayout.setVisibility(View.GONE);
                    imageLayout.setVisibility(View.GONE);
                    loadingLayout.setVisibility(View.VISIBLE);
                } else {
                    Snackbar.make(loadingLayout, R.string.error_input, Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

  @Override
  public void onNewIntent(Intent intent) {
    Log.i(TAG,"ENTER: onNewIntent");
    if (scanPromise == null) return;
    if (!NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) return;

    Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
    Log.i(TAG,"tag="+tag);
    Log.i(TAG,"IsoDep.class.getName()"+IsoDep.class.getName());
    //if (!Arrays.asList(tag.getTechList()).contains(IsoDep.class.getName())) return;
    if (!Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) return;

    BACKeySpec bacKey = new BACKey(
            opts.getString(PARAM_DOC_NUM),
            opts.getString(PARAM_DOB),
            opts.getString(PARAM_DOE)
    );

    Log.i(TAG,"new ReadTask()");
    new ReadTask(IsoDep.get(tag), bacKey).execute();
  }

  @Override
  public String getName() {
    return "RNPassportReader";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    boolean hasNFC = reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    constants.put(KEY_IS_SUPPORTED, hasNFC);
    return constants;
  }

  @ReactMethod
  public void cancel(final Promise promise) {
    if (scanPromise != null) {
      scanPromise.reject(E_SCAN_CANCELED, "canceled");
    }

    resetState();
    promise.resolve(null);
  }

  @ReactMethod
  public void scan(final ReadableMap opts, final Promise promise) {
    Log.i(TAG,"scan()");
    NfcAdapter mNfcAdapter = null;
    try {
      mNfcAdapter = NfcAdapter.getDefaultAdapter(this.reactContext);
    } catch (Exception e) {
      Log.i(TAG,"mNfcAdapter");
      Log.w(TAG, e);
    }
   
    if (mNfcAdapter == null) {
      Log.i(TAG,"NFC chip reading not supported");
      promise.reject(E_NOT_SUPPORTED, "NFC chip reading not supported!");
      return;
    }

    if (!mNfcAdapter.isEnabled()) {
      Log.i(TAG,"NFC chip reading not enabled");
      promise.reject(E_NOT_ENABLED, "NFC chip reading not enabled!");
      return;
    }

    if (scanPromise != null) {
      Log.i(TAG,"Already running a scan");
      promise.reject(E_ONE_REQ_AT_A_TIME, "Already running a scan!");
      return;
    }

    if (!isOptsValid(opts)) {
      Log.i(TAG,"opts are invalid!");
      promise.reject(E_MRZINFO_INVALID, "MRZ Information is invalid!");
      return;
    }

    this.opts = opts;
    this.scanPromise = promise;
  }

  private void resetState() {
    scanPromise = null;
    opts = null;
  }

  @Override
  public void onHostDestroy() {
    resetState();
  }

  @Override
  public void onHostResume() {
    NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this.reactContext);
    if (mNfcAdapter == null) return;

    Activity activity = getCurrentActivity();
    Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(getCurrentActivity(), 0, intent, 0);//PendingIntent.FLAG_UPDATE_CURRENT);
    String[][] filter = new String[][] { new String[] { IsoDep.class.getName()  } };
    mNfcAdapter.enableForegroundDispatch(getCurrentActivity(), pendingIntent, null, filter);
  }

  @Override
  public void onHostPause() {
    NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this.reactContext);
    if (mNfcAdapter == null) return;

    mNfcAdapter.disableForegroundDispatch(getCurrentActivity());
  }

  private static String exceptionStack(Throwable exception) {
    StringBuilder s = new StringBuilder();
    String exceptionMsg = exception.getMessage();
    if (exceptionMsg != null) {
      s.append(exceptionMsg);
      s.append(" - ");
    }
    s.append(exception.getClass().getSimpleName());
    StackTraceElement[] stack = exception.getStackTrace();

    if (stack.length > 0) {
      int count = 3;
      boolean first = true;
      boolean skip = false;
      String file = "";
      s.append(" (");
      for (StackTraceElement element : stack) {
        if (count > 0 && element.getClassName().startsWith("io.tradle")) {
          if (!first) {
            s.append(" < ");
          } else {
            first = false;
          }

          if (skip) {
            s.append("... < ");
            skip = false;
          }

          if (file.equals(element.getFileName())) {
            s.append("*");
          } else {
            file = element.getFileName();
            s.append(file.substring(0, file.length() - 5)); // remove ".java"
            count -= 1;
          }
          s.append(":").append(element.getLineNumber());
        } else {
          skip = true;
        }
      }
      if (skip) {
        if (!first) {
          s.append(" < ");
        }
        s.append("...");
      }
      s.append(")");
    }
    return s.toString();
  }

  private static String toBase64(final Bitmap bitmap, final int quality) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
    byte[] byteArray = byteArrayOutputStream.toByteArray();
    return JPEG_DATA_URI_PREFIX + Base64.encodeToString(byteArray, Base64.NO_WRAP);
  }

  private class ReadTask extends AsyncTask<Void, Void, Exception> {

    private IsoDep isoDep;
    private BACKeySpec bacKey;

    public ReadTask(IsoDep isoDep, BACKeySpec bacKey) {
      this.isoDep = isoDep;
      this.bacKey = bacKey;
    }

    //private COMFile comFile;
    //private SODFile sodFile;
    private DG1File dg1File;
    private DG2File dg2File;

    private Bitmap bitmap;

    @Override
    protected Exception doInBackground(Void... params) {
      Log.i(TAG,"ENTER: doInBackground()");
      try {
        Log.w(TAG,"try doInBackground:");
        CardService cardService = CardService.getInstance(isoDep);
        cardService.open();

        
        //PassportService service = new PassportService(cardService);
        //PassportService service = new PassportService(cardService, org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE);

        PassportService service = new PassportService(cardService, NORMAL_MAX_TRANCEIVE_LENGTH, DEFAULT_MAX_BLOCKSIZE, true, false);
        service.open();
        boolean paceSucceeded = false;


        /*
        try {
          Log.i(TAG,"try cardAccessFile:");
          
          CardAccessFile cardAccessFile = new CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS));
          Collection<PACEInfo> paceInfos = cardAccessFile.getPACEInfos();
          if (paceInfos != null && paceInfos.size() > 0) {
            PACEInfo paceInfo = paceInfos.iterator().next();
            try {
              service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()));
              paceSucceeded = true;
              Log.i(TAG,"paceSucceeded = true");
            } catch (Exception e) {
                Log.i(TAG,"catch service.doPACE(:");
                Log.w(TAG, e);
            }
          } else {
            paceSucceeded = false; //??
            Log.i(TAG,"paceSucceeded = false");
          }
        } catch (Exception e) {
          Log.i(TAG,"catch cardAccessFile:");
          Log.w(TAG, e);
        }*/


        

        try {
          CardSecurityFile cardSecurityFile = new CardSecurityFile(service.getInputStream(PassportService.EF_CARD_SECURITY));
          Collection<SecurityInfo> securityInfoCollection = cardSecurityFile.getSecurityInfos();
          for (SecurityInfo securityInfo : securityInfoCollection) {
              if (securityInfo instanceof PACEInfo) {
                  PACEInfo paceInfo = (PACEInfo) securityInfo;
                  service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), null);
                  paceSucceeded = true;
              } else {
                  paceSucceeded = true;
              }
          }
        } catch (Exception e) {
          Log.i(TAG,"catch cardSecurityFile:");
          Log.w(TAG, e);
        }


        Log.i(TAG,"sendSelectApplet()");
        service.sendSelectApplet(paceSucceeded);
      

        if (!paceSucceeded) {
          try {
            Log.i(TAG,"getInputStream()");
            service.getInputStream(PassportService.EF_COM).read();
          } catch (Exception e) {
            service.doBAC(bacKey);
          }
        }

        //LDS lds = new LDS();

//        CardFileInputStream comIn = service.getInputStream(PassportService.EF_COM);
//        lds.add(PassportService.EF_COM, comIn, comIn.getLength());
//        comFile = lds.getCOMFile();
//
//        CardFileInputStream sodIn = service.getInputStream(PassportService.EF_SOD);
//        lds.add(PassportService.EF_SOD, sodIn, sodIn.getLength());
//        sodFile = lds.getSODFile();


        /*
        CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
        lds.add(PassportService.EF_DG1, dg1In, dg1In.getLength());
        dg1File = lds.getDG1File();
        */
        CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
        dg1File = new DG1File(dg1In);
        Log.i(TAG,"dg1File");

        /*
        CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
        lds.add(PassportService.EF_DG2, dg2In, dg2In.getLength());
        dg2File = lds.getDG2File();
        */
        CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
        dg2File = new DG2File(dg2In);
        Log.i(TAG,"dg2File");

        List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
        List<FaceInfo> faceInfos = dg2File.getFaceInfos();
        for (FaceInfo faceInfo : faceInfos) {
          allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
        }

        if (!allFaceImageInfos.isEmpty()) {
          FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();

          int imageLength = faceImageInfo.getImageLength();
          DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
          byte[] buffer = new byte[imageLength];
          dataInputStream.readFully(buffer, 0, imageLength);
          InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);

          bitmap = ImageUtil.decodeImage(reactContext, faceImageInfo.getMimeType(), inputStream);
          //imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT);
        }


      } catch (Exception e) {
        Log.i(TAG,"catch doInBackground:");
        Log.w(TAG,"error:" + e);
        return e;
      }
      return null; //returns null to doInBackground
    }

    @Override
    protected void onPostExecute(Exception result) {
      Log.i(TAG,"ENTER: onPostExecute()");

      if (scanPromise == null) return;

      if (result != null) {
        Log.i(TAG,"result != null = bad for MRZInfo");
        Log.w(TAG, exceptionStack(result));
        if (result instanceof IOException) {
          Log.i(TAG,"IOException: Lost connection to chip on card");
          Log.w(TAG,"scanPromise.reject = E_SCAN_FAILED_DISCONNECT");
          scanPromise.reject(E_SCAN_FAILED_DISCONNECT, TAG+"::"+"Lost connection to chip on card");
        } else {
          Log.w(TAG,"scanPromise.reject = E_SCAN_FAILED");
          scanPromise.reject(E_SCAN_FAILED, TAG+"::"+result);
        }

        Log.w(TAG,"resetState()");
        resetState();
        Log.w(TAG,"return()");
        return;
      }

      Log.i(TAG,"result == null = good for MRZInfo");
      Log.w(TAG,"dg1File.getMRZInfo()");
      MRZInfo mrzInfo = dg1File.getMRZInfo();

      int quality = 100;
      if (opts.hasKey("quality")) {
        quality = (int)(opts.getDouble("quality") * 100);
      }

      String base64 = toBase64(bitmap, quality);
      WritableMap photo = Arguments.createMap();
      photo.putString("base64", base64);
      photo.putInt("width", bitmap.getWidth());
      photo.putInt("height", bitmap.getHeight());

      String firstName = mrzInfo.getSecondaryIdentifier().replace("<", "");
      String lastName = mrzInfo.getPrimaryIdentifier().replace("<", "");
      WritableMap passport = Arguments.createMap();
      passport.putMap(KEY_PHOTO, photo);
      passport.putString(KEY_FIRST_NAME, firstName);
      passport.putString(KEY_LAST_NAME, lastName);
      passport.putString(KEY_NATIONALITY, mrzInfo.getNationality());
      passport.putString(KEY_GENDER, mrzInfo.getGender().toString());
      passport.putString(KEY_ISSUER, mrzInfo.getIssuingState());

      Log.w(TAG,"scanPromise.resolve");
      scanPromise.resolve(passport);
      Log.w(TAG,"resetState()");
      resetState();
     
    }
  }
}
