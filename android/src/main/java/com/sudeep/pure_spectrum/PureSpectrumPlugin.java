package com.sudeep.pure_spectrum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

// -------- Fusion SDK imports (as per v1.0.19) --------
import com.purespectrum.fusionsdkandroid.FusionError;
import com.purespectrum.fusionsdkandroid.FusionResult;
import com.purespectrum.fusionsdkandroid.FusionSdk;
import com.purespectrum.fusionsdkandroid.ui.FusionCardConfiguration;

public class PureSpectrumPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

  private MethodChannel channel;
  private Activity activity;

  // ---------------- FlutterPlugin ----------------

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), "pure_spectrum");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (channel != null) channel.setMethodCallHandler(null);
    channel = null;
  }

  // ---------------- MethodChannel ----------------

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {

      case "showSurveyCards": {
        if (activity == null) {
          result.error("NO_ACTIVITY", "Plugin not attached to Activity", null);
          return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) call.arguments;

        String accessToken = (String) args.get("accessToken");
        String respondentId = (String) args.get("respondentId");
        String locale = (String) args.get("locale");
        String psid = (String) args.get("psid");             // optional
        String projectId = (String) args.get("projectId");   // optional
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) args.get("headers");

        if (headers == null) headers = new HashMap<>();

        Intent intent = new Intent(activity, FusionCardsActivity.class);
        intent.putExtra("accessToken", accessToken);
        intent.putExtra("respondentId", respondentId);
        intent.putExtra("locale", (locale != null ? locale : "en_US"));
        intent.putExtra("psid", (psid != null ? psid : ""));
        intent.putExtra("projectId", (projectId != null ? projectId : ""));
        // pack headers
        for (Map.Entry<String, String> e : headers.entrySet()) {
          intent.putExtra("hdr__" + e.getKey(), e.getValue());
        }

        activity.startActivity(intent);
        result.success(null);
        break;
      }

      case "shutdown": {
        // Activity onDestroy already calls shutdown, but safe to expose.
        FusionSdk.INSTANCE.shutdown();
        result.success(null);
        break;
      }

      default:
        result.notImplemented();
    }
  }

  // ---------------- ActivityAware ----------------

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.activity = null;
  }

  @Override
  public void onDetachedFromActivity() {
    this.activity = null;
  }

  // =========================================================
  // ===============  Inner Activity (Java-only)  =============
  // =========================================================
  public static class FusionCardsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      FrameLayout container = new FrameLayout(FusionCardsActivity.this);
      container.setId(android.R.id.content);
      setContentView(container);

      String accessToken = getIntent().getStringExtra("accessToken");
      String respondentId = getIntent().getStringExtra("respondentId");
      String locale = getIntent().getStringExtra("locale");
      String psid = getIntent().getStringExtra("psid");
      String projectId = getIntent().getStringExtra("projectId");

      // rebuild headers from Intent extras
      Map<String, String> headers = new HashMap<>();
      Bundle extras = getIntent().getExtras();
      if (extras != null) {
        for (String key : extras.keySet()) {
          if (key != null && key.startsWith("hdr__")) {
            String hk = key.substring("hdr__".length());
            String hv = extras.getString(key);
            if (hv != null) headers.put(hk, hv);
          }
        }
      }

      // Minimal config (customize as needed)
      FusionCardConfiguration config = new FusionCardConfiguration.Builder()
              .accentColor(Color.parseColor("#00AEEF"))
              .textColor(Color.BLACK)
              .starColor(Color.YELLOW)
              .inactiveStarColor(Color.LTGRAY)
              .cardBackgroundColor(Color.WHITE)
              .cardCornerRadiusDp(12f)
              .cardElevationDp(4f)
              .build();

      // Match current SDK signature:
      // (Context, ViewGroup, FusionCardConfiguration, String accessToken, String respondentId,
      //  String locale, Map<String,String> headers, String psid, String projectId,
      //  Function1<FusionError, Unit> onError, Function1<FusionResult, Unit> onSuccess)
      FusionSdk.INSTANCE.showSurveyCards(
              /* context   */ (Context) FusionCardsActivity.this,
              /* target    */ container,
              /* config    */ config,
              /* token     */ accessToken,
              /* userId    */ respondentId,
              /* locale    */ (locale != null ? locale : "en_US"),
              /* headers   */ headers,
              /* psid      */ (psid != null ? psid : ""),
              /* projectId */ (projectId != null ? projectId : ""),
              /* onError   */ new Function1<FusionError, Unit>() {
                @Override public Unit invoke(FusionError error) {
                  Log.e("FusionCardsActivity", "Fusion error: " + error);
                  return Unit.INSTANCE;
                }
              },
              /* onSuccess */ new Function1<FusionResult, Unit>() {
                @Override public Unit invoke(FusionResult result) {
                  // Optionally inspect result (survey count, etc.) if exposed by SDK
                  Log.d("FusionCardsActivity", "Fusion success: " + result);
                  return Unit.INSTANCE;
                }
              }
      );
    }

    @Override
    protected void onDestroy() {
      super.onDestroy();
      FusionSdk.INSTANCE.shutdown();
    }
  }
}
