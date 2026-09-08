package ecole_221.sn.ecole.modules.auth.application.port.command;

public class LoginCommand {
    private String email;
    private String password;

    public LoginCommand() {}
    public LoginCommand(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
