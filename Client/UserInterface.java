package Client;

import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.Hashtable;

import javax.swing.*;

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
        if(motion)
        {
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

class GameComponent extends DrawComponent{
    public static int playerNumber = 5;
    public static String[] playerName = new String[10];
    public GameComponent() {
        super(false);
        //init sth
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

    public JPanel p1;

    public JPanel p2;

    public JPanel p3;

    public JFrame frame = new JFrame("Texas");

    public static Data data;

    public UserInterface(Data datat) {
        //System.out.println("shit");
        //System.out.println(datat);
        data = datat;
        pipe = new PipedOutputStream();
        out = new DataOutputStream(pipe);

        p1 = initializeUI1();
        p2 = initializeUI2();
        p3 = initializeUI3();
    }

    public JPanel initializeUI1()
    {
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
        layout.setConstraints(titleLabel,constraints);

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
        constraints.insets=new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(nameField);
        layout.setConstraints(nameField,constraints);

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
        constraints.insets=new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(startButton);
        layout.setConstraints(startButton,constraints);

        startButton.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("deprecation")
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

    public JPanel initializeUI2()
    {
        DrawComponent drawComponent = new DrawComponent(true);
        drawComponent.setBackground(themeColor);

        GridBagLayout layout = new GridBagLayout();
        drawComponent.setLayout(layout);

        // 创建列表模型，使用数组作为数据源
        DefaultListModel<String> listModel = new DefaultListModel<>();

        Hashtable<String,Integer> dict = new Hashtable<String,Integer>();

        for (int i : data.rooms) {
            listModel.addElement("Room #" + i);
            dict.put("Room #"+i, i);
        }
 
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
        constraints.insets=new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(pane);
        layout.setConstraints(pane,constraints);

        list.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("deprecation")
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()>=2) {
                    p2.hide();
                    frame.add(p3, BorderLayout.CENTER);
                    frame.remove(p2);
                    System.out.println(list.getSelectedValue());
                    int tmp=dict.get(list.getSelectedValue());
                    System.out.println(tmp);
                    try
                    {
                        out.writeInt(tmp);
                    }
                    catch(IOException err)
                    {
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
        constraints.insets=new Insets(0, 150, 0, 150);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        drawComponent.add(startButton);
        layout.setConstraints(startButton,constraints);

        startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try
                {
                    out.writeInt(-1);
                }
                catch(IOException err)
                {
                    System.exit(1);
                }
            }
        });

        return drawComponent;
    }

    public JPanel initializeUI3()
    {
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
                if(maximizeButton.getText()=="O")
                {
                    lastBound=frame.getBounds();
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    Rectangle workArea = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    frame.setBounds(0,workArea.y,screenSize.width,screenSize.height);
                    maximizeButton.setText("=");
                }
                else{
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
                if(maximizeButton.getText()!="O") {
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

        frame.setBounds(300,100,1024, 600);

        frame.setVisible(true);
    }
}