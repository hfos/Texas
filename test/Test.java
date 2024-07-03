
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.DataOutputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.text.Position;

class DrawComponent extends JPanel {

    Timer timer;

    int delta = 120;

    public static String decors = "♠♥♣♦";

    public DrawComponent(boolean motion) {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                repaint();
            }
        });
        if (motion) {
            timer = new Timer(20, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    repaint();
                    delta = (delta + 7) % 240;
                }
            });
            timer.start();
        }
        repaint();
    }

    public Point getMouse() {
        Point cursorPointOnScreen = MouseInfo.getPointerInfo().getLocation();
        Point selfPointOnScreen = this.getLocationOnScreen();

        Point cursorPoint = new Point(cursorPointOnScreen.x - selfPointOnScreen.x, cursorPointOnScreen.y - selfPointOnScreen.y);
        return cursorPoint;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setFont(new Font(Font.SERIF, Font.BOLD, 120));

        int width = this.getWidth();
        int height = this.getHeight();

        for (int i = -1; i < width / 240 + 2; i++) {
            for (int j = -1; j < height / 240 + 2; j++) {
                g2d.drawString((decors.charAt(Math.abs(i - j) % 4)) + "", i * 240 + delta, j * 240 + delta);
                g2d.drawString(".", i * 240 + 146 + delta, j * 240 + 92 + delta);
            }
        }
    }
}

class Card {

    public int color, value;

    public Card(int x, int y) {
        color = x;
        value = y;
    }
}

class GameComponent extends DrawComponent {

    public static int playerNumber = 10;
    public static String[] playerName = new String[10];

    public GameComponent() {
        super(false);
        //init sth
    }

    public Point intersect(int pid, Point delta) {
        Point p = new Point(0, 0);

        int width = this.getWidth();
        int height = this.getHeight();

        double midW = width / 2.0;
        double midH = height / 2.0;

        double theta = Math.atan(((double) width) / height);

        double gamma = pid * 2.0 * Math.PI / playerNumber;

        //System.out.println(gamma);
        if (gamma <= theta || gamma >= 2.0 * Math.PI - theta) {
            p = new Point((int) (midW - Math.tan(gamma) * midH - delta.x / 2.0), height - delta.y / 2);
        } else if (gamma <= Math.PI - theta) {
            p = new Point(delta.x / 2, (int) (midH + Math.tan(Math.PI / 2.0 - gamma) * midW + delta.y / 2.0));
        } else if (gamma <= theta + Math.PI) {
            p = new Point((int) (midW + Math.tan(gamma) * midH - delta.x / 2.0), delta.y);
        } else {
            p = new Point(width - delta.x / 2, (int) (midH - Math.tan(Math.PI / 2.0 - gamma) * midW + delta.y / 2.0));
        }

        return p;
    }

