package com.capricallctx.komodo_xmpp_lib;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jxmpp.jid.Jid;
import java.util.Collection;
import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.*;


public class KomodoXmppLibPlugin extends FlutterActivity implements MethodCallHandler, EventChannel.StreamHandler {
  private static final String TAG = "komodo_xmpp";
  public static final Boolean DEBUG = true;
  private static final String CHANNEL = "komodo_xmpp_lib";
  private static final String CHANNEL_STREAM = "komodo_xmpp_channel";
  private Activity activity;
  private String jid_user = "";
  private String password = "";
  private String host = "";
  private Integer port = 0;
  private BroadcastReceiver mBroadcastReceiver = null;


  @Override
  public void onCancel(Object o) {
    if (mBroadcastReceiver != null) {
      if (DEBUG) {
        Log.w(TAG, "cancelling listener");
      }
      activity.unregisterReceiver(mBroadcastReceiver);
      mBroadcastReceiver = null;
    }
  }

/*
  @Override
  public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), CHANNEL);
    channel.setMethodCallHandler(this);
  }
*/
  /*
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
    channel.setMethodCallHandler(new KomodoXmppLibPlugin());
    final EventChannel event_channel = new EventChannel(registrar.messenger(), CHANNEL_STREAM);
    event_channel.setStreamHandler(new KomodoXmppLibPlugin(registrar.activity()));
  }
*/
  private static BroadcastReceiver get_message(final EventChannel.EventSink events) {
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
          case KomodoXmppLibPluginService.RECEIVE_MESSAGE:
            String from = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_FROM_JID);
            String body = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY);
            String idIncoming = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS);

            if (DEBUG) {
              Log.d(TAG, "msg : " + from + " : " + body);
            }
            Map<String, Object> build = new HashMap<>();
            build.put("type", "incoming");
            build.put("id", idIncoming);
            build.put("from", from);
            build.put("body", body);
            events.success(build);
            break;
          case KomodoXmppLibPluginService.OUTGOING_MESSAGE:
            String to = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_TO_JID);
            String bodyTo = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY);
            String idOutgoing = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS);
            if (DEBUG) {
              Log.d(TAG, "msg : " + to + " : " + bodyTo);
            }
            Map<String, Object> buildTo = new HashMap<>();
            buildTo.put("type", "outgoing");
            buildTo.put("id", idOutgoing);
            buildTo.put("to", to);
            buildTo.put("body", bodyTo);
            events.success(buildTo);
            break;

        }
      }
    };
  }
  @Override
  public void onListen(Object auth, EventChannel.EventSink eventSink) {

    if (mBroadcastReceiver == null) {
      if (DEBUG) {
        Log.w(TAG, "adding listener");
      }
      mBroadcastReceiver = get_message(eventSink);
      IntentFilter filter = new IntentFilter();
      filter.addAction(KomodoXmppLibPluginService.RECEIVE_MESSAGE);
      filter.addAction(KomodoXmppLibPluginService.OUTGOING_MESSAGE);
      activity.registerReceiver(mBroadcastReceiver, filter);
    }

  }

/*
  public void onMethodCall2(@NonNull MethodCall call, @NonNull Result result) {
    if( call.method.equals("isConnected")){
      result.success( isConnected() );
    }
    else if( call.method.equals("login")){
      String userName = call.argument("userName");
      String password = call.argument("password");

      result.success( login(userName,password) );
    }
    else if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }
*/

  KomodoXmppLibPlugin(Activity activity) {
    this.activity = activity;
  }

  public static void registerWith(Registrar registrar) {

    //method channel
    final MethodChannel method_channel = new MethodChannel(registrar.messenger(), CHANNEL);
    method_channel.setMethodCallHandler(new KomodoXmppLibPlugin(registrar.activity()));

    //event channel
    final EventChannel event_channel = new EventChannel(registrar.messenger(), CHANNEL_STREAM);
    event_channel.setStreamHandler(new KomodoXmppLibPlugin(registrar.activity()));

  }

