import 'dart:async';

import 'package:flutter/services.dart';

class KomodoXmppLib {
  static const MethodChannel _channel =
      const MethodChannel('komodo_xmpp_lib');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
