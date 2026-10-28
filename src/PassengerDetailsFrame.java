import java.awt.*;
import java.awt.event.*;
import java.sql.*;

// customer enters details of all passengers in one window (one row for every seat)
// all passengers get the same PNR
public class PassengerDetailsFrame extends Frame implements ActionListener, WindowListener
{
    Label l1, l2, l3, l4, l5, l6, l7, l8;
    Button payBtn, backBtn;
    SeatSelectionFrame seatFrame;
    AvailableFlightsFrame flightsFrame;
    int customerId, flightId, count;
    int[] seatIds;       // seat_id of every selected seat
    String[] seatNos;    // seat number of every selected seat
    double price;        // price of one ticket
    String[] names;      // details of every passenger (read from the rows)
    int[] age;
    String[] genders;

    public PassengerDetailsFrame(SeatSelectionFrame sf, AvailableFlightsFrame af, int cid, int fid, int[] ids, String[] nos, double pr)
    {
        seatFrame = sf;
        flightsFrame = af;
        customerId = cid;
        flightId = fid;
        seatIds = ids;
        seatNos = nos;
        price = pr;
        count = ids.length; // number of passengers
        names = new String[count];
        age = new int[count];
        genders = new String[count];

        // setting the layout (4 columns: seat, name, age, gender)
        // rows: title, headings, one row for every passenger, buttons
        setLayout(new GridLayout(count + 3, 4, 0, 8));

        // creating objects
        l1 = new Label("   PASSENGERS");
        l2 = new Label(count + " passenger(s)");
        l3 = new Label("    Seat");
        l4 = new Label("Full Name");
        l5 = new Label("Age");
        l6 = new Label("Gender");
        l7 = new Label("Total: Rs. " + (price * count) + " + 5% GST");
        l8 = new Label("");
        payBtn = new Button("Proceed to Payment");
        backBtn = new Button("Back");

        // design
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 20));
        l2.setBackground(new Color(22, 54, 110));
        l2.setForeground(Color.white);
        l2.setFont(new Font("Arial", Font.BOLD, 15));
        l7.setBackground(new Color(22, 54, 110));
        l7.setForeground(Color.white);
        l7.setFont(new Font("Arial", Font.BOLD, 15));
        l8.setBackground(new Color(22, 54, 110));
        l3.setFont(new Font("Arial", Font.BOLD, 15));
        l4.setFont(new Font("Arial", Font.BOLD, 15));
        l5.setFont(new Font("Arial", Font.BOLD, 15));
        l6.setFont(new Font("Arial", Font.BOLD, 15));

        // event handling
        payBtn.addActionListener(this);
        backBtn.addActionListener(this);

        // adding components (title row and heading row)
        add(l1);
        add(l2);
        add(l7);
        add(l8);
        add(l3);
        add(l4);
        add(l5);
        add(l6);

        // one row (seat, name, age, gender) for every passenger
        for (int i = 0; i < count; i++)
        {
            Choice gender = new Choice();
            gender.add("Male");
            gender.add("Female");
            gender.add("Other");
            add(new Label("    " + seatNos[i]));
            add(new TextField(18));
            add(new TextField(4));
            add(gender);
        }
        add(payBtn);
        add(backBtn);

        // closing the window
        addWindowListener(this);

        // creating the frame
        setTitle("Passenger Details");
        setSize(760, 160 + count * 55);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // event handling method
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();
        if (btn == backBtn)
        {
            dispose();
            return;
        }

        // reading every passenger row: first 8 components are title and headings, then 4 for every passenger
        String seats = "";
        for (int i = 0; i < count; i++)
        {
            TextField nameTf = (TextField) getComponent(8 + i * 4 + 1);
            TextField ageTf = (TextField) getComponent(8 + i * 4 + 2);
            Choice gender = (Choice) getComponent(8 + i * 4 + 3);
            try
            {
                age[i] = Integer.parseInt(ageTf.getText().trim());
            }
            catch (Exception ex)
            {
                age[i] = 0;
            }

            if (nameTf.getText().trim().isEmpty() || age[i] < 1 || age[i] > 120)
            {
                new MessageBox(this, "Please enter name and valid age for seat " + seatNos[i]);
                return;
            }
            names[i] = nameTf.getText().trim();
            genders[i] = gender.getSelectedItem();
            seats = seats + seatNos[i] + " ";
        }

        // opens the QR payment window, seats are booked only after payment is done
        new PaymentFrame(this, customerId, flightId, seats.trim(), count, price * count);
    }

    // called by PaymentFrame after payment is done
    // books all seats in one transaction and returns the PNR (throws exception if booking fails)
    public String confirmBooking() throws Exception
    {
        Connection con = DBConnection.getConnection();
        try
        {
            con.setAutoCommit(false); // start transaction

            // 1. lock the flight row and check it
            PreparedStatement ps1 = con.prepareStatement("SELECT price FROM flights WHERE flight_id = ? AND departure_time > NOW() FOR UPDATE");
            ps1.setInt(1, flightId);
            ResultSet rs = ps1.executeQuery();
            if (!rs.next())
                throw new Exception("This flight has already departed");
            double ticketPrice = rs.getDouble("price");

            // 2. mark every seat BOOKED only if it is still AVAILABLE
            PreparedStatement ps2 = con.prepareStatement("UPDATE seats SET status = 'BOOKED' WHERE seat_id = ? AND status = 'AVAILABLE'");
            for (int i = 0; i < count; i++)
            {
                ps2.setInt(1, seatIds[i]);
                if (ps2.executeUpdate() != 1)
                    throw new Exception("Sorry, seat " + seatNos[i] + " was just booked by someone else");
            }

            // 3. decrease available seats by number of passengers
            PreparedStatement ps3 = con.prepareStatement("UPDATE flights SET available_seats = available_seats - ? WHERE flight_id = ?");
            ps3.setInt(1, count);
            ps3.setInt(2, flightId);
            ps3.executeUpdate();

            // 4. make a PNR that is not used before
            String pnr = TicketService.randomCode(6);
            PreparedStatement ps4 = con.prepareStatement("SELECT pnr FROM bookings WHERE pnr = ?");
            ps4.setString(1, pnr);
            while (ps4.executeQuery().next())
            {
                pnr = TicketService.randomCode(6);
                ps4.setString(1, pnr);
            }

            // 5. insert one row for every passenger with the same PNR
            PreparedStatement ps5 = con.prepareStatement("INSERT INTO bookings (pnr, customer_id, flight_id, seat_id, passenger_name, "
                    + "passenger_age, passenger_gender, status, ticket_price) VALUES (?, ?, ?, ?, ?, ?, ?, 'CONFIRMED', ?)");
            for (int i = 0; i < count; i++)
            {
                ps5.setString(1, pnr);
                ps5.setInt(2, customerId);
                ps5.setInt(3, flightId);
                ps5.setInt(4, seatIds[i]);
                ps5.setString(5, names[i]);
                ps5.setInt(6, age[i]);
                ps5.setString(7, genders[i]);
                ps5.setDouble(8, ticketPrice);
                ps5.executeUpdate();
            }

            // 6. save everything
            con.commit();
            con.close();
            return pnr;
        }
        catch (Exception e)
        {
            // undo all the steps, so no seat is half booked
            con.rollback();
            con.close();
            throw e;
        }
    }

    // called by PaymentFrame when everything is finished, closes the booking windows
    public void bookingDone()
    {
        if (flightsFrame != null)
            flightsFrame.loadFlights("All", "All");
        seatFrame.dispose();
        dispose();
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
