package br.edu.uniamerica.parceiro_auto.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SessionConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                boolean publicRequest = "POST".equals(request.getMethod())
                        && ("/api/users".equals(path) || "/api/users/login".equals(path));
                var session = request.getSession(false);
                if (!publicRequest && (session == null || session.getAttribute("userId") == null)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"mensagem\":\"Entre para acessar o sistema\",\"dados\":null}");
                    return false;
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
