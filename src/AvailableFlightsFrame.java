import java.awt.*;
import java.awt.event.*;
import java.sql.*;

// shows all flights added by admin, customer selects one and books it
public class AvailableFlightsFrame extends Frame implements ActionListener, WindowListener
{
    Label l1, l2, l3, l4;
    Choice fromChoice, toChoice;
    List flightList;
    Button filterBtn, showAllBtn, bookBtn, backBtn;
    Panel p1, p2, p3;
    int customerId;
    int[] flightIds = new int[500];     // id of every row in the list
    String[] status = new String[500];  // Available / Full / Departed of every row
    int total = 0;                      // number of rows in the list

    public AvailableFlightsFrame(int id)
    {
        customerId = id;

        // setting the layout
        setLayout(new GridLayout(2, 1, 0, 5)); // top part with filter and buttons, bottom part with the list

        // creating objects
        l1 = new Label("AVAILABLE FLIGHTS", Label.CENTER);
        l2 = new Label("From", Label.RIGHT);
        l3 = new Label("To", Label.RIGHT);
        l4 = new Label(String.format(" %-4s %-7s %-10s %-22s %-17s %-11s %-11s %s", "ID", "Flight", "Airline", "Route", "Departure", "Price(Rs)", "Seats Left", "Status"));
        fromChoice = new Choice();
        toChoice = new Choice();
        flightList = new List(14);
        filterBtn = new Button("Filter");
        showAllBtn = new Button("List All");
        bookBtn = new Button("Select Flight");
        backBtn = new Button("Back");

        p1 = new Panel(new GridLayout(4, 1, 5, 8));
        p2 = new Panel(new GridLayout(1, 6, 10, 0));
        p3 = new Panel(new GridLayout(1, 2, 10, 0));
        // design
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 22));
        l4.setFont(new Font("Monospaced", Font.BOLD, 14));
        flightList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        fromChoice.add("All");
        toChoice.add("All");

        // event handling
        filterBtn.addActionListener(this);
        showAllBtn.addActionListener(this);
        bookBtn.addActionListener(this);
        backBtn.addActionListener(this);

        // adding components
        p2.add(l2);
        p2.add(fromChoice);
        p2.add(l3);
        p2.add(toChoice);
        p2.add(filterBtn);
        p2.add(showAllBtn);
        p3.add(bookBtn);
        p3.add(backBtn);
        p1.add(l1);
        p1.add(p2);
        p1.add(p3);
        p1.add(l4);
        add(p1);
        add(flightList);

        // closing the window
        addWindowListener(this);

        // creating the frame
        setTitle("Available Flights");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        // filling both Choice boxes with city names from database
        try
        {
            Connection con = DBConnection.getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT source AS city FROM flights UNION SELECT destination FROM flights ORDER BY city");
            while (rs.next())
            {
                fromChoice.add(rs.getString("city"));
                toChoice.add(rs.getString("city"));
            }
            con.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        loadFlights("All", "All");
    }

    // shows all flights ("All" means any city), departed flights are shown but cannot be booked
    void loadFlights(String from, String to)
    {
        flightList.removeAll();
        total = 0;
        try
        {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT *, departure_time < NOW() AS departed FROM flights "
                    + "WHERE (? = 'All' OR source = ?) AND (? = 'All' OR destination = ?) ORDER BY departure_time");
            ps.setString(1, from);
            ps.setString(2, from);
            ps.setString(3, to);
            ps.setString(4, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next() && total < 500)
            {
                String st = "Available";
                if (rs.getInt("departed") == 1)
                    st = "Departed";
                else if (rs.getInt("available_seats") == 0)
                    st = "Full";

                flightIds[total] = rs.getInt("flight_id");
                status[total] = st;
                total++;
                flightList.add(String.format("%-4d %-7s %-10s %-22s %-17s %-11.2f %-11s %s", rs.getInt("flight_id"), rs.getString("flight_no"),
                        rs.getString("airline"), rs.getString("source") + " -> " + rs.getString("destination"), rs.getString("departure_time").substring(0, 16),
                        rs.getDouble("price"), rs.getInt("available_seats") + "/" + rs.getInt("total_seats"), st));
            }
            con.close();
            if (total == 0)
                new MessageBox(this, "No flights found");
        }
        catch (Exception e)
        {
            System.out.println(e);
            new MessageBox(this, "Error: " + e.getMessage());
        }
    }

    // event handling method
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();
        int i = flightList.getSelectedIndex();

        if (btn == backBtn)
            dispose();
        else if (btn == filterBtn)
            loadFlights(fromChoice.getSelectedItem(), toChoice.getSelectedItem());
        else if (btn == showAllBtn)
        {
            fromChoice.select(0);
            toChoice.select(0);
            loadFlights("All", "All");
        }
        else if (i < 0)
            new MessageBox(this, "Please select a flight from the list");
        else if (!status[i].equals("Available"))
            new MessageBox(this, "Sorry, this flight is " + status[i]);
        else
            new SeatSelectionFrame(this, customerId, flightIds[i]);
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
