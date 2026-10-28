import java.io.*;
import java.net.*;
import java.sql.*;
import com.sun.net.httpserver.*;

// small server on port 8085, the payment website on the phone talks to this server
// HttpServer comes with Java itself, so no extra jar file is needed
// the website sends 3 types of requests:
//   /api/payment/details?ref=PAY3F2A9C  -> send payment and flight details
//   /api/payment/success?ref=PAY3F2A9C  -> "Payment Done" button clicked
//   /api/payment/fail?ref=PAY3F2A9C     -> "Payment Failed" button clicked
// implements HttpHandler is same like implements ActionListener,
// when a request comes, Java calls our handle() method (like actionPerformed() for a button)
public class PaymentServer implements HttpHandler
{
    // payment window that is open on the laptop right now (only one at a time)
    // PaymentFrame puts itself here when it shows the QR code
    static PaymentFrame current = null;

    // starts the server, called only once from main() in AirlineMain
    public static void start()
    {
        try
        {
            // creating the server on port 8085
            HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);

            // every link starting with /api/payment goes to handle() of this class
            server.createContext("/api/payment", new PaymentServer());

            // starting the server, now it waits for requests from the phone
            server.start();
            System.out.println("Payment server started on port 8085");
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    // this method runs every time the website sends a request
    public void handle(HttpExchange ex) throws IOException
    {
        // 1. reading the link, eg. /api/payment/success?ref=PAY3F2A9C
        String path = ex.getRequestURI().getPath();   // /api/payment/success
        String query = ex.getRequestURI().getQuery(); // ref=PAY3F2A9C
        String ref = "";
        if (query != null)
            ref = query.substring(4); // removing "ref=" so only PAY3F2A9C is left

        // 2. checking which button or page it is and doing the work
        String reply = "";
        if (path.endsWith("/details"))
            reply = getDetails(ref);
        else if (path.endsWith("/success"))
            reply = paymentDone(ref);
        else if (path.endsWith("/fail"))
            reply = paymentFailed(ref);

        // 3. sending the reply to the phone
        byte[] data = reply.getBytes();                                   // reply text converted to bytes
        ex.getResponseHeaders().add("Content-Type", "application/json");  // telling the website the reply is JSON
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");  // website is on port 5173 and server on 8085, so browser needs this permission
        ex.sendResponseHeaders(200, data.length);                         // 200 means OK
        OutputStream os = ex.getResponseBody();
        os.write(data);
        os.close();

        // 4. making the PDF and sending the email takes a few seconds,
        //    so it is done after replying, this way the phone does not keep waiting
        if (path.endsWith("/success") && current != null && current.done)
            current.makeTicket();
    }

    // reads the payment from MySQL and sends its details to the website as JSON text
    static String getDetails(String ref)
    {
        // this reply is sent if the payment is not found
        String json = "{\"ok\":false, \"message\":\"Payment not found\"}";
        try
        {
            Connection con = DBConnection.getConnection();

            // joining payments, flights and customers to get all the details in one query
            PreparedStatement ps = con.prepareStatement("SELECT * FROM payments p JOIN flights f ON p.flight_id = f.flight_id "
                    + "JOIN customers c ON p.customer_id = c.customer_id WHERE p.payment_ref = ?");
            ps.setString(1, ref);
            ResultSet rs = ps.executeQuery();

            // making the JSON text one field at a time, eg. {"ok":true, "ref":"PAY3F2A9C", ...}
            // these names (ref, status, amount...) are the same names used in App.jsx of the website
            if (rs.next())
            {
                json = "{\"ok\":true";
                json = json + ", \"ref\":\"" + ref + "\"";
                json = json + ", \"status\":\"" + rs.getString("status") + "\"";
                json = json + ", \"amount\":" + rs.getDouble("amount");
                json = json + ", \"seats\":\"" + rs.getString("seats") + "\"";
                json = json + ", \"passengers\":" + rs.getInt("passengers");
                json = json + ", \"flightNo\":\"" + rs.getString("flight_no") + "\"";
                json = json + ", \"airline\":\"" + rs.getString("airline") + "\"";
                json = json + ", \"from\":\"" + rs.getString("source") + "\"";
                json = json + ", \"to\":\"" + rs.getString("destination") + "\"";
                json = json + ", \"departure\":\"" + rs.getString("departure_time").substring(0, 16) + "\"";
                json = json + ", \"customer\":\"" + rs.getString("name") + "\"";
                json = json + ", \"pnr\":\"" + rs.getString("pnr") + "\"}";
            }
            con.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return json;
    }

    // "Payment Done" button clicked on the phone
    static String paymentDone(String ref)
    {
        // if the payment window on the laptop is closed, we cannot book
        if (current == null || !current.ref.equals(ref))
            return "{\"ok\":false, \"message\":\"Payment window is closed. Please book again.\"}";

        // status changes only if it is still PENDING,
        // so if the button is clicked two times, the second time 0 rows change and nothing is booked again
        if (update("UPDATE payments SET status = 'SUCCESS' WHERE payment_ref = ? AND status = 'PENDING'", ref) == 0)
            return "{\"ok\":false, \"message\":\"Payment is already done or failed.\"}";

        try
        {
            // books the seats and gives the PNR (code is in PaymentFrame and PassengerDetailsFrame)
            String pnr = current.paymentSuccess();
            return "{\"ok\":true, \"message\":\"Booking confirmed! PNR: " + pnr + "\"}";
        }
        catch (Exception e)
        {
            // eg. someone else booked the same seat in between
            return "{\"ok\":false, \"message\":\"Booking failed, seats are not available.\"}";
        }
    }

    // "Payment Failed" button clicked on the phone
    static String paymentFailed(String ref)
    {
        // marking the payment FAILED, only if it is still PENDING
        if (update("UPDATE payments SET status = 'FAILED' WHERE payment_ref = ? AND status = 'PENDING'", ref) == 0)
            return "{\"ok\":false, \"message\":\"Payment is already done or failed.\"}";

        // telling the laptop window, it shows red text and enables the Retry button
        if (current != null && current.ref.equals(ref))
            current.paymentFailed();
        return "{\"ok\":true, \"message\":\"Payment failed. No seat was booked.\"}";
    }

    // runs an UPDATE query for one payment and returns how many rows changed
    // also used by PaymentFrame when the window is closed
    static int update(String sql, String ref)
    {
        int rows = 0;
        try
        {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, ref);
            rows = ps.executeUpdate(); // 1 if the row changed, 0 if not
            con.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return rows;
    }

    // finds the Wi-Fi IP of the laptop (eg. 192.168.1.104)
    // PaymentFrame puts this IP in the QR code link, so the phone can open the website on the laptop
    static String getLaptopIp()
    {
        String ip = "localhost";
        try
        {
            // connecting a UDP socket does not send anything,
            // it only tells which IP address the laptop is using on the Wi-Fi
            DatagramSocket ds = new DatagramSocket();
            ds.connect(InetAddress.getByName("8.8.8.8"), 80);
            ip = ds.getLocalAddress().getHostAddress();
            ds.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        return ip;
    }
}
