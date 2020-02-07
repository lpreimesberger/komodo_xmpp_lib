package com.capricallctx.komodo_xmpp_lib;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

public class KomodoConnection implements ConnectionListener {
    private static final String TAG ="flutter_xmpp";
    private Context mApplicationContext;
    private String username;
    private String password;
    private String serviceName;
    private String resource;
    private String host;
    private Integer post;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;


    public enum ConnectionState{
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED;
    }

    public  enum LoggedInState{
        LOGGED_IN , LOGGED_OUT;
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
                resource = "Android";
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
//        conf.setPort(0);

        conf.setUsernameAndPassword(username, password);
        conf.setResource(resource);
        conf.setKeystoreType(null);
        conf.setDebuggerEnabled(true);
//        conf.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        conf.setCompressionEnabled(true);

        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "Username : " + username);
            Log.d(TAG, "Password : " + password);
            Log.d(TAG, "Server : " + serviceName);
            Log.d(TAG, "Port : " + post.toString());

        }


        //Set up the ui thread broadcast message receiver.

        mConnection = new XMPPTCPConnection(conf.build());
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
            e.printStackTrace();
        }

        setupUiThreadBroadCastMessageReceiver();

        ChatManager.getInstanceFor(mConnection).addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid messageFrom, Message message, Chat chat) {
                if(KomodoXmppLibPlugin.DEBUG) {

                    Log.d(TAG, "INCOMING :" + message.toString());

                    ///ADDED
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
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(KomodoXmppLibPluginService.BUNDLE_FROM_JID,contactJid);
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


        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }


    private void setupUiThreadBroadCastMessageReceiver()
    {
        Log.d(TAG,"setupUiThreadBroadCastMessageReceiver");

        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                Log.d(TAG,"broadcast " + action);

                if( action.equals(KomodoXmppLibPluginService.SEND_MESSAGE)) {
                    //Send the message.
                    sendMessage(intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_BODY),
                            intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_TO),
                            intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS));

                }else  if( action.equals(KomodoXmppLibPluginService.READ_MESSAGE)) {
                    //Send the message.
                    sendRead(intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_TO),
                            intent.getStringExtra(KomodoXmppLibPluginService.BUNDLE_MESSAGE_PARAMS));

                }else  if( action.equals(KomodoXmppLibPluginService.GROUP_SEND_MESSAGE)) {
                    //Send group message.
                    sendGroupMessage(intent.getStringExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_BODY),
                            intent.getStringExtra(KomodoXmppLibPluginService.GROUP_TO),
                            intent.getStringExtra(KomodoXmppLibPluginService.GROUP_MESSAGE_PARAMS));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(KomodoXmppLibPluginService.SEND_MESSAGE);
        filter.addAction(KomodoXmppLibPluginService.READ_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver,filter);

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
//        Log.d(TAG,"Sending group message to :"+ toRoom);
//        EntityBareJid jid = null;
//        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
//
//         try {
//             MultiUserChat muc = chatManager.("test2@conference.cca");
//
//             Message message = new Message(toRoom, Message.Type.groupchat());
//            message.setBody(body);
//            message.addBody("id", id);
//            message.setType(Message.Type.groupchat);
//            message.setTo(toRoom);
//            MultiUserChat.se(message);
//
//        } catch (SmackException.NotConnectedException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
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
            Log.d(TAG, "Disconnecting from serser " + serviceName);
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


}
