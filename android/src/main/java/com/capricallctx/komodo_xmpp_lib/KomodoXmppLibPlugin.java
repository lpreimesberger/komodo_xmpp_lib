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
  private static final String CHANNEL_CONTROL = "komodo_xmpp_lib";
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
        Log.w(TAG, "onCancel is firing...");
      }
      activity.unregisterReceiver(mBroadcastReceiver);
      mBroadcastReceiver = null;
    }
  }

  private static BroadcastReceiver get_message(final EventChannel.EventSink events) {
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if( action == null ){ return; }
        switch (action) {
          case KomodoXmppLibPluginService.GOT_ROSTER:
            Log.d(TAG,"Received somebody's roster - sending upstream");
            String rosterJSON = intent.getStringExtra(KomodoXmppLibPluginService.DATA_READY);
            if( rosterJSON == null ){
              Log.d(TAG, "Null roster - ignoring...");
              return;
            }
            Log.d(TAG, rosterJSON);
            Map<String, Object> build_vcard = new HashMap<>();
            build_vcard.put("type", "roster");
            build_vcard.put("data", rosterJSON);
            events.success(build_vcard);
          case KomodoXmppLibPluginService.GOT_USER_VCARD:
            Log.d(TAG,"Received somebody's vcard - sending upstream");
            String vcardJSON = intent.getStringExtra(KomodoXmppLibPluginService.DATA_READY);
            if( vcardJSON == null ){
              Log.d(TAG, "Null vcard - ignoring...");
              return;
            }
            Log.d(TAG, vcardJSON);
            Map<String, Object> build_roster = new HashMap<>();
            build_roster.put("type", "user_vcard");
            build_roster.put("data", vcardJSON);
            events.success(build_roster);

          case KomodoXmppLibPluginService.UPDATED_MY_VCARD:
            Log.d(TAG,"Received user vcard - sending upstream");
            vcardJSON = intent.getStringExtra(KomodoXmppLibPluginService.DATA_READY);
            Log.d(TAG, vcardJSON);
            build_vcard = new HashMap<>();
            build_vcard.put("type", "my_vcard");
            build_vcard.put("data", vcardJSON);
            events.success(build_vcard);

          case KomodoXmppLibPluginService.RECEIVE_MESSAGE:
            String from = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_FROM_JID);
            String body = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY);
            String type =  intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_TYPE);
            String idIncoming = intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS);

            if (DEBUG) {
              Log.d(TAG, "msg : " + from + " : " + body);
            }
            Map<String, Object> build = new HashMap<>();
            build.put("type", "incoming");
            build.put("id", idIncoming);
            build.put("from", from);
            build.put("message_type", type);
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

  /**
   * this is the filter for things we care about from the worker (service) thread
   * we currently listen for messsages and 'data' - the UI thread needs to register to get
   * @param auth
   * @param eventSink
   */
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
      filter.addAction(KomodoXmppLibPluginService.UPDATED_MY_VCARD);
      filter.addAction(KomodoXmppLibPluginService.GOT_USER_VCARD);
      filter.addAction(KomodoXmppLibPluginService.GOT_MY_VCARD);
      filter.addAction(KomodoXmppLibPluginService.GOT_ROSTER);
      filter.addAction(KomodoXmppLibPluginService.DATA_READY);
      activity.registerReceiver(mBroadcastReceiver, filter);
    }

  }
  KomodoXmppLibPlugin(Activity activity) {
    this.activity = activity;
  }

  public static void registerWith(Registrar registrar) {

    //method channel
    final MethodChannel method_channel = new MethodChannel(registrar.messenger(), CHANNEL_CONTROL);
    method_channel.setMethodCallHandler(new KomodoXmppLibPlugin(registrar.activity()));

    //event channel
    final EventChannel event_channel = new EventChannel(registrar.messenger(), CHANNEL_STREAM);
    event_channel.setStreamHandler(new KomodoXmppLibPlugin(registrar.activity()));

  }

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

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    Log.d(TAG, "METHOD CALL ------------------" + call.method );
    switch (call.method) {
      case "get_roster":
        Log.d(TAG, "Get roster");
        getRoster();
        result.success("SUCCESS");
        break;
      case "my_vcard":
        Log.d(TAG, "Fetching vcard...");
        get_my_vard();
        result.success("SUCCESS");
        break;
      case "get_user_vcard":
        Log.d(TAG, "Fetching user vcard...");
        if( ! call.hasArgument("user")){
          result.error("MISSING", "Missing user.", null);
          return;
        }
        String user = call.argument("user").toString();
        get_user_vard(user);
        result.success("SUCCESS");
        break;
      case "set_user_vcard":
        Log.d(TAG, "Updating user vcard...");
        if (!call.hasArgument("data") ) {
          result.error("MISSING", "Missing data segment.", null);
        }
        String data = call.argument("data").toString();
        update_my_vard(data);
        result.success("SUCCESS");
        break;
      case "login":
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

        break;
      case "logout":
        Log.d(TAG, "LOGGING OUT-------------------------------");
        logout();
        result.success("SUCCESS");
        break;
      case "send_message": {
        if (!call.hasArgument("to_jid") || !call.hasArgument("body") || !call.hasArgument("id")) {
          result.error("MISSING", "Missing argument to_jid / body / id chat.", null);
        }
        String to_jid = call.argument("to_jid");
        String body = call.argument("body");
        String id = call.argument("id");
        send_message(body, to_jid, id);

        result.success("SUCCESS");

        // still development for group message
        break;
      }
      case "send_group_message": {
        if (!call.hasArgument("to_jid") || !call.hasArgument("body") || !call.hasArgument("id")) {
          result.error("MISSING", "Missing argument to_jid / body / id chat.", null);
        }
        String to_jid = call.argument("to_jid");
        String body = call.argument("body");
        String id = call.argument("id");
        send_group_message(body, to_jid, id);

        result.success("SUCCESS");

        break;
      }
      case "read_message": {
        if (!call.hasArgument("to_jid") || !call.hasArgument("id")) {
          result.error("MISSING", "Missing argument to_jid / body / id chat.", null);
        }
        String to_jid = call.argument("to_jid");
        String id = call.argument("id");
        read_message(to_jid, id);

        result.success("SUCCESS");

        break;
      }
      case "current_state":
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

        break;
      default:
        result.notImplemented();
        break;
    }
  }


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

  private void send_group_message(String msg, String jid_user, String id) {
    Log.d(TAG, "Current Status : " + KomodoXmppLibPluginService.getState().toString());
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "group_send : " + jid_user);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.GROUP_SEND_MESSAGE);
      intent.putExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_BODY, msg);
      intent.putExtra(KomodoXmppLibPluginService.GROUP_TO, jid_user);
      intent.putExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_PARAMS, id);

      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "group_send_complete");
      }
    }
  }

  // send message to JID
  private void send_message(String msg, String jid_user, String id) {
    Log.d(TAG, "Current Status : " + KomodoXmppLibPluginService.getState().toString());
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "CONNECTED -> " + jid_user);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.SEND_MESSAGE);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY, msg);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_TO, jid_user);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS, id);

      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Not connected?");
      }
    }
  }

  private void getRoster() {
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      Log.d(TAG, "getRoster -> " );
      Intent intent = new Intent(KomodoXmppLibPluginService.GET_ROSTER);
      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Not connected?");
      }
    }
  }


  private void get_my_vard() {
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "get_my_vcard -> " + jid_user);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.GET_MY_VCARD);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY, "");
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_TO, "");
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS, "");
      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Not connected?");
      }
    }
  }

  private void get_user_vard(String user) {
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "get_user_vcard -> " + jid_user);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.GET_USER_VCARD);
      intent.putExtra(KomodoXmppLibPluginService.GET_USER_VCARD_JID, user);
      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Not connected?");
      }
    }
  }

  private void update_my_vard(String data) {
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      if (DEBUG) {
        Log.d(TAG, "update my_vcard -> " + data);
      }
      Intent intent = new Intent(KomodoXmppLibPluginService.SET_MY_VCARD);
      intent.putExtra(KomodoXmppLibPluginService.SET_MY_VCARD_DATA, data);
      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Not connected?");
      }
    }
  }


  private void read_message( String jid_user, String id) {
    Log.d(TAG, "Current Status : " + KomodoXmppLibPluginService.getState().toString());
    if (KomodoXmppLibPluginService.getState().equals(KomodoConnection.ConnectionState.CONNECTED)) {
      Log.d(TAG, "Got message for -> " + jid_user);
      Intent intent = new Intent(KomodoXmppLibPluginService.READ_MESSAGE);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_TO, jid_user);
      intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS, id);
      activity.sendBroadcast(intent);
    } else {
      if (DEBUG) {
        Log.d(TAG, "Not connected");
      }
    }
  }

  private void set_presence(int state) {

  }

  private void get_presence() {

  }

  }
