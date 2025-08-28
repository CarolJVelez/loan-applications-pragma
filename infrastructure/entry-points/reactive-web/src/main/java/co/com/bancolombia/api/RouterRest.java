package co.com.bancolombia.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    public RouterFunction<ServerResponse> routerFunction(HandlerLoanApplication handlerLoanApplication) {
        return route()
                .build();
    }
}
