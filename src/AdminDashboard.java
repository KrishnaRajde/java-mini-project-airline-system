import java.awt.*;
import java.awt.event.*;

public class AdminDashboard extends Frame implements ActionListener, WindowListener
{
    Label l1, l2;
    Button manageBtn, bookingsBtn, logoutBtn;

    public AdminDashboard(String adminName)
    {
        
        setLayout(new GridLayout(5, 1, 10, 10));

       
        l1 = new Label("ADMIN DASHBOARD", Label.CENTER);
        l2 = new Label("Welcome, " + adminName, Label.CENTER);
        manageBtn = new Button("Manage Flights");
        bookingsBtn = new Button("View Bookings");
        logoutBtn = new Button("Logout");

        
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 22));
        l2.setFont(new Font("Arial", Font.BOLD, 15));

        // event handling
        manageBtn.addActionListener(this);
        bookingsBtn.addActionListener(this);
        logoutBtn.addActionListener(this);

        // adding components
        add(l1);
        add(l2);
        add(manageBtn);
        add(bookingsBtn);
        add(logoutBtn);

        // closing the window
        addWindowListener(this);

        // creating the frame
        setTitle("Admin Dashboard");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // event handling method
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();

        if (btn == manageBtn)
            new ManageFlightsFrame();
        else if (btn == bookingsBtn)
            new ViewBookingsFrame();
        else
        {
            new LoginFrame("Admin");
            dispose();
        }
    }

    // window events (only closing is used, WindowListener needs all 7 methods)
    public void windowClosing(WindowEvent we)
    {
        dispose();
    }
    public void windowOpened(WindowEvent we) {}
    public void windowClosed(WindowEvent we) {}
    public void windowIconified(WindowEvent we) {}
    public void windowDeiconified(WindowEvent we) {}
    public void windowActivated(WindowEvent we) {}
    public void windowDeactivated(WindowEvent we) {}
}
