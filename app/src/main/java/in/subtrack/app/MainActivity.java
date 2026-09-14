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
    private static final int EXPORT=11, IMPORT=12, NOTIFY=13;
    static final String PREF="subtrack", DATA="data";
    static final int LIMIT=5*1024*1024;
    private final String origin="https://app.subtrack.local/";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web=new WebView(this);
        web.setBackgroundColor(0xfff5f7fa);
        setContentView(web);
        web.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets sys=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                v.setPadding(sys.left,sys.top,sys.right,sys.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        WebSettings w=web.getSettings();
        w.setJavaScriptEnabled(true);w.setDomStorageEnabled(false);
        w.setAllowFileAccess(false);w.setAllowContentAccess(false);
        w.setBlockNetworkLoads(true);w.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(),"Android");
        web.setWebViewClient(new WebViewClient(){
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
    void result(String text){runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.nativeResult("+JSONObject.quote(text)+")",null);});}
    class Bridge {
        @JavascriptInterface public String load(){return getSharedPreferences(PREF,0).getString(DATA,"");}
        @JavascriptInterface public boolean save(String raw){
            try{if(raw.getBytes(StandardCharsets.UTF_8).length>LIMIT)return false;JSONObject o=new JSONObject(raw);if(o.getInt("schema")!=2||o.getJSONArray("subscriptions").length()>10000)return false;
                boolean ok=getSharedPreferences(PREF,0).edit().putString(DATA,raw).commit();if(ok)ReminderService.schedule(MainActivity.this);return ok;
            }catch(Exception ex){return false;}
        }
        @JavascriptInterface public void whatsapp(String phone,String text){runOnUiThread(()->{
            if(!phone.matches("[1-9][0-9]{7,14}")||text.length()>10000){result("Invalid reminder details.");return;}
            Uri uri=Uri.parse("https://wa.me/"+phone+"?text="+Uri.encode(text));
            try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException ex){result("Install WhatsApp or a browser to open this reminder.");}
        });}
        @JavascriptInterface public void backup(){runOnUiThread(()->{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"My-Subscription-Manager-backup-"+new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(new java.util.Date())+".json");
            try{startActivityForResult(i,EXPORT);}catch(ActivityNotFoundException ex){result("No file picker found on this device.");}
        });}
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
    private void enableNotifications(){getSharedPreferences(PREF,0).edit().putBoolean("alerts",true).apply();ReminderService.schedule(this);ReminderService.check(this);result("Expiry alerts enabled on this phone.");}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==NOTIFY){if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)enableNotifications();else result("Notification permission denied. Countdown still works in the app.");}}
    @Override protected void onActivityResult(int request,int resultCode,Intent intent){
        super.onActivityResult(request,resultCode,intent);if(resultCode!=RESULT_OK||intent==null||intent.getData()==null)return;Uri uri=intent.getData();
        new Thread(()->{
            try{
                if(request==EXPORT){String raw=getSharedPreferences(PREF,0).getString(DATA,"{\"schema\":1,\"settings\":{\"business\":\"\",\"phonepe\":\"\",\"language\":\"hinglish\"},\"subscriptions\":[]}");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();out.write(raw.getBytes(StandardCharsets.UTF_8));}result("Backup saved successfully.");}
                else if(request==IMPORT){ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>LIMIT)throw new IOException("Backup exceeds 5 MB.");out.write(buf,0,n);}}String raw=new String(out.toByteArray(),StandardCharsets.UTF_8);runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.importBackup("+JSONObject.quote(raw)+")",null);});}
            }catch(Exception ex){result("Could not read or save the backup. Check file access and available space.");}
        }).start();
    }
    @Override public void onBackPressed(){if(web!=null)web.evaluateJavascript("window.closeSubtrackModal()",value->{if(!"true".equals(value))finish();});else super.onBackPressed();}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("Android");web.destroy();web=null;}super.onDestroy();}
}
