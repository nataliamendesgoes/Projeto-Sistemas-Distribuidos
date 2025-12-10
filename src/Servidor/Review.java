package Servidor;

import java.util.List;
import java.util.Locale;
import Servidor.Usuarios;

import Conexao.conexao;
import Conexao.conexao;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.time.LocalDateTime; 
import java.time.format.DateTimeFormatter; 


public class Review {
	private String id;
	private String id_filme;
	private String nome_usuario;
	private String nota;
	private String descricao;
	private String editado;
	
public static boolean excluirReview(String idReview) {
        
        String sqlGetFilmeId = "SELECT id_filme FROM review WHERE id = ?";
        String sqlDelete = "DELETE FROM review WHERE id = ?";
        String sqlRecalcMedia = "SELECT AVG(nota) FROM review WHERE id_filme = ?";
        String sqlUpdateFilme = "UPDATE filme SET nota_media = ? WHERE id = ?";
        
        try (Connection c = conexao.getConexao()) {
            c.setAutoCommit(false); 

            try {
                int idRev = Integer.parseInt(idReview);
                int idFilme = -1;

                try (PreparedStatement psGet = c.prepareStatement(sqlGetFilmeId)) {
                    psGet.setInt(1, idRev);
                    try (ResultSet rs = psGet.executeQuery()) {
                        if (rs.next()) idFilme = rs.getInt("id_filme");
                        else return false; 
                    }
                }

                try (PreparedStatement psDel = c.prepareStatement(sqlDelete)) {
                    psDel.setInt(1, idRev);
                    psDel.executeUpdate();
                }

                double novaMedia = 0.0;
                try (PreparedStatement psAvg = c.prepareStatement(sqlRecalcMedia)) {
                    psAvg.setInt(1, idFilme);
                    try (ResultSet rs = psAvg.executeQuery()) {
                        if (rs.next()) novaMedia = rs.getDouble(1); 
                    }
                }
                
                try (PreparedStatement psUpd = c.prepareStatement(sqlUpdateFilme)) {
                    String mediaStr = String.format(Locale.US, "%.2f", novaMedia);
                    psUpd.setString(1, mediaStr);
                    psUpd.setInt(2, idFilme);
                    psUpd.executeUpdate();
                }

                c.commit(); 
                return true;

            } catch (Exception e) {
                if (c != null) c.rollback();
                return false;
            } finally {
                if (c != null) c.setAutoCommit(true);
            }
        } catch (Exception e) {
            return false;
        }
    }
        
    public static JsonArray listaReviewUsuario(String token) {
        try {
            String[] usuarioInfo = Usuarios.obterUsuarioPorToken(token);
            if (usuarioInfo == null) return null; 
            String nome = usuarioInfo[0]; 

            String sqlReview = "SELECT * FROM review WHERE nome_usuario = ?";
            JsonArray listaReviewUsuario = new JsonArray();

            try (Connection c = conexao.getConexao();
                 PreparedStatement psReview = c.prepareStatement(sqlReview)) {
                
                psReview.setString(1, nome);
                
                try (ResultSet rsReview = psReview.executeQuery()) {
                    while (rsReview.next()) {
                        JsonObject reviewObj = new JsonObject();
                        reviewObj.addProperty("id", String.valueOf(rsReview.getInt("id")));
                        reviewObj.addProperty("id_filme", String.valueOf(rsReview.getInt("id_filme")));
                        reviewObj.addProperty("nome_usuario", rsReview.getString("nome_usuario"));
                        
                        try { reviewObj.addProperty("titulo", rsReview.getString("titulo")); } catch (Exception e) {}
                        
                        reviewObj.addProperty("nota", rsReview.getString("nota"));
                        reviewObj.addProperty("descricao", rsReview.getString("descricao"));
                        
                        String dataStr = rsReview.getString("data");
                        reviewObj.addProperty("data", dataStr != null ? dataStr : "");

                        boolean editado = rsReview.getBoolean("editado");
                        reviewObj.addProperty("editado", String.valueOf(editado));

                        listaReviewUsuario.add(reviewObj);
                    }
                }
            }
            return listaReviewUsuario;
        } catch (SQLException e) {
            return null; 
        }
    }

    public static boolean atualizarReview(String id, String titulo, String descricao, String nota) {

        if (!validarDadosReview(nota, titulo, descricao)) return false;

        String sqlUpdateReview = "UPDATE review SET titulo = ?, descricao = ?, nota = ?, editado = true WHERE id = ?";
        String sqlGetFilmeId = "SELECT id_filme FROM review WHERE id = ?";
        String sqlRecalcMedia = "SELECT AVG(nota) FROM review WHERE id_filme = ?";
        String sqlUpdateFilme = "UPDATE filme SET nota_media = ? WHERE id = ?";
        
        try (Connection c = conexao.getConexao()) {
            c.setAutoCommit(false); 

            try {
                int idRev = Integer.parseInt(id);
                int idFilme = -1;

                
                try (PreparedStatement psGet = c.prepareStatement(sqlGetFilmeId)) {
                    psGet.setInt(1, idRev);
                    try (ResultSet rs = psGet.executeQuery()) {
                        if (rs.next()) idFilme = rs.getInt("id_filme");
                        else return false; 
                    }
                }

                // 2. Atualizar a Review
                try (PreparedStatement psUpdate = c.prepareStatement(sqlUpdateReview)) {
                    psUpdate.setString(1, titulo);
                    psUpdate.setString(2, descricao);
                    psUpdate.setString(3, nota); 
                    psUpdate.setInt(4, idRev); 
                    int r = psUpdate.executeUpdate();
                    if (r == 0) return false; 
                }

               
                double novaMedia = 0.0;
                try (PreparedStatement psAvg = c.prepareStatement(sqlRecalcMedia)) {
                    psAvg.setInt(1, idFilme);
                    try (ResultSet rs = psAvg.executeQuery()) {
                        if (rs.next()) novaMedia = rs.getDouble(1); 
                    }
                }
                
           
                try (PreparedStatement psUpd = c.prepareStatement(sqlUpdateFilme)) {
                    String mediaStr = String.format(Locale.US, "%.2f", novaMedia);
                    psUpd.setString(1, mediaStr);
                    psUpd.setInt(2, idFilme);
                    psUpd.executeUpdate();
                }

                c.commit(); 
                return true;

            } catch (Exception e) {
                if (c != null) c.rollback();
                return false;
            } finally { if (c != null) c.setAutoCommit(true); }
        } catch (Exception e) { return false; }
    }


