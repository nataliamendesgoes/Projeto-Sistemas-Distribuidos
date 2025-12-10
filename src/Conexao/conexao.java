package Conexao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class conexao {

    private static final String URL = "seuCaminho" +
    	    "?useUnicode=true" +
    	    "&characterEncoding=UTF-8" +
    	    "&characterSetResults=UTF-8" +
    	    "&connectionCollation=utf8mb4_general_ci" +
    	    "&serverTimezone=UTC" +
    	    "&useSSL=false";
    private static final String USER = "seuUser";
    private static final String PASSWORD = "suaSenha";

    
    public static Connection getConexao() throws SQLException {
        try {
        	Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Erro ao obter conexão com o BD: " + e.getMessage());
            e.printStackTrace();
            throw e; 
        }catch (ClassNotFoundException e) {
            throw new SQLException("Erro: Driver MySQL (com.mysql.cj.jdbc.Driver) não encontrado.", e);
        }
    }

    
    public static void fecharConexao() {
        
    }
}
