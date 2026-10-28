import java.awt.*;
import java.awt.event.*;
import java.sql.*;

// customer can see his bookings, search by PNR, cancel and get the ticket again
public class MyBookingsFrame extends Frame implements ActionListener, ItemListener, WindowListener
{
    Label l1, l2, l3;
    TextField pnrTf;
    List bookingList;
    TextArea detailsArea;
    Button searchBtn, showAllBtn, cancelBtn, ticketBtn, backBtn;
    Panel p1, p2, p3;
    int customerId;
    String[] pnrs = new String[500];   // pnr of every row in the list
    int[] bookingIds = new int[500];   // booking_id of every row (one row = one passenger)
    String[] names = new String[500];  // passenger name of every row
    int total = 0;                     // number of rows in the list

    public MyBookingsFrame(int id)
    {
        customerId = id;

        // setting the layout
        setLayout(new GridLayout(3, 1, 0, 5)); // top part, list of bookings, details of the selected booking

        // creating objects
        l1 = new Label("MY BOOKINGS", Label.CENTER);
        l2 = new Label("PNR", Label.RIGHT);
        l3 = new Label(String.format(" %-7s %-7s %-22s %-17s %-5s %-18s %s", "PNR", "Flight", "Route", "Departure", "Seat", "Passenger", "Status"));
        pnrTf = new TextField(10);
        bookingList = new List(8);
        detailsArea = new TextArea(14, 60);
        searchBtn = new Button("Search");
        showAllBtn = new Button("List All");
        cancelBtn = new Button("Cancel Ticket");
        ticketBtn = new Button("Get Ticket (PDF + Email)");
        backBtn = new Button("Back");

        p1 = new Panel(new GridLayout(4, 1, 5, 8));
        p2 = new Panel(new GridLayout(1, 4, 10, 0));
        p3 = new Panel(new GridLayout(1, 3, 10, 0));
        // design
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 22));
        l3.setFont(new Font("Monospaced", Font.BOLD, 14));
        bookingList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        detailsArea.setEditable(false);

        // event handling
        searchBtn.addActionListener(this);
        showAllBtn.addActionListener(this);
        cancelBtn.addActionListener(this);
        ticketBtn.addActionListener(this);
        backBtn.addActionListener(this);
        bookingList.addItemListener(this);

        // adding components
        p2.add(l2);
        p2.add(pnrTf);
        p2.add(searchBtn);
        p2.add(showAllBtn);
        p3.add(cancelBtn);
        p3.add(ticketBtn);
        p3.add(backBtn);
        p1.add(l1);
        p1.add(p2);
        p1.add(p3);
        p1.add(l3);
        add(p1);
        add(bookingList);
        add(detailsArea);

        // closing the window
        addWindowListener(this);

        // creating the frame
        setTitle("My Bookings");
        setSize(1000, 740);
        setLocationRelativeTo(null);
        setVisible(true);

        loadBookings("");
    }

    // shows bookings of this customer, if pnr is empty it shows all of them
    void loadBookings(String pnr)
    {
        bookingList.removeAll();
        total = 0;
        try
        {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT b.*, s.seat_no, f.flight_no, f.source, f.destination, f.departure_time FROM bookings b "
                    + "JOIN seats s ON b.seat_id = s.seat_id JOIN flights f ON b.flight_id = f.flight_id "
                    + "WHERE b.customer_id = ? AND (? = '' OR b.pnr = ?) ORDER BY b.booking_date DESC, b.pnr, s.seat_id");
            ps.setInt(1, customerId);
            ps.setString(2, pnr);
            ps.setString(3, pnr);
            ResultSet rs = ps.executeQuery();
            while (rs.next() && total < 500)
            {
                pnrs[total] = rs.getString("pnr");
                bookingIds[total] = rs.getInt("booking_id");
                names[total] = rs.getString("passenger_name");
                total++;
                bookingList.add(String.format("%-7s %-7s %-22s %-17s %-5s %-18s %s", rs.getString("pnr"), rs.getString("flight_no"),
                        rs.getString("source") + " -> " + rs.getString("destination"), rs.getString("departure_time").substring(0, 16),
                        rs.getString("seat_no"), rs.getString("passenger_name"), rs.getString("status")));
            }
            con.close();
            detailsArea.setText("Total tickets: " + total + "\nClick on a booking to see the details.");
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
        int i = bookingList.getSelectedIndex();

        if (btn == backBtn)
            dispose();
        else if (btn == searchBtn)
            loadBookings(pnrTf.getText().trim().toUpperCase());
        else if (btn == showAllBtn)
        {
            pnrTf.setText("");
            loadBookings("");
        }
        else if (i < 0)
            new MessageBox(this, "Please select a booking from the list first");
        else if (btn == ticketBtn)
        {
            // makes the PDF again and emails it
            String msg = "";
            try
            {
                String path = TicketService.makePdf(pnrs[i]);
                msg = "Ticket saved at: " + path;
                TicketService.sendEmail(pnrs[i], path);
                msg = msg + "\nTicket sent to your email";
            }
            catch (Exception ex)
            {
                msg = msg + "\nEmail failed: " + ex.getMessage();
            }
            new MessageBox(this, msg);
        }
        else if (new MessageBox(this, "Cancel the ticket of " + names[i] + " (PNR " + pnrs[i] + ")?", true).yes)
            cancelTicket(i);
    }

    // cancels the ticket of one passenger in one transaction (other passengers of the PNR are not cancelled)
    void cancelTicket(int i)
    {
        Connection con = null;
        try
        {
            con = DBConnection.getConnection();
            con.setAutoCommit(false); // start transaction

            // 1. lock the booking row and check it
            PreparedStatement ps1 = con.prepareStatement("SELECT b.*, f.departure_time < NOW() AS departed FROM bookings b "
                    + "JOIN flights f ON b.flight_id = f.flight_id WHERE b.booking_id = ? AND b.customer_id = ? FOR UPDATE");
            ps1.setInt(1, bookingIds[i]);
            ps1.setInt(2, customerId);
            ResultSet rs = ps1.executeQuery();
            if (!rs.next() || !rs.getString("status").equals("CONFIRMED"))
                throw new Exception("This ticket is already cancelled");
            if (rs.getInt("departed") == 1)
                throw new Exception("Flight has already departed, cannot cancel");

            // 2. mark booking cancelled, free the seat and increase available seats
            PreparedStatement ps2 = con.prepareStatement("UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?");
            ps2.setInt(1, bookingIds[i]);
            ps2.executeUpdate();
            PreparedStatement ps3 = con.prepareStatement("UPDATE seats SET status = 'AVAILABLE' WHERE seat_id = ?");
            ps3.setInt(1, rs.getInt("seat_id"));
            ps3.executeUpdate();
            PreparedStatement ps4 = con.prepareStatement("UPDATE flights SET available_seats = available_seats + 1 WHERE flight_id = ? AND available_seats < total_seats");
            ps4.setInt(1, rs.getInt("flight_id"));
            ps4.executeUpdate();

            // 3. save everything
            con.commit();
            con.close();
            new MessageBox(this, "Ticket of " + names[i] + " cancelled. Seat has been released.");
            loadBookings("");
        }
        catch (Exception e)
        {
            // undo all the steps
            try
            {
                con.rollback();
                con.close();
            }
            catch (Exception ex)
            {
                System.out.println(ex);
            }
            new MessageBox(this, e.getMessage());
        }
    }

    // when a booking is clicked in the list, show its details
    public void itemStateChanged(ItemEvent e)
    {
        try
        {
            detailsArea.setText(TicketService.getDetails(pnrs[bookingList.getSelectedIndex()]));
        }
        catch (Exception ex)
        {
            detailsArea.setText("Error: " + ex.getMessage());
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