    public static boolean criarReview(String id_filme, String titulo, String descricao, String nota, String token) {
        try {
            String[] usuarioInfo = Usuarios.obterUsuarioPorToken(token);
            if (usuarioInfo == null) return false; 
            String nomeUsuario = usuarioInfo[0]; 
            String funcaoUsuario = usuarioInfo[1];

            //if ("admin".equals(funcaoUsuario)) return false; 
            if (!validarDadosReview(nota, titulo, descricao)) return false;

            int idFilmeInt = Integer.parseInt(id_filme);

            //if (jaFezReview(idFilmeInt, nomeUsuario)) return false;

            String sqlGetMedia = "SELECT nota_media FROM filme WHERE id = ? FOR UPDATE"; 
            String sqlGetCount = "SELECT COUNT(*) FROM review WHERE id_filme = ?";
            String sqlInsertReview = "INSERT INTO review (id_filme, titulo, nome_usuario, nota, descricao, data, editado) VALUES (?, ?, ?, ?, ?, ?, ?)";
            String sqlUpdateFilme = "UPDATE filme SET nota_media = ? WHERE id = ?";
            
            try (Connection c = conexao.getConexao()) {
                c.setAutoCommit(false); 
                try {
                    double M = 0.0;
                    try (PreparedStatement psGetMedia = c.prepareStatement(sqlGetMedia)) {
                        psGetMedia.setInt(1, idFilmeInt);
                        try (ResultSet rs = psGetMedia.executeQuery()) {
                            if (rs.next()) {
                                String mediaStr = rs.getString("nota_media");
                                if (mediaStr != null && !mediaStr.isEmpty()) M = Double.parseDouble(mediaStr.replace(",", "."));
                            } else { return false; }
                        }
                    }

                    int n = 0;
                    try (PreparedStatement psGetCount = c.prepareStatement(sqlGetCount)) {
                        psGetCount.setInt(1, idFilmeInt);
                        try (ResultSet rs = psGetCount.executeQuery()) { if (rs.next()) n = rs.getInt(1); }
                    }

                    double x = Double.parseDouble(nota); 
                    double M_new = (M * n + x) / (n + 1);

                    try (PreparedStatement psReview = c.prepareStatement(sqlInsertReview)) {
                        psReview.setInt(1, idFilmeInt); 
                        psReview.setString(2, titulo);
                        psReview.setString(3, nomeUsuario); 
                        psReview.setString(4, nota);
                        psReview.setString(5, descricao);
                        String dataFormatada = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        psReview.setString(6, dataFormatada);
                        psReview.setBoolean(7, false); 
                        psReview.executeUpdate();
                    }

                    try (PreparedStatement psUpdate = c.prepareStatement(sqlUpdateFilme)) {
                        String novaMediaStr = String.format(Locale.US, "%.2f", M_new);
                        psUpdate.setString(1, novaMediaStr);
                        psUpdate.setInt(2, idFilmeInt);
                        psUpdate.executeUpdate();
                    }

                    c.commit(); return true;

                } catch (Exception e) { 
                    if (c != null) c.rollback(); return false; 
                } finally { if (c != null) c.setAutoCommit(true); }
            }
        } catch (Exception e) {
            return false;
        }
    }

    
    private static boolean validarDadosReview(String nota, String titulo, String descricao) {

        try {
            int n = Integer.parseInt(nota);
            if (n < 1 || n > 5) return false;
        } catch (NumberFormatException e) {
            return false; 
        }

        if (descricao == null || descricao.length() > 250) return false;

        if (titulo != null && !titulo.isEmpty()) {
            if (titulo.length() < 3 || titulo.length() > 30) return false;
        }
        
        return true;
    }
	
	public static boolean eDonoDaReview(String idReview, String nomeUsuario) throws SQLException {
	    String sql = "SELECT id FROM review WHERE id = ? AND nome_usuario = ?";
	    
	    try (Connection c = conexao.getConexao();
	         PreparedStatement ps = c.prepareStatement(sql)) {
	        
	        ps.setInt(1, Integer.parseInt(idReview));
	        ps.setString(2, nomeUsuario);
	        
	        try (ResultSet rs = ps.executeQuery()) {
	           
	            return rs.next(); 
	        }
	    } catch (NumberFormatException e) {
	        
	        return false; 
    }
}
	public static boolean jaFezReview(int idFilme, String nomeUsuario) throws SQLException {
        String sql = "SELECT id FROM review WHERE id_filme = ? AND nome_usuario = ?";
        try (Connection c = conexao.getConexao();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFilme);
            ps.setString(2, nomeUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); 
            }
        }
    }
}
