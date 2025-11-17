import 'package:flutter/services.dart';

class PureSpectrum {
  static const channel = MethodChannel('pure_spectrum');

  static Future<void> showSurvey({
    required String token,
    required String userId,
    String locale = 'en_US',
    bool verticalAllowed = true,
    Map<String, String>? headers,
    String? hashedId,
    String? respondentId,
  }) {
    return channel.invokeMethod('showSurveyCards', {
      'accessToken': token,
      'memberId': userId,
      'respondentId': respondentId,
      'hashedId': hashedId,
      'locale': locale,
      'headers': headers ?? {},
      'verticalAllowed': verticalAllowed,
    });
  }
}
