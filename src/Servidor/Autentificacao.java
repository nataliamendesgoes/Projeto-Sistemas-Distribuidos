package Servidor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys; 

// --- REMOVIDO: Imports do Gson ---
// import io.jsonwebtoken.gson.io.GsonSerializer; 
// import io.jsonwebtoken.gson.io.GsonDeserializer;
// import com.google.gson.Gson; 
// --- FIM DA REMOÇÃO ---

import java.nio.charset.StandardCharsets; 
import javax.crypto.SecretKey; 
import java.util.Date;

public class Autentificacao {

   
    private static final String CHAVE_SECRETA_STRING = "chaveSeguraParaJWT_sisDistribuidos2025";

    
    private static final SecretKey CHAVE = Keys.hmacShaKeyFor(CHAVE_SECRETA_STRING.getBytes(StandardCharsets.UTF_8));
    
    private static final long TEMPO_EXPIRACAO = 60 * 60 * 1000;

    public static String gerarToken(String nomeUsuario, String funcao) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + TEMPO_EXPIRACAO);

        return Jwts.builder()
               
                .setSubject(nomeUsuario)
                .claim("funcao", funcao)
                .setIssuedAt(agora)
                .setExpiration(expiracao)
                .signWith(CHAVE, SignatureAlgorithm.HS256)
                .compact();
    }

   
    public static Claims validarToken(String token) {
        try {
            return Jwts.parserBuilder()
                    
                    .setSigningKey(CHAVE) 
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.out.println("Token inválido ou expirado: " + e.getMessage());
            return null;
        }
    }

    public static boolean validarUsuario(String token, String nomeUsuario) {
        Claims claims = validarToken(token);
        if (claims == null) return false;
        return claims.getSubject().equals(nomeUsuario);
    }

    
    public static String getNomeUsuario(String token) {
        Claims claims = validarToken(token);
        return (claims != null) ? claims.getSubject() : null;
    }

    public static String getFuncao(String token) {
        Claims claims = validarToken(token);
        return (claims != null) ? (String) claims.get("funcao") : null;
    }
}
