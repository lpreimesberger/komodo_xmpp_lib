package com.capricallctx.komodo_xmpp_lib;

import androidx.annotation.NonNull;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collection;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** KomodoXmppLibPlugin */
public class KomodoXmppLibPlugin implements FlutterPlugin, MethodCallHandler {
  private static XMPPTCPConnection connection;
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "komodo_xmpp_lib");
    channel.setMethodCallHandler(this);
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "komodo_xmpp_lib");
    channel.setMethodCallHandler(new KomodoXmppLibPlugin());
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  public boolean login(String userName, String password) throws Exception {
    SmackConfiguration.DEBUG = true;
    XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
            .setUsernameAndPassword(userName, password)
            .setResource("komodo")
            .setDebuggerEnabled(true)
            .setXmppDomain("sg01.komodochat.app")
            .setHost("206.189.94.236")
            .setPort(5222)
            .build();

    connection = new XMPPTCPConnection(config);
    System.out.println(connection.isConnected());
    connection.connect();
    System.out.println(connection.isConnected());
    if( ! connection.isConnected() ){
      return false;
    }
    connection.login();
    return true;
  }

  public Chat startChat(String thisUser) {
    ChatManager cm = ChatManager.getInstanceFor(connection);
    EntityBareJid ebi = null;
    try {
      ebi = JidCreate.entityBareFrom(thisUser);
    } catch (XmppStringprepException e) {
      e.printStackTrace();
      return null;
    }
    return cm.chatWith(ebi);
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

  public Collection<RosterEntry> getAllFriends(){
    Roster roster = Roster.getInstanceFor(connection);
    Collection<RosterEntry> entries = roster.getEntries();
    for (RosterEntry entry : entries) {
      System.out.println(entry);
    }
    return entries;
  }

}
