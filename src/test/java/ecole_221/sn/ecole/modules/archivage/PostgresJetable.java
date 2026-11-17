package ecole_221.sn.ecole.modules.archivage;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.net.SocketFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Base PostgreSQL JETABLE pour les tests d'archivage qui exigent le vrai moteur (H2 ne connaît ni jsonb, ni les
 * intervalles, ni les tables temporaires de la restauration). Conteneur SANS RÉSEAU, joint par son socket Unix :
 *
 * <pre>
 * docker run --rm -d --name archivage-pg --network none -e POSTGRES_HOST_AUTH_METHOD=trust \
 *     -v &lt;dossier&gt;:/var/run/postgresql postgres:15-alpine
 * mvn test -Darchivage.pg.socket=&lt;dossier&gt;/.s.PGSQL.5432     (ou variable ARCHIVAGE_PG_SOCKET)
 * </pre>
 * Sans socket indiqué, ces tests sont ignorés (la suite ordinaire reste sur H2).
 */
public final class PostgresJetable {

    private PostgresJetable() {
    }

    public static String socket() {
        String s = System.getProperty("archivage.pg.socket", System.getenv("ARCHIVAGE_PG_SOCKET"));
        return s == null || s.isBlank() || !Files.exists(Path.of(s)) ? null : s;
    }

    public static boolean disponible() {
        return socket() != null;
    }

    private static Properties proprietes() {
        Properties p = new Properties();
        p.setProperty("user", "postgres");
        p.setProperty("sslmode", "disable");
        p.setProperty("socketFactory", SocketUnix.Fabrique.class.getName());
        p.setProperty("socketFactoryArg", socket());
        return p;
    }

    public static String url(String base) {
        return "jdbc:postgresql://localhost/" + base + "?sslmode=disable&socketFactory=" + SocketUnix.Fabrique.class.getName()
                + "&socketFactoryArg=" + socket();
    }

    public static DataSource source(String base) {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:postgresql://localhost/" + base);
        ds.setConnectionProperties(proprietes());
        return ds;
    }

    /** Crée une base vide (la supprime d'abord si elle existe). */
    public static DataSource nouvelleBase(String base) throws SQLException {
        try (Connection c = source("postgres").getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + base + " WITH (FORCE)");
            s.execute("CREATE DATABASE " + base);
        }
        return source(base);
    }

    public static void supprimerBase(String base) {
        try (Connection c = source("postgres").getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + base + " WITH (FORCE)");
        } catch (SQLException e) {
            // conteneur jetable : il sera supprimé avec ses bases
        }
    }

    /** Socket Unix présenté au pilote PostgreSQL comme une socket TCP (le pilote ne connaît que java.net.Socket). */
    public static final class SocketUnix extends Socket {
        private final String chemin;
        private SocketChannel canal;
        private InputStream entree;
        private OutputStream sortie;

        SocketUnix(String chemin) {
            this.chemin = chemin;
        }

        /** Instanciée par le pilote avec {@code socketFactoryArg} (chemin du socket). */
        public static final class Fabrique extends SocketFactory {
            private final String chemin;

            public Fabrique(String chemin) {
                this.chemin = chemin;
            }

            @Override
            public Socket createSocket() {
                return new SocketUnix(chemin);
            }

            @Override
            public Socket createSocket(String h, int p) throws IOException {
                Socket s = createSocket();
                s.connect(null);
                return s;
            }

            @Override
            public Socket createSocket(String h, int p, InetAddress l, int lp) throws IOException {
                return createSocket(h, p);
            }

            @Override
            public Socket createSocket(InetAddress h, int p) throws IOException {
                return createSocket((String) null, p);
            }

            @Override
            public Socket createSocket(InetAddress h, int p, InetAddress l, int lp) throws IOException {
                return createSocket((String) null, p);
            }
        }

        @Override
        public void connect(SocketAddress adresse) throws IOException {
            connect(adresse, 0);
        }

        @Override
        public void connect(SocketAddress adresse, int delai) throws IOException {
            canal = SocketChannel.open(StandardProtocolFamily.UNIX);
            canal.connect(UnixDomainSocketAddress.of(chemin));
            entree = Channels.newInputStream(canal);
            sortie = Channels.newOutputStream(canal);
        }

        @Override
        public InputStream getInputStream() {
            return entree;
        }

        @Override
        public OutputStream getOutputStream() {
            return sortie;
        }

        @Override
        public boolean isConnected() {
            return canal != null && canal.isConnected();
        }

        @Override
        public boolean isClosed() {
            return canal != null && !canal.isOpen();
        }

        @Override
        public synchronized void close() throws IOException {
            if (canal != null) canal.close();
        }

        @Override
        public void shutdownInput() throws IOException {
            canal.shutdownInput();
        }

        @Override
        public void shutdownOutput() throws IOException {
            canal.shutdownOutput();
        }

        @Override
        public void setTcpNoDelay(boolean on) {
        }

        @Override
        public void setKeepAlive(boolean on) {
        }

        @Override
        public synchronized void setSoTimeout(int timeout) {
        }

        @Override
        public synchronized int getSoTimeout() {
            return 0;
        }

        @Override
        public synchronized void setReceiveBufferSize(int size) {
        }

        @Override
        public synchronized void setSendBufferSize(int size) {
        }

        @Override
        public InetAddress getInetAddress() {
            return InetAddress.getLoopbackAddress();
        }
    }
}
