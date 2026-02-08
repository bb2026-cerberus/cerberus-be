//package kr.co.cerberus.global.config;
//
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.servers.Server;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.List;
//
//@Configuration
//public class OpenApiConfig {
//
//    @Bean
//    public OpenAPI customOpenAPI() {
//        Server server = new Server();
//        server.setUrl("https://api.seolberus.co.kr");
//        server.setDescription("Production Server (HTTPS)");
//
//        return new OpenAPI().servers(List.of(server));
//    }
//}