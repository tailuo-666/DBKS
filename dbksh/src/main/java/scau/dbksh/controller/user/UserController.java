package scau.dbksh.controller.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.LoginFormDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/code")
    public Result<Void> sendCode(@RequestParam("wechat") String wechat) {
        return userService.sendCode(wechat);
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginFormDTO loginFormDTO) {
        return userService.login(loginFormDTO);
    }

    @GetMapping("/me")
    public Result<UserDTO> me() {
        return userService.me();
    }
}
