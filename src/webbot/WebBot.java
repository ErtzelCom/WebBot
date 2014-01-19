package WebBot;
        
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JComponent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;    

import org.json.JSONObject;
import org.json.JSONException;
/**
 *
 * @author Ertzel
 */

public class WebBot extends JFrame implements ActionListener {
    public static JTextArea trafficIn;
    public static JTextArea trafficOut;
    public static JTextArea trafficErr;
    private static WebSocketClient wsc;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");

    private final static String wsUri = "ws://chat.hitbox.tv:8000/chat";  // Main chat url
    private final static String wsUri2 = "ws://chat.hitbox.tv:8001/chat";  // Backup chat url
    public static String myToken = ""; // Leave this blank, it will be generated
    public final static String myName = "UserName"; // Replace with your bots username
    private final static String myPass = "Password"; // Replace with your bots password
    public final static String myColor = "2E2E2E"; // Replace with any color code you want for your bots text in the channel
            
    protected static JComponent makePanel(JTextArea ta) {
        JPanel panel = new JPanel(false);
        panel.setLayout(new GridLayout(1, 1));
        JScrollPane scroll = new JScrollPane (ta);
        panel.add(scroll);
        return panel;
    }
    
    public WebBot() {
        super( myName );
        Container c = getContentPane();
        GridLayout layout = new GridLayout();
        layout.setColumns( 1 );
        c.setLayout( layout );        
                       
        JTabbedPane tabbedMsg = new JTabbedPane();
        trafficIn = new JTextArea();
        trafficIn.setEditable(false);
        JComponent panel1 = makePanel(trafficIn);
        tabbedMsg.addTab("Received", panel1);

        trafficOut = new JTextArea();
        trafficOut.setEditable(false);
        JComponent panel2 = makePanel(trafficOut);  
        tabbedMsg.addTab("Sent", panel2);

        trafficErr = new JTextArea();
        trafficErr.setEditable(false);
        JComponent panel3 = makePanel(trafficErr);
        tabbedMsg.addTab("Errors", panel3);
        c.add(tabbedMsg);

        java.awt.Dimension d = new java.awt.Dimension( 900, 600 );
        setPreferredSize( d );
        setSize( d );

        addWindowListener( new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing( WindowEvent e ) {
                if( wsc != null ) {
                    wsc.close();
                    wsc = null;
                }
                dispose();
            }
        } );