    public Point intersectCenter(int pid, Point delta) {
        Point p = new Point(0, 0);

        int width = this.getWidth();
        int height = this.getHeight();

        double midW = width / 2.0;
        double midH = height / 2.0;

        double theta = Math.atan(((double) width) / height);

        double gamma = pid * 2.0 * Math.PI / playerNumber;

        //System.out.println(gamma);
        if (gamma <= theta || gamma >= 2.0 * Math.PI - theta) {
            p = new Point((int) (midW - Math.tan(gamma) * midH), height - delta.y / 2);
        } else if (gamma <= Math.PI - theta) {
            p = new Point(delta.x / 2, (int) (midH + Math.tan(Math.PI / 2.0 - gamma) * midW));
        } else if (gamma <= theta + Math.PI) {
            p = new Point((int) (midW + Math.tan(gamma) * midH), delta.y / 2);
        } else {
            p = new Point(width - delta.x / 2, (int) (midH - Math.tan(Math.PI / 2.0 - gamma) * midW));
        }

        return p;
    }
    public static java.util.List<String> cardString = Arrays.asList("0", "0", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
    public static java.util.List<Color> decorColor = Arrays.asList(Color.BLACK, Color.RED, Color.BLACK, Color.RED);

    public void paintPocker(Graphics2D g, Point p, Card c, boolean vis) {
        // 创建圆角矩形，参数依次为x, y, width, height, arcWidth, arcHeight
        p.x -= 27;
        p.y -= 43;
        RoundRectangle2D roundRect = new RoundRectangle2D.Double(p.x, p.y, 54, 86, 5, 5);
        g.setColor(Color.WHITE);
        g.fill(roundRect);

        if (vis == false) {
            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 33));
            g.drawString("?", p.x + 4, p.y + 28);
            return;
        }
        g.setColor(decorColor.get(c.color));
        g.setFont(new Font(Font.SERIF, Font.BOLD, 33 - 3 * cardString.get(c.value).length()));
        g.drawString(cardString.get(c.value), p.x + 11 - 6 * cardString.get(c.value).length(), p.y + 25);
        g.setFont(new Font(Font.SERIF, Font.BOLD, 30));
        g.drawString(decors.charAt(c.color) + "", p.x + 2, p.y + 50);
    }

    public void paintButton(Graphics2D g, Point p, int w, int h, String txt) {

        RoundRectangle2D roundRect = new RoundRectangle2D.Double(p.x - w / 2, p.y - h / 2, w, h, 5, 5);
        g.setColor(UserInterface.foreColor);
        g.fill(roundRect);
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(4f));
        g.draw(roundRect);

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 35));
        g.setColor(UserInterface.bgColor);

        g.drawString(txt, p.x - 8 * txt.length(), p.y + 10);

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));

        for (int i = 0; i < playerNumber; ++i) {
            Point pos = intersectCenter(i, new Point(200, 200));
            pos.x -= 10;
            pos.y += 10;
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
            g2d.drawString(i + "", pos.x, pos.y);
            paintPocker(g2d, intersectCenter(i, new Point(54, 86)), new Card(1, 10), i == 0);
        }

        int width = this.getWidth();
        int height = this.getHeight();

        paintButton(g2d, new Point(width / 2, height / 2), 300, 70, "Fold/Check");

    }
}

class UserInterface implements Runnable {

    PipedOutputStream pipe;
    DataOutputStream out;

    public static Point dragPoint = new Point(0, 0);

    public static Color bgColor = new Color(28, 28, 28);
    public static Color themeColor = new Color(24, 24, 24);
    public static Color foreColor = new Color(240, 240, 240);

    public static String decors = "♠♥♣♦";
    public static String playerName = "";

    public static Rectangle lastBound = new Rectangle();

    public JPanel p1 = initializeUI1();

    public JPanel p2 = initializeUI2();

    public JPanel p3 = initializeUI3();

    public JFrame frame = new JFrame("Texas");

    public UserInterface() {
        pipe = new PipedOutputStream();
        out = new DataOutputStream(pipe);
    }

