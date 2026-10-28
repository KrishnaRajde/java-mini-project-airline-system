import java.awt.*;
import java.awt.event.*;

public class CustomerDashboard extends Frame implements ActionListener, WindowListener
{
    Label l1, l2;
    Button flightsBtn, bookingsBtn, logoutBtn;
    int customerId; // customer who logged in

    public CustomerDashboard(int id, String name)
    {
        customerId = id;

        // setting the layout
        setLayout(new GridLayout(5, 1, 10, 10));

        // creating objects
        l1 = new Label("CUSTOMER DASHBOARD", Label.CENTER);
        l2 = new Label("Welcome, " + name, Label.CENTER);
        flightsBtn = new Button("View Flights & Book Ticket");
        bookingsBtn = new Button("My Bookings");
        logoutBtn = new Button("Logout");

        // design
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 22));
        l2.setFont(new Font("Arial", Font.BOLD, 15));

        // event handling
        flightsBtn.addActionListener(this);
        bookingsBtn.addActionListener(this);
        logoutBtn.addActionListener(this);

        // adding components
        add(l1);
        add(l2);
        add(flightsBtn);
        add(bookingsBtn);
        add(logoutBtn);

        // closing the window
        addWindowListener(this);

        // creating the frame
        setTitle("Customer Dashboard");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // event handling method
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();

        if (btn == flightsBtn)
            new AvailableFlightsFrame(customerId);
        else if (btn == bookingsBtn)
            new MyBookingsFrame(customerId);
        else
        {
            new LoginFrame("Customer");
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
