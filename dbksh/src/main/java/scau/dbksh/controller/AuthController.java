package scau.dbksh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.LoginFormDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {


    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginFormDTO loginFormDTO) {
        return userService.login(loginFormDTO);
    }
}
