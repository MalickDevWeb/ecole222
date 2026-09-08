package ecole_221.sn.ecole.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class RootController {
    private static final Logger logger = LoggerFactory.getLogger(RootController.class);

    @GetMapping
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(302).header("Location", "/swagger-ui/index.html").build();
    }
}
