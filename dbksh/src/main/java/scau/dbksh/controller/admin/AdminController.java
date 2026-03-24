package scau.dbksh.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.UserService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    @GetMapping("/me")
    public Result<UserDTO> me() {
        return userService.me();
    }
}
