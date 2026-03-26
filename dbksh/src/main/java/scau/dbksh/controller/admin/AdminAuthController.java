package scau.dbksh.controller.admin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.LoginFormDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.UserService;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    private final UserService userService;

    public AdminAuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginFormDTO loginFormDTO) {
        return userService.adminLogin(loginFormDTO);
    }
}
