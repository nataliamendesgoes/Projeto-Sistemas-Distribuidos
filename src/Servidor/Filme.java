package Servidor;

import Conexao.conexao;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale; 
import Servidor.Usuarios;

public class Filme {
	private String id;
	private String titulo;
	private String ano;
	private List <FilmeGenero> generos;
	private String sinopse;
	private List <Review> reviews;
	
	public static boolean criarFilme(String titulo, String diretor, String ano, String sinopse, List<Integer> generosIDs) throws SQLException {
	    String sqlFilme = "INSERT INTO filme (titulo, diretor, ano, sinopse, nota_media) VALUES (?, ?, ?, ?, 0)";
	    String sqlGenero = "INSERT INTO filme_genero (filme_id, genero_id) VALUES (?, ?)";
	    
	    Connection c = null;

	    try {
	        c = conexao.getConexao();
	        c.setAutoCommit(false);
	        long filmeId = -1;

	        try (PreparedStatement psFilme = c.prepareStatement(sqlFilme, Statement.RETURN_GENERATED_KEYS)) {
	            psFilme.setString(1, titulo);
	            psFilme.setString(2, diretor);
	            psFilme.setString(3, ano);
	            psFilme.setString(4, sinopse);
	            psFilme.executeUpdate();

	            try (ResultSet rsKeys = psFilme.getGeneratedKeys()) {
	                if (rsKeys.next()) filmeId = rsKeys.getLong(1);
	            }
	        }

	        if (filmeId > 0 && generosIDs != null && !generosIDs.isEmpty()) {
	            try (PreparedStatement psGenero = c.prepareStatement(sqlGenero)) {
	                for (Integer generoId : generosIDs) {
	                    psGenero.setLong(1, filmeId);
	                    psGenero.setInt(2, generoId);
	                    psGenero.addBatch();
	                }
	                psGenero.executeBatch();
	            }
	        }

	        c.commit();
	        return true;

	    } catch (SQLException e) {
	        if (c != null) c.rollback();
	        return false;

	    } finally {
	        if (c != null) {
	            c.setAutoCommit(true);
	            c.close();
	        }
	    }
	}


   
	public static JsonArray listarFilmes() throws SQLException {
	
	    String sqlFilme = "SELECT " +
	                    "    f.*, " +
	                    "    (SELECT COUNT(*) FROM review r WHERE r.id_filme = f.id) AS qtd_avaliacoes " +
	                    "FROM " +
	                    "    filme f";
	
	    String sqlGeneros = "SELECT g.nome FROM genero g " +
	                        "JOIN filme_genero fg ON g.id = fg.genero_id " +
	                        "WHERE fg.filme_id = ?";
	
	    JsonArray listaFilmes = new JsonArray();
	
	    try (Connection c = conexao.getConexao();
	         PreparedStatement psFilme = c.prepareStatement(sqlFilme);
	         ResultSet rsFilme = psFilme.executeQuery()) {
	
	        while (rsFilme.next()) {
	            JsonObject filme = new JsonObject();
	            long filmeId = rsFilme.getLong("id");
	
	            filme.addProperty("id", String.valueOf(filmeId));
	            filme.addProperty("titulo", rsFilme.getString("titulo"));
	            filme.addProperty("diretor", rsFilme.getString("diretor"));
	            filme.addProperty("ano", rsFilme.getString("ano"));
	            
	            JsonArray generosDoFilme = new JsonArray();
	            try (Connection c2 = conexao.getConexao();
	                 PreparedStatement psGeneros = c2.prepareStatement(sqlGeneros)) {
	                psGeneros.setLong(1, filmeId);
	                try (ResultSet rsGeneros = psGeneros.executeQuery()) {
	                    while (rsGeneros.next()) {
	                        generosDoFilme.add(rsGeneros.getString("nome"));
	                    }
	                }
	            }
	            filme.add("genero", generosDoFilme);
	
	            String notaRaw = rsFilme.getString("nota_media");
                int notaInt = 0;
                try {
                    if (notaRaw != null && !notaRaw.isEmpty()) {
                        double d = Double.parseDouble(notaRaw.replace(",", "."));
                        notaInt = (int) Math.round(d);
                    }
                } catch(Exception e) {}
                filme.addProperty("nota", String.valueOf(notaInt)); 
	            filme.addProperty("qtd_avaliacoes", String.valueOf(rsFilme.getInt("qtd_avaliacoes")));
	            filme.addProperty("sinopse", rsFilme.getString("sinopse"));
	
	            listaFilmes.add(filme);
	        }
	    }
	
	    return listaFilmes;
	}
	
public static JsonObject listarFilmePorId(String id) throws SQLException {
        
        String sqlFilme = "SELECT f.*, " +
                          "(SELECT COUNT(*) FROM review r WHERE r.id_filme = f.id) AS qtd_avaliacoes " +
                          "FROM filme f WHERE f.id = ?";
        
        String sqlGeneros = "SELECT g.nome FROM genero g " +
                            "JOIN filme_genero fg ON g.id = fg.genero_id " +
                            "WHERE fg.filme_id = ?";

        String sqlReviews = "SELECT * FROM review WHERE id_filme = ?";

        JsonObject root = new JsonObject(); 
        long filmeIdLong = Long.parseLong(id);

        try (Connection c = conexao.getConexao()) {
            
           
            try (PreparedStatement psFilme = c.prepareStatement(sqlFilme)) {
                psFilme.setLong(1, filmeIdLong);
                
                try (ResultSet rsFilme = psFilme.executeQuery()) {
                    if (rsFilme.next()) {
                        JsonObject filmeObj = new JsonObject();
                        
                        filmeObj.addProperty("id", String.valueOf(rsFilme.getLong("id")));
                        filmeObj.addProperty("titulo", rsFilme.getString("titulo"));
                        filmeObj.addProperty("diretor", rsFilme.getString("diretor"));
                        filmeObj.addProperty("ano", rsFilme.getString("ano"));
                        filmeObj.addProperty("sinopse", rsFilme.getString("sinopse"));
                        
                        String notaRaw = rsFilme.getString("nota_media");
                        int notaInt = 0;
                        try { if(notaRaw!=null) notaInt = (int) Math.round(Double.parseDouble(notaRaw.replace(",", "."))); } catch(Exception e){}
                        filmeObj.addProperty("nota", String.valueOf(notaInt));
                        filmeObj.addProperty("qtd_avaliacoes", String.valueOf(rsFilme.getInt("qtd_avaliacoes")));

                        JsonArray generosArray = new JsonArray();
                        try (PreparedStatement psGen = c.prepareStatement(sqlGeneros)) {
                            psGen.setLong(1, filmeIdLong);
                            try (ResultSet rsGen = psGen.executeQuery()) {
                                while (rsGen.next()) {
                                    generosArray.add(rsGen.getString("nome"));
                                }
                            }
                        }
                        filmeObj.add("genero", generosArray);
                        
                       
                        root.add("filme", filmeObj);
                        
                    } else {
                        return null; 
                    }
                }
            }

            JsonArray reviewsArray = new JsonArray();
            try (PreparedStatement psRev = c.prepareStatement(sqlReviews)) {
                psRev.setLong(1, filmeIdLong);
                
                try (ResultSet rsReview = psRev.executeQuery()) {
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

                        reviewsArray.add(reviewObj);
                    }
                }
            }
            root.add("reviews", reviewsArray);
        }

