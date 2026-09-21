package in.subtrack.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class BackupProvider extends ContentProvider {
    private static final String DIR="telegram-backups";

    @Override public boolean onCreate(){return true;}

    @Override public String getType(Uri uri){return "application/json";}

    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){
        try{
            File file=resolve(uri);
            String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor cursor=new MatrixCursor(columns,1);
            Object[] row=new Object[columns.length];
            for(int i=0;i<columns.length;i++){
                if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]=file.getName();
                else if(OpenableColumns.SIZE.equals(columns[i]))row[i]=file.length();
                else row[i]=null;
            }
            cursor.addRow(row);
            return cursor;
        }catch(FileNotFoundException ex){return null;}
    }

    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("Read only");}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs){throw new UnsupportedOperationException("Read only");}
    @Override public int delete(Uri uri,String selection,String[] selectionArgs){throw new UnsupportedOperationException("Read only");}

    private File resolve(Uri uri)throws FileNotFoundException{
        if(getContext()==null)throw new FileNotFoundException("Provider unavailable");
        if(!"content".equals(uri.getScheme())||!(getContext().getPackageName()+".backup").equals(uri.getAuthority()))throw new FileNotFoundException("Invalid backup URI");
        List<String> parts=uri.getPathSegments();
        if(parts.size()!=1)throw new FileNotFoundException("Invalid backup path");
        String name=parts.get(0);
        if(!name.matches("[A-Za-z0-9._-]+\\.json"))throw new FileNotFoundException("Invalid backup name");
        File dir=new File(getContext().getCacheDir(),DIR);
        File file=new File(dir,name);
        try{
            File canonicalDir=dir.getCanonicalFile(),canonicalFile=file.getCanonicalFile();
            if(canonicalFile.getParentFile()==null||!canonicalFile.getParentFile().equals(canonicalDir))throw new FileNotFoundException("Invalid backup path");
        }catch(IOException ex){throw new FileNotFoundException("Invalid backup path");}
        if(!file.isFile())throw new FileNotFoundException("Backup not found");
        return file;
    }
}
