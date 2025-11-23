package com.example;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorSnake {
    private static final int PORTA = 12345;
    private static final int MAX_CLIENTES = 4; // Req 3.14
    private static AtomicInteger clientesConectados = new AtomicInteger(0);

    // Configuração do Banco de Dados (Req 3.2 e 3.6)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/db_snake";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "sua_senha_aqui"; // <--- ALTERE AQUI

    public static void main(String[] args) {
        System.out.println("--- Servidor Snake Iniciado ---");
        System.out.println("Porta: " + PORTA);
        System.out.println("Máximo de jogadores: " + MAX_CLIENTES);

        try (ServerSocket server = new ServerSocket(PORTA)) {
            while (true) {
                System.out.println("Aguardando conexões...");
                Socket socket = server.accept();

                if (clientesConectados.get() < MAX_CLIENTES) {
                    clientesConectados.incrementAndGet();
                    System.out.println("Cliente conectado! Total: " + clientesConectados.get());
                    
                    // Inicia uma nova Thread para este cliente (Req 3.14)
                    new Thread(new ClientHandler(socket)).start();
                } else {
                    System.out.println("Conexão rejeitada: Servidor cheio.");
                    PrintStream out = new PrintStream(socket.getOutputStream());
                    out.println("ERRO;SERVIDOR_CHEIO");
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- CLASSE CLIENT HANDLER ---
    // Responsável por tratar as requisições de UM cliente específico
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private Connection conn;
        private Scanner entrada;
        private PrintStream saida;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            conectarBanco();
        }

        private void conectarBanco() {
            try {
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            } catch (SQLException e) {
                System.err.println("Erro ao conectar no MySQL: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                entrada = new Scanner(socket.getInputStream());
                saida = new PrintStream(socket.getOutputStream());

                while (entrada.hasNextLine()) {
                    String mensagem = entrada.nextLine();
                    processarComando(mensagem);
                }

            } catch (Exception e) {
                System.out.println("Erro na comunicação com cliente: " + e.getMessage());
            } finally {
                // Limpeza ao desconectar
                clientesConectados.decrementAndGet();
                try { if (conn != null) conn.close(); } catch (SQLException e) {}
                try { socket.close(); } catch (IOException e) {}
                System.out.println("Cliente desconectou.");
            }
        }

        private void processarComando(String mensagem) throws SQLException {
            String[] partes = mensagem.split(";");
            String comando = partes[0];

            switch (comando) {
                case "LOGIN": // LOGIN;user;hash
                    if (partes.length < 3) return;
                    if (autenticarUsuario(partes[1], partes[2])) {
                        saida.println("LOGIN_OK");
                    } else {
                        saida.println("LOGIN_FAIL");
                    }
                    break;

                case "CADASTRAR": // CADASTRAR;user;hash
                    if (partes.length < 3) return;
                    if (cadastrarUsuario(partes[1], partes[2])) {
                        saida.println("CADASTRO_OK");
                    } else {
                        saida.println("CADASTRO_ERRO");
                    }
                    break;

                case "SALVAR_PONTOS": // SALVAR_PONTOS;user;pontos
                    salvarPontos(partes[1], Integer.parseInt(partes[2]));
                    break;

                case "GET_RANKING":
                    saida.println(buscarRanking());
                    break;
            }
        }

        // --- MÉTODOS DE BANCO DE DADOS (JDBC) ---

        private boolean autenticarUsuario(String user, String hash) throws SQLException {
            String sql = "SELECT id FROM usuarios WHERE login = ? AND senha_hash = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user);
                stmt.setString(2, hash); // A senha já chega hasheada do cliente (Req 3.13)
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        }

        private boolean cadastrarUsuario(String user, String hash) {
            String sql = "INSERT INTO usuarios (login, senha_hash) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user);
                stmt.setString(2, hash);
                stmt.execute();
                return true;
            } catch (SQLException e) {
                return false; // Provavelmente duplicidade de usuário
            }
        }

        private void salvarPontos(String user, int pontos) {
            String sql = "INSERT INTO ranking (usuario_login, pontos) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user);
                stmt.setInt(2, pontos);
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private String buscarRanking() {
            StringBuilder sb = new StringBuilder("RANKING_DATA;");
            String sql = "SELECT usuario_login, pontos FROM ranking ORDER BY pontos DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    sb.append(rs.getString("usuario_login"))
                      .append(":")
                      .append(rs.getInt("pontos"))
                      .append("#");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return sb.toString(); // Retorna ex: RANKING_DATA;User1:100#User2:90#
        }
    }
}
