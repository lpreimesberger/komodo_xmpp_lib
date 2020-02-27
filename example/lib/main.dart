import 'dart:convert';
import 'dart:math';

import 'package:flutter/material.dart';
import 'dart:async';

import 'dart:io';
import 'package:flutter/services.dart';
import 'package:komodo_xmpp_lib/komodo_xmpp_lib.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  KomodoXmppLib session;
  String receiveMessageFrom = '';
  String receiveMessageBody = '';

  @override
  void initState() {
    super.initState();
    initPlatformState();
    doSetup();
  }

  Future<void> doSetup() async{
    var auth = {
      "user_jid": "komodo@sg01.komodochat.app/komodo_library",
      "password":"KomodoTest123!",
      "host":"sg01.komodochat.app",
      "port":5222
    };
    session = new KomodoXmppLib(auth);

    // login
    await session.login();

    // start listening receive message
    await session.start(_onReceiveMessage,_onError);
    sleep(const Duration(seconds:2)); // just sample wait for get current state
    print(await session.currentState()); // get current state
    // sending Message
//    await session.sendMessage("lee@sg01.komodochat.app","test","random_id_for_sync_with_sqlite");
    // read Message
    var vcard = { "nickname": "cats" };
//    await session.setMyVcard(vcard);
 // var qqqq = await session.getMyVcard();

  await session.createGroup("xxxx@conference.sg01.komodochat.app", "lolgroup", ["lee@sg01.komodochat.app", "komodo@sg01.komodochat.app"]);
//    await session.getUserVcard("caprica@sg01.komodochat.app");
//    await session.getRoster();
    // life cycle, if app not active, kill stream get incoming message ..
    lifeCycle();

    // logout
//    await session.logout();

  }

  void lifeCycle() async{
    SystemChannels.lifecycle.setMessageHandler((msg) async{
      if(msg == "AppLifecycleState.inactive" || msg == "AppLifecycleState.suspending" ){
        await session.stop();
      }else if(msg == "AppLifecycleState.resumed"){
        await session.start(_onReceiveMessage, _onError);
      }
      print('SystemChannels> $msg');
      return "Lifecycle";
    });
  }


  void _onReceiveMessage(dynamic event) {
    /**
     * async message from worker thread in background
     */
    print(event);
    if( event["type"] == "my_vcard"){
      print(event["data"]);
      var parsed = session.vcardToMap(event["data"]);
      print(parsed);
      parsed["DESC"] = "tacos";
      parsed["VOICE"] = "123-123-1234";
      parsed["FN"] = "Test User";
      parsed["NICKNAME"] = "Test User";
      parsed["USERID"] = "komodo@gmail.com";
      session.setMyVcard(parsed);
    }
    else if(event["type"] == "roster") {
      print("roster test passed");
      print(event["data"]);
      var rosterData = jsonDecode(event["data"]);
      print(rosterData);
    }
    else if(event["type"] == "incoming") {
      setState(() {
        receiveMessageFrom = event['from'];
        receiveMessageBody = event['body'];
        receiveMessageBody = event['id']; // chat ID
      });
    } else {
      setState(() {
        receiveMessageFrom = event['to'];
        receiveMessageBody = event['body'];
        receiveMessageBody = event['id']; // chat ID
      });
    }
  }

  void _onError(Object error) {
    print(error);
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
  //  var loginOk = await KomodoXmppLib.login(username: "lee", password: "IbraTookMyShoes!");
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = "XX"; // await KomodoXmppLib.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('XMPP Test App'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
