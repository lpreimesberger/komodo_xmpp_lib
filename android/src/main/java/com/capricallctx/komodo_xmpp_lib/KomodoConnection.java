package com.capricallctx.komodo_xmpp_lib;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.JSONUtil;

public class KomodoConnection implements ConnectionListener {
    private static final String TAG ="flutter_xmpp";
    private Context mApplicationContext;
    private String username;
    private String password;
    private String serviceName;
    private String resource;
    private String host;
    private Integer post;
    public String myVCard = "";
    private XMPPTCPConnection mConnection;
    ReconnectionManager reconnectionManager;
    private BroadcastReceiver uiThreadMessageReceiver;
    private Roster roster;

    public enum ConnectionState{
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED, LOGINFAILURE;
    }

    public  enum LoggedInState{
        LOGGED_IN , LOGGED_OUT
    }

    public KomodoConnection(Context context,String jid_user,String password,String host,Integer port){
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "Connection Constructor called.");
        }
        mApplicationContext = context.getApplicationContext();
        String jid = jid_user;
        this.password = password;
        post = port;
        this.host = host;
        if(jid != null) {
            String[] jid_list = jid.split("@");
            username = jid_list[0];
            if(jid_list[1].contains("/")) {
                String[] domain_resource = jid_list[1].split("/");
                serviceName = domain_resource[0];
                resource = domain_resource[1];
            }else{
                serviceName = jid_list[1];
                resource = "komodo_xmpp_lib";
            }
        }else{
            username ="";
            serviceName ="";
            resource = "";
        }
    }


    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


    public void connect() throws IOException, XMPPException, SmackException
    {
        XMPPTCPConnectionConfiguration.Builder conf = XMPPTCPConnectionConfiguration.builder();

        conf.setXmppDomain(serviceName);
        if(validIP(host)) {
            InetAddress addr = InetAddress.getByName(host);
            conf.setHostAddress(addr);
        }else{
            conf.setHost(host);
        }
        if(post != 0) {
            conf.setPort(post);
        }
        conf.setUsernameAndPassword(username, password);
        conf.setResource(resource);
        conf.setKeystoreType(null);
        conf.setDebuggerEnabled(true);
        conf.setCompressionEnabled(true);

        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "Username : " + username);
            Log.d(TAG, "Password : " + password);
            Log.d(TAG, "Server : " + serviceName);
            Log.d(TAG, "Port : " + post.toString());

        }


        //Set up the ui thread broadcast message receiver.

        mConnection = new XMPPTCPConnection(conf.build());
        roster = Roster.getInstanceFor(mConnection);
        reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

        roster.addRosterListener(new RosterListener() {
                                     @Override
                                     public void entriesAdded(Collection<Jid> addresses) {
                                         Log.d(TAG, "Added roster");
                                     }

                                     @Override
                                     public void entriesUpdated(Collection<Jid> addresses) {
                                         Log.d(TAG, "Updated roster");

                                     }

                                     @Override
                                     public void entriesDeleted(Collection<Jid> addresses) {
                                         Log.d(TAG, "Deleted roster");

                                     }

                                     @Override
                                     public void presenceChanged(Presence presence) {
                                         Log.d(TAG, "Presence changed - autoaccepting");
                                         try{
                                             Presence presence1 = new Presence(Presence.Type.subscribed);
                                             presence1.setTo(presence.getFrom());
                                             mConnection.sendStanza(presence1);
                                         }catch (SmackException.NotConnectedException e) {
                                             e.printStackTrace();
                                         } catch (InterruptedException e) {
                                             e.printStackTrace();
                                         }
                                         Log.d(TAG, presence.toString());

                                     }
                                 }

        );
        // accept everyone (default)
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        mConnection.addConnectionListener(this);
        try {
            if(KomodoXmppLibPlugin.DEBUG) {
                Log.d(TAG, "Calling connect() ");
            }
            mConnection.connect();
            mConnection.login(username, password);
            if(KomodoXmppLibPlugin.DEBUG) {
                Log.d(TAG, " login() Called ");
            }
        } catch (InterruptedException e) {
            KomodoXmppLibPluginService.sConnectionState=ConnectionState.LOGINFAILURE;
            e.printStackTrace();
        }
        setupUiThreadBroadCastMessageReceiver();
        PacketExtensionFilter invitationFilter = new PacketExtensionFilter("x", "http://jabber.org/protocol/muc#user");
        PacketListener invitationPacketListener = new PacketListener() {
            @Override
            public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException {
                    Log.d(TAG, "GOT MUC PACKET");
            }
        };
        mConnection.addPacketListener(invitationPacketListener, invitationFilter);
        StanzaFilter filter = new AndFilter(StanzaTypeFilter.MESSAGE);
        StanzaCollector myCollector = mConnection.createStanzaCollector(filter);
        StanzaListener myListener = new StanzaListener() {
		public void processStanza(Stanza stanza) {
                // Do something with the incoming stanza here._
                Log.d(TAG, stanza.toString());
            }
        };
        mConnection.addAsyncStanzaListener(myListener, filter);

        ChatManager.getInstanceFor(mConnection).addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid messageFrom, Message message, Chat chat) {
                if(KomodoXmppLibPlugin.DEBUG) {
                    Log.d(TAG, message.getType().toString());
                    Log.d(TAG, "INCOMING :" + message.toString());
                    Log.d(TAG, "message.getBody() :" + message.getBody());
                    Log.d(TAG, "message.getFrom() :" + message.getFrom());
                }
                DelayInformation inf = null;
                inf = (DelayInformation)message.getExtension(DelayInformation.ELEMENT,DelayInformation.NAMESPACE);
                if (inf != null){
                    Date date = inf.getStamp();
                    Log.d(TAG,"date: "+date);
                }

                String from = message.getFrom().toString();
                String contactJid="";
                if (from.contains("/")){
                    contactJid = from.split("/")[0];
                    if(KomodoXmppLibPlugin.DEBUG) {
                        Log.d(TAG, "The real jid is :" + contactJid);
                        Log.d(TAG, "The message is from :" + from);
                    }
                }else {
                    contactJid = from;
                }

                String id = message.getBody("id");

                //Bundle up the intent and send the broadcast.
                Intent intent = new Intent(KomodoXmppLibPluginService.RECEIVE_MESSAGE);
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_FROM_JID,contactJid);
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_TYPE, message.getType().toString());
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY,message.getBody());
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS,id);
                mApplicationContext.sendBroadcast(intent);
                if(KomodoXmppLibPlugin.DEBUG) {
                    Log.d(TAG, "Received message from :" + contactJid + " broadcast sent.");
                }
                ///ADDED

            }
        });

        ChatManager.getInstanceFor(mConnection).addOutgoingListener(new OutgoingChatMessageListener() {
            @Override
            public void newOutgoingMessage(EntityBareJid to, Message message, Chat chat) {
                if(KomodoXmppLibPlugin.DEBUG) {
                    Log.d(TAG, "OUTGOING :" + message.toString());
                    ///ADDED
                    Log.d(TAG, "message.getBody() :" + message.getBody());
                    Log.d(TAG, "message.getTo() :" + message.getTo());

                }

                DelayInformation inf = null;
                inf = (DelayInformation)message.getExtension(DelayInformation.ELEMENT,DelayInformation.NAMESPACE);
                if (inf != null){
                    Date date = inf.getStamp();
                    Log.d(TAG,"date: "+date);
                }

                String from = message.getTo().toString();
                String contactJid="";
                if (from.contains("/")){
                    contactJid = from.split("/")[0];
                    if(KomodoXmppLibPlugin.DEBUG) {
                        Log.d(TAG, "The real jid is :" + contactJid);
                        Log.d(TAG, "The message is from :" + from);
                    }
                }else {
                    contactJid = from;
                }
                String id = message.getBody("id");
                //Bundle up the intent and send the broadcast.
                Intent intent = new Intent(KomodoXmppLibPluginService.OUTGOING_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_TO_JID,contactJid);
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY,message.getBody());
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS,id);
                mApplicationContext.sendBroadcast(intent);
                if(KomodoXmppLibPlugin.DEBUG) {
                    Log.d(TAG, "Outgoing message from :" + contactJid + " broadcast sent.");
                }
            }
        });
        reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }


    private void setupUiThreadBroadCastMessageReceiver()
    {
        Log.d(TAG,"Receiver THREAD has started.....");

        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                Log.d(TAG,">>>>>>>>>>>>>>service broadcast " + action);
                if( action == null){ return; }
                switch (action) {
                    case KomodoXmppLibPluginService.GET_ROSTER:
                        Log.d(TAG, "Get request for roster...");
                        getRoster();
                        break;
                    case KomodoXmppLibPluginService.SET_ROSTER:
                        Log.d(TAG, "Set request for roster...");
                        addToRoster(
                                intent.getStringExtra(KomodoXmppLibPluginService.JID),
                                intent.getStringExtra(KomodoXmppLibPluginService.NICKNAME)
                                );
                        break;
                    case KomodoXmppLibPluginService.GET_MY_VCARD:
                        Log.d(TAG, "Get request for vcard...");
                        getMyVcard();
                        break;
                    case KomodoXmppLibPluginService.CREATE_GROUP:
                        Log.d(TAG, "Get request for creating group...");
                        try {
                            createChatGroup(
                                    intent.getStringExtra(KomodoXmppLibPluginService.CREATE_GROUP_JID),
                                    intent.getStringExtra(KomodoXmppLibPluginService.CREATE_GROUP_NICKNAME),
                                    intent.getStringExtra(KomodoXmppLibPluginService.DATA_READY)
                            );
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                    case KomodoXmppLibPluginService.JOIN_GROUP:
                        Log.d(TAG, "Get request for creating group...");
                        joinChatGroup(
                            intent.getStringExtra(KomodoXmppLibPluginService.JOIN_GROUP_JID),
                            intent.getStringExtra(KomodoXmppLibPluginService.JOIN_GROUP_AS)
                        );
                        break;
                    case KomodoXmppLibPluginService.SET_MY_VCARD:
                        Log.d(TAG, "Get request for updating my vcard...");
                        updateMyVcard(intent.getStringExtra(KomodoXmppLibPluginService.SET_MY_VCARD_DATA));
                        break;
                    case KomodoXmppLibPluginService.GET_USER_VCARD:
                        Log.d(TAG, "Get request for user vcard...");
                        getUserVcard(intent.getStringExtra(KomodoXmppLibPluginService.GET_USER_VCARD_JID));
                        break;
                    case KomodoXmppLibPluginService.SEND_MESSAGE:
                        sendMessage(intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY),
                                intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_TO),
                                intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS));

                        break;
                    case KomodoXmppLibPluginService.READ_MESSAGE:
                        sendRead(intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_TO),
                                intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS));

                        break;
                    case KomodoXmppLibPluginService.GROUP_SEND_MESSAGE:
                        //Send group message.
                        Log.d(TAG, "Group send in worker received");
                        sendGroupMessage(intent.getStringExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_BODY),
                                intent.getStringExtra(KomodoXmppLibPluginService.GROUP_TO),
                                intent.getStringExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_PARAMS));
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(KomodoXmppLibPluginService.SEND_MESSAGE);
        filter.addAction(KomodoXmppLibPluginService.READ_MESSAGE);
        filter.addAction(KomodoXmppLibPluginService.GET_MY_VCARD);
        filter.addAction(KomodoXmppLibPluginService.SET_MY_VCARD);
        filter.addAction(KomodoXmppLibPluginService.GET_USER_VCARD);
        filter.addAction(KomodoXmppLibPluginService.GET_ROSTER);
        filter.addAction(KomodoXmppLibPluginService.SET_ROSTER);
        filter.addAction(KomodoXmppLibPluginService.CREATE_GROUP);
        filter.addAction(KomodoXmppLibPluginService.JOIN_GROUP);
        filter.addAction(KomodoXmppLibPluginService.GROUP_SEND_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver,filter);

    }



    private void updateMyVcard(String mapString){
        try {
            Log.d(TAG, "Updating vcard...");
            Log.d(TAG, mapString);
            JSONObject jo = new JSONObject(mapString);
            VCard ownVCard = new VCard();
            ownVCard.load(mConnection);
            Iterator<String> keys = jo.keys();
            while( keys.hasNext()){
                String field = keys.next();
                Log.d(TAG, "Processing vcard -> " + field);
                switch (field){
                    case "AVATAR":
                        if( jo.has("AVATAR")) {
                            try {
                                Log.d(TAG, "Attempting to set avatar");
                                ownVCard.setAvatar(new URL(jo.getString("AVATAR")));
                            } catch (MalformedURLException e) {
                                Log.d(TAG, "User has sent an invalid avatar URL?");
                                e.printStackTrace();
                            }
                        }
                        break;

                    case "NICKNAME":
                        if( jo.has("NICKNAME"))
                            ownVCard.setNickName(jo.getString("NICKNAME"));
                        break;
                    case "EMAIL":
                        if( jo.has("EMAIL"))
                            ownVCard.setEmailHome(jo.get("EMAIL").toString());
                        break;
                    case "VOICE":
                        if( jo.has("VOICE"))
                            ownVCard.setPhoneHome("VOICE", jo.getString("VOICE"));
                        break;
                    case "DESC":
                        if( jo.has("DESC"))
                            ownVCard.setField("DESC", jo.get("DESC").toString());
                    case "FN":
                        if( jo.has("FN"))
                            ownVCard.setField("FN", jo.get("FN").toString());

                }
            }
            ownVCard.save(mConnection);
        } catch (JSONException | SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getMyVcard(){
        VCard vCard = new VCard();
        Log.d(TAG, "XMPP Request << vcard...");
        try {
            vCard = VCardManager.getInstanceFor(mConnection).loadVCard();
//            vCard.load(mConnection);
        } catch (SmackException.NoResponseException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
        this.myVCard =  vCard.toXML().toString();
        Log.d(TAG, this.myVCard);
        //Bundle up the intent and send the broadcast.
        Intent intent = new Intent(KomodoXmppLibPluginService.GOT_MY_VCARD);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putExtra(KomodoXmppLibPluginService.DATA_READY,this.myVCard);
        mApplicationContext.sendBroadcast(intent);
    }

    private void getUserVcard(String thisUser){
        VCard vCard = new VCard();
        Log.d(TAG, "XMPP Request << vcard...");
        try {
            EntityBareJid jid = JidCreate.entityBareFrom(thisUser);
            vCard = VCardManager.getInstanceFor(mConnection).loadVCard(jid);
        } catch (SmackException.NoResponseException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException | XmppStringprepException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
        this.myVCard =  vCard.toXML().toString();
        // send data
        Intent intent = new Intent(KomodoXmppLibPluginService.GOT_USER_VCARD);
        //intent.setPackage(mApplicationContext.getPackageName());
        intent.putExtra(KomodoXmppLibPluginService.DATA_READY,this.myVCard);
        mApplicationContext.sendBroadcast(intent);
        Log.d(TAG, this.myVCard);
    }

    private void sendRead (String toJid, String id)
    {
        Log.d(TAG,"Sending message to :"+ toJid);
        EntityBareJid jid = null;
        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        try {
            jid = JidCreate.entityBareFrom(toJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid,Message.Type.normal);
            message.setBody("icm_send_message");
            message.addBody("id", id);
            chat.send(message);

        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    private void sendGroupMessage (String body , String toRoom, String id)
    {
        Log.d(TAG,"Sending group message to :"+ toRoom);
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(toRoom);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        MultiUserChatManager chatManager = MultiUserChatManager.getInstanceFor(mConnection);
        MultiUserChat multiUserChat = chatManager.getMultiUserChat(jid);
        Message message = new Message(jid, Message.Type.groupchat);
        message.setBody(body);
        try {
            multiUserChat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    private void sendMessage (String body , String toJid, String id)
    {
        Log.d(TAG,"Sending message to :"+ toJid);
        EntityBareJid jid = null;
        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
        try {
            jid = JidCreate.entityBareFrom(toJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(body);
            message.addBody("id", id);

//            try {
//                final JSONArray parameters = new JSONArray(custom_parameter);
//                for(int cs = 0;cs < parameters.length();cs++) {
//                    final JSONObject param = parameters.getJSONObject(cs);
//                    String key = param.getString("key");
//                    String value = param.getString("value");
//                    message.addBody(key, value);
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }

            chat.send(message);

        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void disconnect() {
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "Disconnecting from server " + serviceName);
        }
        if (mConnection != null){
            mConnection.disconnect();
            mConnection = null;
        }

        // Unregister the message broadcast receiver.
        if( uiThreadMessageReceiver != null) {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }
    }


    @Override
    public void connected(XMPPConnection connection) {
        KomodoXmppLibPluginService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG,"Connected Successfully");

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        KomodoXmppLibPluginService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG,"Authenticated Successfully");
//        showContactListActivityWhenAuthenticated();
    }


    @Override
    public void connectionClosed() {
        KomodoXmppLibPluginService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"Connectionclosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        KomodoXmppLibPluginService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"ConnectionClosedOnError, error "+ e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        KomodoXmppLibPluginService.sConnectionState = ConnectionState.CONNECTING;
        Log.d(TAG,"ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        KomodoXmppLibPluginService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG,"ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        KomodoXmppLibPluginService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG,"ReconnectionFailed()");

    }

    private void setPresence(String state){
        Presence presence;
        try {
        switch (state) {
            // 0. Online 1. Q me 2. Busy 3. Do not disturb 4. Leave 5. Stealth 6. Offline
            case KomodoXmppLibPluginService.PRESENCE_ONLINE:
                presence = new Presence(Presence.Type.available);
                    mConnection.sendPacket(presence);
                break;
            case KomodoXmppLibPluginService.PRESENCE_LONELY:
                presence = new Presence(Presence.Type.available);
                presence.setMode(Presence.Mode.chat);
                mConnection.sendPacket(presence);
                break;
            case KomodoXmppLibPluginService.PRESENCE_BUSY:
                presence = new Presence(Presence.Type.available);
                presence.setMode(Presence.Mode.dnd);
                mConnection.sendPacket(presence);
                break;
            case KomodoXmppLibPluginService.PRESENCE_DND:
                presence = new Presence(Presence.Type.available);
                presence.setMode(Presence.Mode.xa);
                mConnection.sendPacket(presence);
                break;
            case KomodoXmppLibPluginService.PRESENCE_AWAY:
                presence = new Presence(Presence.Type.available);
                presence.setMode(Presence.Mode.away);
                mConnection.sendPacket(presence);
                break;
            case KomodoXmppLibPluginService.PRESENCE_STEALTH:
                break;
        }
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void addToRoster(String jidText, String nickname){
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(jidText);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        try {
            roster.createEntry(jid, nickname, null);
        } catch (SmackException.NotLoggedInException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void getRoster(){
        List<HashMap> list = new ArrayList<>();
        Gson gson = new GsonBuilder().create();
        StringBuilder encoded = new StringBuilder("{[");
        try{
            // get the roster and if it is not loaded reload it

            if (!roster.isLoaded())
                roster.reloadAndWait();
            RosterEntry[] result = new RosterEntry[roster.getEntries().size()];
            int i = 0;
            // loop through all roster entries and append them to the array
            for (RosterEntry entry: roster.getEntries()){
                Log.d(TAG, entry.toString());
                result[i++] = entry;
                VCard vCard = VCardManager.getInstanceFor(mConnection).loadVCard((EntityBareJid) entry.getJid());
                HashMap<String,String> ms = new HashMap<>();
                ms.put("jid", entry.getJid().toString());
                ms.put("fn", entry.getName() );
                ms.put("nn", vCard.getNickName());
                ms.put("ph", vCard.getPhoneHome("VOICE"));
                ms.put("avatar_hash", vCard.getAvatarHash());
                byte[] icon = vCard.getAvatar();
                String theIcon = Base64.encodeToString(icon, Base64.NO_WRAP);
                ms.put("avatar_mime", theIcon);
                ms.put("about_me", vCard.getField("DESC"));
                ms.put("can_see", entry.canSeeHisPresence() ? "true" : "false" );
                ms.put("can_be_seen_by", entry.canSeeMyPresence() ? "true" : "false" );
                List<RosterGroup>  groups = entry.getGroups();
                StringBuilder groupList = new StringBuilder("");
                for( RosterGroup  group: groups){
                    groupList.append(",");
                    groupList.append(group.getName());
                }
                ms.put("groups", groupList.toString());
                list.add(ms);


            }
            encoded.append("\n]}");
        }catch (Exception e){
            e.printStackTrace();
        }
        Intent intent = new Intent(KomodoXmppLibPluginService.GOT_ROSTER);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putExtra(KomodoXmppLibPluginService.DATA_READY, gson.toJson(list) );
        mApplicationContext.sendBroadcast(intent);
    }

    public void createChatGroup( String chatJid, String nickname, String  addUsers ) throws JSONException {
        Log.d(TAG, "CREATE CHAT GROUP" + chatJid);
        JSONArray names = new JSONArray(addUsers);
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(chatJid);
        } catch (XmppStringprepException e) {
            Log.d(TAG, "Failed to create jid");
            e.printStackTrace();
            return;
        }
        Resourcepart shroudedNickname = null;
        try {
            shroudedNickname = Resourcepart.from(nickname);
        } catch (XmppStringprepException e) {
            Log.d(TAG, "Failed to create nn for group");
            e.printStackTrace();
            return;
        }
        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(mConnection);
        MultiUserChat muc = manager.getMultiUserChat(jid);
        // Create the room and send an empty configuration form to make this an instant room
        try {
            muc.create(shroudedNickname); // .makeInstant();
            Form form = muc.getConfigurationForm().createAnswerForm();
            for (Iterator fields = form.getFields().iterator(); fields.hasNext(); ) {
                FormField field = (FormField) fields.next();
                if (!FormField.Type.hidden.equals(field.getType())
                        && field.getVariable() != null) {
                    form.setDefaultAnswer(field.getVariable());
                }
            }
            /*
            form.setAnswer("muc#roomconfig_roomdesc", nickname);
            form.setAnswer("muc#roomconfig_publicroom", false); //
            form.setAnswer("muc#roomconfig_persistentroom", true);
            form.setAnswer("muc#roomconfig_membersonly", false);
            form.setAnswer("muc#roomconfig_allowinvites", true);
            form.setAnswer("muc#roomconfig_enablelogging", true);

             */
            muc.sendConfigurationForm(form);
        } catch( java.lang.IllegalArgumentException e){
                Log.d("TAG", "field exception?");
                e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            Log.d(TAG, "Failed to create snn");
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            Log.d(TAG, "Failed to create snn");
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            Log.d(TAG, "Failed to create snn");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.d(TAG, "Failed to create snn");
            e.printStackTrace();
        } catch (MultiUserChatException.MucAlreadyJoinedException e) {
            Log.d(TAG, "Failed to create snn");
            e.printStackTrace();
        } catch (MultiUserChatException.MissingMucCreationAcknowledgeException e) {
            Log.d(TAG, "Failed to create snn");
            e.printStackTrace();
        } catch (MultiUserChatException.NotAMucServiceException e) {
            Log.d(TAG, "Failed to create snn");
            e.printStackTrace();
        }
        Log.d(TAG, "ADDING USERS TO LIST");
        for( int i = 0; i < names.length(); i++){
            Log.d(TAG, "Inviting -->" + names.get(i).toString());
            try {
                jid = JidCreate.entityBareFrom(names.get(i).toString());
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
            try {
                muc.invite(jid, "Join my group!");
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Completed operation of creating the group :)");
    }

    public void joinChatGroup(String chatJid, String displayUserAs){
        EntityBareJid jid = null;
        Resourcepart nickname;
        try {
            jid = JidCreate.entityBareFrom(chatJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return;
        }
        try {
            nickname = Resourcepart.from(displayUserAs);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return;
        }
        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(mConnection);
        MultiUserChat muc2 = manager.getMultiUserChat(jid);
        DiscussionHistory history = new DiscussionHistory();
        history.setMaxStanzas(0);
        try {
            muc2.join(nickname, null, history, SmackConfiguration.getDefaultPacketReplyTimeout());
            muc2.addMessageListener(new MessageListener() {
                @Override
                public void processMessage(Message message) {
                    if( message.getBody() == null){
                        Log.d(TAG, "Ignoring jump message (no body)");
                    }
                    Jid messageFrom = message.getFrom();

                    if(KomodoXmppLibPlugin.DEBUG) {
                        Log.d(TAG, message.getType().toString());
                        Log.d(TAG, "INCOMING GROUP MESSAGE :" + message.toString());
                        Log.d(TAG, "message.getBody() :" + message.getBody());
                        Log.d(TAG, "message.getFrom() :" + message.getFrom());
                    }
                    DelayInformation inf = null;
                    inf = (DelayInformation)message.getExtension(DelayInformation.ELEMENT,DelayInformation.NAMESPACE);
                    if (inf != null){
                        Date date = inf.getStamp();
                        Log.d(TAG,"date: "+date);
                    }

                    String from = message.getFrom().toString();
                    /*
                    // this is stripping the nickname from the messsage - annoying
                    String contactJid="";
                    if (from.contains("/")){
                        contactJid = from.split("/")[0];
                        if(KomodoXmppLibPlugin.DEBUG) {
                            Log.d(TAG, "The real jid is :" + contactJid);
                            Log.d(TAG, "The message is from :" + from);
                        }
                    }else {
                        contactJid = from;
                    }
*/
                    String id = message.getBody("id");

                    //Bundle up the intent and send the broadcast.
                    Intent intent = new Intent(KomodoXmppLibPluginService.RECEIVE_MESSAGE);
                    intent.putExtra(KomodoXmppLibPluginService.BUNDLE_FROM_JID,from);
                    intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_TYPE, message.getType().toString());
                    intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY, message.getBody());
                    intent.putExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS,id);
                    mApplicationContext.sendBroadcast(intent);
                    if(KomodoXmppLibPlugin.DEBUG) {
                        Log.d(TAG, "Received MUC from :" + from + " broadcast sent.");
                    }
                    ///ADDED

                }
            });
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MultiUserChatException.NotAMucServiceException e) {
            e.printStackTrace();
        }
    }
}
