import 'package:flutter/services.dart';

class PureSpectrum {
  static const channel = MethodChannel('pure_spectrum');

  static Future<void> showSurvey({
    required String token,
    required String userId,
  }) => channel.invokeMethod('showSurveyCards', {
    "accessToken": token,
    "respondentId": userId,
    "locale": "en_US",
  });
}
