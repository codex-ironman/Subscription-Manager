package in.subtrack.app;

import android.Manifest;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;
import java.util.*;

public class ReminderService extends JobService {
    private static final String CHANNEL="subscription_expiry";
    static void schedule(Context c){
        if(!c.getSharedPreferences(MainActivity.PREF,0).getBoolean("alerts",false))return;
        JobScheduler s=c.getSystemService(JobScheduler.class);
        if(s.getPendingJob(1701)==null)s.schedule(new JobInfo.Builder(1701,new ComponentName(c,ReminderService.class)).setPeriodic(60*60*1000L).setPersisted(true).build());
    }
    static void check(Context c){
        android.content.SharedPreferences p=c.getSharedPreferences(MainActivity.PREF,0);
        if(!p.getBoolean("alerts",false))return;
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=c.getSystemService(NotificationManager.class);if(!nm.areNotificationsEnabled())return;
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Subscription expiry alerts",NotificationManager.IMPORTANCE_DEFAULT));
        try{
            JSONArray rows=new JSONObject(SecureStore.read(c)).optJSONArray("subscriptions");if(rows==null)return;
            long now=System.currentTimeMillis();String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(now));
            Set<String> previous=p.getStringSet("notified",new HashSet<>()),next=new HashSet<>();int due=0,expired=0;
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.getJSONObject(i);long expiry=r.getLong("expiry");
                if(!r.isNull("stoppedAt")||r.getLong("start")>now||expiry-now>3*86400000L)continue;
                String key=today+":"+r.getString("id")+":"+expiry+":"+(expiry<=now?"expired":"due");
                next.add(key);if(previous.contains(key))continue;
                if(expiry<=now)expired++;else due++;
            }
            if(due+expired>0){
                Intent launch=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pi=PendingIntent.getActivity(c,0,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                String summary=(expired>0?expired+" expired":"")+(due>0?(expired>0?" · ":"")+due+" expiring within 3 days":"");
                Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Subscription reminders").setContentText(summary+". Tap to review and send reminders.").setContentIntent(pi).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_REMINDER).build();
                nm.notify(1702,n);
            }
            p.edit().putStringSet("notified",next).apply();
        }catch(Exception ignored){}
    }
    @Override public boolean onStartJob(JobParameters params){new Thread(()->{check(this);jobFinished(params,false);}).start();return true;}
    @Override public boolean onStopJob(JobParameters params){return true;}
}

