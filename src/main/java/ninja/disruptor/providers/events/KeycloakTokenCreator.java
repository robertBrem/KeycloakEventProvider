package ninja.disruptor.providers.events;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class KeycloakTokenCreator {

    private String clientId;
    private String realmName;
    private String keycloakUrl;

    public KeycloakTokenCreator(String clientId, String realmName, String keycloakUrl) {
        this.clientId = clientId;
        this.realmName = realmName;
        this.keycloakUrl = keycloakUrl;
    }

    public AccessTokenResponse getTokenResponse(String user, String password) {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(keycloakUrl)
                    .path(ServiceUrlConstants.TOKEN_PATH).build(realmName));
            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair("username", user));
            formparams.add(new BasicNameValuePair("password", password));

            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));

            UrlEncodedFormEntity form = null;
            try {
                form = new UrlEncodedFormEntity(formparams, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
            post.setEntity(form);
            HttpResponse response = null;
            try {
                response = client.execute(post);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                throw new IllegalArgumentException("Bad status: " + status);
            }
            if (entity == null) {
                throw new IllegalArgumentException("No Entity");
            }
            InputStream is = null;
            try {
                is = entity.getContent();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            try {
                AccessTokenResponse tokenResponse = JsonSerialization.readValue(is, AccessTokenResponse.class);
                return tokenResponse;
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

}

