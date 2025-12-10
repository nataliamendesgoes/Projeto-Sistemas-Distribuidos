# 🎬 VoteFlix --- Sistema de Avaliação de Filmes

VoteFlix é um sistema distribuído cliente-servidor desenvolvido em
**Java**, usando **Sockets TCP/IP**, **MySQL**, e uma interface gráfica
construída com **Java Swing**.\
O sistema permite cadastro de usuários, login, listagem de filmes,
criação de reviews e gestão completa para administradores.

------------------------------------------------------------------------

## 🚀 Funcionalidades

### 👤 Usuário Comum

-   Criar conta e fazer login\
-   Listar filmes\
-   Buscar filmes por ID\
-   Criar review (nota + comentário)\
-   Editar e excluir suas próprias reviews\
-   Editar perfil e excluir conta

### 🛡️ Administrador

-   Criar, editar e excluir filmes\
-   Excluir reviews de qualquer usuário\
-   Gerenciar usuários

------------------------------------------------------------------------

## 🛠️ Tecnologias Utilizadas

-   Java 17+\
-   Java Swing (GUI)\
-   Sockets TCP/IP\
-   Google Gson (JSON)\
-   MySQL 8.0 + JDBC\
-   Arquitetura Cliente/Servidor

------------------------------------------------------------------------

## 📂 Estrutura do Projeto

    src/
    ├── Cliente/          # Lógica de rede (client-side)
    ├── Conexao/          # Conexão JDBC MySQL
    ├── Interface/        # Telas (ClientGUI, ServerGUI, Tema)
    └── Servidor/         # Lógica servidor + DAO
        ├── Servidor.java
        ├── Usuarios.java
        ├── Filme.java
        └── Review.java

------------------------------------------------------------------------

# 📸 Screenshots da Interface

## ➤ Tela Inicial --- Área de Filmes

![Tela de Filmes](./Screenshot_141151.png)

------------------------------------------------------------------------

## ➤ Listagem de Filmes

![Listagem de Filmes](./Screenshot_141213.png)

------------------------------------------------------------------------

## ➤ Detalhes do Filme + Reviews

![Detalhes e Reviews](./Screenshot_141239.png)

------------------------------------------------------------------------

## ➤ Tela do Servidor (Logs)

![Logs Servidor](./Screenshot_141446.png)

------------------------------------------------------------------------

# 🔧 Configuração do Ambiente

## 1️⃣ Banco de Dados

Crie o banco:

``` sql
CREATE DATABASE sisdistribuidos;
```

Execute o script das tabelas e insira o admin inicial:

``` sql
INSERT INTO usuarios (nome, senha, funcao)
VALUES ('admin', 'admin123', 'admin');
```

------------------------------------------------------------------------

## 2️⃣ Configure o JDBC

No arquivo:

`src/Conexao/conexao.java`

``` java
private static final String USER = "root";
private static final String PASSWORD = "SUA_SENHA_AQUI";
```

------------------------------------------------------------------------

## 3️⃣ Rodando o Servidor

``` bash
java Interface.ServerGUI
```

Clique em **INICIAR**.

------------------------------------------------------------------------

## 4️⃣ Rodando o Cliente

``` bash
java Interface.ClientGUI
```

Conecte:

-   IP: **ip do computador**\
-   Porta: **5000**

------------------------------------------------------------------------

# 📝 Licença

Projeto desenvolvido para fins educacionais na disciplina de **Sistemas
Distribuídos**.

------------------------------------------------------------------------

# 👩‍💻 Autor(a)

**Natália Mendes**
