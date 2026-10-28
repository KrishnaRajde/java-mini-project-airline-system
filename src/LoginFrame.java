import java.awt.*;
import java.awt.event.*;
import java.sql.*;

// one login window for both admin and customer
public class LoginFrame extends Frame implements ActionListener, WindowListener
{
    Label l1, l2, l3, l4;
    TextField userTf, passTf;
    Button loginBtn, registerBtn, backBtn;
    String type; // "Admin" or "Customer"

    public LoginFrame(String t)
    {
        type = t;

        // setting the layout (2 columns, like label and text field)
        if (type.equals("Admin"))
            setLayout(new GridLayout(4, 2, 0, 10));
        else
            setLayout(new GridLayout(5, 2, 0, 10));

        // creating objects
        l1 = new Label("   " + type.toUpperCase() + " LOGIN");
        l4 = new Label("");
        if (type.equals("Admin"))
            l2 = new Label("    Username");
        else
            l2 = new Label("    Email");
        l3 = new Label("    Password");
        userTf = new TextField(20);
        passTf = new TextField(20);
        loginBtn = new Button("Login");
        registerBtn = new Button("Register (new user)");
        backBtn = new Button("Back");

        // design
        setBackground(new Color(232, 240, 250));
        setFont(new Font("Arial", Font.PLAIN, 15));
        l1.setBackground(new Color(22, 54, 110));
        l1.setForeground(Color.white);
        l1.setFont(new Font("Arial", Font.BOLD, 22));
        l4.setBackground(new Color(22, 54, 110));
        passTf.setEchoChar('*');

        
        loginBtn.addActionListener(this);
        registerBtn.addActionListener(this);
        backBtn.addActionListener(this);

        // adding components
        add(l1);
        add(l4);
        add(l2);
        add(userTf);
        add(l3);
        add(passTf);
        add(loginBtn);
        add(backBtn);
        if (type.equals("Customer")){
            add(registerBtn); 
        }

        
        addWindowListener(this);

       
        setTitle(type + " Login");
        setSize(450, 280);
        setLocationRelativeTo(null);
        setVisible(true);
    }

   
    public void actionPerformed(ActionEvent e)
    {
        Button btn = (Button) e.getSource();
        String user = userTf.getText().trim();
        String pass = passTf.getText();

        if (btn == backBtn){
            dispose();
        }
        else if (btn == registerBtn){
          RegisterFrame rf =  new RegisterFrame();
        }
        else if (user.isEmpty() || pass.isEmpty()){
          MessageBox mb =  new MessageBox(this, "Please fill both the fields");
        }
        else
        {
            try
            {
                Connection con = DBConnection.getConnection();
                String sql = "SELECT * FROM customers WHERE email = ?";
                if (type.equals("Admin"))
                    sql = "SELECT * FROM admins WHERE username = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, user);
                ResultSet rs = ps.executeQuery();

                // password is checked in java so that it is case sensitive
                if (!rs.next() || !rs.getString("password").equals(pass))
                    new MessageBox(this, "Wrong login details");
                else
                {
                    if (type.equals("Admin"))
                        new AdminDashboard(user);
                    else
                        new CustomerDashboard(rs.getInt("customer_id"), rs.getString("name"));
                    dispose();
                }
                con.close();
            }
            catch (Exception ex)
            {
                System.out.println(ex);
                new MessageBox(this, "Error: " + ex.getMessage());
            }
        }
    }

   
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
