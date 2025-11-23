package demo.src;
import java.security.MessageDigest;
public class CriptoUtils {
    public static String hashSenha(String senha) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("SHA-256");
            byte messageDigest[] = algorithm.digest(senha.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(String.format("%02X", 0xFF & b));
            }
            return hexString.toString().toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }
}
