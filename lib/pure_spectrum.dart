import 'package:flutter/services.dart';

class PureSpectrum {
  static const channel = MethodChannel('pure_spectrum');

  static Future<void> showSurvey({
    required String token,
    required String userId,
    Map<String, String>? headers,
    bool verticalAllowed = true,
    String? hashedId,
    String? respondentId, // optional, auto-generated on Android if null
  }) {
    return channel.invokeMethod('showSurveyCards', {
      "accessToken": token,
      "memberId": userId, // userId will be passed as memberId
      "respondentId":
          respondentId, // if null → Java side will auto-generate unique
      "hashedId": hashedId,
      "locale": "en_US",
      "headers": headers ?? {},
      "verticalAllowed": verticalAllowed,
    });
  }
}