/*
  @Override
  public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
*/
  /*
  public String login(String userName, String password)  {
    SmackConfiguration.DEBUG = true;
    Log.d(LOG_NAME, "Starting login");
    Log.d(LOG_NAME, userName);
    Log.d(LOG_NAME, password);
    XMPPTCPConnectionConfiguration config = null;
    try {
      config = XMPPTCPConnectionConfiguration.builder()
              .setUsernameAndPassword(userName, password)
              .setResource("komodo")
              .setDebuggerEnabled(true)
              .setXmppDomain("sg01.komodochat.app")
              .setHost("206.189.94.236")
              .setPort(5222)
              .build();
    } catch (XmppStringprepException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating configuration");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    }

    connection = new XMPPTCPConnection(config);
    System.out.println(connection.isConnected());
    try {
      Log.d(LOG_NAME, "Starting connection");
      connection.connect();
    } catch (SmackException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating configuration");
      Log.e(LOG_NAME, e.getMessage());

      return e.getMessage();
    } catch (IOException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating configuration");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    } catch (XMPPException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating configuration");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating configuration");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    }
    if( ! connection.isConnected() ){
      Log.d(LOG_NAME, "Not connected after login :(");
      return "failed to connect";
    }
    try {
      connection.login();
    } catch (XMPPException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating login");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    } catch (SmackException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating login");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    } catch (IOException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating login");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Log.e(LOG_NAME, "Exception creating login");
      Log.e(LOG_NAME, e.getMessage());
      return e.getMessage();
    }
    Log.d(LOG_NAME, "Connection success");
    return "ok";
  }

  public boolean isConnected(){
    if( connection == null){
      return false;
    }
    return connection.isConnected();
  }
  public String startChat(String thisUser) {
    if( chatTable.containsKey(thisUser)){
      // already exists
      return "kc-" + thisUser;
    }
    ChatManager cm = ChatManager.getInstanceFor(connection);
    EntityBareJid ebi = null;
    try {
      ebi = JidCreate.entityBareFrom(thisUser);
    } catch (XmppStringprepException e) {
      e.printStackTrace();
      return null;
    }
//    chatTable.entrySet(thisUser, cm.chatWith(ebi));
    //return cm.chatWith(ebi);
    return "";
  }
*/
  public boolean listenRosterChanges(Roster roster){
    roster.addRosterListener(new RosterListener() {
      @Override
      public void entriesAdded(Collection<Jid> addresses) {

      }

      @Override
      public void entriesUpdated(Collection<Jid> addresses) {

      }

      @Override
      public void entriesDeleted(Collection<Jid> addresses) {

      }

      @Override
      public void presenceChanged(Presence presence) {

      }

    });
    return true;
  }

  /*
  public boolean registerStanzaListener(){
    connection.addAsyncStanzaListener(new StanzaListener() {
      public void processStanza(Stanza stanza)
              throws SmackException.NotConnectedException,InterruptedException,
              SmackException.NotLoggedInException {
        // handle stanza
      }
    }, StanzaTypeFilter.MESSAGE);
    return true;
  }

  public void setStatus(boolean available, String status) {

    Presence.Type type = available? Presence.Type.available: Presence.Type.unavailable;
    Presence presence = new Presence(type);

    presence.setStatus(status);
    try {
      connection.sendPacket(presence);
    } catch (SmackException.NotConnectedException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
  public void destroy() {
    if (connection!=null && connection.isConnected()) {
      connection.disconnect();
    }
  }
*/
  /*
  public boolean createChatRoom(String userName, String roomName){
    EntityBareJid ebi = null;
    MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
    try {
      ebi = JidCreate.entityBareFrom(userName);
    } catch (XmppStringprepException e) {
      e.printStackTrace();
      return false;
    }
    MultiUserChat muc = manager.getMultiUserChat(ebi);
    Resourcepart room = null;
    try {
      room = Resourcepart.from(roomName);
    } catch (XmppStringprepException e) {
      e.printStackTrace();
      return false;
    }
    try {
      muc.create(room).makeInstant();
    } catch (SmackException.NoResponseException e) {
      e.printStackTrace();
    } catch (XMPPException.XMPPErrorException e) {
      e.printStackTrace();
    } catch (SmackException.NotConnectedException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (MultiUserChatException.MucAlreadyJoinedException e) {
      e.printStackTrace();
    } catch (MultiUserChatException.MissingMucCreationAcknowledgeException e) {
      e.printStackTrace();
    } catch (MultiUserChatException.NotAMucServiceException e) {
      e.printStackTrace();
    }
    return true;
  }
*/
  /*
  public boolean sendMessage(Chat thisChat, String thisMessage){
    try {
      thisChat.send(thisMessage);
    } catch (SmackException.NotConnectedException e) {
      e.printStackTrace();
      return false;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
*/
  /*
  public Collection<RosterEntry> getAllFriends(){
    Roster roster = Roster.getInstanceFor(connection);
    Collection<RosterEntry> entries = roster.getEntries();
    for (RosterEntry entry : entries) {
      System.out.println(entry);
    }
    return entries;
  }
*/
  @Override
  public void onMethodCall(MethodCall call, Result result) {

    // send_message
    if (call.method.equals("login")) {

      if (!call.hasArgument("user_jid") || !call.hasArgument("password") || !call.hasArgument("host")) {
        result.error("MISSING", "Missing auth.", null);
      }
      this.jid_user = call.argument("user_jid").toString();
      this.password = call.argument("password").toString();
      this.host = call.argument("host").toString();
      if (call.hasArgument("port")) {
        this.port = Integer.parseInt(call.argument("port").toString());
      }
      login();

      result.success("SUCCESS");

    } else if (call.method.equals("logout")) {

      logout();

      result.success("SUCCESS");

    } else if (call.method.equals("send_message")) {
      if (!call.hasArgument("to_jid") || !call.hasArgument("body") || !call.hasArgument("id")) {
        result.error("MISSING", "Missing argument to_jid / body / id chat.", null);
      }
      String to_jid = call.argument("to_jid");
      String body = call.argument("body");
      String id = call.argument("id");
      send_message(body, to_jid, id);

      result.success("SUCCESS");

      // still development for group message
    } else if (call.method.equals("send_group_message")) {
      if (!call.hasArgument("to_jid") || !call.hasArgument("body") || !call.hasArgument("id")) {
        result.error("MISSING", "Missing argument to_jid / body / id chat.", null);
      }
      String to_jid = call.argument("to_jid");
      String body = call.argument("body");
      String id = call.argument("id");
      send_group_message(body, to_jid, id);

      result.success("SUCCESS");

    } else if (call.method.equals("read_message")) {
      if (!call.hasArgument("to_jid") || !call.hasArgument("id")) {
        result.error("MISSING", "Missing argument to_jid / body / id chat.", null);
      }
      String to_jid = call.argument("to_jid");
      String id = call.argument("id");
      read_message(to_jid, id);

      result.success("SUCCESS");

    } else if (call.method.equals("current_state")) {
      String state = "UNKNOWN";
      switch (KomodoXmppLibPluginService.getState()) {
        case CONNECTED:
          state = "CONNECTED";
          break;
        case AUTHENTICATED:
          state = "AUTHENTICATED";
          break;
        case CONNECTING:
          state = "CONNECTING";
          break;
        case DISCONNECTING:
          state = "DISCONNECTING";
          break;
        case DISCONNECTED:
          state = "DISCONNECTED";
          break;
      }

      if (DEBUG) {
        Log.d(TAG, state);
      }
      result.success(state);

    } else {
      result.notImplemented();
    }
  }

  // login
  private void login() {
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.DISCONNECTED)) {
      Intent i = new Intent(activity, KomodoXmppLibPluginService.class);
      i.putExtra("jid_user", jid_user);
      i.putExtra("password", password);
      i.putExtra("host", host);
      i.putExtra("port", port);
      activity.startService(i);
    }
  }

  private void logout() {
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      Intent i1 = new Intent(activity, KomodoXmppLibPluginService.class);
      activity.stopService(i1);
    }
  }

  // send message to JID
  private void send_group_message(String msg, String jid_user, String id) {
    Log.d(TAG, "Current Status : " + KomodoXmppLibPluginService.getState().toString());
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "ngirim pesan ke : " + jid_user);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.GROUP_SEND_MESSAGE);
      intent.putExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_BODY, msg);
      intent.putExtra(KomodoXmppLibPluginService.GROUP_TO, jid_user);
      intent.putExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_PARAMS, id);

      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Tidak terhubung ke server");
      }
    }
  }

  // send message to JID
  private void send_message(String msg, String jid_user, String id) {
    Log.d(TAG, "Current Status : " + KomodoXmppLibPluginService.getState().toString());
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "ngirim pesan ke : " + jid_user);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.SEND_MESSAGE);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY, msg);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_TO, jid_user);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS, id);

      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Tidak terhubung ke server");
      }
    }
  }

  // send message to JID
  private void read_message( String jid_user, String id) {
    Log.d(TAG, "Current Status : " + KomodoXmppLibPluginService.getState().toString());
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "read pesan dari : " + jid_user);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.READ_MESSAGE);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_TO, jid_user);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS, id);

      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Tidak terhubung ke server");
      }
    }
  }


  private void set_presence() {

  }

  private void get_presence() {

  }

  }
