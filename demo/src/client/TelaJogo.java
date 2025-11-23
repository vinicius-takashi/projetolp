package demo.src.client;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.PrintStream;
import java.util.Random;

public class TelaJogo extends JFrame {

    private PainelSnake painel;
    
    // Dados recebidos do Menu
    private Socket socket;
    private String usuario;
    private PrintStream saida; // Para enviar pontuação ao servidor

    public TelaJogo(Socket socket, String usuario, PrintStream saida, Color corCobra, int delayVelocidade) {
        super("Snake Game - Jogando...");
        this.socket = socket;
        this.usuario = usuario;
        this.saida = saida;

        // Configuração da Janela
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Instancia a lógica do jogo com as configs escolhidas
        painel = new PainelSnake(corCobra, delayVelocidade);
        add(painel);
        
        pack(); // Ajusta o tamanho da janela ao tamanho do painel
        setLocationRelativeTo(null);
    }

    // --- CLASSE INTERNA: O JOGO ---
    // Atende Req 3.8 (Interface Manual) e Lógica Complexa
    private class PainelSnake extends JPanel implements ActionListener, KeyListener {
        
        private final int LARGURA_TELA = 600;
        private final int ALTURA_TELA = 600;
        private final int TAMANHO_BLOCO = 25; // Tamanho de cada quadrado
        private final int UNIDADES = (LARGURA_TELA * ALTURA_TELA) / (TAMANHO_BLOCO * TAMANHO_BLOCO);
        
        // Arrays para guardar as coordenadas X e Y do corpo da cobra
        private final int x[] = new int[UNIDADES];
        private final int y[] = new int[UNIDADES];
        
        private int partesCorpo = 5; // Tamanho inicial
        private int pontos = 0;
        private int comidaX;
        private int comidaY;
        
        private char direcao = 'D'; // C=Cima, B=Baixo, E=Esquerda, D=Direita
        private boolean rodando = false;
        
        private Timer timer;
        private Color corCobra;
        private Random random;

        // Req 3.12: Log de movimentos para salvar em arquivo depois
        private StringBuilder logMovimentos; 

        public PainelSnake(Color cor, int delay) {
            this.corCobra = cor;
            random = new Random();
            logMovimentos = new StringBuilder();
            
            setPreferredSize(new Dimension(LARGURA_TELA, ALTURA_TELA));
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this); // Escuta o teclado
            
            iniciarJogo(delay);
        }

        public void iniciarJogo(int delay) {
            criarComida();
            rodando = true;
            timer = new Timer(delay, this); // 'this' refere-se ao ActionListener (loop)
            timer.start();
        }