        setLocationRelativeTo( null );
        setVisible( true );
    }
    
    @Override    
    public void actionPerformed( ActionEvent e ) {}
    
    public void createSocket(String url){
        try {
            wsc = new WebSocketClient(new URI(url)) {
                @Override
                public void onMessage( String message ) {
                    try{               
                        String curTime = sdf.format(new Date());
                        trafficIn.append( "[" + curTime + "] " + "Received: " + message + "\n" );
                        trafficIn.setCaretPosition( trafficIn.getDocument().getLength() );
                        JSONObject obj = new JSONObject(message);       
                        String param;
                        switch (obj.getString("method")) {
                            case "infoMsg":           
                                param = obj.toString().replace("{\"method\":\"infoMsg\",\"params\":", "");
                                param = param.substring(0,param.length()-1);
                                obj = new JSONObject(param);                                         
                                //obj.getString("channel")
                            break;    
                            case "chatMsg":           
                                param = obj.toString().replace("{\"method\":\"chatMsg\",\"params\":", "");
                                param = param.substring(0,param.length()-1);
                                obj = new JSONObject(param);                 
                                //obj.getString("channel"), obj.getString("name"), obj.getString("text"), obj.getString("role")
                            break;  
                        }
                    } catch(JSONException ex){               
                        String curTime = sdf.format(new Date());
                        trafficErr.append( "[" + curTime + "] " + "Exception: in onMessage " + ex.getMessage() + "\n" );
                        trafficErr.setCaretPosition( trafficErr.getDocument().getLength() );
                    }
                };        
                
                @Override
                public void onOpen( ServerHandshake handshake ) {
                    trafficIn.append( "You are connected to: " + getURI() + "\n" );
                    trafficIn.setCaretPosition( trafficIn.getDocument().getLength() ); 
                    joinChannel(myName, false);   
                };

                @Override
                public void onClose( int code, String reason, boolean remote ) {   
                    String curTime = sdf.format(new Date());
                    trafficIn.append( "[" + curTime + "] " + "You have been disconnected from: " + getURI() + "; Code: " + code + " " + reason + "\n" );
                    trafficIn.setCaretPosition( trafficIn.getDocument().getLength() );
                };

                @Override
                public void onError( Exception ex ) {               
                    String curTime = sdf.format(new Date());
                    trafficErr.append( "[" + curTime + "] " + "Exception: in onError " + ex.getMessage() + "\n" );
                    trafficErr.setCaretPosition( trafficErr.getDocument().getLength() );                    
                    wsc = null;
                    createSocket(wsUri);  
                };
            };

            wsc.connect();
        } catch ( URISyntaxException ex ) {                        
            String curTime = sdf.format(new Date());
            trafficErr.append( "[" + curTime + "] " + "Exception: in createSocket " + ex.getMessage() + "\n" );
            trafficErr.setCaretPosition( trafficErr.getDocument().getLength() );
        }       
    };
    
    public static void sendMsg(final String message, final int waitTime) {   
        try{               
            // Make sure the message isn't blank
            if (!message.equals("undefined") && !message.equals("")){      
                // Send message on a timer to avoid flood protection blocks
                Timer msgTimer = new Timer();
                msgTimer.schedule( new TimerTask() {
                    @Override
                    public void run() {            
                        String curTime = sdf.format(new Date());
                        trafficOut.append( "[" + curTime + "] " + "Sending: " + message + "\n" );
                        trafficOut.setCaretPosition( trafficOut.getDocument().getLength() );
                        wsc.send(message);          
                    }
                }, waitTime);                
            }
       } catch ( Exception ex ) {
           trafficErr.append( "Exception: in sendMsg " + ex.getMessage() + "\n" );
           trafficErr.setCaretPosition( trafficErr.getDocument().getLength() );
       }     
    };   
    
    
    ///Start of Bot Scripts //   
    public void joinChannel(String chanName, Boolean showMsg){  
        if (!chanName.equals("undefined") && !chanName.equals("")){     
            trafficIn.append("Joined Channel: " + chanName + "\n");
            trafficIn.setCaretPosition( trafficIn.getDocument().getLength() ); 
            if (chanName.equals(myName)) {
                String json = "{"
                    + "\"method\":\"joinChannel\","
                    + "\"params\":{"
                        + "\"channel\":\"" + chanName + "\","
                        + "\"name\":\"" + myName + "\","
                        + "\"token\":\"" + myToken + "\","
                        + "\"isAdmin\":true"
                    + "}"
                + "}";
                sendMsg(json, 0);               
            } else {
                String json = "{"
                    + "\"method\":\"joinChannel\","
                    + "\"params\":{"
                        + "\"channel\":\"" + chanName + "\","
                        + "\"name\":\"" + myName + "\","
                        + "\"token\":\"" + myToken + "\","
                        + "\"isAdmin\":false"
                    + "}"
                + "}";
                sendMsg(json, 0);                   
            }

            if (showMsg == true){             
                String json = "{"
                    + "\"method\":\"chatMsg\","
                    + "\"params\":{"
                        + "\"channel\":\"" + chanName + "\","
                        + "\"name\":\"" + myName + "\","
                        + "\"nameColor\":\"" + myColor + "\","
                        + "\"text\":\"I have joined the channel!\""
                    + "}"
                + "}";
                sendMsg(json, 2000);
            }
        }
    };
    
    public void leaveChannel(String chanName, int chanID){
        String json = "{"
            + "\"method\":\"chatMsg\","
            + "\"params\":{"
                + "\"channel\":\"" + chanName + "\","
                + "\"name\":\"" + myName + "\","
                + "\"nameColor\":\"" + myColor + "\","
                + "\"text\":\"Bye bye!\"" 
            + "}"
        + "}";
        sendMsg(json, 1000);

        json = "{"
            + "\"method\":\"partChannel\","
            + "\"params\":{"
                + "\"channel\":\"" + chanName + "\","
                + "\"name\":\"" + myName + "\""
            + "}"
        + "}";          
        sendMsg(json, 4000);     
           
        trafficIn.append("Left Channel: " + chanName + "\n");
        trafficIn.setCaretPosition( trafficIn.getDocument().getLength() ); 
    };
    ///End of Bot Scripts //   
    
    public static void main( String[] args ) {
        try {
            // Get user Auth
            URL url = new URL ("http://api.hitbox.tv/auth/token");
            URLConnection urlConn = url.openConnection();
            urlConn.setDoInput (true);
            urlConn.setDoOutput (true);
            urlConn.setUseCaches (false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ())) {
                String content = "login=" + myName + "&pass=" + myPass;
                printout.writeBytes (content);
                printout.flush ();
            } 
            try (DataInputStream input = new DataInputStream (urlConn.getInputStream ())) {
                String str;
                while (null != ((str = input.readLine())))
                    myToken = str.replace("{\"authToken\":\"", "").replace("\"}", "");
            }
        } catch (IOException ex) { System.out.println( "IOException: " + ex.getMessage()); }             
        
        // Make sure AuthToken was generated
        if (!myToken.equals("")){
            new WebBot().createSocket(wsUri);            
        } else {
           JOptionPane.showMessageDialog(null,
            "Unable to generate AuthToken!",
            "Error",
            JOptionPane.ERROR_MESSAGE); 
        }
    }   
}
