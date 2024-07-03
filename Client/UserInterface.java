package Client;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

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

class GameComponent extends DrawComponent {

    public static Data data;
    public static int playerNumber = 5;

    public GameComponent() {
        super(false);
        //init sth
        data = UserInterface.data;
        //playerNumber = data.players.size();

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
            p = new Point((int) (midW - Math.tan(gamma) * midH - delta.x / 2.0), height);
        } else if (gamma <= Math.PI - theta) {
            p = new Point(0, (int) (midH + Math.tan(Math.PI / 2.0 - gamma) * midW + delta.y / 2.0));
        } else if (gamma <= theta + Math.PI) {
            p = new Point((int) (midW + Math.tan(gamma) * midH - delta.x / 2.0), delta.y);
        } else {
            p = new Point(width - delta.x, (int) (midH - Math.tan(Math.PI / 2.0 - gamma) * midW + delta.y / 2.0));
        }

        return p;
    }

    public static java.util.List<String> cardString = Arrays.asList("0", "0", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
    public static java.util.List<Color> decorColor = Arrays.asList(Color.BLACK, Color.RED, Color.BLACK, Color.RED);

    public void paintPocker(Graphics2D g, Point p, Card c) {
        RoundRectangle2D roundRect = new RoundRectangle2D.Double(p.x, p.y, 54, 86, 10, 10);
        g.setColor(Color.WHITE);
        g.fill(roundRect);

        g.setColor(decorColor.get(c.color));
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 35 - 5 * cardString.get(c.value).length()));
        g.drawString(cardString.get(c.value), p.x + 11 - 6 * cardString.get(c.value).length(), p.y + 25);
        g.setFont(new Font(Font.SERIF, Font.BOLD, 26));
        g.drawString(decors.charAt(c.color) + "", p.x + 5, p.y + 50);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));

        for (int i = 0; i < playerNumber; ++i) {
            Point pos = intersect(i, new Point(30, 30));
            g2d.drawString(i + "", pos.x, pos.y);
            paintPocker(g2d, pos, new Card(2, 2));
        }

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

    public JPanel p1, p2, p3, p4;

    public JFrame frame = new JFrame("Texas");

    public static Data data;

    Timer timer;

    public UserInterface(Data datat) {
        //System.out.println("shit");
        //System.out.println(datat);
        data = datat;
        pipe = new PipedOutputStream();
        out = new DataOutputStream(pipe);

        p1 = initializeUI1();
        p2 = initializeUI2();
        p3 = initializeUI3();
        p4 = initializeUI4();
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


        /*
        JTextField nameField = new JTextField("Your Name");
        nameField.setForeground(Color.LIGHT_GRAY);
        nameField.setBackground(foreColor);
        nameField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 54));
        nameField.setPreferredSize(new Dimension(200, 100));
        nameField.setHorizontalAlignment(SwingConstants.CENTER);

        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (nameField.getText().equals("Your Name")) {
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
         */
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
            @SuppressWarnings("deprecation")
            @Override
            public void mousePressed(MouseEvent e) {
                p1.hide();
                frame.add(p2, BorderLayout.CENTER);
                frame.remove(p1);
            }
        });

        return drawComponent;
    }

    @SuppressWarnings("deprecation")
    public void ChangeStatus(JPanel p1, JPanel p2) {
        Component[] c = frame.getContentPane().getComponents();
        for (Component i : c) {
            //System.out.println("getComponent: " + i);
            if (i == p2) {
                return;
            }
            if (i == p1) {
                p1.hide();
                frame.remove(p1);
            }
        }
        frame.add(p2);
    }

    public JPanel initializeUI2() {
        DrawComponent drawComponent = new DrawComponent(true);
        drawComponent.setBackground(themeColor);

        GridBagLayout layout = new GridBagLayout();
        drawComponent.setLayout(layout);

        // 创建列表模型，使用数组作为数据源
        DefaultListModel<String> listModel = new DefaultListModel<>();

        Hashtable<String, Integer> dict = new Hashtable<String, Integer>();

        timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (data.status == 0) {
                    listModel.clear();
                    dict.clear();
                    for (int i = 0;; ++i) {
                        try {
                            int t = data.rooms.get(i);
                            listModel.addElement("Room #" + t);
                            dict.put("Room #" + t, t);
                        } catch (IndexOutOfBoundsException err) {
                            break;
                        }
                    }
                }
            }
        });
        timer.start();

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
        constraints.insets = new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(pane);
        layout.setConstraints(pane, constraints);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    ChangeStatus(p2, p3);
                    //System.out.println(list.getSelectedValue());
                    int tmp = dict.get(list.getSelectedValue());
                    //System.out.println(tmp);
                    try {
                        out.writeInt(tmp);
                        out.writeInt(-1);
                    } catch (IOException err) {
                        System.out.println("Error: Entering room failed.");
                        System.exit(1);
                    }
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
            public void mousePressed(MouseEvent e) {
                ChangeStatus(p2, p3);
                try {
                    out.writeInt(-1);
                } catch (IOException err) {
                    System.out.println("Error: Creating room failed.");
                    System.exit(1);
                }
            }
        });

        return drawComponent;
    }

    public JPanel initializeUI3() {
        DrawComponent drawComponent = new DrawComponent(true);
        drawComponent.setBackground(themeColor);

        GridBagLayout layout = new GridBagLayout();
        drawComponent.setLayout(layout);

        JLabel titleLabel = new JLabel(" ");
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

        timer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (data.status == 1 || data.status == 2) {
                    if (data.playerNumber < 2 || data.playerNumber > 10) {
                        titleLabel.setText("<html>Player Count:  " + data.readyNumber + " / <font color='red'>" + data.playerNumber + "</font></html>");
                    } else {
                        titleLabel.setText("<html>Player Count:  " + data.readyNumber + " / " + data.playerNumber + "</html>");
                    }
                }
                if (data.status == 3) {
                    ChangeStatus(p3, p4);
                }
            }
        });
        timer.start();

        JButton startButton = new JButton("Ready!");
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
            public void mousePressed(MouseEvent e) {
                if (startButton.getText().equals("Ready!")) {
                    startButton.setText("<html><font color='grey'>Ready!</font></html>");
                    startButton.setEnabled(false);
                    try {
                        // System.out.println("tmp");
                        out.writeInt(1);
                    } catch (IOException ex) {
                    }
                }
            }
        });

        return drawComponent;
    }

    public JPanel initializeUI4() {
        GameComponent gameComponent = new GameComponent();
        gameComponent.setBackground(themeColor);

        for (int i = 1; i < 10; ++i) {
            JButton b1 = new JButton("" + i);
            gameComponent.add(b1);
            b1.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    out.writeInt(Integer.parseInt(b1.getText()));
            });
        }

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
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }
        });

        buttonPanel.add(exitButton);

        titleBar.add(buttonPanel, BorderLayout.EAST);

        frame.getContentPane().add(titleBar, BorderLayout.NORTH);

        frame.add(p1, BorderLayout.CENTER);

        // Rectangle workArea = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setBounds(300, 100, 1024, 600);

        frame.setVisible(true);
    }
}
