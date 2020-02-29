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

  // retrieve my vcard in the form of a flatted map - set set for supported fields
  Future<String> getMyVcard() async {
    String status = await _channel.invokeMethod('my_vcard');
    return status;
  }

  Future<String> getRoster() async {
    String status = await _channel.invokeMethod('get_roster');
    return status;
  }

  Future<String> setRoster( jid, nickname) async {
    var params = {
      "jid": jid,
      "nickname": nickname,
    };
    String status = await _channel.invokeMethod('set_roster', params);
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

  /**
   * because of the way vcards work - we only update a few fields to prevent
   * wiping external provider data
   * send a map of strings to update - supported special fields are:
   *  parsed["DESC"] = "tacos";
      parsed["VOICE"] = "123-123-1234";
      parsed["FN"] = "Test User";
      parsed["NICKNAME"] = "Test User";
      parsed["USERID"] = "komodo@gmail.com";
      AVATAR = URL or byte array
   */
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
    var parsed;
    try {
      parsed = xml.parse(jsonString);
    }catch(e){
      return map;
    }
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

Future<void> createGroup(String groupJID, String nickname, List<String> addUsers) async {
  var params = {
    "chatJid": groupJID,
    "nickname":nickname,
    "addusers": json.encode(addUsers)
  };
  print(params);
  print(params["addUsers"]);
  String status = await _channel.invokeMethod('create_group',params);
  return status;
}

  Future<void> joinGroup(String groupJID, String nickname) async {
    var params = {
      "chatJid": groupJID,
      "nickname":nickname
    };
    String status = await _channel.invokeMethod('joinChatGroup',params);
    return status;
  }

}
