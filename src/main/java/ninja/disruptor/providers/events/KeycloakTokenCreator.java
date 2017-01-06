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
import java.util.ArrayList;
import java.util.List;

public class KeycloakTokenCreator {

    public static final String CLIENT_ID = System.getenv("KEYCLOAK_CLIENT");
    public static final String REALM = System.getenv("REALM_NAME");
    public static final String KEYCLOAK_URL = System.getenv("KEYCLOAK_URL");

    public static AccessTokenResponse getTokenResponse(String user, String password) throws IOException {
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(KEYCLOAK_URL)
                    .path(ServiceUrlConstants.TOKEN_PATH).build(REALM));
            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair("username", user));
            formparams.add(new BasicNameValuePair("password", password));

            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, CLIENT_ID));

            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);
            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                throw new IOException("Bad status: " + status);
            }
            if (entity == null) {
                throw new IOException("No Entity");
            }
            InputStream is = entity.getContent();
            try {
                AccessTokenResponse tokenResponse = JsonSerialization.readValue(is, AccessTokenResponse.class);
                return tokenResponse;
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

