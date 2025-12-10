package Servidor;

import Conexao.conexao;
import java.sql.*;
import java.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.*;

public class Usuarios {

    public static boolean criarUsuario(String nome, String senha, String funcao) throws SQLException {
        String sql = "INSERT INTO usuarios (nome, senha, jwt_token, funcao) VALUES (?, ?, NULL, ?)";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, senha);
            ps.setString(3, funcao);
            ps.executeUpdate();
            return true;
        }
    }

    public static String[] obterSenhaEFuncao(String nome) throws SQLException {
        String sql = "SELECT senha, funcao FROM usuarios WHERE nome = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String senha = rs.getString("senha");
                    String funcao = rs.getString("funcao");
                    return new String[]{senha, funcao};
                } else {
                    return null;
                }
            }
        }
    }

    public static boolean setToken(String nome, String token) throws SQLException {
        String sql = "UPDATE usuarios SET jwt_token = ? WHERE nome = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, nome);
            int r = ps.executeUpdate();
            return r > 0;
        }
    }

    public static boolean clearToken(String token) throws SQLException {
        String sql = "UPDATE usuarios SET jwt_token = NULL WHERE jwt_token = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            int r = ps.executeUpdate();
            return r > 0;
        }
    }

    public static String[] obterUsuarioPorToken(String token) throws SQLException {
        String sql = "SELECT nome, funcao FROM usuarios WHERE jwt_token = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString("nome");
                    String funcao = rs.getString("funcao");
                    return new String[]{nome, funcao};
                } else return null;
            }
        }
    }

    public static boolean atualizarPorNome(String nome, String novaSenha, String novaFuncao) throws SQLException {
        String sel = "SELECT senha, funcao FROM usuarios WHERE nome = ?";
        String curSenha = null, curFuncao = null;
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    curSenha = rs.getString("senha");
                    curFuncao = rs.getString("funcao");
                } else {
                    return false;
                }
            }
        }

        if (novaSenha == null) novaSenha = curSenha;
        if (novaFuncao == null) novaFuncao = curFuncao;

        String upd = "UPDATE usuarios SET senha = ?, funcao = ? WHERE nome = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, novaSenha);
            ps.setString(2, novaFuncao);
            ps.setString(3, nome);
            int r = ps.executeUpdate();
            return r > 0;
        }
    }

    public static boolean excluirPorNome(String nome) throws SQLException {
        // 1. Obter ID do usuário (para garantir integridade)
        String idUsuario = obterIdPorNome(nome);
        if (idUsuario == null) return false;

        // 2. Buscar todas as Reviews desse usuário
        // Precisamos apagar uma por uma para garantir que a MÉDIA dos filmes seja recalculada
        // (O método Review.excluirReview já faz o recálculo)
        List<String> idsReviews = new ArrayList<>();
        String sqlGetReviews = "SELECT id FROM review WHERE nome_usuario = ?";
        
        try (Connection c = conexao.getConexao(); 
             PreparedStatement ps = c.prepareStatement(sqlGetReviews)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) idsReviews.add(String.valueOf(rs.getInt("id")));
            }
        }
        
        // 3. Apagar cada Review (Recalculando médias)
        for (String idReview : idsReviews) {
            Review.excluirReview(idReview);
        }

        // 4. Apagar o Usuário
        String del = "DELETE FROM usuarios WHERE nome = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(del)) {
            ps.setString(1, nome);
            int r = ps.executeUpdate();
            return r > 0;
        }
    }
    
    public static JsonArray listarUsuariosADM(String token) throws SQLException {

        String[] usuarioLogado = obterUsuarioPorToken(token);
        if (usuarioLogado == null || !"admin".equals(usuarioLogado[1])) {
            return null; 
        }
        String sql = "SELECT  id, nome FROM usuarios";
        JsonArray listaUsuarios = new JsonArray();

        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
             try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject usuario = new JsonObject();
                    usuario.addProperty("id", rs.getString("id"));
                    usuario.addProperty("nome", rs.getString("nome"));
                    listaUsuarios.add(usuario);
                }
             }
        }
        
        return listaUsuarios;
    }
    
    public static boolean excluirUsuarioADM(String id, String token)throws SQLException {
    	String[] usuarioLogado = obterUsuarioPorToken(token);
        if (usuarioLogado == null || !"admin".equals(usuarioLogado[1])) {
            return false; 
        }

        String nomeDoAdmin = usuarioLogado[0];
        String adminId = obterIdPorNome(nomeDoAdmin); 
        if (adminId != null && adminId.equals(id)) {
            return false; 
        }

        return excluirPorId(id);
    }
    public static boolean atualizarPorNomeADM(String id, String novaSenha, String novaFuncao, String token) throws SQLException {
        String[] usuarioLogado = obterUsuarioPorToken(token);
        if (usuarioLogado == null || !"admin".equals(usuarioLogado[1])) {
            return false; 
        }
       
        return atualizarPorId(id, novaSenha, novaFuncao);
    }
    public static boolean excluirPorId(String id) throws SQLException {
        // Precisa pegar o nome primeiro para apagar as reviews (que usam nome_usuario)
        String nome = null;
        String sqlGetNome = "SELECT nome FROM usuarios WHERE id = ?";
        try (Connection c = conexao.getConexao(); PreparedStatement ps = c.prepareStatement(sqlGetNome)) {
            ps.setInt(1, Integer.parseInt(id));
            try(ResultSet rs = ps.executeQuery()) {
                if(rs.next()) nome = rs.getString("nome");
            }
        }
        
        if (nome != null) {
            // Reutiliza a lógica de cascata do excluirPorNome
            return excluirPorNome(nome);
        }
        return false;
    }
    public static String obterIdPorNome(String nome) throws SQLException {
        String sql = "SELECT id FROM usuarios WHERE nome = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id"); 
                }
            }
        }
        return null; 
    }
    public static boolean atualizarPorId(String id, String novaSenha, String novaFuncao) throws SQLException {

        String sel = "SELECT senha, funcao FROM usuarios WHERE id = ?";
        String curSenha = null, curFuncao = null;
        
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sel)) {
           
            ps.setInt(1, Integer.parseInt(id)); 
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    curSenha = rs.getString("senha");
                    curFuncao = rs.getString("funcao");
                } else {
                    return false; 
                }
            }
        }

        if (novaSenha == null) novaSenha = curSenha;
        if (novaFuncao == null) novaFuncao = curFuncao;
        
        String upd = "UPDATE usuarios SET senha = ?, funcao = ? WHERE id = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, novaSenha);
            ps.setString(2, novaFuncao);
            ps.setInt(3, Integer.parseInt(id)); 
            int r = ps.executeUpdate();
            return r > 0;
        }
    }
    
    public static List<String> listarUsuariosLogados() throws SQLException {
        String sql = "SELECT nome FROM usuarios WHERE jwt_token IS NOT NULL";
        List<String> lista = new ArrayList<>();
        
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                lista.add(rs.getString("nome"));
            }
        }
        return lista;
    }

    public static void clearTokenByNome(String nome) {
        String sql = "UPDATE usuarios SET jwt_token = NULL WHERE nome = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, nome);
            ps.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    
}
