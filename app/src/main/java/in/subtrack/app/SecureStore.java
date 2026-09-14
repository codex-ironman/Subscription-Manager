package in.subtrack.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureStore {
    private static final String ALIAS="subscription-manager-local-data-v1";
    private static synchronized SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return generator.generateKey();
    }
    static String read(Context c) throws Exception {
        String raw=c.getSharedPreferences(MainActivity.PREF,0).getString(MainActivity.DATA,"");
        if(raw.isEmpty())return raw;
        if(!raw.startsWith("enc1:")){
            // Preserve the legacy record if encryption fails; the caller reports the error.
            if(!write(c,raw))throw new java.io.IOException("Could not migrate saved data");
            return raw;
        }
        String[] parts=raw.split(":",3);if(parts.length!=3)throw new java.io.IOException("Invalid encrypted data");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[1],Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(parts[2],Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    static boolean write(Context c,String raw) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        String encrypted="enc1:"+Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        return c.getSharedPreferences(MainActivity.PREF,0).edit().putString(MainActivity.DATA,encrypted).commit();
    }
}
