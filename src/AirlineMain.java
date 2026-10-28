import java.awt.*;
import java.awt.event.*;
import java.sql.*;


public class AirlineMain extends Frame implements ActionListener, WindowListener
{
    
    Label l1, l2;
    Button adminBtn, customerBtn, exitBtn;

    public AirlineMain()
    {
        // setting the layout
        setLayout(new GridLayout(5, 1, 10, 10));

        // creating objects
        l1 = new Label("SKYWAY AIRLINES", Label.CENTER);
        l2 = new Label("Please choose how you want to login", Label.CENTER);
        adminBtn = new Button("Admin Login");
        customerBtn = new Button("Customer Login");
        exitBtn = new Button("Exit");

        // design
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 26));
        l2.setFont(new Font("Arial", Font.BOLD, 15));

        // event handling
        adminBtn.addActionListener(this);
        customerBtn.addActionListener(this);
        exitBtn.addActionListener(this);

        // adding components
        add(l1);
        add(l2);
        add(adminBtn);
        add(customerBtn);
        add(exitBtn);

        
        addWindowListener(this);

        
        setTitle("SkyWay Airlines");
        setSize(450, 360);
        setLocationRelativeTo(null);
        setVisible(true);

        
        try
        {
            Connection con = DBConnection.getConnection();
            System.out.println("Database connected successfully!");
            con.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
            new MessageBox(this, "Could not connect to MySQL.\nStart MySQL in XAMPP and check the port in DBConnection.java");
        }
    }

   
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();

        if (btn == adminBtn){
          LoginFrame lf =  new LoginFrame("Admin");
        }
        else if (btn == customerBtn){
          LoginFrame lf =   new LoginFrame("Customer");
        }
        else
            System.exit(0);
    }

    public static void main(String args[])
    {
        //ctrl one 
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener()
        {
            public void eventDispatched(AWTEvent ev)
            {
                KeyEvent ke = (KeyEvent) ev;
                int key = ke.getKeyCode();
                if (ke.getID() != KeyEvent.KEY_PRESSED || !ke.isControlDown() || key < 'A' || key > 'Z')
                    return;

                // finding the window in which the key was pressed
                Component c = ke.getComponent();
                while (c != null && !(c instanceof Window))
                    c = c.getParent();

                Button btn = null;
                if (c != null)
                    btn = findButton((Container) c, key);
                if (btn != null)
                {
                    ke.consume();
                    btn.dispatchEvent(new ActionEvent(btn, ActionEvent.ACTION_PERFORMED, btn.getLabel()));
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);

        PaymentServer.start(); // small HTTP server for the demo QR payment website
        new AirlineMain();
    }

   
    static Button findButton(Container box, int key)
    {
        Component[] list = box.getComponents();
        for (int i = 0; i < list.length; i++)
        {
            Button b = null;
            if (list[i] instanceof Button)
                b = (Button) list[i];
            else if (list[i] instanceof Container)
                b = findButton((Container) list[i], key);

            if (b != null && b.isEnabled() && b.getLabel().trim().toUpperCase().charAt(0) == key)
                return b;
        }
        return null;
    }

    // window events (only closing is used, WindowListener needs all 7 methods)
    public void windowClosing(WindowEvent we)
    {
        System.exit(0);
    }
    public void windowOpened(WindowEvent we) {}
    public void windowClosed(WindowEvent we) {}
    public void windowIconified(WindowEvent we) {}
    public void windowDeiconified(WindowEvent we) {}
    public void windowActivated(WindowEvent we) {}
    public void windowDeactivated(WindowEvent we) {}
}
