package ninja.disruptor.providers.events;

import lombok.NoArgsConstructor;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor
public class CreateUserEventListenerProvider implements EventListenerProvider {

    public static final String CLIENT_ID = System.getenv("KEYCLOAK_CLIENT");
    public static final String REALM = System.getenv("REALM_NAME");
    public static final String KEYCLOAK_URL = System.getenv("KEYCLOAK_URL");
    public static final String APPLICATION_USER_NAME = System.getenv("APPLICATION_USER_NAME");
    public static final String APPLICATION_PASSWORD = System.getenv("APPLICATION_PASSWORD");
    public static final String APPLICATION_URL = System.getenv("APPLICATION_URL");

    private Client client = ClientBuilder.newClient();
    private WebTarget target = client.target(APPLICATION_URL);

    @Override
    public void onEvent(final Event event) {
        if (event.getType().equals(EventType.REGISTER)) {
            CompletableFuture
                    .runAsync(() -> createUser(event));
        }
    }

    private void createUser(Event event) {
        KeycloakTokenCreator tokenCreator = new KeycloakTokenCreator(CLIENT_ID, REALM, KEYCLOAK_URL);
        String token = tokenCreator
                .getTokenResponse(APPLICATION_USER_NAME, APPLICATION_PASSWORD)
                .getToken();

        if (token == null) {
            throw new IllegalArgumentException("No Token available!");
        }

        User userToCreate = new User();
        userToCreate.setId(event.getUserId());
        userToCreate.setNickname(event.getDetails().get("username"));

        target
                .request()
                .header("Authorization", "Bearer " + token)
                .post(Entity.json(userToCreate));
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    @Override
    public void close() {
    }

}