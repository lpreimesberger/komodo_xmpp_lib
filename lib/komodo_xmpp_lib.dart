import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:xml/xml.dart' as xml;

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

  Future<String> getMyVcard() async {
    String status = await _channel.invokeMethod('my_vcard');
    return status;
  }

  Future<String> getRoster() async {
    String status = await _channel.invokeMethod('get_roster');
    return status;
  }


  Future<String> getUserVcard(user) async {
    var params = {
      "user": user,
    };
    print(params);
    String status = await _channel.invokeMethod('get_user_vcard', params);
    return status;
  }


  Future<String> setMyVcard(map) async {
    
    var params = {
      "data": jsonEncode(map),
    };
    String status = await _channel.invokeMethod('set_user_vcard', params);
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

  // utility
  Map<String,String> vcardToMap(String jsonString  ){
    /**
     * convert a vcard xml (as a string) to a flat map of fieldsv
     */
  Map<String, String> map = new Map<String,String>();
  var parsed =  xml.parse(jsonString);
  Iterable<xml.XmlElement> i = parsed.findAllElements("iq");
  for( var element in i ){
    for( var attr in element.attributes ){
      print(attr.name);
      print(attr.value);
      map[attr.name.toString()] = attr.value;
    }
    print(element.name.toString());
    for( var innerElement in element.children){
      for( var vcardElement in innerElement.children){
        print( vcardElement.toXmlString());
        var node = vcardElement.toXmlString().split(">")[0];
        node = node.substring(1);
        map[node] = vcardElement.text;
        print(vcardElement.attributes);
        print(vcardElement.text);
      }
    }
  }
  return map;
}
}
