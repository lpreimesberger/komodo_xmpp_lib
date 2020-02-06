import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:komodo_xmpp_lib/komodo_xmpp_lib.dart';

void main() {
  const MethodChannel channel = MethodChannel('komodo_xmpp_lib');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await KomodoXmppLib.platformVersion, '42');
  });
}
