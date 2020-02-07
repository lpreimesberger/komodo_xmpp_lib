import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class KomodoXmppLib {
  static const MethodChannel _channel =
      const MethodChannel('komodo_xmpp_lib');
  static const EventChannel _eventChannel = EventChannel('komodo_xmpp_channel');
  static StreamSubscription streamGetMsg;
  dynamic auth;

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /*
  static Future<bool> get isConnected async {
    return await _channel.invokeMethod('isConnected');
  }

  static Future<String> login( {@required String username, @required String password}) async {
    assert(password != null);
    assert(username != null);
    final Map<String, dynamic> params = <String, dynamic> {
      'user_jid': username,
      'host': 'sg01.komodochat.app',
      'password': password,
    };
    return await _channel.invokeMethod('login', params);
  }

  static Future<int> startChat( {@required String username}) async {
    assert(username != null);
    final Map<String, dynamic> params = <String, dynamic> {
      'userName': username

    };
    return await _channel.invokeMethod('startChat', params);
  }
*/
  KomodoXmppLib(dynamic params){
    this.auth = params;
  }

  Future<void> login() async {
    await _channel.invokeMethod('login',this.auth);
  }

  Future<void> logout() async {
    await _channel.invokeMethod('logout');
  }

  Future<String> sendMessage(String toJid,String body,String Id) async {
    var params = {
      "to_jid": toJid,
      "body":body,
      "id":Id
    };
    String status = await _channel.invokeMethod('send_message',params);
    return status;
  }

  Future<String> sendGroupMessage(String toJid,String body,String Id) async {
    var params = {
      "to_jid": toJid,
      "body":body,
      "id":Id
    };
    String status = await _channel.invokeMethod('send_group_message',params);
    return status;
  }

  Future<String> readMessage(String toJid,String Id) async {
    var params = {
      "to_jid": toJid,
      "id":Id
    };
    String status = await _channel.invokeMethod('read_message',params);
    return status;
  }

  Future<String> currentState() async {
    String state = await _channel.invokeMethod('current_state');
    return state;
  }

  Future<void> start(_onEvent,_onError) async {
    streamGetMsg = _eventChannel.receiveBroadcastStream().listen(_onEvent, onError: _onError);
  }

  Future<void> stop() async {
    streamGetMsg.cancel();
  }




}
