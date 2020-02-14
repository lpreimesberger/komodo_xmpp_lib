package com.capricallctx.komodo_xmpp_lib;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import java.io.IOException;

public class KomodoXmppLibPluginService extends Service {
    public static final String UPDATED_MY_VCARD = "com.capricallctx.komodo_xmpp_lib.GOT_MY_VCARD";;
    public static final String UPDATED_USER_VCARD = "com.capricallctx.komodo_xmpp_lib.GOT_USER_VCARD";;
    private static final String TAG ="flutter_xmpp";
    public static final String READ_MESSAGE     = "com.capricallctx.komodo_xmpp_lib.readmessage";
    public static final String SEND_MESSAGE     = "com.capricallctx.komodo_xmpp_lib.sendmessage";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_MESSAGE_PARAMS = "b_body_params";
    public static final String BUNDLE_TO            = "b_to";
    public static final String OUTGOING_MESSAGE     = "com.capricallctx.komodo_xmpp_lib.outgoingmessage";
    public static final String BUNDLE_TO_JID        = "c_from";
    public static final String GROUP_SEND_MESSAGE   = "com.capricallctx.komodo_xmpp_lib.sendGroupMessage";
    public static final String GROUP_MESSAGE_BODY   = "group_body";
    public static final String GROUP_MESSAGE_PARAMS = "group_body_params";
    public static final String GROUP_TO             = "group_to";
    public static final String RECEIVE_MESSAGE      = "com.capricallctx.komodo_xmpp_lib.receivemessage";
    public static final String BUNDLE_FROM_JID      = "b_from";
    public static final String GET_MY_VCARD         = "com.capricallctx.komodo_xmpp_lib.GET_MY_VCARD";
    public static final String SET_MY_VCARD         = "com.capricallctx.komodo_xmpp_lib.SET_MY_VCARD";
    public static final String SET_MY_VCARD_DATA    = "user_vcard_jid";
    public static final String GET_USER_VCARD       = "com.capricallctx.komodo_xmpp_lib.GET_USER_VCARD";
    public static final String GET_USER_VCARD_JID   = "user_vcard_jid";
    public static final String GOT_USER_VCARD       = "com.capricallctx.komodo_xmpp_lib.GOT_USER_VCARD";
    public static final String GOT_MY_VCARD       = "com.capricallctx.komodo_xmpp_lib.GOT_MY_VCARD";
    public static final String GET_ROSTER         = "com.capricallctx.komodo_xmpp_lib.GET_ROSTER";
    public static final String GOT_ROSTER       = "com.capricallctx.komodo_xmpp_lib.GOT_ROSTER";
    // data back messages
    public static final String DATA_READY       = "com.capricallctx.komodo_xmpp_lib.DATA_READY";
    // presence
    public static final String PRESENCE_ONLINE       = "PRESENCE_ONLINE";
    public static final String PRESENCE_LONELY       = "PRESENCE_LONELY";
    public static final String PRESENCE_BUSY         = "PRESENCE_BUSY";
    public static final String PRESENCE_DND          = "PRESENCE_DND";
    public static final String PRESENCE_AWAY         = "PRESENCE_AWAY";
    public static final String PRESENCE_STEALTH      = "PRESENCE_STEALTH";
    public static final String PRESENCE_OFFLINE      = "PRESENCE_OFFLINE";
    public static KomodoConnection.ConnectionState sConnectionState;
    public static KomodoConnection.LoggedInState sLoggedInState;
    private boolean mActive;
    private Thread mThread;
    private Handler mTHandler;
    private KomodoConnection mConnection;
    private String jid_user = "";
    private String password = "";
    private String host = "";
    private Integer port;


    public KomodoXmppLibPluginService() {
    }

    public static KomodoConnection.ConnectionState getState() {
        if (sConnectionState == null) {
            return KomodoConnection.ConnectionState.DISCONNECTED;
        }
        return sConnectionState;
    }

    public static KomodoConnection.LoggedInState getLoggedInState() {
        if (sLoggedInState == null) {
            return KomodoConnection.LoggedInState.LOGGED_OUT;
        }
        return sLoggedInState;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "onCreate()");
        }
    }

    private void initConnection() {
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "initConnection()");
        }
        if( mConnection == null) {
            mConnection = new KomodoConnection(this,this.jid_user,this.password,this.host,this.port);
        }
        try {
            mConnection.connect();
        }catch (IOException | SmackException | XMPPException e) {
            if(KomodoXmppLibPlugin.DEBUG) {
                Log.d(TAG, "Something went wrong while connecting ,make sure the credentials are right and try again");
            }
            e.printStackTrace();
            stopSelf();
        }
    }

    public void start() {
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, " xmpp pump started.");
        }
        if(!mActive) {
            mActive = true;
            if( mThread ==null || !mThread.isAlive()) {
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        mTHandler = new Handler();
                        initConnection();
                        Looper.loop();
                    }
                });
                mThread.start();
            }
        }
    }

    public void stop() {
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "xmpp pump stopped.");
        }
        mActive = false;
        mTHandler.post(new Runnable() {
            @Override
            public void run() {
                if( mConnection != null) {
                    mConnection.disconnect();
                }
            }
        });
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "onStartCommand()");
        }
        Bundle extras = intent.getExtras();

        if(extras == null) {
            if(KomodoXmppLibPlugin.DEBUG) {
                Log.d(TAG, "Missing User JID/Password/Host/Port");
            }
        } else {
            this.jid_user = (String) extras.get("jid_user");
            this.password = (String) extras.get("password");
            this.host = (String) extras.get("host");
            this.port = (Integer) extras.get("port");
        }
        start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(KomodoXmppLibPlugin.DEBUG) {
            Log.d(TAG, "onDestroy()");
        }
        super.onDestroy();
        stop();
    }
}
