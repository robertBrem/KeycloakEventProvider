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
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor
public class CreateUserEventListenerProvider implements EventListenerProvider {

    private Client client = ClientBuilder.newClient();
    private WebTarget target = client.target(System.getenv("APPLICATION_URL"));

    @Override
    public void onEvent(final Event event) {
        if (event.getType().equals(EventType.REGISTER)) {
            CompletableFuture
                    .runAsync(() -> createUser(event));
        }
    }

    private void createUser(Event event) {
        String token = null;
        try {
            token = KeycloakTokenCreator
                    .getTokenResponse(
                            System.getenv("APPLICATION_USER_NAME"),
                            System.getenv("APPLICATION_PASSWORD"))
                    .getToken();
        } catch (IOException e) {
            e.printStackTrace();
            return;
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