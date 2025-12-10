package Cliente;

import com.google.gson.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import com.google.gson.*; // Gson, JsonObject, JsonParser se usar
import java.util.*;



public class ClienteCore {

    private final String serverIp;
    private final int serverPort;
    private String token; 
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping() 
            .create();
    
    public ClienteCore(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }

    public void start() {
        try (Socket socket = new Socket(serverIp, serverPort);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             Scanner sc = new Scanner(System.in, "UTF-8")) {

            System.out.println("Conectado ao servidor " + serverIp + ":" + serverPort);

            while (true) {
            	if (token == null) {
                    System.out.println("\n--- MENU ---");
                    System.out.println("1 - Criar usuário");
                    System.out.println("2 - Login");
                    System.out.println("0 - Sair");
                }else{
                	System.out.println("\n--- MENU ---");
	                System.out.println("3 - Ler meu cadastro");
	                System.out.println("4 - Atualizar cadastro"); 
	                System.out.println("5 - Logout");
	                System.out.println("6 - Excluir meu usuário");
	                System.out.println("7 - Listar Filmes");
	                System.out.println("8 - Criar Filme (Admin)");
	                System.out.println("9 - Editar Filme (Admin)");
	                System.out.println("10 - Excluir Filme (Admin)");
	                System.out.println("11 - Listar TODOS os Usuários (Admin)");
	                System.out.println("12 - Atualizar cadastro (Admin)"); 
	                System.out.println("13 - Excluir cadastro (Admin)");
	                System.out.println("14 - Criar resenha");
	                System.out.println("15 - Editar resenha");
	                System.out.println("16 - Busca todas as resenhas de um filme");
	                System.out.println("17 - Apagar resenha");
	                System.out.println("18 - Apagar resenhas (Admin)");
	                System.out.println("19 - Listar todas as minhas resenhas");
	                System.out.println("0 - Sair");
	            }
                System.out.print("Escolha: ");
                String opc = sc.nextLine().trim();

                if (opc.equals("0")) break;

                JsonObject req = new JsonObject();

                switch (opc) {
                    case "1" -> {
                        req.addProperty("operacao", "CRIAR_USUARIO");
                        JsonObject u = new JsonObject();
                        System.out.print("Nome: ");
                        u.addProperty("nome", sc.nextLine().trim());
                        System.out.print("Senha: ");
                        u.addProperty("senha", sc.nextLine().trim());
                       
                        
                        req.add("usuario", u);
                    }
                    case "2" -> {
                        req.addProperty("operacao", "LOGIN");
                        System.out.print("Nome: ");
                        req.addProperty("usuario", sc.nextLine().trim());
                        System.out.print("Senha: ");
                        req.addProperty("senha", sc.nextLine().trim());
                    }
                    case "3" -> {
                        req.addProperty("operacao", "LISTAR_PROPRIO_USUARIO");
                        req.addProperty("token", token);
                    }
                    case "4" -> {
                        req.addProperty("operacao", "EDITAR_PROPRIO_USUARIO");
                        req.addProperty("token", token);
                        JsonObject u2 = new JsonObject();
                        System.out.print("Nova senha (enter para pular): ");
                        String ns = sc.nextLine();
                        if (!ns.isEmpty()) u2.addProperty("senha", ns);
                        req.add("usuario", u2);
                    }
                    case "5" -> {
                        req.addProperty("operacao", "LOGOUT");
                        req.addProperty("token", token);
                    }
                    case "6" -> {
                        req.addProperty("operacao", "EXCLUIR_PROPRIO_USUARIO");
                        req.addProperty("token", token);
                    }
                    case "7" -> {
                        req.addProperty("operacao", "LISTAR_FILMES");
                        req.addProperty("token", token);
                    }
                    case "8" -> {
                    	req.addProperty("operacao", "CRIAR_FILME");
                        req.addProperty("token", token);
                        req.add("filme", criarObjetoFilme(sc, false));
                    }
                    case "9" -> {
                        req.addProperty("operacao", "EDITAR_FILME");
                        req.addProperty("token", token);
                        req.add("filme", criarObjetoFilme(sc, true));
                    }
                    case "10" -> {
                        req.addProperty("operacao", "EXCLUIR_FILME");
                        req.addProperty("token", token);
                        System.out.print("Digite o ID do filme a ser EXCLUÍDO: ");
                        req.addProperty("id", sc.nextLine().trim());
                    }
                    case "11" -> {
                        req.addProperty("operacao", "LISTAR_USUARIOS");
                        req.addProperty("token", token);
                    }
                    case "12" -> {
                        req.addProperty("operacao", "ADMIN_EDITAR_USUARIO");
                        req.addProperty("token", token);
                        JsonObject u2 = new JsonObject();
                        System.out.print("Digite o id do usuario a ser EDITADO: ");
                        req.addProperty("id", sc.nextLine().trim());
                        System.out.print("Nova senha (enter para pular): ");
                        String ns = sc.nextLine();
                        if (!ns.isEmpty()) u2.addProperty("senha", ns);
                        req.add("usuario", u2);
                    }
                    case "13" -> {
                        req.addProperty("operacao", "ADMIN_EXCLUIR_USUARIO");
                        req.addProperty("token", token);
                        System.out.print("Digite o id do usuario a ser EXCLUÍDO: ");
                        req.addProperty("id", sc.nextLine().trim());
                    }
                    case "14" -> {
                        req.addProperty("operacao", "CRIAR_REVIEW");
                        req.addProperty("token", token);
                        JsonObject reviewObj = new JsonObject();
                        System.out.print("Digite o ID do filme para avaliar: ");
                        reviewObj.addProperty("id_filme", sc.nextLine().trim());
                        System.out.print("Digite o título da review: ");
                        reviewObj.addProperty("titulo", sc.nextLine().trim());
                        System.out.print("Digite a descrição: ");
                        reviewObj.addProperty("descricao", sc.nextLine().trim());
                        System.out.print("Digite a nota (1-5): ");
                        reviewObj.addProperty("nota", sc.nextLine().trim());
                        req.add("review", reviewObj);
                        
                    }
                    case "15" -> {
                        req.addProperty("operacao", "EDITAR_REVIEW");
                        req.addProperty("token", token);
                        JsonObject reviewObj = new JsonObject();
                        System.out.print("Digite o ID da Review para editar: ");
                        reviewObj.addProperty("id", sc.nextLine().trim());
                        System.out.print("Novo título (ou enter para manter): ");
                        String t = sc.nextLine().trim();
                        if(!t.isEmpty()) reviewObj.addProperty("titulo", t);
                        System.out.print("Nova descrição (ou enter para manter): ");
                        String d = sc.nextLine().trim();
                        if(!d.isEmpty()) reviewObj.addProperty("descricao", d);
                        System.out.print("Nova nota (1-5) (ou enter para manter): ");
                        String n = sc.nextLine().trim();
                        if(!n.isEmpty()) reviewObj.addProperty("nota", n);
                        req.add("review", reviewObj);
                    }
                    case "16" -> {
                        req.addProperty("operacao", "BUSCAR_FILME_ID");
                        req.addProperty("token", token);
                        System.out.print("Digite o ID do Filme para ver detalhes: ");
                        req.addProperty("id_filme", sc.nextLine().trim());
                    }
                    case "17" -> {
                        req.addProperty("operacao", "EXCLUIR_REVIEW");
                        req.addProperty("token", token);
                        System.out.print("Digite o id do review a ser EXCLUÍDO: ");
                        req.addProperty("id", sc.nextLine().trim());
                    }
                    case "18" -> {
                        req.addProperty("operacao", "EXCLUIR_REVIEW");
                        req.addProperty("token", token);
                        System.out.print("Digite o id do review a ser EXCLUÍDO: ");
                        req.addProperty("id", sc.nextLine().trim());
                    }
                    case "19"-> {
                    	req.addProperty("operacao", "LISTAR_REVIEWS_USUARIO");
                        req.addProperty("token", token);
                    }
                    
                    default -> {
                        System.out.println("Opção inválida.");
                        continue;
                    }
                }

                String jsonReq = gson.toJson(req);
                out.println(jsonReq);
                System.out.println("[ENVIADO] " + jsonReq);

                String resp = in.readLine();
                if (resp == null) {
                    System.out.println("Servidor encerrou a conexão.");
                    break;
                }
                System.out.println("[RECEBIDO] " + resp);

                try {
                    JsonObject jresp = JsonParser.parseString(resp).getAsJsonObject();
                    if (jresp.has("token")) {
                        token = jresp.get("token").getAsString();
                        System.out.println("Token salvo localmente.");
                    }
                 
                    if (req.has("operacao") && req.get("operacao").getAsString().equals("LOGOUT")) {
                        if (jresp.has("status") && jresp.get("status").getAsString().equals("200")) {
                            token = null;
                            System.out.println("Token limpo localmente (logout).");
                        }
                    }
                } catch (Exception ignored) {}
            }

        } catch (IOException e) {
            System.err.println("Erro de rede: " + e.getMessage());
        }
    }
    private JsonObject criarObjetoFilme(Scanner sc, boolean pedirId) {
        JsonObject filme = new JsonObject();
        
        if (pedirId) {
            System.out.print("ID do Filme: ");
            filme.addProperty("id", sc.nextLine().trim());
        }
        
        System.out.print("Título: ");
        filme.addProperty("titulo", sc.nextLine().trim());
        System.out.print("Diretor: ");
        filme.addProperty("diretor", sc.nextLine().trim());
        System.out.print("Ano: ");
        filme.addProperty("ano", sc.nextLine().trim());
        System.out.print("Sinopse: ");
        filme.addProperty("sinopse", sc.nextLine().trim());
        
        JsonArray generos = new JsonArray();
        while (true) {
            System.out.print("Adicionar Gênero (ou deixe em branco para parar): ");
            String g = sc.nextLine().trim();
            if (g.isEmpty()) {
                break;
            }
            generos.add(g);
        }
        filme.add("genero", generos);
        
        return filme;
    }
}
