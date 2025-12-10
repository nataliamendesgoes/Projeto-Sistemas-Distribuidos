package Servidor;

import java.sql.*; 
import java.util.*;
import Conexao.conexao; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FilmeGenero {

    public static int obterOuCriarIdPorNome(String nomeGenero) throws SQLException {
        String sqlSelect = "SELECT id FROM genero WHERE nome = ?";
        
        try (Connection c = conexao.getConexao(); 
             PreparedStatement psSelect = c.prepareStatement(sqlSelect)) {
            
            psSelect.setString(1, nomeGenero);
            
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        String sqlInsert = "INSERT INTO genero (nome) VALUES (?)";
        
        try (Connection c = conexao.getConexao();
             PreparedStatement psInsert = c.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            
            psInsert.setString(1, nomeGenero);
            int rowsAffected = psInsert.executeUpdate();
            
            if (rowsAffected == 0) {
                throw new SQLException("Criação do gênero falhou, nenhuma linha afetada.");
            }

            try (ResultSet rsKeys = psInsert.getGeneratedKeys()) {
                if (rsKeys.next()) {
                    
                    return rsKeys.getInt(1);
                } else {
                    throw new SQLException("Criação do gênero falhou, não foi possível obter o ID.");
                }
            }
        }
    }
}