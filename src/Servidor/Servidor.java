package Servidor;

import com.google.gson.*;
import io.jsonwebtoken.Claims;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import com.google.gson.*;   
import java.sql.*;     
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap; 


public class Servidor {

	private final Socket socket;
    private final Gson gson = new Gson();
    private String usuarioLogadoNestaThread = null; 
    
    private static final Map<String, String> ipsUsuariosOnline = new ConcurrentHashMap<>();


    public Servidor(Socket socket) {
        this.socket = socket;
    }
    
    static {
        limparTokensAoIniciar();
    }

    
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            String linha;
           
            while ((linha = in.readLine()) != null) {
                System.out.println("[RECEBIDO] " + linha);
                
                JsonObject resp = processar(linha);
                
                
                atualizarEstadoUsuario(linha, resp);
                
               
                String respStr = gson.toJson(resp);
                out.println(respStr);
                System.out.println("[ENVIADO] " + respStr);
            }

        } catch (IOException e) {
            System.err.println("IO erro cliente: " + e.getMessage());
        } finally {
            
            if (this.usuarioLogadoNestaThread != null) {
                System.out.println("\n<<< DESCONEXÃO ABRUPTA: " + this.usuarioLogadoNestaThread);
                
                Usuarios.clearTokenByNome(this.usuarioLogadoNestaThread);
                imprimirUsuariosOnline();
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void atualizarEstadoUsuario(String linhaReq, JsonObject resp) {
        try {
            JsonObject req = JsonParser.parseString(linhaReq).getAsJsonObject();
            if (!req.has("operacao")) return;
            String op = req.get("operacao").getAsString();
            String status = resp.get("status").getAsString();

            if (op.equals("LOGIN") && status.equals("200")) {
                String nome = req.get("usuario").getAsString();
                this.usuarioLogadoNestaThread = nome; 
               
                String ipCliente = socket.getInetAddress().getHostAddress();
                ipsUsuariosOnline.put(nome, ipCliente); 
                
                System.out.println("\n>>> LOGIN: " + nome + " entrou.");
                imprimirUsuariosOnline(); 
            }
            
            if (op.equals("LOGOUT") && status.equals("200")) {
                if (this.usuarioLogadoNestaThread != null) {
                    ipsUsuariosOnline.remove(this.usuarioLogadoNestaThread);
                    
                    System.out.println("\n<<< LOGOUT: " + this.usuarioLogadoNestaThread + " saiu.");
                    this.usuarioLogadoNestaThread = null;
                    imprimirUsuariosOnline(); 
                }
            }
        } catch (Exception ignored) {}
    }
    private JsonObject processar(String texto) {
        JsonObject resposta = new JsonObject();
        JsonObject req;
        try {
            req = JsonParser.parseString(texto).getAsJsonObject();
        } catch (Exception e) {
            resposta.addProperty("status", "400");
            resposta.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resposta;
        }

        String operacao = req.has("operacao") ? req.get("operacao").getAsString() : "";
        try {
            switch (operacao) {
                case "CRIAR_USUARIO":
                    return handleCriarUsuario(req);
                case "LOGIN":
                    return handleLogin(req);
                case "LOGOUT":
                    return handleLogout(req);
                case "LISTAR_PROPRIO_USUARIO":
                    return handleLerUsuario(req);
                case "EDITAR_PROPRIO_USUARIO":
                    return handleAtualizarUsuario(req);
                case "EXCLUIR_PROPRIO_USUARIO":
                    return handleExcluirUsuario(req);
                case "LISTAR_FILMES":
                    return handleListarFilmes(req);
                case "CRIAR_FILME":
                    return handleCriarFilme(req);
                case "EDITAR_FILME":
                    return handleEditarFilme(req);
                case "EXCLUIR_FILME":
                    return handleExcluirFilme(req);
                case "LISTAR_USUARIOS":
                    return handleListarUsuariosADM(req);
                case "ADMIN_EDITAR_USUARIO":
                    return handleEditarUsuarios(req);
                case "ADMIN_EXCLUIR_USUARIO":
                    return handleExcluirUsuarios(req);
                case "CRIAR_REVIEW":
                    return handleCriarReview(req);
                case "LISTAR_REVIEWS_USUARIO":
                    return handleListaReview(req);
                case "EDITAR_REVIEW":
                    return handleEditarReview(req);
                case "EXCLUIR_REVIEW":
                    return handleExcluirReview(req);
                case "BUSCAR_FILME_ID":
                    return handleBuscarFilme(req);
                   
                default:
                    JsonObject r = new JsonObject();
                    r.addProperty("status", "400");
                    r.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
                    return r;
            }
        } catch (SQLException ex) {
            JsonObject r = new JsonObject();
            r.addProperty("status", "500");
            r.addProperty("mensagem", "Erro: Falha interna do servidor (SQL: " + ex.getMessage() + ")");
            ex.printStackTrace();
            return r;
        } catch (Exception ex) {
            JsonObject r = new JsonObject();
            r.addProperty("status", "500");
            r.addProperty("mensagem", "Erro: Falha interna do servidor");
            ex.printStackTrace(); 
            return r;
        }
    }
    
    private static void imprimirUsuariosOnline() {
        try {
            List<String> nomesLogados = Usuarios.listarUsuariosLogados();
            
            System.out.println("┌──────────────────────────────────────────────┐");
            System.out.println("│          LISTA DE USUÁRIOS ONLINE            │");
            System.out.println("├──────────────────────────────────────────────┤");
            
            if (nomesLogados.isEmpty()) {
                System.out.println("│ (Nenhum usuário logado no momento)           │");
            } else {
                for (String nome : nomesLogados) {
                    String ip = ipsUsuariosOnline.getOrDefault(nome, "IP Desconhecido");
                    System.out.printf("│ ● %-42s │%n", nome + " (" + ip + ")");
                }
            }
            System.out.println("└──────────────────────────────────────────────┘");
        } catch (SQLException e) {
            System.err.println("Erro ao listar usuários online: " + e.getMessage());
        }
    }
    
    private static void limparTokensAoIniciar() {
        try (java.sql.Connection c = Conexao.conexao.getConexao();
             java.sql.PreparedStatement ps = c.prepareStatement("UPDATE usuarios SET jwt_token = NULL")) {
            ps.executeUpdate();
            System.out.println(">> Estado do banco resetado (Tokens limpos).");
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    private JsonObject handleCriarReview(JsonObject req) { 
    	JsonObject resp = new JsonObject();
        
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        String token = req.get("token").getAsString();
       
        String[] usuarioLogado = null;
        try {
             usuarioLogado = Usuarios.obterUsuarioPorToken(token);
        } catch (SQLException e) {
             resp.addProperty("status", "500"); 
             resp.addProperty("mensagem", "Erro: Falha interna do servidor");
             return resp;
        }

        if (usuarioLogado == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }
        if ("admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }

        if (!req.has("review")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        JsonObject u = req.getAsJsonObject("review");
        
        String erroValidacao = validarReview(u);
        if (erroValidacao != null) {
            resp.addProperty("status", "405"); 
            resp.addProperty("mensagem", "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
            return resp;
        }
        
        String idFilme = u.has("id_filme") ? u.get("id_filme").getAsString() : null;
        String titulo = u.has("titulo") ? u.get("titulo").getAsString() : null; 
        String descricao = u.has("descricao") ? u.get("descricao").getAsString() : null;
        String nota = u.has("nota") ? u.get("nota").getAsString() : null;
        
        if (idFilme == null || titulo == null || descricao == null || nota == null) {
            resp.addProperty("status", "422"); 
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }

        try {
            int idFilmeInt = Integer.parseInt(idFilme);

            if (Review.jaFezReview(idFilmeInt, usuarioLogado[0])) {
                resp.addProperty("status", "409");
                resp.addProperty("mensagem", "Erro: Recurso ja existe");
                return resp;
            }
        
            boolean ok = Review.criarReview(idFilme, titulo, descricao, nota, token);
            
            if (ok) {
                resp.addProperty("status", "201");
                resp.addProperty("mensagem", "Sucesso: Recurso cadastrado");
            } else {
                resp.addProperty("status", "404");
                resp.addProperty("mensagem", "Erro: Recurso inexistente");
            }

        } catch (NumberFormatException e) {
            resp.addProperty("status", "405");
            resp.addProperty("mensagem", "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
        } catch (Exception e) { 
             e.printStackTrace();
             resp.addProperty("status", "500");
             resp.addProperty("mensagem", "Erro: Falha interna do servidor");
        }
        
        return resp;
    }
    private JsonObject handleListaReview(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();
    	
    	String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token);
        if (usuarioLogado == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido.");
            return resp;
        }
    	
        JsonArray lista = Review.listaReviewUsuario(token); 
        if (lista == null) {
            
            resp.addProperty("status", "400"); 
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        
        resp.addProperty("status", "200");
        resp.addProperty("mensagem", "Sucesso: Operação realizada com sucesso");
        resp.add("reviews", lista); 
        
        return resp;
    }
    
    private JsonObject handleEditarReview(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        
        String token = req.get("token").getAsString();
        
        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token);
        if (usuarioLogado == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido.");
            return resp;
        }
        if ("admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }
        String nomeUsuario = usuarioLogado[0];
        String funcaoUsuario = usuarioLogado[1];
        
        if (!req.has("review")) {
            resp.addProperty("status", "400"); 
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        
        JsonObject reviewJson = req.getAsJsonObject("review");
        
        
        String id = reviewJson.get("id").getAsString();
        
        boolean isDono = Review.eDonoDaReview(id, nomeUsuario);
        if (!isDono) {
            resp.addProperty("status", "403");
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }
        String erroValidacao = validarReview(reviewJson);
        if (erroValidacao != null) {
            resp.addProperty("status", "405"); 
            resp.addProperty("mensagem", "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
            return resp;
        }
        //String idFilme = reviewJson.has("id_filme") ? reviewJson.get("id_filme").getAsString() : null;
        String titulo = reviewJson.has("titulo") ? reviewJson.get("titulo").getAsString() : null;
        String descricao = reviewJson.has("descricao") ? reviewJson.get("descricao").getAsString() : null;
        String nota = reviewJson.has("nota") ? reviewJson.get("nota").getAsString() : null;
        
        if (titulo == null || descricao == null || nota == null) {
            resp.addProperty("status", "422"); 
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }
                
        boolean sucesso = Review.atualizarReview(
            id,
            titulo,
            descricao,
            nota
        );
        
        if (sucesso) {
            resp.addProperty("status", "200");
            resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        } else {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
        }
        return resp;
    }
    
    private JsonObject handleExcluirReview(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        if (!req.has("token") || !req.has("id")) { 
        	resp.addProperty("status", "400"); 
        	resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
        	return resp; 
        	}
        String token = req.get("token").getAsString();
        String[] u = Usuarios.obterUsuarioPorToken(token);
        if (u == null) { 
        	resp.addProperty("status", "401"); 
        	resp.addProperty("mensagem", "Erro: Token inválido.");
        	return resp; 
        	}
        if (!req.has("id")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        String id = req.get("id").getAsString();
        String funcao = u[1];
        String nome = u[0];
        
        boolean podeExcluir = false;
        
        if ("admin".equals(funcao)) {
            podeExcluir = true; 
        } else {
            if (Review.eDonoDaReview(id, nome)) {
                podeExcluir = true;
            }
        }
        
        if (!podeExcluir) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão"); 
            return resp; 
        }

        boolean ok = Review.excluirReview(id);
        if(ok) {
        	resp.addProperty("status", "200"); 
        	resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        }else {
        	resp.addProperty("status", "404");
        	resp.addProperty("mensagem", "Erro: Recurso inexistente");
        	}
        return resp;
    }
    
    private static JsonObject handleExcluirReviewADM(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        
        if (!req.has("token")) { 
        	resp.addProperty("status", "400"); 
        	resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
        	return resp; }
        
        String token = req.get("token").getAsString();
        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token); 
        if (usuarioLogado == null) { 
        	resp.addProperty("status", "401"); 
        	resp.addProperty("mensagem", "Erro: Token inválido.");
        	return resp; 
        	}
        
        if (!"admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }

        if (!req.has("id")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        String idReview = req.get("id").getAsString();
        
        boolean sucesso = Review.excluirReview(idReview);
        
        if (sucesso) {
            resp.addProperty("status", "200");
            resp.addProperty("mensagem", "Sucesso: Operação realizada com sucesso");
        } else {
            resp.addProperty("status", "404");
            resp.addProperty("mensagem", "Erro: Recurso inexistente");
        }
        return resp;
    }

    private JsonObject handleBuscarFilme(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();

        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token);
        if (usuarioLogado == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }

        if (!req.has("id_filme")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        String idFilme = req.get("id_filme").getAsString(); 
        
        JsonObject dados = Filme.listarFilmePorId(idFilme);
        
        if (dados != null) {
            resp.addProperty("status", "200");
            resp.addProperty("mensagem", "Sucesso: Operação realizada com sucesso");
            resp.add("filme", dados.get("filme"));
            resp.add("reviews", dados.get("reviews"));
        } else {
            resp.addProperty("status", "404");
            resp.addProperty("mensagem", "Erro: Recurso inexistente");
        }
        return resp;
    }
    
    
    private JsonObject handleAtualizarUsuario(JsonObject req) throws SQLException {

    	JsonObject resp = new JsonObject();
    	if (!req.has("token") || !req.has("usuario")) {
    		resp.addProperty("status", "400");
    		resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
    		return resp;
    	}

    	String token = req.get("token").getAsString();
    	Claims claims = Autentificacao.validarToken(token);

    	if (claims == null) {
    		resp.addProperty("status", "401");
    		resp.addProperty("mensagem", "Erro: Token inválido");
    		return resp;
    	}

    	String[] u = Usuarios.obterUsuarioPorToken(token);

    	if (u == null) {
    		resp.addProperty("status", "404");
    		resp.addProperty("mensagem", "Erro: Recurso inexistente");
    		return resp;
    	}

    	String nome = u[0];
    	JsonObject dados = req.getAsJsonObject("usuario");
    	String novaSenha = dados.has("senha") ? dados.get("senha").getAsString() : null;


    	if (novaSenha != null && !isInputValido(novaSenha)) {
	    	resp.addProperty("status", "405");
	    	resp.addProperty("mensagem", "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
	    	return resp;
    	}

    	String novaFuncao = null;

    	boolean updated = Usuarios.atualizarPorNome(nome, novaSenha, novaFuncao);

    	if (updated) {
    		resp.addProperty("status", "200");
    		resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
    	}else {
    		resp.addProperty("status", "500");
    		resp.addProperty("mensagem", "Erro: Falha interna do servidor");
    	}
    	return resp;
    }

    private JsonObject handleCriarUsuario(JsonObject req) throws SQLException {
        JsonObject resp = new JsonObject();
        if (!req.has("usuario")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        JsonObject u = req.getAsJsonObject("usuario");
        String nome = u.has("nome") ? u.get("nome").getAsString() : null;
        String senha = u.has("senha") ? u.get("senha").getAsString() : null;
        String funcao = "normal"; 

        if (nome == null || senha == null) {
            resp.addProperty("status", "422");
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }
        if (!isInputValido(nome) || !isInputValido(senha)) {
            resp.addProperty("status", "405");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }

        try {
            boolean ok = Usuarios.criarUsuario(nome, senha, funcao);
            if (ok) {
                resp.addProperty("status", "201");
                resp.addProperty("mensagem", "Sucesso: Recurso cadastrado");
            } else {
                resp.addProperty("status", "500");
                resp.addProperty("mensagem", "Erro: Falha interna do servidor");
            }
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("duplicate") || e.getMessage().toLowerCase().contains("unique")) {
                resp.addProperty("status", "409");
                resp.addProperty("mensagem", "Erro: Recurso ja existe");
            } else throw e;
        }
        return resp;
    }

    private JsonObject handleLogin(JsonObject req) throws SQLException {
        JsonObject resp = new JsonObject();
        String usuario = req.has("usuario") ? req.get("usuario").getAsString() : null;
        String senha = req.has("senha") ? req.get("senha").getAsString() : null;
        if (usuario == null || senha == null) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }

        String[] dados = Usuarios.obterSenhaEFuncao(usuario);
        if (dados == null) {
            resp.addProperty("status", "422");
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }

        String dbSenha = dados[0];
        String funcao = dados[1];
        if (!dbSenha.equals(senha)) {
            resp.addProperty("status", "403");
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }

        String token = Autentificacao.gerarToken(usuario, funcao);
        boolean saved = Usuarios.setToken(usuario, token);
        if (!saved) {
            resp.addProperty("status", "500");
            resp.addProperty("mensagem", "Erro: Falha interna do servidor");
            return resp;
        }
        resp.addProperty("status", "200" );
        resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        resp.addProperty("token", token);
        return resp;
    }

    private JsonObject handleLogout(JsonObject req) throws SQLException {
        JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        String token = req.get("token").getAsString();

        boolean ok = Usuarios.clearToken(token);
        if (ok) {
            resp.addProperty("status", "200");
            resp.addProperty("mensagem", "Sucesso: Operação realizada com sucesso");
        } else {
            resp.addProperty("status", "404");
            resp.addProperty("mensagem", "Erro: Recurso inexistente");
        }
        return resp;
    }

    private JsonObject handleLerUsuario(JsonObject req) throws SQLException {
        JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        String token = req.get("token").getAsString();

        Claims claims = Autentificacao.validarToken(token);
        if (claims == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }

        String[] u = Usuarios.obterUsuarioPorToken(token);
        if (u == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }
        resp.addProperty("status", "200");
        resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        
        
        resp.addProperty("usuario", u[0]);
        
        return resp;
    }

    

    private JsonObject handleExcluirUsuario(JsonObject req) throws SQLException {
        JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
        String token = req.get("token").getAsString();
        Claims claims = Autentificacao.validarToken(token);
        if (claims == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }

        String[] u = Usuarios.obterUsuarioPorToken(token);
        if (u == null) {
            resp.addProperty("status", "401");
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }
        String nome = u[0];

        boolean ok = Usuarios.excluirPorNome(nome);
        if (ok) {
            resp.addProperty("status", "200");
            resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        } else {
            resp.addProperty("status", "500");
            resp.addProperty("mensagem", "Erro: Falha interna do servidor");
        }
        return resp;
    }
    
    private static JsonObject handleCriarFilme(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();
        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token); 
        
        if (usuarioLogado == null || !"admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }

        JsonObject filmeJson = req.getAsJsonObject("filme");
        String erroValidacao = validarFilme(filmeJson, false); 
        if (erroValidacao != null) {
            resp.addProperty("status", "405");
            resp.addProperty("mensagem", "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
            return resp;
        }
        
        String titulo = filmeJson.has("titulo") ? filmeJson.get("titulo").getAsString() : null;
        String diretor = filmeJson.has("diretor") ? filmeJson.get("diretor").getAsString() : null;
        String ano = filmeJson.has("ano") ? filmeJson.get("ano").getAsString() : null;
        String sinopse = filmeJson.has("sinopse") ? filmeJson.get("sinopse").getAsString() : null;
        
        if (titulo == null || diretor == null || ano == null) {
            resp.addProperty("status", "422");
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }

        List<Integer> generosIDs = new ArrayList<>();
        if (filmeJson.has("genero") && filmeJson.get("genero").isJsonArray()) {
            JsonArray generosNomes = filmeJson.getAsJsonArray("genero");
            for (JsonElement el : generosNomes) {
                int idGenero = FilmeGenero.obterOuCriarIdPorNome(el.getAsString()); 
                generosIDs.add(idGenero);
            }
        }
        
        boolean sucesso = Filme.criarFilme(
            titulo,
            diretor,
            ano,
            sinopse,
            generosIDs
        );

        if (sucesso) {
            resp.addProperty("status", "201"); 
            resp.addProperty("mensagem", "Sucesso: Recurso cadastrado");
        } else {
            resp.addProperty("status", "500");
            resp.addProperty("mensagem", "Erro: Falha interna do servidor");
        }
        return resp;
    }
    
    private static JsonObject handleListarFilmes(JsonObject req) throws SQLException {
        JsonObject resp = new JsonObject();
        JsonArray lista = Filme.listarFilmes(); 
        resp.addProperty("status", "200");
        resp.addProperty("mensagem", "Sucesso: Operação realizada com sucesso");
        resp.add("filmes", lista); 
        return resp;
    }


    private static JsonObject handleEditarFilme(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();
        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token); 
        if (usuarioLogado == null || !"admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão.");
            return resp;
        }
        if (!req.has("filme")) {
            resp.addProperty("status", "422");
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }
        JsonObject filmeJson = req.getAsJsonObject("filme");
        String erroValidacao = validarFilme(filmeJson, true); 
        if (erroValidacao != null) {
            resp.addProperty("status", "405");
            resp.addProperty("mensagem", "Erro: Campos inválidos, verifique o tipo e quantidade de caracteres");
            return resp;
        }
        
        String id = filmeJson.get("id").getAsString();
        String titulo = filmeJson.has("titulo") ? filmeJson.get("titulo").getAsString() : null;
        String diretor = filmeJson.has("diretor") ? filmeJson.get("diretor").getAsString() : null;
        String ano = filmeJson.has("ano") ? filmeJson.get("ano").getAsString() : null;
        String sinopse = filmeJson.has("sinopse") ? filmeJson.get("sinopse").getAsString() : null;
        
        List<Integer> generosIDs = new ArrayList<>();
        if (filmeJson.has("genero") && filmeJson.get("genero").isJsonArray()) {
            JsonArray generosNomes = filmeJson.getAsJsonArray("genero");
            for (JsonElement el : generosNomes) {
                int idGenero = FilmeGenero.obterOuCriarIdPorNome(el.getAsString());
                generosIDs.add(idGenero);
            }
        }
        
        boolean sucesso = Filme.atualizarFilme(
            id,
            titulo,
            diretor,
            ano,
            sinopse,
            generosIDs
        );
        
        if (sucesso) {
            resp.addProperty("status", "200");
            resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        } else {
            resp.addProperty("status", "404");
            resp.addProperty("mensagem", "Erro: Recurso inexistente");
        }
        return resp;
    }

    private static JsonObject handleExcluirFilme(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();
        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token); 
        if (usuarioLogado == null || !"admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }
        if (!req.has("id")) {
            resp.addProperty("status", "422");
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }
        String filmeId = req.get("id").getAsString(); 
        boolean sucesso = Filme.excluirFilme(filmeId);
        if (sucesso) {
            resp.addProperty("status", "200");
            resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        } else {
            resp.addProperty("status", "404");
            resp.addProperty("mensagem", "Erro: Recurso inexistente");
        }
        return resp;
    }

    private static JsonObject handleListarUsuariosADM(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();
        JsonArray lista = Usuarios.listarUsuariosADM(token); 
        if (lista == null) {
            resp.addProperty("status", "403");
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }
        resp.addProperty("status", "200");
        resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
        resp.add("usuarios", lista);
        return resp;
    }
    
    private JsonObject handleEditarUsuarios(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
    	
        if (!req.has("token") || !req.has("usuario")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();

        
        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token); 
        if (usuarioLogado == null) {
            resp.addProperty("status", "401"); 
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }

        if (!"admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }
        if (!req.has("id")) { 
            resp.addProperty("status", "422");
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }
        String idAlvo = req.get("id").getAsString(); 
        
        JsonObject dados = req.getAsJsonObject("usuario");
        String novaSenha = dados.has("senha") ? dados.get("senha").getAsString() : null;
        String novaFuncao = dados.has("funcao") ? dados.get("funcao").getAsString() : null;

        boolean sucesso = Usuarios.atualizarPorNomeADM(idAlvo, novaSenha, novaFuncao, token); 

    	if (sucesso) {
    		resp.addProperty("status", "200");
    		resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
    	} else {
    		resp.addProperty("status", "404");
    		resp.addProperty("mensagem", "Erro: Recurso inexistente");
    	}
    	return resp;
    }

    private static JsonObject handleExcluirUsuarios(JsonObject req) throws SQLException {
    	JsonObject resp = new JsonObject();
    	
        if (!req.has("token")) {
            resp.addProperty("status", "400");
            resp.addProperty("mensagem", "Erro: Operação não encontrada ou inválida");
            return resp;
        }
    	String token = req.get("token").getAsString();

        if (!req.has("id")) {
            resp.addProperty("status", "422");
            resp.addProperty("mensagem", "Erro: Chaves faltantes ou invalidas");
            return resp;
        }
        String idAlvo = req.get("id").getAsString();

        String[] usuarioLogado = Usuarios.obterUsuarioPorToken(token); 
        if (usuarioLogado == null) {
            resp.addProperty("status", "401"); 
            resp.addProperty("mensagem", "Erro: Token inválido");
            return resp;
        }

        if (!"admin".equals(usuarioLogado[1])) {
            resp.addProperty("status", "403"); 
            resp.addProperty("mensagem", "Erro: sem permissão");
            return resp;
        }
        boolean sucesso = Usuarios.excluirUsuarioADM(idAlvo, token);
    	if (sucesso) {
    		resp.addProperty("status", "200");
    		resp.addProperty("mensagem", "Sucesso: operação realizada com sucesso");
    	} else {
    		resp.addProperty("status", "404");
    		resp.addProperty("mensagem", "Erro: Recurso inexistente");
    	}
    	return resp;
    }
    private static String validarFilme(JsonObject filmeJson, boolean isEditMode) {
        
        if (isEditMode) {
            if (!filmeJson.has("id") || filmeJson.get("id").getAsString().isEmpty()) {
                return "Erro: O 'id' do filme é obrigatório para editar.";
            }
        }
        
        if (!filmeJson.has("titulo")) return "Erro: 'titulo' é obrigatório.";
        String titulo = filmeJson.get("titulo").getAsString();
        if (titulo.length() < 3 || titulo.length() > 30) {
            return "Erro: O 'titulo' deve ter entre 3 e 30 caracteres.";
        }
        
        if (!filmeJson.has("diretor")) return "Erro: 'diretor' é obrigatório.";
        String diretor = filmeJson.get("diretor").getAsString();
        if (diretor.length() < 3 || diretor.length() > 30) {
            return "Erro: O 'diretor' deve ter entre 3 e 30 caracteres.";
        }

        if (!filmeJson.has("ano")) return "Erro: 'ano' é obrigatório.";
        String ano = filmeJson.get("ano").getAsString();
        if (!ano.matches("^\\d{3,4}$")) { 
            return "Erro: O 'ano' deve conter apenas 3 ou 4 dígitos numéricos.";
        }

        if (!filmeJson.has("sinopse")) return "Erro: 'sinopse' é obrigatória.";
        String sinopse = filmeJson.get("sinopse").getAsString();
        if (sinopse.length() > 250) {
            return "Erro: A 'sinopse' deve ter no máximo 250 caracteres.";
        }
        
        if (!filmeJson.has("genero") || !filmeJson.get("genero").isJsonArray()) {
            return "Erro: 'genero' (em formato de lista) é obrigatório.";
        }
        JsonArray generos = filmeJson.getAsJsonArray("genero");
        if (generos.size() == 0) {
            return "Erro: O filme deve ter pelo menos um gênero.";
        }

        return null;
    }
    private static String validarReview(JsonObject reviewJson) {
        
        String descricao = reviewJson.get("descricao").getAsString();
        if (descricao.length() > 250) {
            return "Erro: A 'descricao' excede o limite de 250 caracteres.";
        }

        try {
            int nota = Integer.parseInt(reviewJson.get("nota").getAsString());
            if (nota < 1 || nota > 5) {
                return "Erro: A 'nota' deve ser um inteiro entre 1 e 5.";
            }
        } catch (NumberFormatException e) {
            return "Erro: A 'nota' deve conter apenas dígitos numéricos.";
        }
        
        if (reviewJson.has("titulo") && !reviewJson.get("titulo").isJsonNull()) {
            String titulo = reviewJson.get("titulo").getAsString();
            if (!titulo.isEmpty()) {
                if (titulo.length() < 3 || titulo.length() > 30) {
                    return "Erro: O 'titulo' deve ter entre 3 e 30 caracteres.";
                }
            }
        }

        return null;
    }
    private boolean isInputValido(String input) {
        return input != null && input.matches("^[a-zA-Z0-9]{3,20}$");
    }
    
}
