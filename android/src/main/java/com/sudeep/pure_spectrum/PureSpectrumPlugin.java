package com.sudeep.pure_spectrum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

// Fusion SDK imports
import com.purespectrum.fusionsdkandroid.FusionError;
import com.purespectrum.fusionsdkandroid.FusionResult;
import com.purespectrum.fusionsdkandroid.FusionSdk;
import com.purespectrum.fusionsdkandroid.ui.FusionCardConfiguration;

public class PureSpectrumPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Activity activity;

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
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) args.get("headers");
                String memberId = (String) args.get("memberId");     // user id to pass as memberId
                String hashedId = (String) args.get("hashedId");     // optional hashed id
                Boolean verticalAllowed = (Boolean) args.get("verticalAllowed");

                if (headers == null) headers = new HashMap<>();

                // if respondentId not provided by the caller, generate a unique one (UUID or order-like)
                if (respondentId == null || respondentId.isEmpty()) {
                    // example order-like id: "order_<timestamp>_<random>"
                    respondentId = "order_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
                }

                // Create Intent and put extras (we prefix custom extras with "hdr__" for headers, and use keys for memberId/hashedId)
                Intent intent = new Intent(activity, FusionCardsActivity.class);
                intent.putExtra("accessToken", accessToken);
                intent.putExtra("respondentId", respondentId);
                intent.putExtra("locale", (locale != null ? locale : "en_US"));
                intent.putExtra("memberId", memberId);
                intent.putExtra("hashedId", hashedId);
                if (verticalAllowed != null) intent.putExtra("verticalAllowed", verticalAllowed);

                for (Map.Entry<String, String> e : headers.entrySet()) {
                    intent.putExtra("hdr__" + e.getKey(), e.getValue());
                }

                activity.startActivity(intent);
                result.success(null);
                break;
            }

            case "shutdown": {
                FusionSdk.INSTANCE.shutdown();
                result.success(null);
                break;
            }

            default:
                result.notImplemented();
        }
    }

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

            // optional extras
            String memberId = getIntent().getStringExtra("memberId");
            String hashedId = getIntent().getStringExtra("hashedId");
            boolean verticalAllowed = getIntent().getBooleanExtra("verticalAllowed", true);

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

            // Call the SDK with the extended parameter list. Matches the Kotlin signature shown below.
            FusionSdk.INSTANCE.showSurveyCards(
                    /* context        */ (Context) FusionCardsActivity.this,
                    /* targetView     */ (ViewGroup) container,
                    /* config         */ config,
                    /* accessToken    */ accessToken,
                    /* respondentId   */ respondentId,
                    /* locale         */ (locale != null ? locale : "en_US"),
                    /* profileData    */ headers,
                    /* verticalAllowed*/ verticalAllowed,
                    /* memberId       */ (memberId != null ? memberId : null),
                    /* hashedId       */ (hashedId != null ? hashedId : null),
                    /* onError        */ new Function1<FusionError, Unit>() {
                        @Override public Unit invoke(FusionError error) {
                            Log.e("FusionCardsActivity", "Fusion error: " + error);
                            return Unit.INSTANCE;
                        }
                    },
                    /* onResult       */ new Function1<FusionResult, Unit>() {
                        @Override public Unit invoke(FusionResult result) {
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