    public JPanel initializeUI1() {
        DrawComponent drawComponent = new DrawComponent(true);
        drawComponent.setBackground(themeColor);

        GridBagLayout layout = new GridBagLayout();
        drawComponent.setLayout(layout);

        JLabel titleLabel = new JLabel("Texas Holdem Pocker");
        titleLabel.setForeground(foreColor);
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 74));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(titleLabel);
        layout.setConstraints(titleLabel, constraints);

        JTextField nameField = new JTextField("Your Name");
        nameField.setForeground(Color.LIGHT_GRAY);
        nameField.setBackground(foreColor);
        nameField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 54));
        nameField.setPreferredSize(new Dimension(200, 100));
        nameField.setHorizontalAlignment(SwingConstants.CENTER);

        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (nameField.getForeground() == Color.LIGHT_GRAY && nameField.getText().equals("Your Name")) {
                    nameField.setText("");
                    nameField.setForeground(bgColor);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (nameField.getText().isEmpty()) {
                    nameField.setText("Your Name");
                    nameField.setForeground(Color.LIGHT_GRAY);
                }
            }
        });

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(nameField);
        layout.setConstraints(nameField, constraints);

        JButton startButton = new JButton("Start!");
        startButton.setForeground(bgColor);
        startButton.setFocusable(false);
        startButton.setBackground(foreColor);
        startButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 54));
        startButton.setPreferredSize(new Dimension(200, 100));
        startButton.setHorizontalAlignment(SwingConstants.CENTER);

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 2.0;
        constraints.weighty = 2.0;
        constraints.insets = new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(startButton);
        layout.setConstraints(startButton, constraints);

        startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                p1.hide();
                frame.add(p2, BorderLayout.CENTER);
                frame.remove(p1);
                System.out.println(nameField.getText());
            }
        });

        return drawComponent;
    }

    public JPanel initializeUI2() {
        DrawComponent drawComponent = new DrawComponent(true);
        drawComponent.setBackground(themeColor);

        GridBagLayout layout = new GridBagLayout();
        drawComponent.setLayout(layout);

        // 创建列表模型，使用数组作为数据源
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("item 1");
        listModel.addElement("item 2");
        listModel.addElement("item 3");
        listModel.addElement("item 4");
        listModel.addElement("item 5");

        // 创建JList组件并设置模型
        JList<String> list = new JList<>(listModel);
        list.setForeground(bgColor);
        list.setFocusable(false);
        list.setBackground(foreColor);
        list.setFont(new Font(Font.MONOSPACED, Font.BOLD, 54));
        JScrollPane pane = new JScrollPane(list);
        // 将列表添加到窗口中
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 7.0;
        constraints.weighty = 7.0;
        constraints.insets = new Insets(70, 150, 70, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(pane);
        layout.setConstraints(pane, constraints);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    p2.hide();
                    frame.add(p3, BorderLayout.CENTER);
                    frame.remove(p2);
                    System.out.println(list.getSelectedValue());
                }
            }
        });

        JButton startButton = new JButton("Create A New Room");
        startButton.setForeground(bgColor);
        startButton.setFocusable(false);
        startButton.setBackground(foreColor);
        startButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 54));
        startButton.setPreferredSize(new Dimension(200, 100));
        startButton.setHorizontalAlignment(SwingConstants.CENTER);

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 2.0;
        constraints.weighty = 2.0;
        constraints.insets = new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(startButton);
        layout.setConstraints(startButton, constraints);

        startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //
            }
        });

        return drawComponent;
    }

    public JPanel initializeUI3() {
        GameComponent gameComponent = new GameComponent();
        gameComponent.setBackground(themeColor);
        return gameComponent;
    }

    @Override
    public void run() {

        frame.getContentPane().setBackground(themeColor);
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(bgColor);
        titleBar.setPreferredSize(new Dimension(0, 70));

        JLabel titleLabel = new JLabel("");
        titleLabel.setForeground(foreColor);
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));
        titleBar.add(titleLabel, BorderLayout.CENTER);

        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                UserInterface.dragPoint = new Point(e.getX(), e.getY());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(bgColor);

        JButton minimizeButton = new JButton("-");
        minimizeButton.setFocusable(false);
        minimizeButton.setForeground(foreColor);
        minimizeButton.setBackground(bgColor);
        minimizeButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));
        minimizeButton.setPreferredSize(new Dimension(60, 60));
        minimizeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                frame.setState(Frame.ICONIFIED);
            }
        });

        buttonPanel.add(minimizeButton);

        JButton maximizeButton = new JButton("O");
        maximizeButton.setFocusable(false);
        maximizeButton.setForeground(foreColor);
        maximizeButton.setBackground(bgColor);
        maximizeButton.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
        maximizeButton.setPreferredSize(new Dimension(60, 60));
        maximizeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (maximizeButton.getText() == "O") {
                    lastBound = frame.getBounds();
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    Rectangle workArea = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    frame.setBounds(0, workArea.y, screenSize.width, screenSize.height);
                    maximizeButton.setText("=");
                } else {
                    frame.setBounds(lastBound);
                    maximizeButton.setText("O");
                }
            }
        });
        buttonPanel.add(maximizeButton);

        titleBar.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = frame.getLocation();
                p.translate(e.getX() - UserInterface.dragPoint.x, e.getY() - UserInterface.dragPoint.y);
                frame.setLocation(p);
                titleBar.requestFocus();
                if (maximizeButton.getText() != "O") {
                    frame.setBounds(lastBound);
                    maximizeButton.setText("O");
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });

        JButton exitButton = new JButton("X");
        exitButton.setFocusable(false);
        exitButton.setForeground(foreColor);
        exitButton.setBackground(bgColor);
        exitButton.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
        exitButton.setPreferredSize(new Dimension(60, 60));
        exitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                System.exit(0);
            }
        });

        buttonPanel.add(exitButton);

        titleBar.add(buttonPanel, BorderLayout.EAST);

        frame.getContentPane().add(titleBar, BorderLayout.NORTH);

        frame.add(p1, BorderLayout.CENTER);

        Rectangle workArea = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        frame.setBounds(300, 100, 1024, 600);

        frame.setVisible(true);
    }
}

class test {

    public static void main(String[] args) throws InterruptedException {
        UserInterface ui = new UserInterface();
        Thread UIThread = new Thread(ui);
        UIThread.start();
        UIThread.join();
    }
}