        return root;
    }

public static boolean excluirFilme(String filmeId) throws SQLException {
    
    String sqlDelGeneros = "DELETE FROM filme_genero WHERE filme_id = ?";
    String sqlDelReviews = "DELETE FROM review WHERE id_filme = ?"; 
    String sqlDelFilme = "DELETE FROM filme WHERE id = ?";
    
    try (Connection c = conexao.getConexao()) {
        c.setAutoCommit(false); 

        try {
            long idLong = Long.parseLong(filmeId);

           
            try (PreparedStatement psGeneros = c.prepareStatement(sqlDelGeneros)) {
                psGeneros.setLong(1, idLong);
                psGeneros.executeUpdate(); 
            }

           
            try (PreparedStatement psReviews = c.prepareStatement(sqlDelReviews)) {
                psReviews.setLong(1, idLong);
                psReviews.executeUpdate(); 
            }
            
            int r;
            try (PreparedStatement psFilme = c.prepareStatement(sqlDelFilme)) {
                psFilme.setLong(1, idLong);
                r = psFilme.executeUpdate();
            }
            
            c.commit(); 
            return r > 0; 

        } catch (SQLException e) {
            System.err.println("Erro ao excluir filme: " + e.getMessage());
            c.rollback(); 
            e.printStackTrace();
            return false; 
        } finally {
            if (c != null) c.setAutoCommit(true);
        }
    } 
}
    
    
	public static boolean atualizarFilme(String filmeId, String titulo, String diretor, String ano, String sinopse, List<Integer> generosIDs) throws SQLException {
		
        String sqlUpdateFilme = "UPDATE filme SET titulo = ?, diretor = ?, ano = ?, sinopse = ? WHERE id = ?";
        String sqlDelGeneros = "DELETE FROM filme_genero WHERE filme_id = ?";
        String sqlInsertGeneros = "INSERT INTO filme_genero (filme_id, genero_id) VALUES (?, ?)";

        Connection c = null; 
        
        try {
            c = conexao.getConexao();
            c.setAutoCommit(false); 
            
            try (PreparedStatement psUpdate = c.prepareStatement(sqlUpdateFilme)) {
                psUpdate.setString(1, titulo);
                psUpdate.setString(2, diretor);
                psUpdate.setString(3, ano);
                psUpdate.setString(4, sinopse);
                psUpdate.setString(5, filmeId); 
                
                int r = psUpdate.executeUpdate();
                if (r == 0) {
                    return false;
                }
            }
            
            try (PreparedStatement psDel = c.prepareStatement(sqlDelGeneros)) {
                psDel.setString(1, filmeId); 
                psDel.executeUpdate();
            }

           
            if (generosIDs != null && generosIDs.size() > 0) {
                try (PreparedStatement psInsert = c.prepareStatement(sqlInsertGeneros)) {
                    for (Integer generoId : generosIDs) {
                        psInsert.setString(1, filmeId); 
                        psInsert.setInt(2, generoId);
                        psInsert.addBatch();
                    }
                    psInsert.executeBatch();
                }
            }
            
            c.commit(); 
            return true;

        } catch (SQLException e) {
            if (c != null) c.rollback(); 
            e.printStackTrace();
            return false;
        } finally {
            if (c != null) {
                c.setAutoCommit(true);
                c.close();
            }
        }
    }
}

