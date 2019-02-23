package authentication;

import clients.symphony.api.APIClient;
import configuration.SymConfig;
import model.SessionToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MethodNotSupportedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

public final class SymOBOUserAuth extends APIClient implements ISymAuth {
    private final Logger logger = LoggerFactory.getLogger(SymOBOUserAuth.class);
    private String sessionToken;
    private SymConfig config;
    private Client sessionAuthClient;
    private Long uid;
    private String username;
    private SymOBOAuth appAuth;

    public SymOBOUserAuth(final SymConfig config,
                          final Client sessionAuthClient,
                          final Long uid, final SymOBOAuth appAuth) {
        this.config = config;
        this.sessionAuthClient = sessionAuthClient;
        this.uid = uid;
        this.appAuth = appAuth;
    }

    public SymOBOUserAuth(final SymConfig config,
                          final Client sessionAuthClient,
                          final String username, final SymOBOAuth appAuth) {
        this.config = config;
        this.sessionAuthClient = sessionAuthClient;
        this.username = username;
        this.appAuth = appAuth;
    }

    @Override
    public void authenticate() {
        sessionAuthenticate();
    }

    @Override
    public void sessionAuthenticate() {
        Response response = null;
        if (uid != null) {
            response = sessionAuthClient.target(
                    AuthEndpointConstants.HTTPSPREFIX
                            + config.getSessionAuthHost()
                            + ":" + config.getSessionAuthPort())
                .path(AuthEndpointConstants.OBOUSERAUTH
                        .replace("{uid}", Long.toString(uid)))
                .request(MediaType.APPLICATION_JSON)
                    .header("sessionToken", appAuth.getSessionToken())
                .post(null);
        } else {
            response = sessionAuthClient.target(
                    AuthEndpointConstants.HTTPSPREFIX
                            + config.getSessionAuthHost()
                            + ":" + config.getSessionAuthPort())
                .path(AuthEndpointConstants.OBOUSERAUTHUSERNAME
                        .replace("{username}", username))
                .request(MediaType.APPLICATION_JSON)
                    .header("sessionToken", appAuth.getSessionToken())
                    .post(null);
        }
        if (response.getStatusInfo().getFamily()
                != Response.Status.Family.SUCCESSFUL) {
            try {
                handleError(response, null);
            } catch (Exception e) {
                logger.error("Unexpected error, "
                        + "retry authentication in 30 seconds");
            }
            try {
                TimeUnit.SECONDS.sleep(AuthEndpointConstants.TIMEOUT);
            } catch (InterruptedException e) {
                logger.error("Error with authentication", e);
            }
            appAuth.sessionAppAuthenticate();
            sessionAuthenticate();
        } else {
            SessionToken session =
                    response.readEntity(SessionToken.class);
            this.sessionToken = session.getSessionToken();
        }
    }

    @Override
    public void kmAuthenticate() {
        logger.warn("method kmAuthenticate is invalid");
        throw new RuntimeException( "this method is not supported");
    }

    @Override
    public String getSessionToken() {
        return sessionToken;
    }

    @Override
    public void setSessionToken(final String sessionToken) {
        this.sessionToken = sessionToken;
    }

    @Override
    public String getKmToken() {
        logger.warn("method kmAuthenticate is invalid");
        throw new RuntimeException( "this method kmAuthenticate is not supported");
    }

    @Override
    public void setKmToken(final String kmToken) {
        logger.warn("method setKmToken is invalid");
        throw new RuntimeException( "this method setKmToken is not supported");
    }

    @Override
    public void logout() {
    }
}
