package in.subtrack.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.view.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingExport;
    private static final int EXPORT=11, IMPORT=12, NOTIFY=13;
    private boolean startupBackupPromptShown;
    static final String PREF="subtrack", DATA="data";
    static final int LIMIT=5*1024*1024;
    private final String origin="https://app.subtrack.local/";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        startupBackupPromptShown=state!=null;
        web=new WebView(this);
        web.setBackgroundColor(0xfff5f7fa);
        android.widget.FrameLayout root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xfff5f7fa);
        root.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v,insets)->{
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                // Inset the WebView's containing layout. WebView padding does not reliably
                // constrain fixed-position HTML content away from the system status bar.
                v.setPadding(safe.left,safe.top,safe.right,safe.bottom);
                return WindowInsets.CONSUMED;
            });
        }else root.setFitsSystemWindows(true);
        setContentView(root);
        root.requestApplyInsets();
        WebSettings w=web.getSettings();
        w.setJavaScriptEnabled(true);w.setDomStorageEnabled(false);
        w.setAllowFileAccess(false);w.setAllowContentAccess(false);
        w.setBlockNetworkLoads(true);w.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(),"Android");
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                super.onPageFinished(view,url);
                if(origin.equals(url))view.postDelayed(()->showStartupBackupPrompt(),250);
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){
                try {
                    if(req.getUrl().toString().equals(origin))return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));
                }catch(IOException ignored){}
                return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){return !req.getUrl().toString().equals(origin);}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsAlert(WebView v,String url,String message,JsResult result){
                new AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("OK",(d,b)->result.confirm()).setOnCancelListener(d->result.cancel()).show();return true;
            }
            @Override public boolean onJsConfirm(WebView v,String url,String message,JsResult result){
                new AlertDialog.Builder(MainActivity.this).setTitle("My Subscription Manager").setMessage(message).setPositiveButton("Continue",(d,b)->result.confirm()).setNegativeButton("Cancel",(d,b)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;
            }
            @Override public boolean onJsPrompt(WebView v,String url,String message,String defaultValue,JsPromptResult result){
                android.widget.EditText input=new android.widget.EditText(MainActivity.this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(defaultValue);input.setSelectAllOnFocus(true);
                int pad=(int)(24*getResources().getDisplayMetrics().density);android.widget.LinearLayout box=new android.widget.LinearLayout(MainActivity.this);box.setPadding(pad,0,pad,0);box.addView(input,new android.widget.LinearLayout.LayoutParams(-1,-2));
                new AlertDialog.Builder(MainActivity.this).setTitle("Renew subscription").setMessage(message).setView(box).setPositiveButton("Renew",(d,b)->result.confirm(input.getText().toString())).setNegativeButton("Cancel",(d,b)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;
            }
        });
        web.loadUrl(origin);
        ReminderService.schedule(this);
    }
    private void showStartupBackupPrompt(){
        if(startupBackupPromptShown||isFinishing()||web==null)return;
        startupBackupPromptShown=true;
        new AlertDialog.Builder(this)
            .setTitle("Backup your subscriptions")
            .setMessage("Create a password-encrypted backup and send the JSON file to Telegram now?")
            .setPositiveButton("Backup to Telegram",(d,b)->{if(web!=null)web.evaluateJavascript("window.startTelegramBackup()",null);})
            .setNegativeButton("Later",null)
            .show();
    }
    void result(String text){runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.nativeResult("+JSONObject.quote(text)+")",null);});}
    class Bridge {
        @JavascriptInterface public String load(){try{return SecureStore.read(MainActivity.this);}catch(Exception ex){return "unreadable-saved-data";}}
        @JavascriptInterface public boolean save(String raw){
            try{if(raw.getBytes(StandardCharsets.UTF_8).length>LIMIT)return false;JSONObject o=new JSONObject(raw);if(o.getInt("schema")!=4||o.getJSONArray("subscriptions").length()>10000)return false;
                boolean ok=SecureStore.write(MainActivity.this,raw);if(ok)ReminderService.schedule(MainActivity.this);return ok;
            }catch(Exception ex){return false;}
        }
        @JavascriptInterface public void whatsapp(String phone,String text){runOnUiThread(()->{
            if(!phone.matches("[1-9][0-9]{7,14}")||text.length()>10000){result("Invalid reminder details.");return;}
            Uri uri=Uri.parse("https://wa.me/"+phone+"?text="+Uri.encode(text));
            try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException ex){result("Install WhatsApp or a browser to open this reminder.");}
        });}
        @JavascriptInterface public void backup(String encryptedBackup){
            try{JSONObject envelope=new JSONObject(encryptedBackup);if(!"MSM-ENCRYPTED".equals(envelope.getString("format"))||encryptedBackup.getBytes(StandardCharsets.UTF_8).length>LIMIT*2){result("Invalid backup.");return;}}catch(Exception ex){result("Invalid backup.");return;}
            runOnUiThread(()->{
            pendingExport=encryptedBackup;
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"My-Subscription-Manager-backup-"+new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(new java.util.Date())+".json");
            try{startActivityForResult(i,EXPORT);}catch(ActivityNotFoundException ex){result("No file picker found on this device.");}
        });}
        @JavascriptInterface public void telegramBackup(String encryptedBackup){
            try{JSONObject envelope=new JSONObject(encryptedBackup);if(!"MSM-ENCRYPTED".equals(envelope.getString("format"))||encryptedBackup.getBytes(StandardCharsets.UTF_8).length>LIMIT*2){result("Invalid backup.");return;}}catch(Exception ex){result("Invalid backup.");return;}
            shareTelegramBackup(encryptedBackup);
        }
        @JavascriptInterface public void restore(){runOnUiThread(()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            try{startActivityForResult(i,IMPORT);}catch(ActivityNotFoundException ex){result("No file picker found on this device.");}
        });}
        @JavascriptInterface public void enableAlerts(){runOnUiThread(()->{
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY);return;}
            NotificationManager nm=getSystemService(NotificationManager.class);
            if(!nm.areNotificationsEnabled()){startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));result("Allow notifications in Android settings, then tap Enable again.");return;}
            enableNotifications();
        });}
    }
    private void shareTelegramBackup(String encryptedBackup){
        new Thread(()->{
            try{
                File dir=new File(getCacheDir(),"telegram-backups");
                if(!dir.exists()&&!dir.mkdirs())throw new IOException("Could not create backup folder.");
                String name="My-Subscription-Manager-backup-"+new java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss",java.util.Locale.US).format(new java.util.Date())+".json";
                File file=new File(dir,name);
                try(OutputStream out=new FileOutputStream(file)){out.write(encryptedBackup.getBytes(StandardCharsets.UTF_8));}
                Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".backup").appendPath(name).build();
                Intent send=new Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,uri).putExtra(Intent.EXTRA_SUBJECT,"My Subscription Manager backup").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                send.setClipData(ClipData.newRawUri("Encrypted subscription backup",uri));
                runOnUiThread(()->{
                    if(startShare(send,"org.telegram.messenger"))return;
                    if(startShare(send,"org.telegram.messenger.web"))return;
                    try{startActivity(Intent.createChooser(new Intent(send).setPackage(null),"Send encrypted backup"));}catch(ActivityNotFoundException ex){result("No compatible sharing app found.");}
                });
            }catch(Exception ex){result("Could not prepare the Telegram backup file.");}
        }).start();
    }
    private boolean startShare(Intent base,String packageName){
        try{startActivity(new Intent(base).setPackage(packageName));return true;}catch(ActivityNotFoundException|SecurityException ex){return false;}
    }
    private void enableNotifications(){getSharedPreferences(PREF,0).edit().putBoolean("alerts",true).apply();ReminderService.schedule(this);ReminderService.check(this);result("Expiry alerts enabled on this phone.");}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==NOTIFY){if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)enableNotifications();else result("Notification permission denied. Countdown still works in the app.");}}
    @Override protected void onActivityResult(int request,int resultCode,Intent intent){
        super.onActivityResult(request,resultCode,intent);if(resultCode!=RESULT_OK||intent==null||intent.getData()==null)return;Uri uri=intent.getData();
        new Thread(()->{
            try{
                if(request==EXPORT){String raw=pendingExport;if(raw==null)throw new IOException("Export interrupted; please export again.");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();out.write(raw.getBytes(StandardCharsets.UTF_8));}pendingExport=null;result("Encrypted backup saved successfully.");}
                else if(request==IMPORT){ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>LIMIT*2)throw new IOException("Backup is too large.");out.write(buf,0,n);}}String raw=new String(out.toByteArray(),StandardCharsets.UTF_8);runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.importBackup("+JSONObject.quote(raw)+")",null);});}
            }catch(Exception ex){result("Could not read or save the backup. Check file access and available space.");}
        }).start();
    }
    @Override public void onBackPressed(){if(web!=null)web.evaluateJavascript("window.closeSubtrackModal()",value->{if(!"true".equals(value))finish();});else super.onBackPressed();}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("Android");web.destroy();web=null;}super.onDestroy();}
}

