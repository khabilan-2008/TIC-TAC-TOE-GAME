import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Arrays;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.EmptyBorder;


public class TicTacToe extends JFrame {

    // ---------- STATE ----------
    JPanel mainPanel;

    char[] board = new char[9];

    String gameMode = "";
    String difficulty = "Easy";

    String player1 = "";
    String player2 = "";
    String humanName = "";
    String computerName = "Computer";

    String xName = "X";
    String oName = "O";

    char humanSymbol = 'X';
    char computerSymbol = 'O';
    char currentPlayer = 'X';
    char starter = 'X';

    boolean gameOver = false;
    boolean computerThinking = false;

    int xScore = 0;
    int oScore = 0;
    int draws = 0;

    Timer computerTimer;

    BoardPanel boardPanel;
    ScoreCard cardX, cardO, cardDraw;
    JLabel status;

    RoundedField nameField, player1Field, player2Field;
    JLabel errorLabel;

    // ---------- THEME ----------
    Color background, bgBottom, panelColor, accent, secondary, muted;
    Color textColor = Color.WHITE;

    Font titleFont = new Font("SansSerif", Font.BOLD, 30);
    Font normalFont = new Font("SansSerif", Font.BOLD, 15);

    Random random = new Random();

    static final int[][] WINS = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
        {0, 4, 8}, {2, 4, 6}
    };

    TicTacToe() {
        setTitle("Tic Tac Toe");
        setSize(500, 720);
        setMinimumSize(new Dimension(440, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        generateTheme();

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(background);
        setContentPane(mainPanel);

        Arrays.fill(board, ' ');
        showModeScreen();

        setVisible(true);
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.max(0, Math.min(255, a)));
    }

    static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static float ease(float p) {
        p = Math.max(0f, Math.min(1f, p));
        float q = 1f - p;
        return 1f - q * q * q;
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }

    static String ellipsize(String s, FontMetrics fm, int maxW) {
        if (fm.stringWidth(s) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "...") > maxW) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    static void drawCentered(Graphics2D g2, String t, double cx, double baseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(t, (float) (cx - fm.stringWidth(t) / 2.0), (float) baseline);
    }

    Font fitFont(Graphics2D g2, String text, int style, float size, double maxW) {
        Font f = normalFont.deriveFont(style, size);
        while (size > 12 && g2.getFontMetrics(f).stringWidth(text) > maxW) {
            size -= 1;
            f = f.deriveFont(size);
        }
        return f;
    }

    Color colorOf(char symbol) {
        return symbol == 'X' ? accent : secondary;
    }

    String nameOf(char symbol) {
        return symbol == 'X' ? xName : oName;
    }

    boolean isSingle() {
        return gameMode.equals("Single Player");
    }

    /** Draws an X or O with an animated stroke and a soft glow. */
    void drawMark(Graphics2D g2, char sym, double cx, double cy,
                  double r, float progress, float alphaMul) {

        float p = ease(progress);
        if (p <= 0.001f) return;

        Color c = colorOf(sym);
        float sw = (float) Math.max(3.0, r * 0.30);

        for (int pass = 0; pass < 2; pass++) {

            float width = pass == 0 ? sw * 2.3f : sw;
            int a = pass == 0 ? (int) (48 * alphaMul) : (int) (255 * alphaMul);

            g2.setStroke(new BasicStroke(width,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(alpha(c, a));

            if (sym == 'X') {

                double ax = cx - r, ay = cy - r, bx = cx + r, by = cy + r;
                double t1 = Math.min(1.0, p * 2.0);

                g2.draw(new Line2D.Double(ax, ay,
                        ax + (bx - ax) * t1, ay + (by - ay) * t1));

                if (p > 0.5f) {
                    double t2 = (p - 0.5) * 2.0;
                    double cx2 = cx + r, cy2 = cy - r, dx = cx - r, dy = cy + r;
                    g2.draw(new Line2D.Double(cx2, cy2,
                            cx2 + (dx - cx2) * t2, cy2 + (dy - cy2) * t2));
                }
            } else {
                g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2,
                        90, -360.0 * p, Arc2D.OPEN));
            }
        }
    }

    void drawWinLine(Graphics2D g2, double x1, double y1, double x2, double y2,
                     double ext, float p, Color col, float sw) {

        if (p <= 0f) return;

        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        dx /= len;
        dy /= len;

        double sx = x1 - dx * ext, sy = y1 - dy * ext;
        double total = len + 2 * ext;
        double ex = sx + dx * total * p, ey = sy + dy * total * p;

        g2.setStroke(new BasicStroke(sw * 2.4f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(alpha(col, 55));
        g2.draw(new Line2D.Double(sx, sy, ex, ey));

        g2.setStroke(new BasicStroke(sw,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(blend(col, Color.WHITE, 0.55f));
        g2.draw(new Line2D.Double(sx, sy, ex, ey));
    }

    // =====================================================================
    //  THEME
    // =====================================================================

    void generateTheme() {

        Color[][] themes = {
            {
                new Color(15, 10, 35), new Color(35, 22, 65),
                new Color(0, 225, 255), new Color(255, 80, 190)
            },
            {
                new Color(10, 20, 35), new Color(20, 45, 65),
                new Color(0, 255, 170), new Color(80, 150, 255)
            },
            {
                new Color(30, 10, 35), new Color(65, 20, 70),
                new Color(255, 100, 190), new Color(180, 100, 255)
            },
            {
                new Color(10, 15, 30), new Color(25, 35, 65),
                new Color(255, 210, 70), new Color(0, 220, 255)
            }
        };

        Color[] t = themes[random.nextInt(themes.length)];

        background = t[0];
        panelColor = t[1];
        accent = t[2];
        secondary = t[3];
        bgBottom = blend(t[0], t[1], 0.55f);
        muted = blend(textColor, background, 0.45f);
    }

    // =====================================================================
    //  CUSTOM COMPONENTS
    // =====================================================================

    /** Gradient background with two soft neon glows. */
    class GradientPanel extends JPanel {

        GradientPanel() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);

            int w = getWidth(), h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, background, 0, h, bgBottom));
            g2.fillRect(0, 0, w, h);

            float radius = Math.max(w, h) * 0.65f;
            float[] fr = {0f, 1f};

            g2.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), radius, fr,
                    new Color[]{alpha(accent, 48), alpha(accent, 0)}));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new RadialGradientPaint(new Point2D.Float(w, h), radius, fr,
                    new Color[]{alpha(secondary, 48), alpha(secondary, 0)}));
            g2.fillRect(0, 0, w, h);

            g2.dispose();
        }
    }

    /** Rounded button with hover glow. Supports sub-text, primary and selected styles. */
    class RoundedButton extends JButton {

        Color color;
        String sub;
        boolean primary;
        float hover = 0f;
        boolean hovering = false;
        Timer hoverTimer;

        RoundedButton(String text, Color color) {
            super(text);
            this.color = color;

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(normalFont);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(304, 58));

            hoverTimer = new Timer(15, e -> {
                hover += hovering ? 0.16f : -0.16f;
                if (hover >= 1f) { hover = 1f; hoverTimer.stop(); }
                if (hover <= 0f) { hover = 0f; hoverTimer.stop(); }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    hoverTimer.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    hoverTimer.start();
                }
            });
        }

        RoundedButton sub(String s) {
            this.sub = s;
            return this;
        }

        RoundedButton primary() {
            this.primary = true;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);

            int w = getWidth(), h = getHeight();
            boolean pressed = getModel().isPressed();

            RoundRectangle2D shape =
                    new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 22, 22);

            Color fill, textCol;

            if (primary) {
                fill = blend(color, Color.WHITE, hover * 0.25f);
                textCol = background;
            } else if (isSelected()) {
                fill = alpha(color, 80 + (int) (60 * hover));
                textCol = Color.WHITE;
            } else {
                fill = blend(panelColor, color, 0.10f + 0.18f * hover);
                textCol = blend(color, Color.WHITE, hover * 0.45f);
            }

            if (pressed) fill = blend(fill, Color.BLACK, 0.18f);

            float glow = Math.max(hover, (isSelected() || primary) ? 0.5f : 0f);
            if (glow > 0f) {
                g2.setStroke(new BasicStroke(8f));
                g2.setColor(alpha(color, (int) (60 * glow)));
                g2.draw(shape);
            }

            g2.setColor(fill);
            g2.fill(shape);

            if (!primary) {
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(alpha(color, 150 + (int) (105 * hover)));
                g2.draw(shape);
            }

            int off = pressed ? 1 : 0;
            FontMetrics fm = g2.getFontMetrics(getFont());
            g2.setFont(getFont());

            if (sub == null) {
                int y = (h - fm.getHeight()) / 2 + fm.getAscent() + off;
                g2.setColor(textCol);
                drawCentered(g2, getText(), w / 2.0, y);
            } else {
                Font sf = normalFont.deriveFont(Font.PLAIN, 12f);
                FontMetrics sfm = g2.getFontMetrics(sf);

                int total = fm.getHeight() + sfm.getHeight();
                int start = (h - total) / 2;

                g2.setColor(textCol);
                drawCentered(g2, getText(), w / 2.0, start + fm.getAscent() + off);

                g2.setFont(sf);
                g2.setColor(primary ? alpha(background, 200) : muted);
                drawCentered(g2, sub, w / 2.0,
                        start + fm.getHeight() + sfm.getAscent() + off);
            }

            g2.dispose();
        }
    }

    /** Rounded text field with focus glow and placeholder. */
    class RoundedField extends JTextField {

        String placeholder;
        Color border;

        RoundedField(String placeholder, Color border) {
            this.placeholder = placeholder;
            this.border = border;

            setOpaque(false);
            setFont(normalFont.deriveFont(16f));
            setHorizontalAlignment(CENTER);
            setForeground(textColor);
            setCaretColor(textColor);
            setSelectionColor(alpha(border, 120));
            setBorder(new EmptyBorder(8, 14, 8, 14));
            setPreferredSize(new Dimension(304, 48));

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) { repaint(); }

                @Override
                public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);

            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape =
                    new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 18, 18);

            g2.setColor(alpha(panelColor, 235));
            g2.fill(shape);

            boolean focused = hasFocus();

            if (focused) {
                g2.setStroke(new BasicStroke(7f));
                g2.setColor(alpha(border, 45));
                g2.draw(shape);
            }

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(focused ? border : alpha(border, 130));
            g2.draw(shape);

            g2.dispose();

            super.paintComponent(g);

            if (getText().isEmpty() && !focused) {
                Graphics2D g3 = (Graphics2D) g.create();
                aa(g3);
                g3.setFont(getFont().deriveFont(Font.PLAIN));
                g3.setColor(alpha(muted, 200));
                FontMetrics fm = g3.getFontMetrics();
                drawCentered(g3, placeholder, w / 2.0,
                        (h - fm.getHeight()) / 2 + fm.getAscent());
                g3.dispose();
            }
        }
    }

    /** Small card that shows a player's score. */
    class ScoreCard extends JPanel {

        String name = "";
        String tag = "";
        int score = 0;
        boolean active = false;
        Color color;

        ScoreCard(Color color) {
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(130, 84));
        }

        void set(String name, String tag, int score, boolean active) {
            this.name = name;
            this.tag = tag;
            this.score = score;
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);

            int w = getWidth(), h = getHeight();
            RoundRectangle2D r = new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 20, 20);

            g2.setColor(alpha(panelColor, active ? 240 : 170));
            g2.fill(r);

            if (active) {
                g2.setStroke(new BasicStroke(7f));
                g2.setColor(alpha(color, 40));
                g2.draw(r);
            }

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(active ? color : alpha(color, 70));
            g2.draw(r);

            g2.setFont(normalFont.deriveFont(11f));
            FontMetrics fm = g2.getFontMetrics();

            String pre = tag.isEmpty() ? "" : tag + "  ";
            String nm = ellipsize(name.toUpperCase(), fm,
                    w - 34 - fm.stringWidth(pre));

            int tw = fm.stringWidth(pre) + fm.stringWidth(nm);
            int x = (w - tw) / 2;
            int y = 28;

            g2.setColor(color);
            g2.drawString(pre, x, y);
            g2.setColor(muted);
            g2.drawString(nm, x + fm.stringWidth(pre), y);

            g2.setFont(titleFont.deriveFont(32f));
            fm = g2.getFontMetrics();
            String sc = String.valueOf(score);
            g2.setColor(active ? color : textColor);
            g2.drawString(sc, (w - fm.stringWidth(sc)) / 2, h - 20);

            g2.dispose();
        }
    }

    /** Little animated-style logo for the main menu. */
    class LogoPanel extends JPanel {

        LogoPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(120, 120));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);

            double s = Math.min(getWidth(), getHeight()) - 10;
            double x0 = (getWidth() - s) / 2, y0 = (getHeight() - s) / 2;
            double cs = s / 3, pad = cs * 0.08;

            g2.setStroke(new BasicStroke(4f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(alpha(muted, 150));

            for (int k = 1; k <= 2; k++) {
                g2.draw(new Line2D.Double(x0 + k * cs, y0 + pad, x0 + k * cs, y0 + s - pad));
                g2.draw(new Line2D.Double(x0 + pad, y0 + k * cs, x0 + s - pad, y0 + k * cs));
            }

            char[] m = {'X', ' ', 'O', ' ', 'X', ' ', 'O', ' ', 'X'};

            for (int i = 0; i < 9; i++) {
                if (m[i] == ' ') continue;
                double cx = x0 + (i % 3) * cs + cs / 2;
                double cy = y0 + (i / 3) * cs + cs / 2;
                drawMark(g2, m[i], cx, cy, cs * 0.28, 1f, 1f);
            }

            drawWinLine(g2,
                    x0 + cs / 2, y0 + cs / 2,
                    x0 + 2.5 * cs, y0 + 2.5 * cs,
                    cs * 0.30, 1f, accent, 4f);

            g2.dispose();
        }
    }

    /** The game board: a single custom-painted, animated component. */
    class BoardPanel extends JPanel {

        float[] prog = new float[9];
        int hover = -1;

        int[] winLine = null;
        float winProg = 0f;
        int overlayTicks = 0;
        float overlay = 0f;

        String line1 = "", line2 = "";
        Color resultColor = Color.WHITE;

        Timer anim;

        BoardPanel() {
            setOpaque(false);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setHover(cellAt(e.getX(), e.getY()));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setHover(-1);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (gameOver) {
                        if (overlay > 0.3f) newRound();
                        return;
                    }
                    handleMove(cellAt(e.getX(), e.getY()));
                }
            };

            addMouseListener(ma);
            addMouseMotionListener(ma);

            // keyboard: numpad layout
            InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();
            int[] map = {-1, 6, 7, 8, 3, 4, 5, 0, 1, 2};

            for (int d = 1; d <= 9; d++) {
                final int cell = map[d];
                String key = "cell" + d;
                im.put(KeyStroke.getKeyStroke(Character.forDigit(d, 10)), key);
                am.put(key, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handleMove(cell);
                    }
                });
            }

            im.put(KeyStroke.getKeyStroke("ENTER"), "again");
            am.put("again", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (gameOver) newRound();
                }
            });

            anim = new Timer(16, e -> tick());
        }

        @Override
        public void addNotify() {
            super.addNotify();
            anim.start();
        }

        @Override
        public void removeNotify() {
            anim.stop();
            super.removeNotify();
        }

        void reset() {
            Arrays.fill(prog, 0f);
            winLine = null;
            winProg = 0f;
            overlay = 0f;
            overlayTicks = 0;
            hover = -1;
            repaint();
        }

        void markPlaced(int i) {
            prog[i] = 0f;
        }

        void showResult(int[] line, String l1, String l2, Color c) {
            winLine = line;
            winProg = 0f;
            overlay = 0f;
            overlayTicks = 0;
            line1 = l1;
            line2 = l2;
            resultColor = c;
        }

        void setHover(int v) {
            if (v != hover) {
                hover = v;
                setCursor(canPlay(v)
                        ? new Cursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                repaint();
            }
        }

        Rectangle2D area() {
            Insets in = getInsets();
            int aw = getWidth() - in.left - in.right;
            int ah = getHeight() - in.top - in.bottom;
            int size = Math.max(60, Math.min(aw, ah) - 16);
            double x = in.left + (aw - size) / 2.0;
            double y = in.top + (ah - size) / 2.0;
            return new Rectangle2D.Double(x, y, size, size);
        }

        int cellAt(int mx, int my) {
            Rectangle2D a = area();
            if (!a.contains(mx, my)) return -1;
            double cs = a.getWidth() / 3;
            int c = (int) ((mx - a.getX()) / cs);
            int r = (int) ((my - a.getY()) / cs);
            c = Math.max(0, Math.min(2, c));
            r = Math.max(0, Math.min(2, r));
            return r * 3 + c;
        }

        boolean inWin(int i) {
            if (winLine == null) return false;
            for (int w : winLine) if (w == i) return true;
            return false;
        }

        void tick() {
            boolean changed = false;
            boolean marksDone = true;

            for (int i = 0; i < 9; i++) {
                if (board[i] != ' ' && prog[i] < 1f) {
                    prog[i] = Math.min(1f, prog[i] + 0.08f);
                    changed = true;
                    if (prog[i] < 1f) marksDone = false;
                }
            }

            if (gameOver && marksDone) {
                if (winLine != null && winProg < 1f) {
                    winProg = Math.min(1f, winProg + 0.045f);
                    changed = true;
                } else {
                    overlayTicks++;
                    if (overlayTicks > 22 && overlay < 1f) {
                        overlay = Math.min(1f, overlay + 0.07f);
                        changed = true;
                    }
                }
            }

            if (changed) repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);

            Rectangle2D a = area();
            double s = a.getWidth(), cs = s / 3;
            double x0 = a.getX(), y0 = a.getY();

            // board backing
            RoundRectangle2D back =
                    new RoundRectangle2D.Double(x0 - 8, y0 - 8, s + 16, s + 16, 32, 32);

            g2.setColor(alpha(panelColor, 190));
            g2.fill(back);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(alpha(accent, 70));
            g2.draw(back);

            // grid lines
            g2.setStroke(new BasicStroke((float) Math.max(3, s / 90),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(alpha(muted, 110));
            double pad = cs * 0.12;

            for (int k = 1; k <= 2; k++) {
                g2.draw(new Line2D.Double(x0 + k * cs, y0 + pad, x0 + k * cs, y0 + s - pad));
                g2.draw(new Line2D.Double(x0 + pad, y0 + k * cs, x0 + s - pad, y0 + k * cs));
            }

            boolean won = winLine != null && winProg > 0f;
            Color winColor = winLine != null ? colorOf(board[winLine[0]]) : accent;

            // hover highlight + ghost mark
            if (canPlay(hover)) {
                int c = hover % 3, r = hover / 3;
                g2.setColor(alpha(colorOf(currentPlayer), 30));
                g2.fill(new RoundRectangle2D.Double(
                        x0 + c * cs + 10, y0 + r * cs + 10, cs - 20, cs - 20, 22, 22));
                drawMark(g2, currentPlayer,
                        x0 + c * cs + cs / 2, y0 + r * cs + cs / 2,
                        cs * 0.26, 1f, 0.28f);
            }

            // winning cells highlight
            if (won) {
                g2.setColor(alpha(winColor, (int) (45 * winProg)));
                for (int idx : winLine) {
                    int c = idx % 3, r = idx / 3;
                    g2.fill(new RoundRectangle2D.Double(
                            x0 + c * cs + 10, y0 + r * cs + 10, cs - 20, cs - 20, 22, 22));
                }
            }

            // marks
            for (int i = 0; i < 9; i++) {
                if (board[i] == ' ') continue;
                int c = i % 3, r = i / 3;
                float am = (won && !inWin(i)) ? 1f - 0.65f * winProg : 1f;
                drawMark(g2, board[i],
                        x0 + c * cs + cs / 2, y0 + r * cs + cs / 2,
                        cs * 0.26, prog[i], am);
            }

            // winning line
            if (won) {
                int i0 = winLine[0], i2 = winLine[2];
                drawWinLine(g2,
                        x0 + (i0 % 3) * cs + cs / 2, y0 + (i0 / 3) * cs + cs / 2,
                        x0 + (i2 % 3) * cs + cs / 2, y0 + (i2 / 3) * cs + cs / 2,
                        cs * 0.32, ease(winProg), winColor,
                        (float) Math.max(5, cs * 0.07));
            }

            // result overlay
            if (overlay > 0f) {
                float o = overlay;

                g2.setColor(alpha(background, (int) (225 * o)));
                g2.fill(back);

                double cx = x0 + s / 2, cy = y0 + s / 2;

                g2.setFont(fitFont(g2, line1, Font.BOLD,
                        (float) Math.max(20, cs * 0.30), s * 0.88));
                g2.setColor(alpha(resultColor, (int) (255 * o)));
                drawCentered(g2, line1, cx, cy - 4);

                g2.setFont(normalFont.deriveFont(Font.PLAIN, 16f));
                g2.setColor(alpha(muted, (int) (255 * o)));
                drawCentered(g2, line2, cx, cy + 28);

                g2.setFont(normalFont.deriveFont(Font.BOLD, 13f));
                g2.setColor(alpha(accent, (int) (255 * o)));
                drawCentered(g2, "Click anywhere or press ENTER to play again",
                        cx, cy + s * 0.22);
            }

            g2.dispose();
        }
    }

    /** Small helper to stack centered components in a GridBagLayout. */
    class Form {

        final GradientPanel panel = new GradientPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        int row = 0;

        Form() {
            panel.setLayout(new GridBagLayout());
            gbc.gridx = 0;
        }

        void add(Component c) {
            add(c, 6, 6);
        }

        void add(Component c, int top, int bottom) {
            gbc.gridy = row++;
            gbc.insets = new Insets(top, 24, bottom, 24);
            panel.add(c, gbc);
        }
    }

    // =====================================================================
    //  FACTORIES
    // =====================================================================

    JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    RoundedButton createButton(String text, Color color) {
        return new RoundedButton(text, color);
    }

    // =====================================================================
    //  SCREEN MANAGEMENT
    // =====================================================================

    void showScreen(JPanel panel) {
        stopComputerTimer();

        mainPanel.removeAll();
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    void stopComputerTimer() {
        if (computerTimer != null) {
            computerTimer.stop();
            computerTimer = null;
        }
        computerThinking = false;
    }

    // =====================================================================
    //  MENU SCREENS
    // =====================================================================

    void showModeScreen() {

        Form f = new Form();

        f.add(new LogoPanel(), 0, 4);
        f.add(createLabel("TIC TAC TOE", titleFont.deriveFont(38f), textColor), 4, 0);
        f.add(createLabel("Choose how you want to play", normalFont, muted), 0, 22);

        RoundedButton single = createButton("SINGLE PLAYER", accent)
                .sub("Challenge the computer");
        single.addActionListener(e -> {
            gameMode = "Single Player";
            showDifficultyScreen();
        });
        f.add(single);

        RoundedButton two = createButton("TWO PLAYERS", secondary)
                .sub("Play with a friend on this device");
        two.addActionListener(e -> {
            gameMode = "Two Players";
            showTwoPlayerDetailsScreen();
        });
        f.add(two);

        showScreen(f.panel);
    }

    void showDifficultyScreen() {

        Form f = new Form();

        f.add(createLabel("CHOOSE DIFFICULTY", titleFont, textColor), 0, 0);
        f.add(createLabel("Select your challenge level", normalFont, muted), 0, 18);

        RoundedButton easy = createButton("EASY", new Color(0, 220, 255))
                .sub("Random moves - relaxed play");
        RoundedButton medium = createButton("MEDIUM", new Color(255, 195, 60))
                .sub("Blocks and attacks, but slips up");
        RoundedButton hard = createButton("HARD", new Color(255, 80, 190))
                .sub("Unbeatable. Good luck!");

        easy.addActionListener(e -> { difficulty = "Easy"; showSinglePlayerDetailsScreen(); });
        medium.addActionListener(e -> { difficulty = "Medium"; showSinglePlayerDetailsScreen(); });
        hard.addActionListener(e -> { difficulty = "Hard"; showSinglePlayerDetailsScreen(); });

        f.add(easy);
        f.add(medium);
        f.add(hard);

        RoundedButton back = createButton("BACK", muted);
        back.setPreferredSize(new Dimension(304, 48));
        back.addActionListener(e -> showModeScreen());
        f.add(back, 14, 6);

        showScreen(f.panel);
    }

    JPanel createSymbolPicker() {

        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(304, 56));

        RoundedButton bx = createButton("X", accent);
        RoundedButton bo = createButton("O", secondary);

        bx.setFont(titleFont.deriveFont(22f));
        bo.setFont(titleFont.deriveFont(22f));

        bx.setSelected(humanSymbol == 'X');
        bo.setSelected(humanSymbol == 'O');

        bx.addActionListener(e -> {
            humanSymbol = 'X';
            bx.setSelected(true);
            bo.setSelected(false);
            bx.repaint();
            bo.repaint();
        });

        bo.addActionListener(e -> {
            humanSymbol = 'O';
            bo.setSelected(true);
            bx.setSelected(false);
            bx.repaint();
            bo.repaint();
        });

        p.add(bx);
        p.add(bo);
        return p;
    }

    void showSinglePlayerDetailsScreen() {

        Form f = new Form();

        f.add(createLabel("YOUR DETAILS", titleFont, accent), 0, 0);
        f.add(createLabel("Difficulty: " + difficulty, normalFont, secondary), 0, 16);

        f.add(createLabel("Your name", normalFont.deriveFont(13f), muted), 4, 0);

        nameField = new RoundedField("Enter your name", accent);
        nameField.setText(humanName);
        f.add(nameField);

        f.add(createLabel("Choose your symbol", normalFont.deriveFont(13f), muted), 10, 0);
        f.add(createSymbolPicker());

        errorLabel = createLabel(" ", normalFont.deriveFont(13f), new Color(255, 110, 130));
        f.add(errorLabel, 4, 0);

        ActionListener startAction = e -> {
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                errorLabel.setText("Please enter your name");
                nameField.requestFocusInWindow();
                return;
            }

            if (name.length() > 14) name = name.substring(0, 14);

            humanName = name;
            computerSymbol = humanSymbol == 'X' ? 'O' : 'X';
            xName = humanSymbol == 'X' ? humanName : computerName;
            oName = humanSymbol == 'O' ? humanName : computerName;

            startMatch();
        };

        nameField.addActionListener(startAction);

        RoundedButton start = createButton("START GAME", accent).primary();
        start.addActionListener(startAction);
        f.add(start, 8, 6);

        RoundedButton back = createButton("BACK", muted);
        back.setPreferredSize(new Dimension(304, 48));
        back.addActionListener(e -> showDifficultyScreen());
        f.add(back);

        showScreen(f.panel);
        SwingUtilities.invokeLater(() -> nameField.requestFocusInWindow());
    }

    void showTwoPlayerDetailsScreen() {

        Form f = new Form();

        f.add(createLabel("TWO PLAYER MODE", titleFont, accent), 0, 0);
        f.add(createLabel("Who's playing?", normalFont, muted), 0, 16);

        f.add(createLabel("Player 1  (X)", normalFont.deriveFont(13f), accent), 4, 0);
        player1Field = new RoundedField("Enter name", accent);
        player1Field.setText(player1);
        f.add(player1Field);

        f.add(createLabel("Player 2  (O)", normalFont.deriveFont(13f), secondary), 10, 0);
        player2Field = new RoundedField("Enter name", secondary);
        player2Field.setText(player2);
        f.add(player2Field);

        errorLabel = createLabel(" ", normalFont.deriveFont(13f), new Color(255, 110, 130));
        f.add(errorLabel, 6, 0);

        ActionListener startAction = e -> {
            String first = player1Field.getText().trim();
            String second = player2Field.getText().trim();

            if (first.isEmpty() || second.isEmpty()) {
                errorLabel.setText("Please enter both player names");
                return;
            }

            if (first.equalsIgnoreCase(second)) {
                errorLabel.setText("Please enter different player names");
                return;
            }

            if (first.length() > 14) first = first.substring(0, 14);
            if (second.length() > 14) second = second.substring(0, 14);

            player1 = first;
            player2 = second;
            xName = player1;
            oName = player2;

            startMatch();
        };

        player1Field.addActionListener(e -> player2Field.requestFocusInWindow());
        player2Field.addActionListener(startAction);

        RoundedButton start = createButton("START GAME", accent).primary();
        start.addActionListener(startAction);
        f.add(start, 8, 6);

        RoundedButton back = createButton("BACK", muted);
        back.setPreferredSize(new Dimension(304, 48));
        back.addActionListener(e -> showModeScreen());
        f.add(back);

        showScreen(f.panel);
        SwingUtilities.invokeLater(() -> player1Field.requestFocusInWindow());
    }

    // =====================================================================
    //  GAME SCREEN
    // =====================================================================

    void showGameScreen() {

        GradientPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout());

        // ---- top: mode, scoreboard, status ----
        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.setBorder(new EmptyBorder(20, 22, 4, 22));

        String mode = isSingle()
                ? "SINGLE PLAYER   |   " + difficulty.toUpperCase()
                : "TWO PLAYERS";

        north.add(createLabel(mode, normalFont.deriveFont(12f), muted), BorderLayout.NORTH);

        cardX = new ScoreCard(accent);
        cardO = new ScoreCard(secondary);
        cardDraw = new ScoreCard(muted);

        JPanel scores = new JPanel(new GridLayout(1, 3, 12, 0));
        scores.setOpaque(false);
        scores.add(cardX);
        scores.add(cardDraw);
        scores.add(cardO);
        north.add(scores, BorderLayout.CENTER);

        status = createLabel(" ", normalFont.deriveFont(18f), textColor);
        status.setBorder(new EmptyBorder(6, 0, 0, 0));
        north.add(status, BorderLayout.SOUTH);

        panel.add(north, BorderLayout.NORTH);

        // ---- center: board ----
        boardPanel = new BoardPanel();
        boardPanel.setBorder(new EmptyBorder(6, 22, 6, 22));
        panel.add(boardPanel, BorderLayout.CENTER);

        // ---- bottom: actions ----
        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(6, 22, 22, 22));

        RoundedButton again = createButton("NEW ROUND", accent);
        again.setPreferredSize(new Dimension(150, 56));
        again.addActionListener(e -> newRound());

        RoundedButton menu = createButton("MAIN MENU", secondary);
        menu.addActionListener(e -> showModeScreen());

        bottom.add(again);
        bottom.add(menu);
        panel.add(bottom, BorderLayout.SOUTH);

        showScreen(panel);
    }

    // =====================================================================
    //  MATCH / ROUND FLOW
    // =====================================================================

    void startMatch() {
        xScore = 0;
        oScore = 0;
        draws = 0;
        starter = 'X';

        showGameScreen();
        startRound();
    }

    void startRound() {
        stopComputerTimer();

        Arrays.fill(board, ' ');
        currentPlayer = starter;
        gameOver = false;

        if (boardPanel != null) boardPanel.reset();

        updateStatus();

        if (isSingle() && currentPlayer == computerSymbol) {
            computerMove();
        }
    }

    void newRound() {
        // the starting player alternates after every finished round
        if (gameOver) starter = starter == 'X' ? 'O' : 'X';
        startRound();
    }

    boolean canPlay(int pos) {
        return pos >= 0 && pos < 9
                && !gameOver
                && !computerThinking
                && board[pos] == ' '
                && (!isSingle() || currentPlayer == humanSymbol);
    }

    void handleMove(int pos) {
        if (!canPlay(pos)) return;
        placeMark(pos, currentPlayer);
    }

    void placeMark(int pos, char symbol) {

        board[pos] = symbol;
        boardPanel.markPlaced(pos);

        int[] line = getWinLine(symbol);

        if (line != null) {
            finishGame(symbol, line);
            return;
        }

        if (checkDraw()) {
            finishGame('D', null);
            return;
        }

        currentPlayer = symbol == 'X' ? 'O' : 'X';
        updateStatus();

        if (isSingle() && currentPlayer == computerSymbol) {
            computerMove();
        }
    }

    void refreshScores() {
        if (cardX == null) return;

        cardX.set(xName, "X", xScore, !gameOver && currentPlayer == 'X');
        cardO.set(oName, "O", oScore, !gameOver && currentPlayer == 'O');
        cardDraw.set("DRAWS", "", draws, false);
    }

    void updateStatus() {
        refreshScores();

        if (status == null || gameOver) return;

        if (computerThinking) {
            status.setForeground(muted);
            status.setText("Computer is thinking...");
        } else {
            status.setForeground(colorOf(currentPlayer));
            status.setText(nameOf(currentPlayer) + "'s turn");
        }
    }

    void finishGame(char winner, int[] line) {

        gameOver = true;
        stopComputerTimer();

        String l1, l2;
        Color c;

        if (winner == 'D') {
            draws++;
            l1 = "IT'S A DRAW";
            l2 = "Evenly matched!";
            c = textColor;
            status.setForeground(textColor);
            status.setText("Draw!");
        } else {
            if (winner == 'X') xScore++; else oScore++;

            String n = nameOf(winner);
            c = colorOf(winner);

            if (isSingle()) {
                if (winner == humanSymbol) {
                    l1 = n.toUpperCase() + " WINS!";
                    l2 = "Brilliant play!";
                } else {
                    l1 = "COMPUTER WINS";
                    l2 = "Good try - go again!";
                }
            } else {
                l1 = n.toUpperCase() + " WINS!";
                l2 = "Great game, both of you!";
            }

            status.setForeground(c);
            status.setText(n + " wins the round!");
        }

        boardPanel.showResult(line, l1, l2, c);
        refreshScores();
    }

    // =====================================================================
    //  COMPUTER AI
    // =====================================================================

    void computerMove() {

        if (gameOver) return;

        computerThinking = true;
        updateStatus();

        computerTimer = new Timer(550, e -> {

            computerTimer = null;
            computerThinking = false;

            if (gameOver) return;

            int move;

            if (difficulty.equals("Easy")) {
                move = findRandomMove();
            } else if (difficulty.equals("Medium")) {
                move = findMediumMove();
            } else {
                move = findBestMove();
            }

            if (move != -1) {
                placeMark(move, computerSymbol);
            }
        });

        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    int findRandomMove() {

        int[] empty = new int[9];
        int count = 0;

        for (int i = 0; i < 9; i++) {
            if (board[i] == ' ') empty[count++] = i;
        }

        if (count == 0) return -1;

        return empty[random.nextInt(count)];
    }

    int findMediumMove() {

        int move = findTacticalMove(computerSymbol);
        if (move != -1) return move;

        if (random.nextInt(100) < 65) {
            move = findTacticalMove(humanSymbol);
            if (move != -1) return move;
        }

        return findRandomMove();
    }

    int findTacticalMove(char symbol) {

        for (int i = 0; i < 9; i++) {
            if (board[i] == ' ') {
                board[i] = symbol;
                boolean win = checkWin(symbol);
                board[i] = ' ';
                if (win) return i;
            }
        }

        return -1;
    }

    int findBestMove() {

        // empty board: any corner or the centre is optimal - vary the opening
        boolean empty = true;
        for (char c : board) if (c != ' ') { empty = false; break; }

        if (empty) {
            int[] openings = {0, 2, 4, 6, 8};
            return openings[random.nextInt(openings.length)];
        }

        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;

        for (int i = 0; i < 9; i++) {
            if (board[i] == ' ') {

                board[i] = computerSymbol;
                int score = minimax(false, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                board[i] = ' ';

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = i;
                }
            }
        }

        return bestMove;
    }

    int minimax(boolean maximizing, int depth, int alpha, int beta) {

        if (checkWin(computerSymbol)) return 10 - depth;
        if (checkWin(humanSymbol)) return depth - 10;
        if (checkDraw()) return 0;

        if (maximizing) {

            int bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < 9; i++) {
                if (board[i] == ' ') {

                    board[i] = computerSymbol;
                    int score = minimax(false, depth + 1, alpha, beta);
                    board[i] = ' ';

                    bestScore = Math.max(bestScore, score);
                    alpha = Math.max(alpha, score);

                    if (beta <= alpha) break;
                }
            }

            return bestScore;

        } else {

            int bestScore = Integer.MAX_VALUE;

            for (int i = 0; i < 9; i++) {
                if (board[i] == ' ') {

                    board[i] = humanSymbol;
                    int score = minimax(true, depth + 1, alpha, beta);
                    board[i] = ' ';

                    bestScore = Math.min(bestScore, score);
                    beta = Math.min(beta, score);

                    if (beta <= alpha) break;
                }
            }

            return bestScore;
        }
    }

    // =====================================================================
    //  RULES
    // =====================================================================

    int[] getWinLine(char symbol) {

        for (int[] w : WINS) {
            if (board[w[0]] == symbol
                    && board[w[1]] == symbol
                    && board[w[2]] == symbol) {
                return w;
            }
        }

        return null;
    }

    boolean checkWin(char symbol) {
        return getWinLine(symbol) != null;
    }

    boolean checkDraw() {
        for (char cell : board) {
            if (cell == ' ') return false;
        }
        return true;
    }

    // =====================================================================
    //  MAIN
    // =====================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TicTacToe::new);
    }
}