        // Req 3.8: Desenho manual sem componentes prontos
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            desenhar(g);
        }

        public void desenhar(Graphics g) {
            if (rodando) {
                // (Opcional) Desenhar linhas de grid para facilitar visualização
                // g.setColor(Color.DARK_GRAY);
                // for(int i=0; i<LARGURA_TELA/TAMANHO_BLOCO; i++) {
                //     g.drawLine(i*TAMANHO_BLOCO, 0, i*TAMANHO_BLOCO, ALTURA_TELA);
                //     g.drawLine(0, i*TAMANHO_BLOCO, LARGURA_TELA, i*TAMANHO_BLOCO);
                // }

                // Desenhar Comida
                g.setColor(Color.RED);
                g.fillOval(comidaX, comidaY, TAMANHO_BLOCO, TAMANHO_BLOCO);

                // Desenhar Cobra
                for (int i = 0; i < partesCorpo; i++) {
                    if (i == 0) { // Cabeça
                        g.setColor(Color.WHITE); // Cabeça branca pra destacar
                    } else { // Corpo
                        g.setColor(corCobra);
                    }
                    g.fillRect(x[i], y[i], TAMANHO_BLOCO, TAMANHO_BLOCO);
                }

                // Desenhar Placar
                g.setColor(Color.WHITE);
                g.setFont(new Font("Ink Free", Font.BOLD, 25));
                FontMetrics metrics = getFontMetrics(g.getFont());
                g.drawString("Pontos: " + pontos, (LARGURA_TELA - metrics.stringWidth("Pontos: " + pontos)) / 2, g.getFont().getSize());
            } else {
                fimDeJogo(g);
            }
        }

        public void criarComida() {
            comidaX = random.nextInt((int) (LARGURA_TELA / TAMANHO_BLOCO)) * TAMANHO_BLOCO;
            comidaY = random.nextInt((int) (ALTURA_TELA / TAMANHO_BLOCO)) * TAMANHO_BLOCO;
        }

        public void mover() {
            // Move o corpo seguindo a cabeça (de trás pra frente)
            for (int i = partesCorpo; i > 0; i--) {
                x[i] = x[i - 1];
                y[i] = y[i - 1];
            }

            // Move a cabeça
            switch (direcao) {
                case 'C': y[0] = y[0] - TAMANHO_BLOCO; break;
                case 'B': y[0] = y[0] + TAMANHO_BLOCO; break;
                case 'E': x[0] = x[0] - TAMANHO_BLOCO; break;
                case 'D': x[0] = x[0] + TAMANHO_BLOCO; break;
            }
            
            // Registra movimento para o log (Req 3.12)
            logMovimentos.append(direcao).append(" ");
        }

        public void checarComida() {
            if ((x[0] == comidaX) && (y[0] == comidaY)) {
                partesCorpo++;
                pontos++;
                criarComida();
            }
        }

        public void checarColisoes() {
            // Verifica se cabeça bateu no corpo
            for (int i = partesCorpo; i > 0; i--) {
                if ((x[0] == x[i]) && (y[0] == y[i])) {
                    rodando = false;
                }
            }
            // Verifica se bateu nas paredes
            if (x[0] < 0 || x[0] >= LARGURA_TELA || y[0] < 0 || y[0] >= ALTURA_TELA) {
                rodando = false;
            }

            if (!rodando) {
                timer.stop();
                processarFimDeJogo();
            }
        }

        // Loop principal chamado pelo Timer
        @Override
        public void actionPerformed(ActionEvent e) {
            if (rodando) {
                mover();
                checarComida();
                checarColisoes();
            }
            repaint(); // Chama o paintComponent novamente para redesenhar a tela
        }

        // Controle via Teclado
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (direcao != 'D') direcao = 'E'; // Não pode virar 180 graus
                    break;
                case KeyEvent.VK_RIGHT:
                    if (direcao != 'E') direcao = 'D';
                    break;
                case KeyEvent.VK_UP:
                    if (direcao != 'B') direcao = 'C';
                    break;
                case KeyEvent.VK_DOWN:
                    if (direcao != 'C') direcao = 'B';
                    break;
            }
        }
        
        // Métodos KeyListener não usados
        @Override public void keyTyped(KeyEvent e) {}
        @Override public void keyReleased(KeyEvent e) {}

        private void fimDeJogo(Graphics g) {
            // Texto de Game Over
            g.setColor(Color.RED);
            g.setFont(new Font("Ink Free", Font.BOLD, 75));
            FontMetrics metrics1 = getFontMetrics(g.getFont());
            g.drawString("Game Over", (LARGURA_TELA - metrics1.stringWidth("Game Over")) / 2, ALTURA_TELA / 2);
            
            // Texto Pontuação
            g.setColor(Color.WHITE);
            g.setFont(new Font("Ink Free", Font.BOLD, 40));
            FontMetrics metrics2 = getFontMetrics(g.getFont());
            g.drawString("Pontos: " + pontos, (LARGURA_TELA - metrics2.stringWidth("Pontos: " + pontos)) / 2, ALTURA_TELA / 2 + 50);
        }

        private void processarFimDeJogo() {
            // Req 3.14: Enviar dados ao Servidor
            try {
                saida.println("SALVAR_PONTOS;" + usuario + ";" + pontos);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(TelaJogo.this, "Erro ao salvar pontos no servidor!");
            }

            // Req 3.12: Salvar Arquivo Texto Local
            salvarArquivoLocal();

            // Mensagem Final
            JOptionPane.showMessageDialog(TelaJogo.this, "Fim de Jogo! Seus pontos foram salvos.");
            TelaJogo.this.dispose(); // Fecha a tela do jogo
        }

        // Req 3.12: Implementação de acesso a arquivos texto
        private void salvarArquivoLocal() {
            String nomeArquivo = "historico_" + usuario + ".txt";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String dataHora = sdf.format(new Date());

            try (FileWriter fw = new FileWriter(nomeArquivo, true); // true = append (adiciona no fim)
                 PrintWriter pw = new PrintWriter(fw)) {
                
                pw.println("--- Partida ---");
                pw.println("Data: " + dataHora);
                pw.println("Pontos: " + pontos);
                pw.println("Movimentos: " + logMovimentos.toString()); // Salva sequencia WDSA...
                pw.println("----------------");
                
                System.out.println("Arquivo de log salvo em: " + new File(nomeArquivo).getAbsolutePath());
                
            } catch (IOException e) {
                System.err.println("Erro ao salvar arquivo local: " + e.getMessage());
            }
        }
    }
}
